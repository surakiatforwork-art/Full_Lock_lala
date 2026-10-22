# ตรวจรุ่น 0.1.0 — 2026-10-22

- `gradlew.bat :app:assembleDebug :app:lintDebug`: ผ่าน, lint 0 errors / 3 warnings (target SDK 35, backup configuration และกำหนดเมนูด้านซ้ายตามคำขอ)
- `ParserChecks`: ผ่าน 11 ข้อ รวมทศนิยม หลักพัน สูตรค่าธรรมเนียม พื้นที่ และการปฏิเสธรูปแบบทิปที่ยังไม่ยืนยัน
- ติดตั้ง debug APK และเปิดบน emulator Android API 36 ได้ ตรวจภาพหน้าตั้งค่าและเมนูสิทธิ์ด้านซ้ายแล้ว
- กดทดสอบแจ้งเตือนตัวอย่างจากเมนู แสดงเส้นทาง เสาธงหิน → วังใหม่ ราคา 156.00 และผลผ่านเงื่อนไขได้
- ไม่พบ crash ใน crash log ระหว่างตรวจ
- Emulator ตอบสนองช้า มี launch timeout จาก `am start -W` ก่อนหน้าจอเปิดได้ภายหลัง จึงไม่ใช่ผลวัดความเร็วบนมือถือจริง
- ยังไม่ได้ทดสอบแจ้งเตือนจาก Lalamove จริง, สิทธิ์จากผู้ใช้จริง, การเปิด contentIntent ใน foreground/background/lock screen หรือการยืนยันว่าหน้างานเฉพาะรายการเปิดได้

ภาพประกอบ: `fulllock-ui.png`, `fulllock-menu.png`, `fulllock-sample.png`
