import assert from 'node:assert/strict';
import {readFile,readdir,mkdir,writeFile} from 'node:fs/promises';
import {inflateSync} from 'node:zlib';
import {scopeAreas,collisionNames,routeSettings,zones} from '../web/profile-model.mjs';
import {encodeProfile,crc32} from '../tools/flp-web-codec.mjs';
const catalog=JSON.parse(await readFile('web/data/catalog.json','utf8'));
const ids=new Map(catalog.areas.map(a=>[a.id,a]));
assert.equal(ids.size,7436);assert.equal(catalog.provinces.length,77);assert.equal(catalog.districts.length,928);
assert.equal(new Set(Object.entries(zones).filter(([key])=>key!=='metro').flatMap(([,values])=>values)).size,77);
let geometries=0;const geometryIds=new Set();
for(const file of await readdir('web/data/provinces')){
 const fc=JSON.parse(await readFile('web/data/provinces/'+file,'utf8'));
 for(const feature of fc.features){assert.ok(ids.has(feature.properties.id));assert.ok(!geometryIds.has(feature.properties.id));geometryIds.add(feature.properties.id);assert.ok(['Polygon','MultiPolygon'].includes(feature.geometry.type));geometries++;}
}
assert.equal(geometries,7436);
const metro=scopeAreas(catalog.areas,{zone:'metro'});assert.equal(metro.length,488);
const north=scopeAreas(catalog.areas,{zone:'north'});assert.equal(north.length,836);
const bkk=scopeAreas(catalog.areas,{province:'10'});assert.equal(bkk.length,180);
assert.ok(scopeAreas(catalog.areas,{district:'1001'}).every(a=>a.district==='1001'));
const results=collisionNames(catalog.areas,new Set(['คลองสาม']));assert.ok(results.some(x=>x.name==='คลองสามประเวศ'));assert.ok(!results.some(x=>x.name==='คลองสาม'));
assert.ok(!collisionNames(catalog.areas,new Set(['คลองสาม','คลองสามประเวศ'])).some(x=>x.name==='คลองสามประเวศ'));
assert.ok(!collisionNames(catalog.areas,new Set(['คลองสาม']),new Set(['คลองสามประเวศ'])).some(x=>x.name==='คลองสามประเวศ'));
const origin=catalog.areas.find(a=>a.name==='คลองสาม'),excluded=catalog.areas.find(a=>a.name==='คลองสามประเวศ');
const buckets={origins:new Set([origin.id]),destinations:new Set(),excludedOrigins:new Set([excluded.id]),excludedDestinations:new Set()};
const settings=routeSettings(buckets,ids);assert.equal(settings.origins,'คลองสาม');assert.equal(settings.excludedOrigins,'คลองสามประเวศ');assert.equal(settings.originOn,true);assert.equal(settings.destOn,false);
const duplicate=catalog.areas.filter(a=>a.name==='บ้านใหม่');assert.ok(duplicate.length>1);assert.equal(routeSettings({...buckets,origins:new Set(duplicate.map(a=>a.id))},ids).origins,'บ้านใหม่');
assert.throws(()=>routeSettings({...buckets,origins:new Set(north.map(a=>a.id))},ids),/มากเกินขนาด/);
const bytes=Buffer.from(await encodeProfile('แผนที่ประเทศไทย',settings));const raw=inflateSync(bytes.subarray(21));assert.equal(crc32(raw),bytes.readUInt32BE(17));
await mkdir('app/build/profile-fixtures',{recursive:true});await writeFile('app/build/profile-fixtures/website.flp',bytes);
const html=await readFile('index.html','utf8');assert.ok(html.includes('/releases/latest'));assert.ok(!html.includes('/releases/download/v'));
// Every local asset linked by the root entry point is present before publication.
for(const [,path] of html.matchAll(/(?:src|href)="((?:web|tools)\/[^"#]+)"/g))await readFile(path);
const motion=await readFile('web/motion.mjs','utf8'),polish=await readFile('web/polish.css','utf8');
assert.ok(motion.includes('prefers-reduced-motion: reduce'));assert.ok(polish.includes('@media(prefers-reduced-motion:reduce)'));
assert.ok(html.includes('class="mobile-dock"'));assert.ok(html.includes('aria-labelledby="dialog-title"'));
console.log(`Website checks passed: ${geometries} boundaries; scope, collisions, exclusions, size guard, FLP checksum, stable download URL`);
