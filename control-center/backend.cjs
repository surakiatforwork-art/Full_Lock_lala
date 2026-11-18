const { cert, initializeApp, getApps } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore } = require('firebase-admin/firestore');
const { createHmac, timingSafeEqual, randomBytes } = require('node:crypto');

const admins = new Set([
  'phantom.master1245@gmail.com',
  'surakiatforwork@gmail.com',
  'surakiat16082000@gmail.com'
]);
const deviceId = value => typeof value === 'string' && /^[a-f0-9]{64}$/.test(value);
const clean = (value, max) => typeof value === 'string' ? value.trim().slice(0, max) : '';
class RequestError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}
function database() {
  if (!getApps().length) {
    initializeApp({ credential: cert(JSON.parse(process.env.FULL_LOCK_FIREBASE_SERVICE_ACCOUNT)) });
  }
  return getFirestore();
}
function endpoint(handler) {
  return async (req, res) => {
    res.setHeader('Cache-Control', 'no-store');
    if (req.method !== 'POST') {
      res.setHeader('Allow', 'POST');
      return res.status(405).json({ ok: false });
    }
    try {
      if (process.env.FULL_LOCK_DATA_BACKEND === 'migration') {
        res.setHeader('Retry-After', '300');
        throw new RequestError(503, 'กำลังย้ายระบบ กรุณาลองใหม่ภายหลัง');
      }
      const body = typeof req.body === 'string' ? JSON.parse(req.body) : req.body;
      if (!body || typeof body !== 'object' || Array.isArray(body) || JSON.stringify(body).length > 4096) {
        throw new RequestError(400, 'คำขอไม่ถูกต้อง');
      }
      return res.status(200).json({ ok: true, ...await handler(req, body) });
    } catch (error) {
      return res.status(error instanceof RequestError ? error.status : error instanceof SyntaxError ? 400 : 503)
        .json({ ok: false, error: error instanceof RequestError ? error.message : 'ดำเนินการไม่สำเร็จ' });
    }
  };
}
function deviceToken(id) {
  const secret = process.env.FULL_LOCK_DEVICE_SECRET;
  if (!secret || secret.length < 32) throw new Error('Device credentials unavailable');
  return createHmac('sha256', secret).update('device:' + id).digest('hex');
}
const bearer = req => /^Bearer ([^\s]+)$/.exec(req.headers.authorization || '')?.[1] || '';

async function sheets(action, payload = {}, actor = '') {
  const url = process.env.FULL_LOCK_SHEETS_URL;
  const secret = process.env.FULL_LOCK_SHEETS_SECRET;
  if (!secret || secret.length < 32 || !/^https:\/\/script\.google\.com\/macros\/s\/[a-zA-Z0-9_-]+\/exec$/.test(url || '')) {
    throw new Error('Sheets bridge is not configured');
  }
  const body = JSON.stringify({ action, payload, actor });
  const timestamp = Date.now();
  const nonce = randomBytes(16).toString('hex');
  const signature = createHmac('sha256', secret).update(timestamp + '.' + nonce + '.' + body).digest('hex');
  const response = await fetch(url, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ timestamp, nonce, body, signature }), signal: AbortSignal.timeout(18000)
  });
  const result = await response.json();
  if (!response.ok || result?.ok !== true) throw new Error('Sheets unavailable');
  if (Number.isInteger(result.status) && result.status >= 400) {
    throw new RequestError(result.status, result.error || 'ดำเนินการไม่สำเร็จ');
  }
  return result;
}
const usesSheets = () => process.env.FULL_LOCK_DATA_BACKEND === 'sheets';

const access = endpoint(async (req, body) => {
  if (!deviceId(body.deviceId) || !['register', 'checkin'].includes(body.type)) {
    throw new RequestError(400, 'คำขอไม่ถูกต้อง');
  }
  const token = deviceToken(body.deviceId);
  if (body.type === 'checkin') {
    const supplied = bearer(req);
    if (!deviceId(supplied) || !timingSafeEqual(Buffer.from(token), Buffer.from(supplied))) {
      throw new RequestError(401, 'ตรวจสิทธิ์ไม่สำเร็จ');
    }
  }
  if (usesSheets()) {
    const result = await sheets('access', {
      type: body.type, deviceId: body.deviceId, model: clean(body.model, 100),
      version: clean(body.version, 30), mode: body.mode
    });
    if (typeof result.enabled !== 'boolean' || typeof result.allowed !== 'boolean') throw Error('Invalid decision');
    return body.type === 'register' ? { enabled: result.enabled, allowed: result.allowed, token }
      : { enabled: result.enabled, allowed: result.allowed };
  }
  const db = database();
  const device = db.collection('devices').doc(body.deviceId);
  const now = Date.now();
  const mode = ['normal', 'root_connected', 'root_waiting'].includes(body.mode) ? body.mode : null;
  const version = clean(body.version, 30);
  const decision = await db.runTransaction(async tx => {
    const [policy, current] = await Promise.all([tx.get(db.doc('control/settings')), tx.get(device)]);
    if (body.type === 'checkin' && !current.exists) throw new RequestError(401, 'กรุณาลงทะเบียนใหม่');
    const enabled = policy.get('enabled') !== false;
    const allowed = enabled && current.get('enabled') !== false;
    if (!current.exists) {
      const ip = clean(req.headers['x-vercel-forwarded-for'], 80) || 'unknown';
      const key = createHmac('sha256', process.env.FULL_LOCK_DEVICE_SECRET).update('registration:' + ip).digest('hex');
      const limit = db.collection('registrationLimits').doc(key);
      const prior = await tx.get(limit);
      const hour = Math.floor(now / 3600000);
      const count = prior.get('hour') === hour ? prior.get('count') : 0;
      if (count >= 30) throw new RequestError(429, 'กรุณาลองใหม่ภายหลัง');
      tx.set(limit, { hour, count: count + 1, expiresAt: new Date(now + 7200000) });
      tx.create(device, { enabled: true, firstSeen: now, lastSeen: now, model: clean(body.model, 100), version, mode });
    } else if (now - (current.get('lastSeen') || 0) >= 600000 || current.get('version') !== version
        || (current.get('mode') ?? null) !== mode) {
      tx.update(device, { lastSeen: now, model: clean(body.model, 100), version, mode });
    }
    return { enabled, allowed };
  });
  return body.type === 'register' ? { ...decision, token } : decision;
});

