import {encodeProfile} from '../tools/flp-web-codec.mjs';
import {bucketLabels, opposite, zones, scopeAreas, collisionNames, routeSettings} from './profile-model.mjs';
const $ = id => document.getElementById(id);
const make = (tag, text, className) => { const el=document.createElement(tag); if(text!==undefined) el.textContent=text; if(className)el.className=className; return el; };
let catalog, byId, map, polygons, resultLimit=60, generation=0, toastTimer, downloadUrl;
const stage=new Set(), buckets=Object.fromEntries(Object.keys(bucketLabels).map(key=>[key,new Set()])), cache=new Map();
const bucketLimits=Object.fromEntries(Object.keys(bucketLabels).map(key=>[key,60]));
// Keep the in-progress profile when switching between the two pages in this tab.
let draft;try{draft=JSON.parse(sessionStorage.getItem('fulllock-profile-draft')||'null');if(draft?.version!==1||!Array.isArray(draft.form)||!Array.isArray(draft.stage)||!draft.buckets||Object.values(draft.buckets).some(value=>!Array.isArray(value)))draft=null;}catch{}
function formInputs(){return [...document.querySelectorAll('#profile-form input, #profile-form select')];}
function inputKey(input){return input.id||input.name+':'+input.value;}
function saveDraft(){try{sessionStorage.setItem('fulllock-profile-draft',JSON.stringify({version:1,form:formInputs().map(input=>({key:inputKey(input),value:input.value,checked:input.checked})),scope:catalog?scope():draft?.scope||scope(),stage:catalog?[...stage]:draft?.stage||[],buckets:catalog?Object.fromEntries(Object.entries(buckets).map(([key,ids])=>[key,[...ids]])):draft?.buckets||{}}));}catch{}}
function restoreDraft(){if(!draft)return;
 for(const input of formInputs()){const value=draft.form?.find(x=>x.key===inputKey(input));if(!value)continue;if(input.type==='radio'||input.type==='checkbox')input.checked=value.checked===true;else if(typeof value.value==='string')input.value=value.value;}
 const setSelect=(id,value)=>{if([...$(id).options].some(o=>o.value===value))$(id).value=value;};setSelect('zone',draft.scope?.zone);provinces();setSelect('province',draft.scope?.province);districts();setSelect('district',draft.scope?.district);if(typeof draft.scope?.search==='string')$('search').value=draft.scope.search;
 for(const key of Object.keys(buckets))for(const id of draft.buckets?.[key]||[])if(byId.has(id))buckets[key].add(id);
 for(const id of draft.stage||[])if(byId.has(id))stage.add(id);$('formula').onchange();$('price').onchange();
}
addEventListener('pagehide',saveDraft);

