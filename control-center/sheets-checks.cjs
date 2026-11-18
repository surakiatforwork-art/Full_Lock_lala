const vm = require('node:vm');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const tables = new Map(), properties = new Map(), cache = new Map();
let exported;
function sheet(name) {
  const rows = [];
  const api = {
    getLastRow: () => rows.length,
    setFrozenRows() {},
    appendRow(row) { rows.push([...row]); },
    getDataRange: () => ({ getValues: () => rows.map(row => [...row]) }),
    getRange(r, c, height = 1, width = 1) {
      return {
        getValues: () => Array.from({ length: height }, (_, i) =>
          Array.from({ length: width }, (_, j) => rows[r - 1 + i]?.[c - 1 + j] ?? '')),
        setValues(values) { values.forEach((row, i) => {
          const target = rows[r - 1 + i] ||= [];
          row.forEach((value, j) => target[c - 1 + j] = value);
        }); },
        setValue(value) { (rows[r - 1] ||= [])[c - 1] = value; }
      };
    }
  };
  tables.set(name, api);
  return api;
}
const book = { getSheetByName: name => tables.get(name), insertSheet: sheet };
const runtime = vm.createContext({
  console: { log() {} }, Date,
  SpreadsheetApp: { openById: () => book, flush() {} },
  LockService: { getScriptLock: () => ({ tryLock: () => true, releaseLock() {} }) },
  CacheService: { getScriptCache: () => ({
    get: key => cache.get(key) ?? null, put: (key, value) => cache.set(key, value), remove: key => cache.delete(key)
  }) },
  PropertiesService: { getScriptProperties: () => ({
    getProperty: key => properties.get(key), setProperty: (key, value) => properties.set(key, value)
  }) },
  Utilities: {
    getUuid: () => crypto.randomUUID(),
    computeHmacSha256Signature: (message, secret) => [...crypto.createHmac('sha256', secret).update(message).digest()]
  },
  ContentService: { MimeType: { JSON: 'json' }, createTextOutput: text => ({ setMimeType: () => JSON.parse(text) }) },
  DriveApp: { getFileById: () => ({ getBlob: () => ({ getDataAsString: () => JSON.stringify(exported) }) }) }
});
vm.runInContext(fs.readFileSync('control-center/apps-script/Code.gs', 'utf8'), runtime);
runtime.setupDatabase();
properties.set('FULL_LOCK_IMPORT_FILE_ID', 'fixture');
const id = 'a'.repeat(64);
exported = { schema: 1, enabled: true, devices: [
  { deviceId: id, enabled: false, label: 'Existing blocked device', firstSeen: 123, lastSeen: 456 }
], audit: [] };
runtime.importLegacyDatabase();
runtime.importLegacyDatabase();
assert.equal(tables.get('Devices').getLastRow(), 2, 'idempotent migration');
function request(action, payload = {}, actor = '') {
  const body = JSON.stringify({ action, payload, actor });
  const timestamp = Date.now(), nonce = crypto.randomBytes(16).toString('hex');
  const signature = crypto.createHmac('sha256', properties.get('FULL_LOCK_BRIDGE_SECRET'))
    .update(timestamp + '.' + nonce + '.' + body).digest('hex');
  return runtime.doPost({ postData: { contents: JSON.stringify({ body, timestamp, nonce, signature }) } });
}
assert.equal(request('access', { type: 'checkin', deviceId: id }).allowed, false);
const admin = 'surakiatforwork@gmail.com';
assert.equal(request('setDeviceEnabled', { deviceId: id, enabled: true }, admin).ok, true);
assert.equal(request('access', { type: 'checkin', deviceId: id }).allowed, true, 'unblock invalidates cache');
request('setGlobalEnabled', { enabled: false }, admin);
assert.equal(request('access', { type: 'checkin', deviceId: id }).allowed, false, 'global block invalidates cache');
assert.equal(request('setDeviceEnabled', { deviceId: id, enabled: false }, 'outsider@example.com').status, 403);
assert.equal(request('access', { type: 'checkin', deviceId: 'b'.repeat(64) }).status, 401);
assert.equal(request('access', { type: 'register', deviceId: 'b'.repeat(64) }).enabled, false);
assert.equal(request('count').registeredDevices, 2);
assert.equal(runtime.doPost({ postData: { contents: '{}' } }).status, 401);
const state = request('getState', {}, admin).state;
assert.equal(state.counts.total, 2);
assert.equal(state.devices.find(d => d.deviceId === id).firstSeen, 123);
console.log('Sheets checks passed: migration, permissions, cache invalidation, auth, count, legacy identity.');
