/** Full Lock control backend. Deploy as owner, access Anyone. Only signed Vercel requests are accepted. */
const SPREADSHEET_ID = '1xKMRCm3z0M8dYa_TgNKlMFmddfH9aTsxZrozaYLmwOI';
const DEVICE_HEADERS = ['deviceId', 'label', 'enabled', 'notes', 'model', 'version',
  'mode', 'firstSeen', 'lastSeen', 'updatedAt', 'updatedBy'];
const ADMIN_EMAILS = ['phantom.master1245@gmail.com', 'surakiatforwork@gmail.com', 'surakiat16082000@gmail.com'];
const MODES = ['normal', 'root_connected', 'root_waiting'];

function book_() { return SpreadsheetApp.openById(SPREADSHEET_ID); }
function json_(data) { return ContentService.createTextOutput(JSON.stringify(data)).setMimeType(ContentService.MimeType.JSON); }
function text_(value, limit) { return typeof value === 'string' ? value.trim().slice(0, limit) : ''; }
function id_(value) {
  if (typeof value !== 'string' || !/^[a-f0-9]{64}$/.test(value)) throw Error('Invalid device ID');
  return value;
}
function sheetText_(value) {
  return /^[=+@-]/.test(value) ? "'" + value : value;
}
function lock_(run) {
  const lock = LockService.getScriptLock();
  if (!lock.tryLock(4000)) throw Error('Busy; retry later');
  try { return run(); } finally { lock.releaseLock(); }
}
function table_(name, headers) {
  const sheet = book_().getSheetByName(name);
  if (!sheet) throw Error('Run setupDatabase first');
  if (sheet.getRange(1, 1, 1, headers.length).getValues()[0].some((v, i) => v !== headers[i])) {
    throw Error('Unexpected headers in ' + name);
  }
  return sheet;
}
function setupDatabase() {
  lock_(() => {
    const book = book_();
    for (const [name, headers] of [
      ['Devices', DEVICE_HEADERS], ['Settings', ['key', 'value']],
      ['AuditLogs', ['time', 'actor', 'action', 'target', 'enabled']]
    ]) {
      let sheet = book.getSheetByName(name);
      if (!sheet) sheet = book.insertSheet(name);
      if (sheet.getLastRow() && sheet.getRange(1, 1, 1, headers.length).getValues()[0].some((v, i) => v !== headers[i])) {
        throw Error('Existing data has different headers: ' + name);
      }
      sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
      sheet.setFrozenRows(1);
    }
    const settings = table_('Settings', ['key', 'value']);
    if (settings.getLastRow() === 1) settings.appendRow(['enabled', 'true']);
    const props = PropertiesService.getScriptProperties();
    if (!props.getProperty('FULL_LOCK_BRIDGE_SECRET')) {
      props.setProperty('FULL_LOCK_BRIDGE_SECRET', Utilities.getUuid() + Utilities.getUuid());
    }
    SpreadsheetApp.flush();
  });
  console.log('Ready. Copy FULL_LOCK_BRIDGE_SECRET from Project settings > Script properties into Vercel.');
}
function doGet() { return json_({ ok: true, service: 'Full Lock Sheets', apiVersion: 3 }); }