document.querySelectorAll('[data-target]').forEach(button=>button.disabled=true);
function toast(message) { $('toast').textContent=message; $('toast').classList.add('visible');clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('toast').classList.remove('visible'),6000); }
function scope() {return {zone:$('zone').value,province:$('province').value,district:$('district').value,search:$('search').value};}
function scoped(){return scopeAreas(catalog.areas,scope());}
function names(key){return new Set([...buckets[key]].map(id=>byId.get(id).name));}
function style(feature){const id=feature.properties.id;const chosen=stage.has(id);return {color:chosen?'#e94a19':'#68745b',weight:chosen?2:0.6,fillColor:chosen?'#e94a19':Object.values(buckets).some(set=>set.has(id))?'#449983':'#a3b68c',fillOpacity:chosen?.65:.23};}
function render(){
 clearDownload();
 $('selected-count').textContent=`เลือกไว้ ${stage.size.toLocaleString('th-TH')} พื้นที่`;
 document.querySelectorAll('[data-target]').forEach(button=>button.disabled=!stage.size);
 polygons?.setStyle(style);renderResults();
 $('buckets').replaceChildren(...Object.entries(bucketLabels).map(([key,label])=>{
  const details=make('details',undefined,`bucket ${key.startsWith('excluded')?'negative':'positive'}`);details.open=buckets[key].size>0;
  details.append(make('summary',`${label} · ${buckets[key].size}`));const chips=make('div',undefined,'chips');
  if(!buckets[key].size)chips.append(make('p','ไม่ได้กำหนดพื้นที่','empty'));
  for(const id of [...buckets[key]].slice(0,bucketLimits[key])){const area=byId.get(id),chip=make('div',undefined,'chip'),text=make('span',area.name);text.append(make('small',area.context));const remove=make('button','×');remove.type='button';remove.setAttribute('aria-label',`ลบ ${area.name} จาก${label}`);remove.onclick=()=>{buckets[key].delete(id);render();};chip.append(text,remove);chips.append(chip);}
  if(buckets[key].size>bucketLimits[key]){const more=make('button',`ดูเพิ่ม · เหลือ ${buckets[key].size-bucketLimits[key]} พื้นที่`,'text-link');more.type='button';more.onclick=()=>{bucketLimits[key]+=100;render();};chips.append(more);}
  if(buckets[key].size){const clear=make('button','ล้างรายการนี้','text-link');clear.type='button';clear.onclick=()=>{buckets[key].clear();render();};chips.append(clear);}details.append(chips);return details;
 }));
 const count=Object.values(buckets).reduce((sum,set)=>sum+set.size,0);$('summary').textContent=count?`ตั้งค่าพื้นที่ ${count.toLocaleString('th-TH')} รายการ พร้อมบันทึก`:'ยังไม่ได้เลือกพื้นที่ · รับทุกเส้นทาง';
}
function clearDownload(){const previous=document.getElementById('save-profile');previous?.remove();if(downloadUrl){URL.revokeObjectURL(downloadUrl);downloadUrl=undefined;}}
function renderResults(){if(!catalog)return;const areas=scoped();$('area-results').replaceChildren(...areas.slice(0,resultLimit).map(area=>{
 const label=make('label',undefined,'area-option'),input=make('input');input.type='checkbox';input.checked=stage.has(area.id);input.onchange=()=>{input.checked?stage.add(area.id):stage.delete(area.id);render();};const text=make('span',area.name);text.append(make('small',area.context));label.append(input,text);return label;
}));$('more').hidden=areas.length<=resultLimit;}
function option(value,text){const el=make('option',text);el.value=value;return el;}
function provinces(){const ids=zones[$('zone').value];$('province').replaceChildren(option('','ทุกจังหวัดในโซน'),...catalog.provinces.filter(p=>!ids||ids.includes(p.id)).map(p=>option(p.id,p.name)));districts();}
function districts(){$('district').replaceChildren(option('','ทุกเขต / อำเภอ'),...catalog.districts.filter(d=>d.province===$('province').value).map(d=>option(d.id,d.name)));}
async function showScope(){
 const version=++generation,areas=scoped();resultLimit=60;renderResults();
 if(!areas.length){$('map-status').textContent='ไม่พบพื้นที่ ลองเปลี่ยนคำค้นหรือขอบเขต';polygons?.clearLayers();return;}
 if(map){map.fitBounds(areas.flatMap(a=>[[a.bounds[0],a.bounds[1]],[a.bounds[2],a.bounds[3]]]),{padding:[20,20],maxZoom:13});}
 $('map-status').textContent=`${areas.length.toLocaleString('th-TH')} แขวง/ตำบล · กำลังโหลดขอบเขต…`;
 const allowed=new Set(areas.map(a=>a.id)),ids=[...new Set(areas.map(a=>a.province))];
 try{const collections=await Promise.all(ids.map(id=>{if(!cache.has(id))cache.set(id,fetch(`web/data/provinces/${id}.geojson`).then(r=>{if(!r.ok)throw Error();return r.json();}).catch(e=>{cache.delete(id);throw e;}));return cache.get(id);}));if(version!==generation)return;
 polygons?.clearLayers();collections.forEach(fc=>polygons?.addData({...fc,features:fc.features.filter(f=>allowed.has(f.properties.id))}));
 $('map-status').textContent=`${areas.length.toLocaleString('th-TH')} แขวง/ตำบล · แตะพื้นที่เพื่อเลือก / แตะซ้ำเพื่อเอาออก`;
 }catch{if(version===generation){$('map-status').textContent='โหลดขอบเขตไม่สำเร็จ ยังเลือกจากรายชื่อด้านล่างได้';toast('โหลดแผนที่ไม่สำเร็จ ลองเปลี่ยนพื้นที่อีกครั้ง');}}
}
function ask(title,description,items=[]){return new Promise(resolve=>{
 const dialog=$('collision');$('dialog-title').textContent=title;$('dialog-description').textContent=description;
 $('collision-options').replaceChildren(...items.map(item=>{const label=make('label',undefined,'area-option'),box=make('input');box.type='checkbox';box.checked=true;box.value=item.name;const text=make('span',item.name);text.append(make('small',item.detail));label.append(box,text);return label;}));
 dialog.returnValue='cancel';dialog.onclose=()=>resolve(dialog.returnValue==='confirm'?[...dialog.querySelectorAll('input:checked')].map(i=>i.value):null);dialog.showModal();
});}
let assigning=false;
async function assign(key){if(assigning||!stage.size)return;assigning=true;try{
 const ids=[...stage],newNames=new Set(ids.map(id=>byId.get(id).name)),other=opposite[key];
 const conflicts=[...buckets[other]].filter(id=>newNames.has(byId.get(id).name));
 if(conflicts.length){const result=await ask('พื้นที่อยู่ในรายการตรงข้าม',`บางชื่ออยู่ใน${bucketLabels[other]}แล้ว ต้องการย้ายมา${bucketLabels[key]}หรือไม่?`);if(result===null)return;conflicts.forEach(id=>buckets[other].delete(id));}
 ids.forEach(id=>buckets[key].add(id));stage.clear();render();
 if(key==='origins'||key==='destinations'){
  const candidates=collisionNames(catalog.areas,names(key),names(other));
  if(candidates.length){const selected=await ask('มีชื่ออื่นที่อาจตรงเงื่อนไขด้วย',`ชื่อที่ยาวกว่าอาจมีคำที่คุณเลือกอยู่ เช่น “คลองสาม” กับ “คลองสามประเวศ” ต้องการเพิ่มชื่อด้านล่างเป็น${bucketLabels[other]}หรือไม่? ชื่อเหมือนกันทุกตัวอักษรจะไม่แสดงที่นี่`,candidates.map(c=>({name:c.name,detail:`มีคำว่า ${c.matches.join(', ')} · ${c.areas.map(a=>a.context).join(' / ')}`})));
   if(selected){const selectedNames=new Set(selected);candidates.filter(c=>selectedNames.has(c.name)).forEach(c=>c.areas.forEach(a=>buckets[other].add(a.id)));render();}
  }
 }toast(`บันทึก${bucketLabels[key]}แล้ว`);
}finally{assigning=false;}}
async function init(){
 try{const response=await fetch('web/data/catalog.json');if(!response.ok)throw Error();catalog=await response.json();
 const p=new Map(catalog.provinces.map(x=>[x.id,x.name])),d=new Map(catalog.districts.map(x=>[x.id,x.name]));catalog.areas.forEach(a=>a.context=`${d.get(a.district)} · ${p.get(a.province)}`);byId=new Map(catalog.areas.map(a=>[a.id,a]));
 if(globalThis.L){map=L.map('map',{preferCanvas:true,scrollWheelZoom:false}).setView([13.7,100.55],9);L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:18,attribution:'© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'}).addTo(map);map.attributionControl.addAttribution('<a href="https://bangkok.traffy.in.th">Traffy Fondue</a> · GeoThai · mapthai');
 polygons=L.geoJSON(null,{style,onEachFeature(feature,layer){const area=byId.get(feature.properties.id);const tooltip=make('span',`${area.name} · ${area.context}`);layer.bindTooltip(tooltip);layer.on('click',()=>{stage.has(area.id)?stage.delete(area.id):stage.add(area.id);render();});}}).addTo(map);
 }else $('map').append(make('p','แผนที่ไม่พร้อมใช้งาน เลือกจากรายชื่อด้านล่างได้'));
 provinces();restoreDraft();render();await showScope();
 $('zone').onchange=()=>{provinces();showScope();};$('province').onchange=()=>{districts();showScope();};$('district').onchange=showScope;
 let searchTimer;$('search').oninput=()=>{clearTimeout(searchTimer);searchTimer=setTimeout(showScope,300);};
 $('select-scope').onclick=()=>{scoped().forEach(a=>stage.add(a.id));render();};$('clear-stage').onclick=()=>{stage.clear();render();};$('more').onclick=()=>{resultLimit+=100;renderResults();};
 document.querySelectorAll('[data-target]').forEach(button=>button.onclick=()=>assign(button.dataset.target));
 }catch{ $('map-status').textContent='โหลดรายชื่อพื้นที่ไม่สำเร็จ กรุณารีเฟรชหน้าเว็บ';toast('โหลดข้อมูลไม่สำเร็จ กรุณาตรวจการเชื่อมต่อแล้วรีเฟรช');$('download').disabled=true;}
}
$('profile-form').addEventListener('input',clearDownload);
$('formula').onchange=()=>{$('fee').disabled=Number($('formula').value)<2;};$('formula').onchange();
$('price').onchange=()=>{for(const id of ['min','max','formula'])$(id).disabled=!$('price').checked;$('fee').disabled=!$('price').checked||Number($('formula').value)<2;};
$('profile-form').onsubmit=async event=>{event.preventDefault();try{
 if(!catalog)throw Error('กำลังโหลดข้อมูล กรุณารอสักครู่');
 if(stage.size)throw Error('ยังมีพื้นที่ที่เลือกไว้ กรุณากำหนดเป็นต้นทางหรือปลายทาง หรือล้างที่เลือกก่อน');
 const name=$('profile-name').value.trim().normalize('NFC');if(!name||[...name].length>60||/[\u0000-\u001f\u007f-\u009f]/u.test(name))throw Error('ตั้งชื่อโปรไฟล์ 1–60 ตัวอักษร');
 const min=$('min').value,max=$('max').value;if(!/^\d{1,10}(\.\d{1,2})?$/.test(min)||!/^\d{1,10}(\.\d{1,2})?$/.test(max)||Number(min)>Number(max))throw Error('ตรวจช่วงราคา ราคาต่ำสุดต้องไม่เกินราคาสูงสุด');
 const selectedMask=name=>[...document.querySelectorAll(`input[name="${name}"]:checked`)].reduce((mask,input)=>mask|Number(input.value),0);
 const serviceMask=selectedMask('service'),timeMask=selectedMask('job-time');
 if(!serviceMask||!timeMask){toast('เลือกบริการและเวลาอย่างน้อยอย่างละ 1 รายการ');return;}
 const settings={...routeSettings(buckets,byId),price:$('price').checked,min,max,formula:Number($('formula').value),fee:Number($('fee').value),type:timeMask===3?0:timeMask,serviceMask,themeColor:(0xff000000|parseInt($('theme').value.slice(1),16)),invertText:$('invert').checked};
 const bytes=await encodeProfile(name,settings);clearDownload();downloadUrl=URL.createObjectURL(new Blob([bytes],{type:'application/octet-stream'}));const link=make('a','กดที่นี่หากดาวน์โหลดไม่เริ่ม','text-link');link.id='save-profile';link.href=downloadUrl;link.download=name.replace(/[<>:"/\\|?*]/g,'_')+'.flp';$('download').after(link);link.click();toast('ไฟล์พร้อมแล้ว นำเข้าโปรไฟล์ใน Full Lock ได้เลย');
 }catch(error){toast(error.message.startsWith('รายชื่อ')||/[ก-๙]/.test(error.message)?error.message:'สร้างโปรไฟล์ไม่ได้ กรุณาตรวจค่าที่ตั้งไว้');}};
if('IntersectionObserver' in window){const loader=new IntersectionObserver(entries=>{if(entries.some(entry=>entry.isIntersecting)){loader.disconnect();init();}},{rootMargin:'400px'});loader.observe($('builder'));}else init();
