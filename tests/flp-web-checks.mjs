import assert from 'node:assert/strict';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { inflateSync } from 'node:zlib';
import { fields, encodeProfile, crc32 } from '../tools/flp-web-codec.mjs';
let checks = 0;
const check = (ok) => { assert.ok(ok); checks++; };
const bytes = await readFile('app/build/profile-fixtures/java.flp');
check(bytes.subarray(0, 8).toString() === 'FLP-PHTM');
const raw = inflateSync(bytes.subarray(21)); check(crc32(raw) === bytes.readUInt32BE(17));
let pos = 2; const decoded = {};
for (let i = 0; i < raw.readUInt16BE(0); i++) {
  const id = raw[pos], type = raw[pos + 1], len = raw.readUInt32BE(pos + 2); pos += 6;
  const value = type === 1 ? raw.subarray(pos, pos + len).toString('utf8') : type === 2 ? raw[pos] === 1 : raw.readInt32BE(pos);
  decoded[id === 1 ? 'name' : fields[id - 10][0]] = value; pos += len;
}
check(decoded.name === 'กรุงเทพ / กลางคืน');check(decoded.origins === 'บางพูด,คลองสาม');check(decoded.themeColor === (0xff2563eb | 0));check(decoded.invertText === true);check(decoded.serviceMask === 2);check(bytes[8] === 2);
const web = await encodeProfile('จากเว็บไซต์', { origins: 'บางพูด,คลองสาม', excludedDestinations: 'คลองสามประเวศ', serviceMask: 2, type: 2 });
await mkdir('app/build/profile-fixtures', { recursive: true });await writeFile('app/build/profile-fixtures/web.flp', web);
check(web.length < 1024);check(await encodeProfile('ทดสอบ') instanceof Uint8Array);
await assert.rejects(() => encodeProfile('bad', { enabled: true }));checks++;
await assert.rejects(() => encodeProfile('bad', { min: '1000', max: '100' }));checks++;
for(const serviceMask of [0,3,4,-1,1.5]){await assert.rejects(()=>encodeProfile('bad',{serviceMask}));checks++;}
console.log(`FlpWebChecks: ${checks} checks passed; web sample ${web.length} bytes`);
