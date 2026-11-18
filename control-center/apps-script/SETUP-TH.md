# ย้าย Full Lock ไป Google Sheets และ Apps Script

## ไฟล์ที่จะใช้

- Code.gs เป็น backend ใหม่สำหรับชีต 1xKMRCm3z0M8dYa_TgNKlMFmddfH9aTsxZrozaYLmwOI
- appsscript.json ตั้ง timezone เป็น Asia/Bangkok และ runtime V8
- Vercel ยังคง API /api/access, /api/admin และ /api/install-count เดิม
- Firebase Authentication ยังคงรับผิดชอบ Google Sign-In ของสามบัญชีแอดมิน ข้อมูลอุปกรณ์หลังสลับจะอยู่ในชีต

## คุณตั้งค่า Apps Script

1. เปิดชีต แล้วเลือก ส่วนขยาย > Apps Script
2. วางไฟล์ Code.gs แทนต้นแบบเก่า กดบันทึก
3. เรียก setupDatabase และอนุญาตการเข้าถึงชีต จะสร้าง Devices, Settings, AuditLogs โดยคงแท็บเดิมไว้
4. เปิด Project settings > Script properties คัดลอก FULL_LOCK_BRIDGE_SECRET เก็บเป็นความลับสำหรับใส่ Vercel ไม่ต้องส่งในแชท
5. Deploy > New deployment > Web app เลือก Execute as Me และ Who has access Anyone จากนั้น deploy
6. ส่ง URL ที่ลงท้าย /exec ให้ผู้พัฒนา สำหรับตรวจ health และเตรียมเชื่อมต่อ

ทุกครั้งที่แก้ Code.gs หลัง deploy ต้องแก้ deployment ให้ใช้เวอร์ชันใหม่ด้วย URL เดิม

## การย้ายข้อมูล

เครื่องมือ tools/export-control-database.cjs ส่งออก Firestore ผ่านบัญชี Firebase CLI ที่มีสิทธิ์
ต้องกำหนด FIREBASE_TOOLS_LIB เป็นโฟลเดอร์ lib ของ firebase-tools และระบุ absolute output path นอก repository
ไฟล์ JSON เก็บอุปกรณ์ สิทธิ์ สถานะส่วนกลาง และประวัติแอดมิน ข้อมูลนี้เป็นข้อมูลส่วนตัว ให้เก็บบน Drive แบบจำกัดสิทธิ์
ห้าม commit ไฟล์ส่งออก กุญแจเชื่อมต่อ หรือ service account

อัปโหลด JSON ไป Drive ส่วนตัว กำหนด FULL_LOCK_IMPORT_FILE_ID ใน Script properties แล้วเรียก importLegacyDatabase
นำเข้าซ้ำได้โดยไม่เพิ่มอุปกรณ์ซ้ำและไม่เขียนทับสิทธิ์ของรายการที่มีอยู่
หากการนำเข้าหยุดบางส่วน ให้ตรวจ Settings.enabled เทียบไฟล์ส่งออกก่อนเปิดใช้จริง
ตรวจจำนวน IDs สถานะทุกเครื่อง สถานะส่วนกลาง วันลงทะเบียน และประวัติให้ครบ
หากมีการเปลี่ยนสิทธิ์หรือเพิ่มเครื่องในระบบเก่าระหว่างเตรียมข้อมูล ต้องเก็บส่วนต่างและตรวจเทียบอีกครั้งก่อนสลับ

## การตั้งค่า Vercel โดยผู้พัฒนา

เพิ่ม FULL_LOCK_SHEETS_URL และ FULL_LOCK_SHEETS_SECRET ให้ตรงกับ deployment และ Script properties
คง FULL_LOCK_DEVICE_SECRET เดิม เพื่อให้ token ที่ APK เก่าได้รับยังใช้ต่อได้
FULL_LOCK_DATA_BACKEND ยังไม่ตั้ง หรือใช้ค่าเดิม ระหว่างเตรียมจะยังใช้ Firestore
เมื่อย้ายข้อมูลและทดสอบแล้วจึงตั้ง FULL_LOCK_DATA_BACKEND=sheets และ deploy ใหม่
Firebase env ที่ใช้ Google Sign-In ยังต้องอยู่

Vercel ลงนามคำขอด้วย HMAC SHA-256 พร้อมเวลาและ nonce Apps Script ตรวจลายเซ็นก่อนเข้าถึงข้อมูล
การระบุ admin actor ทำได้เฉพาะ Vercel หลังตรวจ Google ID token และ allowlist แล้ว
คำขอที่ล้มเหลวจะตอบข้อผิดพลาด แอปจะใช้ผลสิทธิ์ล่าสุด ห้ามแปลงเป็น allowed=false
Apps Script cache ผลอุปกรณ์ 5 นาที และล้างรายเครื่องเมื่อ admin เปลี่ยนสิทธิ์
การเปลี่ยนสถานะส่วนกลางล้างผลสถานะ global ทันที
การแก้เซลล์เองในชีตอาจรอ cache สูงสุด 5 นาที ให้ใช้หน้าแอดมินเพื่อเปลี่ยนสิทธิ์ตามปกติ

## ความถี่

APK ใหม่ตรวจอัตโนมัติทุก 12–18 นาที สุ่มแต่ละรอบ และคง deadline ข้ามการเปิดแอปใหม่
การกลับหน้าแอปก่อนถึงรอบจะไม่ส่งซ้ำ ปุ่มตรวจอีกครั้งยังทำงานทันที
เมื่อระบบขัดข้อง ลองใหม่ใน 3–6 นาที แอปไม่ตีความว่าเครื่องถูกระงับ
เวลาจริงอาจช้ากว่ากำหนดเมื่อ Android พัก process
APK เก่ายังคงส่งทุก 2 นาที ลดความถี่ใน binary เดิมจาก server ไม่ได้
cache ใน Apps Script ช่วยลดการอ่านชีต แต่คำขอเก่ายังนับเป็นการเรียก Web app
จึงต้องวัดจำนวนคำขอจริงหลังเปลี่ยนและติดตาม Apps Script executions

## ตรวจรับ

- ตรวจ APK เก่าใช้ token เดิมและได้รับสิทธิ์เดิมหลังย้าย
- ทดลอง register, checkin, block/unblock รายเครื่อง และ global disable/recovery
- ทดสอบบัญชีแอดมินทั้ง allowlist และบัญชีที่ไม่มีสิทธิ์
- ตรวจโหมดล่าสุด ตัวกรอง รายการเกิน 200 เครื่อง และค้น ID เต็ม
- ตรวจยอดสาธารณะ ไม่มีข้อมูลระบุตัวเครื่องรั่วออก endpoint สาธารณะ
- จำลอง upstream timeout และ malformed JSON ต้องไม่ระงับเครื่อง
- เก็บ Firestore snapshot เดิมไว้จนตรวจรับ หากจะย้อนกลับหลังเริ่มเขียนชีต ต้องนำส่วนต่างกลับก่อน