const admin = endpoint(async (req, body) => {
  const token = bearer(req);
  if (!token || token.length > 4096) throw new RequestError(401, 'กรุณาเข้าสู่ระบบ');
  const db = database();
  let identity;
  try { identity = await getAuth().verifyIdToken(token, true); }
  catch { throw new RequestError(401, 'กรุณาเข้าสู่ระบบใหม่'); }
  if (identity.email_verified !== true || identity.firebase?.sign_in_provider !== 'google.com' || !admins.has(identity.email?.toLowerCase())) {
    throw new RequestError(403, 'บัญชีนี้ไม่มีสิทธิ์ผู้ดูแล');
  }
  const { action, payload = {} } = body;
  if (usesSheets()) {
    if (!['getState', 'setGlobalEnabled', 'setDeviceEnabled', 'upsertDevice'].includes(action)) {
      throw new RequestError(400, 'คำขอไม่ถูกต้อง');
    }
    const result = await sheets(action, payload, identity.email.toLowerCase());
    if (!result.state) throw Error('Invalid admin response');
    return { state: result.state };
  }
  const config = db.doc('control/settings');
  const logs = db.collection('audit');
  if (action !== 'getState') {
    if (!['setGlobalEnabled', 'setDeviceEnabled', 'upsertDevice'].includes(action) ||
        !payload || typeof payload.enabled !== 'boolean' ||
        (action !== 'setGlobalEnabled' && !deviceId(payload.deviceId))) {
      throw new RequestError(400, 'คำขอไม่ถูกต้อง');
    }
    const target = action === 'setGlobalEnabled' ? 'ALL' : payload.deviceId;
    const batch = db.batch();
    if (target === 'ALL') batch.set(config, { enabled: payload.enabled }, { merge: true });
    else {
      const fields = { enabled: payload.enabled };
      if (action === 'upsertDevice') Object.assign(fields, { label: clean(payload.label, 80), notes: clean(payload.notes, 500) });
      batch.update(db.collection('devices').doc(target), fields);
    }
    batch.create(logs.doc(), { time: Date.now(), actor: identity.email, action, target, enabled: payload.enabled });
    await batch.commit();
  }
  const devices = db.collection('devices');
  const [configSnapshot, total, active, blocked, history, rows] = await Promise.all([
    config.get(), devices.count().get(),
    devices.where('lastSeen', '>=', Date.now() - 86400000).count().get(),
    devices.where('enabled', '==', false).count().get(),
    logs.orderBy('time', 'desc').limit(30).get(),
    deviceId(payload?.search) ? devices.doc(payload.search).get() : devices.orderBy('lastSeen', 'desc').limit(200).get()
  ]);
  const entries = rows.docs || (rows.exists ? [rows] : []);
  return { state: {
    enabled: configSnapshot.get('enabled') !== false,
    counts: { total: total.data().count, active: active.data().count, blocked: blocked.data().count },
    devices: entries.map(row => ({ ...row.data(), deviceId: row.id })),
    audit: history.docs.map(row => row.data())
  }};
});
async function installCount(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).end();
  }
  if (process.env.FULL_LOCK_DATA_BACKEND === 'migration') {
    res.setHeader('Cache-Control', 'no-store');
    res.setHeader('Retry-After', '300');
    return res.status(503).json({ error: 'Migration in progress' });
  }
  try {
    const registeredDevices = usesSheets()
      ? (await sheets('count')).registeredDevices
      : (await database().collection('devices').count().get()).data().count;
    if (!Number.isSafeInteger(registeredDevices) || registeredDevices < 0) throw Error('Invalid count');
    res.setHeader('Cache-Control', 'public, max-age=60');
    res.setHeader('Vercel-CDN-Cache-Control', 'public, s-maxage=900, stale-while-revalidate=86400');
    return res.json({ registeredDevices });
  } catch {
    res.setHeader('Cache-Control', 'no-store');
    return res.status(503).json({ error: 'Count unavailable' });
  }
}
module.exports = { admin, access, installCount };