function doPost(event) {
  try {
    const raw = event && event.postData && event.postData.contents;
    if (!raw || raw.length > 16000) throw Error('Invalid request');
    const envelope = JSON.parse(raw);
    const secret = PropertiesService.getScriptProperties().getProperty('FULL_LOCK_BRIDGE_SECRET');
    if (!secret || !Number.isSafeInteger(envelope.timestamp)
        || Math.abs(Date.now() - envelope.timestamp) > 60000
        || !/^[a-f0-9]{32}$/.test(envelope.nonce) || typeof envelope.body !== 'string') {
      return json_({ ok: false, status: 401, error: 'Unauthorized bridge' });
    }
    const signed = envelope.timestamp + '.' + envelope.nonce + '.' + envelope.body;
    const signature = Utilities.computeHmacSha256Signature(signed, secret)
      .map(b => ('0' + ((b + 256) % 256).toString(16)).slice(-2)).join('');
    let difference = signature.length ^ String(envelope.signature).length;
    for (let i = 0; i < signature.length; i++) difference |= signature.charCodeAt(i) ^ String(envelope.signature).charCodeAt(i);
    if (difference) return json_({ ok: false, status: 401, error: 'Unauthorized bridge' });
    const request = JSON.parse(envelope.body);
    const result = lock_(() => {
      const cache = CacheService.getScriptCache();
      const replayKey = 'request:' + envelope.nonce;
      const prior = cache.get(replayKey);
      if (prior) return JSON.parse(prior);
      const result = handle_(request);
      SpreadsheetApp.flush();
      const encoded = JSON.stringify(result);
      if (encoded.length < 80000) cache.put(replayKey, encoded, 90);
      return result;
    });
    return json_({ ok: true, ...result });
  } catch (error) {
    // Infrastructure failures must never be interpreted as a device suspension.
    return json_({ ok: false, status: 503, error: 'Sheets backend unavailable' });
  }
}
function records_() {
  const sheet = table_('Devices', DEVICE_HEADERS);
  const size = sheet.getLastRow() - 1;
  const rows = size ? sheet.getRange(2, 1, size, DEVICE_HEADERS.length).getValues() : [];
  const devices = rows.map((row, index) => {
    const result = { row: index + 2 };
    DEVICE_HEADERS.forEach((key, i) => result[key] = row[i]);
    result.enabled = row[2] === true || row[2] === 'true';
    for (const key of ['firstSeen', 'lastSeen', 'updatedAt']) result[key] = Number(result[key]) || 0;
    result.mode = MODES.includes(result.mode) ? result.mode : null;
    return result;
  }).filter(d => /^[a-f0-9]{64}$/.test(String(d.deviceId)));
  if (new Set(devices.map(d => d.deviceId)).size !== devices.length) throw Error('Duplicate IDs');
  return { sheet, devices };
}
function global_() {
  const cache = CacheService.getScriptCache();
  const cached = cache.get('globalEnabled');
  if (cached === 'true' || cached === 'false') return cached === 'true';
  const rows = table_('Settings', ['key', 'value']).getDataRange().getValues().slice(1);
  const row = rows.find(r => r[0] === 'enabled');
  if (!row || ![true, false, 'true', 'false'].includes(row[1])) throw Error('Invalid global settings');
  const enabled = row[1] === true || row[1] === 'true';
  cache.put('globalEnabled', String(enabled), 300);
  return enabled;
}
function writeDevice_(sheet, device) {
  const values = DEVICE_HEADERS.map(key => {
    const value = device[key] == null ? '' : device[key];
    return typeof value === 'string' ? sheetText_(value) : value;
  });
  sheet.getRange(device.row, 1, 1, values.length).setValues([values]);
}
function handle_(request) {
  const action = request.action, payload = request.payload || {};
  const cache = CacheService.getScriptCache();
  if (action === 'access') {
    id_(payload.deviceId);
    if (!['register', 'checkin'].includes(payload.type)) throw Error('Invalid request');
    const cached = cache.get('device:' + payload.deviceId);
    if (cached) {
      const device = JSON.parse(cached);
      if (device.version === text_(payload.version, 30)
          && device.model === text_(payload.model, 100)
          && device.mode === (MODES.includes(payload.mode) ? payload.mode : null)) {
        const enabled = global_();
        return { enabled, allowed: enabled && device.enabled };
      }
    }
  }
  const { sheet, devices } = records_();
  let enabled = global_();
  const now = Date.now();
  if (action === 'access') {
    const id = id_(payload.deviceId);
    if (!['register', 'checkin'].includes(payload.type)) throw Error('Invalid request');
    let device = devices.find(d => d.deviceId === id);
    if (!device && payload.type === 'checkin') return { status: 401, error: 'Register again' };
    const mode = MODES.includes(payload.mode) ? payload.mode : null;
    const version = text_(payload.version, 30);
    if (!device) {
      device = { deviceId: id, label: '', notes: '', enabled: true, firstSeen: now,
        row: sheet.getLastRow() + 1, updatedAt: now, updatedBy: 'APK' };
      devices.push(device);
    }
    cache.put('device:' + id, JSON.stringify({
      enabled: device.enabled, version, model: text_(payload.model, 100), mode
    }), 300);
    if (!device.lastSeen || now - device.lastSeen >= 900000 || device.version !== version || device.mode !== mode) {
      Object.assign(device, { model: text_(payload.model, 100), version, mode, lastSeen: now });
      writeDevice_(sheet, device);
    }
    return { enabled, allowed: enabled && device.enabled };
  }
  if (action === 'count') return { registeredDevices: devices.length };
  if (!ADMIN_EMAILS.includes(request.actor)) return { status: 403, error: 'Admin required' };
  const log = table_('AuditLogs', ['time', 'actor', 'action', 'target', 'enabled']);
  if (action === 'setGlobalEnabled') {
    if (typeof payload.enabled !== 'boolean') throw Error('Invalid status');
    const settings = table_('Settings', ['key', 'value']);
    const row = settings.getDataRange().getValues().findIndex(r => r[0] === 'enabled') + 1;
    settings.getRange(row, 2).setValue(String(payload.enabled));
    enabled = payload.enabled;
    cache.put('globalEnabled', String(enabled), 300);
    log.appendRow([now, request.actor, action, 'ALL', enabled]);
  } else if (['setDeviceEnabled', 'upsertDevice'].includes(action)) {
    const device = devices.find(d => d.deviceId === id_(payload.deviceId));
    if (!device) return { status: 404, error: 'Device not found' };
    if (typeof payload.enabled !== 'boolean') throw Error('Invalid status');
    Object.assign(device, { enabled: payload.enabled, updatedAt: now, updatedBy: request.actor });
    if (action === 'upsertDevice') Object.assign(device, {
      label: text_(payload.label, 80), notes: text_(payload.notes, 500)
    });
    writeDevice_(sheet, device);
    cache.remove('device:' + device.deviceId);
    log.appendRow([now, request.actor, action, device.deviceId, device.enabled]);
  } else if (action !== 'getState') throw Error('Unknown action');
  const size = Math.min(30, log.getLastRow() - 1);
  const audit = size ? log.getRange(log.getLastRow() - size + 1, 1, size, 5).getValues()
    .reverse().map(r => ({ time: Number(r[0]), actor: r[1], action: r[2], target: r[3], enabled: r[4] === true || r[4] === 'true' })) : [];
  const search = text_(payload.search, 64);
  const selected = /^[a-f0-9]{64}$/.test(search) ? devices.filter(d => d.deviceId === search)
    : devices.slice().sort((a, b) => b.lastSeen - a.lastSeen).slice(0, 200);
  return { state: {
    enabled,
    counts: { total: devices.length, active: devices.filter(d => d.lastSeen >= now - 86400000).length,
      blocked: devices.filter(d => !d.enabled).length },
    devices: selected.map(({ row, ...device }) => device), audit
  } };
}

