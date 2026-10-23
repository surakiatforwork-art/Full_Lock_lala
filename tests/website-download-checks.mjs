import assert from 'node:assert/strict';
import {readFile,readdir,lstat} from 'node:fs/promises';
import {createHash} from 'node:crypto';
const info=JSON.parse(await readFile('web/downloads/version.json','utf8'));
const apk=await readFile('dist/web/downloads/FullLock.apk');
assert.equal(createHash('sha256').update(apk).digest('hex'),info.sha256);
assert.equal(apk.length,info.bytes);assert.equal(apk.subarray(0,2).toString(),'PK');
assert.equal((await readFile('dist/web/downloads/SHA256SUMS.txt','utf8')).trim(),`${info.sha256}  FullLock.apk`);
const html=await readFile('dist/index.html','utf8');
assert.equal([...html.matchAll(/href="web\/downloads\/FullLock.apk" download="FullLock.apk"/g)].length,4);
assert.ok(!html.includes('github.com/surakiatforwork-art'));
assert.deepEqual((await readdir('dist')).sort(),['.nojekyll','index.html','tools','web']);
assert.deepEqual(await readdir('dist/tools'),['flp-web-codec.mjs']);
async function scan(folder){for(const name of await readdir(folder)){const path=folder+'/'+name,stat=await lstat(path);assert.ok(!stat.isSymbolicLink());assert.ok(!/\.(java|gradle|jks|keystore|p12|properties)$/i.test(name));if(stat.isDirectory())await scan(path);}}
await scan('dist');
console.log(`Website download checks passed: ${info.version}, verified APK, 4 direct links, public-only package`);
