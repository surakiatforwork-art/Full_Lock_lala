const vm = require('node:vm');
const fs = require('node:fs');
const assert = require('node:assert/strict');
let responseBody, upstreamFailure = false;
const secret = 'bridge-test-'.repeat(4);
const runtime = vm.createContext({
  process: { env: { FULL_LOCK_DATA_BACKEND: 'sheets', FULL_LOCK_SHEETS_URL: 'https://script.google.com/macros/s/fixture/exec',
    FULL_LOCK_SHEETS_SECRET: secret, FULL_LOCK_DEVICE_SECRET: 'device-test-'.repeat(4) } },
  require(name) {
    if (name === 'firebase-admin/app') return { getApps: () => [{}] };
    if (name === 'firebase-admin/auth') return { getAuth: () => ({ verifyIdToken: async () => ({
      email: 'surakiatforwork@gmail.com', email_verified: true, firebase: { sign_in_provider: 'google.com' }
    }) }) };
    if (name === 'firebase-admin/firestore') return { getFirestore: () => { throw Error('Firestore accessed'); } };
    return require(name);
  },
  module: { exports: {} }, Buffer, AbortSignal, Date, console,
  fetch: async (url, options) => {
    const envelope = JSON.parse(options.body);
    const crypto = require('node:crypto');
    assert.equal(envelope.signature, crypto.createHmac('sha256', secret)
      .update(envelope.timestamp + '.' + envelope.nonce + '.' + envelope.body).digest('hex'));
    assert.equal(JSON.parse(envelope.body).action, 'access');
    if (upstreamFailure) throw Error('Unavailable');
    return { ok: true, json: async () => ({ ok: true, enabled: true, allowed: true }) };
  }
});
vm.runInContext(fs.readFileSync('control-center/backend.cjs', 'utf8'), runtime);
function response() {
  return { statusCode: 200, setHeader() {}, status(n) { this.statusCode = n; return this; },
    json(data) { responseBody = data; return this; } };
}
(async () => {
  const res = response();
  const body = { type: 'register', deviceId: 'a'.repeat(64), version: 'fixture' };
  await runtime.module.exports.access({ method: 'POST', headers: {}, body }, res);
  assert.equal(res.statusCode, 200);
  assert.equal(responseBody.allowed, true);
  const token = responseBody.token;
  assert.match(token, /^[a-f0-9]{64}$/);
  const check = response();
  await runtime.module.exports.access({ method: 'POST', headers: { authorization: 'Bearer ' + token },
    body: { ...body, type: 'checkin' } }, check);
  assert.equal(check.statusCode, 200, 'legacy token retained');
  upstreamFailure = true;
  const failed = response();
  await runtime.module.exports.access({ method: 'POST', headers: {}, body }, failed);
  assert.equal(failed.statusCode, 503);
  assert.equal(responseBody.allowed, undefined, 'failure must not suspend device');
  console.log('Bridge checks passed: HMAC, legacy tokens, Firestore independence, failure semantics.');
})().catch(error => { console.error(error); process.exitCode = 1; });
