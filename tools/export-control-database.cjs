/** Export privately with the Firebase CLI account. No credentials or device data are printed. */
const fs = require('node:fs');
const path = require('node:path');
async function main() {
  const output = process.argv[2];
  const cli = process.env.FIREBASE_TOOLS_LIB;
  if (!output || !path.isAbsolute(output) || !cli) throw Error('Supply an absolute output path and FIREBASE_TOOLS_LIB');
  const root = path.resolve(__dirname, '..');
  if (path.resolve(output).toLowerCase().startsWith(root.toLowerCase() + path.sep)) throw Error('Export must be outside repository');
  const auth = require(path.join(cli, 'auth'));
  const api = require(path.join(cli, 'apiv2'));
  auth.setActiveAccount({}, auth.getGlobalDefaultAccount());
  const token = await api.getAccessToken();
  const base = 'https://firestore.googleapis.com/v1/projects/full-lock-control/databases/(default)/documents';
  async function get(url, optional = false) {
    const response = await fetch(url, { headers: { Authorization: 'Bearer ' + token }, signal: AbortSignal.timeout(20000) });
    if (optional && response.status === 404) return {};
    if (!response.ok) throw Error('Firestore export HTTP ' + response.status + '. Existing service remains in place.');
    return response.json();
  }
  function fields(document) {
    const data = {};
    for (const [key, value] of Object.entries(document.fields || {})) {
      if ('stringValue' in value) data[key] = value.stringValue;
      else if ('booleanValue' in value) data[key] = value.booleanValue;
      else if ('integerValue' in value) data[key] = Number(value.integerValue);
      else if ('doubleValue' in value) data[key] = value.doubleValue;
      else if ('nullValue' in value) data[key] = null;
    }
    return data;
  }
  const config = fields(await get(base + '/control/settings', true));
  const devices = [];
  let page = '';
  do {
    const result = await get(base + '/devices?pageSize=300' + (page ? '&pageToken=' + encodeURIComponent(page) : ''));
    for (const doc of result.documents || []) {
      const deviceId = doc.name.split('/').pop();
      const data = fields(doc);
      if (!/^[a-f0-9]{64}$/.test(deviceId) || typeof data.enabled !== 'boolean') throw Error('Unexpected device record');
      devices.push({ ...data, deviceId });
    }
    page = result.nextPageToken || '';
  } while (page);
  // A never-configured global policy defaults to enabled in the existing backend.
  const enabled = config.enabled !== false;
  const audit = [];
  page = '';
  do {
    const result = await get(base + '/audit?pageSize=300' + (page ? '&pageToken=' + encodeURIComponent(page) : ''));
    for (const doc of result.documents || []) audit.push(fields(doc));
    page = result.nextPageToken || '';
  } while (page);
  const result = { schema: 1, exportedAt: new Date().toISOString(), enabled, devices, audit };
  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, JSON.stringify(result, null, 2), { flag: 'wx', mode: 0o600 });
  console.log('Private export complete. Devices: ' + devices.length);
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
