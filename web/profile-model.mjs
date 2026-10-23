export const bucketLabels = {origins:'ต้นทางที่ต้องการ', destinations:'ปลายทางที่ต้องการ', excludedOrigins:'ต้นทางที่ไม่ต้องการ', excludedDestinations:'ปลายทางที่ไม่ต้องการ'};
export const opposite = {origins:'excludedOrigins', destinations:'excludedDestinations', excludedOrigins:'origins', excludedDestinations:'destinations'};
export const zones = {
 metro:['10','11','12','13','73','74'], north:['50','51','52','53','54','55','56','57','58'],
 northeast:['30','31','32','33','34','35','36','37','38','39','40','41','42','43','44','45','46','47','48','49'],
 central:['10','11','12','13','14','15','16','17','18','19','26','60','61','62','64','65','66','67','72','73','74','75'],
 east:['20','21','22','23','24','25','27'], west:['63','70','71','76','77'], south:['80','81','82','83','84','85','86','90','91','92','93','94','95','96']
};
export function scopeAreas(areas, {zone='all', province='', district='', search=''} = {}) {
 const needle = search.trim().normalize('NFC');
 return areas.filter(a => (!zones[zone] || zones[zone].includes(a.province)) && (!province || a.province===province) && (!district || a.district===district) && (!needle || `${a.name} ${a.context}`.includes(needle)));
}
export function collisionNames(areas, desiredNames, excludedNames = new Set()) {
 const found = new Map();
 for (const area of areas) {
  if (desiredNames.has(area.name) || excludedNames.has(area.name)) continue;
  const matches = [...desiredNames].filter(name => area.name !== name && area.name.includes(name));
  if (!matches.length) continue;
  if (!found.has(area.name)) found.set(area.name, {name:area.name, matches, areas:[]});
  found.get(area.name).areas.push(area);
 }
 return [...found.values()].sort((a,b)=>a.name.localeCompare(b.name,'th'));
}
export function routeSettings(buckets, byId) {
 const result = {};
 const toggles = {origins:'originOn', destinations:'destOn', excludedOrigins:'excludeOriginOn', excludedDestinations:'excludeDestOn'};
 for (const [key, ids] of Object.entries(buckets)) {
  const names = [...new Set([...ids].map(id => byId.get(id)?.name).filter(Boolean))].sort((a,b)=>a.localeCompare(b,'th'));
  result[key] = names.join(','); result[toggles[key]] = names.length > 0;
  if (new TextEncoder().encode(result[key]).length > 16384) throw Error(`รายชื่อ${bucketLabels[key]}มากเกินขนาดที่แอปรุ่นปัจจุบันนำเข้าได้ กรุณาลดพื้นที่ก่อนดาวน์โหลด`);
 }
 return result;
}
