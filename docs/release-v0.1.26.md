# Full Lock v0.1.26

- เพิ่มโหมด “อ่านงานบนหน้า Driver (รูท)” ค่าเริ่มต้นปิด ต้องมีสิทธิ์รูทและ LSPosed
- เพิ่มตัวกรองการจ่าย รูปแบบงาน ระยะทาง ราคาเฉลี่ย/กม. บริการเสริม และลำดับความสำคัญของงาน
- เลือกวาดรายได้สุทธิ ระยะทาง และราคาเฉลี่ย/กม. บนการ์ด Driver โดยใช้ค่าธรรมเนียมที่ตั้งไว้และส่วนลดค่าคอมที่ตรวจพบ
- ตั้งค่าวาดทับในเมนูด้านข้าง และซ่อนปุ่มลอยบนหน้า Driver
- เครื่องทั่วไปอ่านแจ้งเตือนได้ตามเดิม ผู้ใช้ยังปัดรับงานเอง
- รองรับโปรไฟล์เดิมและไฟล์จากเว็บไซต์

## ขอบเขตการเผยแพร่

เผยแพร่ APK เต็มพร้อมโมดูลที่ฝังอยู่ภายใน แต่ไม่เผยแพร่ซอร์สใหม่ ซอร์ส Android ใน repository จึงยังเป็นรุ่นก่อนหน้า ไม่ใช่ซอร์สสำหรับสร้าง APK v0.1.26 ไม่เผยแพร่ APK ของ Driver, ผลการแกะแอป, session, token, บัญชีทดสอบ หรือกุญแจลงนาม

Full Lock เป็นเครื่องมืออิสระ ไม่ได้เป็นผลิตภัณฑ์หรือได้รับการรับรองโดย Lalamove การใช้งานกับบริการภายนอกต้องพิจารณาเงื่อนไขบริการนั้นแยกต่างหาก

## ผลตรวจ

- บิลด์ release พร้อม R8 และชุดทดสอบตัวกรอง/ราคา/โปรไฟล์ผ่าน ยังไม่ได้ทดสอบ runtime ของ release นี้บนอุปกรณ์ในรอบเผยแพร่
- ตรวจ package/version, signer app/Probe และโมดูลฝังตรงกัน ไม่พบ private key/signing store/token ตามรูปแบบที่ตรวจใน APK และโมดูลซ้อน
- ไม่พบ FLAG_SECURE bypass ใน source ที่ใช้สร้างโมดูล
- Xposed API ใช้ compileOnly ไม่ฝัง implementation ของ framework; upstream ระบุ Apache-2.0: https://raw.githubusercontent.com/rovo89/XposedBridge/master/NOTICE.txt
- ตรวจนโยบาย GitHub ณ 30 ต.ค. 2026 ไม่พบข้อห้าม root/Xposed โดยชื่อ แต่การละเมิดสิทธิ์ ความเป็นส่วนตัว และสนับสนุนการโจมตีที่ผิดกฎหมายยังเป็นสิ่งต้องห้าม ไม่ใช่การรับรองจาก GitHub หรือคำรับประกันทางกฎหมาย
- แจ้งปัญหาได้ที่ https://github.com/surakiatforwork-art/Full_Lock_lala/issues

อ้างอิง:
https://docs.github.com/en/site-policy/acceptable-use-policies/github-active-malware-or-exploits
https://docs.github.com/en/site-policy/acceptable-use-policies/github-acceptable-use-policies