/** Set FULL_LOCK_IMPORT_FILE_ID to the private Drive JSON export. Run before switching Vercel. */
function importLegacyDatabase() {
  const fileId = PropertiesService.getScriptProperties().getProperty('FULL_LOCK_IMPORT_FILE_ID');
  if (!fileId) throw Error('Set FULL_LOCK_IMPORT_FILE_ID');
  const data = JSON.parse(DriveApp.getFileById(fileId).getBlob().getDataAsString('UTF-8'));
  if (data.schema !== 1 || !Array.isArray(data.devices) || typeof data.enabled !== 'boolean') throw Error('Invalid export');
  const ids = data.devices.map(d => id_(d.deviceId));
  if (new Set(ids).size !== ids.length || data.devices.some(d => typeof d.enabled !== 'boolean')) throw Error('Invalid devices');
  lock_(() => {
    const { sheet, devices } = records_();
    const settings = table_('Settings', ['key', 'value']);
    const row = settings.getDataRange().getValues().findIndex(r => r[0] === 'enabled') + 1;
    if (!devices.length) {
      settings.getRange(row, 2).setValue(String(data.enabled));
      CacheService.getScriptCache().remove('globalEnabled');
      SpreadsheetApp.flush();
    }
    const existing = new Set(devices.map(d => d.deviceId));
    let added = 0;
    for (const item of data.devices) {
      if (existing.has(item.deviceId)) continue;
      const device = {
        deviceId: item.deviceId, label: text_(item.label, 80), enabled: item.enabled,
        notes: text_(item.notes, 500), model: text_(item.model, 100), version: text_(item.version, 30),
        mode: MODES.includes(item.mode) ? item.mode : null,
        firstSeen: Number(item.firstSeen) || 0, lastSeen: Number(item.lastSeen) || 0,
        updatedAt: Number(item.updatedAt) || 0, updatedBy: 'Migration', row: sheet.getLastRow() + 1
      };
      writeDevice_(sheet, device);
      existing.add(item.deviceId);
      added++;
    }
    const log = table_('AuditLogs', ['time', 'actor', 'action', 'target', 'enabled']);
    const previous = new Set(log.getDataRange().getValues().slice(1).map(row => JSON.stringify(row)));
    for (const item of data.audit || []) {
      if (!Number.isFinite(item.time) || !ADMIN_EMAILS.includes(item.actor)
          || typeof item.enabled !== 'boolean') throw Error('Invalid audit record');
      const values = [item.time, item.actor, text_(item.action, 40), text_(item.target, 64), item.enabled];
      const key = JSON.stringify(values);
      if (!previous.has(key)) {
        log.appendRow(values);
        previous.add(key);
      }
    }
    SpreadsheetApp.flush();
    CacheService.getScriptCache().remove('globalEnabled');
    for (const id of ids) CacheService.getScriptCache().remove('device:' + id);
    console.log('Imported ' + added + '. Total ' + existing.size + '. Compare permissions before cutover.');
  });
}
