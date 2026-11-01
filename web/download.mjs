const $=id=>document.getElementById(id);
try{
 const response=await fetch('web/downloads/version.json',{cache:'no-store'});if(!response.ok)throw Error('metadata');const data=await response.json();
 if(!/^v\d+\.\d+\.\d+$/.test(data.version)||data.filename!==`FullLock-${data.version}.apk`||!Number.isSafeInteger(data.bytes)||data.bytes<=0)throw Error('invalid');
 $('release-version').textContent=`เวอร์ชัน ${data.version.replace(/^v/,'')}`;
 const button=$('apk-download');button.href=`web/downloads/${data.filename}`;button.download=data.filename;button.textContent='ดาวน์โหลดแอป ↓';button.removeAttribute('aria-disabled');
 $('download-info').textContent=`${data.filename} · ${(data.bytes/1024/1024).toFixed(2)} MB`;
 const date=new Date(data.publishedAt);if(!Number.isFinite(date.getTime()))throw Error('date');$('release-date').dateTime=data.publishedAt;$('release-date').textContent=`อัปเดต ${new Intl.DateTimeFormat('th-TH',{dateStyle:'medium',timeStyle:'short',timeZone:'Asia/Bangkok'}).format(date)} น.`;
 const items=Array.isArray(data.changes)?data.changes.filter(x=>typeof x==='string'&&x.length<=1000).slice(0,20):[];$('release-changes').replaceChildren(...items.map(text=>{const li=document.createElement('li');li.textContent=text;return li}));
 button.addEventListener('click',()=>{$('download-status').textContent='หากไฟล์โหลดเสร็จแล้ว เปิดไฟล์เพื่อติดตั้งได้เลย';});
}catch{const b=$('apk-download');b.removeAttribute('href');b.setAttribute('aria-disabled','true');b.textContent='ยังเตรียมไฟล์ไม่ได้';$('release-version').textContent='ตรวจรุ่นล่าสุดไม่สำเร็จ';$('download-info').textContent='ตรวจการเชื่อมต่อ แล้วรีเฟรชหน้านี้เพื่อลองอีกครั้ง';$('release-changes').replaceChildren();}
