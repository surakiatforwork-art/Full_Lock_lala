# ปุ่มลอยและการยืนยันหยุด

- เมนูซ้ายใต้สวิตช์ปุ่มลอย: dropdown “ตั้งค่าปุ่มลอย” รวม “แสดงรายการที่เปิดไม่ได้แล้ว” (เริ่มต้นเปิด) และความเร็วการกาง–หุบเดิม
- ปิดแสดงงานที่เปิดไม่ได้: กรองเฉพาะการแสดงผล ไม่ลบข้อมูลเดิม เปิดกลับมาเห็นรายการเดิม ไม่เปลี่ยนประวัติหรือจำนวนงานยังไม่เปิดดู แผงงานใหม่ไม่แสดงงานเก่าทดแทนหากงานล่าสุดเปิดไม่ได้
- ปุ่มลอย 80×56dp ใช้ภาพผีน้อยสีม่วงกับกล่องสีส้ม พื้นหลังตามธีม วาดตัวเลขบนหน้ากล่องด้วยโค้ด รองรับ 0–99 และ 99+ ย้าย/กางแผงด้านข้างโดยคำนวณขนาดปุ่มจริง
- Full Lock แสดงปุ่มลอยได้ ยกเว้นหน้าประวัติงาน ซ่อนใน Driver และขณะล็อกจอ/จอดับเหมือนเดิม
- ขณะทำงาน แถบหยุดใช้สีพื้นหลังแอปและเส้นขอบสีธีม แตะแล้วมีปุ่มยืนยันด้านขวา แตะที่อื่น/กดกลับ/เปลี่ยนหน้า/แอปเสีย foreground จะยกเลิกการยืนยันและยังทำงานต่อ การแตะส่วนซ้ายของแถบไม่เปิดยืนยันซ้ำ
- ตัวเลือกโปรไฟล์เป็น dropdown ใต้ header มีชื่อ รายการที่ใช้อยู่พร้อม ✓ และเมนูเพิ่ม/จัดการ/นำเข้า เลือกขณะทำงานยังต้องยืนยันเปลี่ยนโปรไฟล์ตามเดิม

## ภาพ

เตรียมด้วย built-in image_gen จากภาพที่ผู้ใช้ออกแบบ ตรวจ alpha เป็น RGBA และมุมภาพโปร่งใส บันทึกที่ `app/src/main/res/drawable-nodpi/floating_phantom.png` ภาพต้นฉบับที่ผู้ใช้ส่งยังคงเดิม

Prompt ที่ใช้:

> Edit target: supplied Full Lock purple ghost mascot button. Production Android floating button foreground asset. Preserve the purple hooded ghost exactly, its orange parcel, pose, face, glossy style and proportions as faithfully as possible. Remove the number 0 and its dark outline completely from the parcel front, reconstruct a clean blank orange parcel face for drawing dynamic numbers in code. Remove BOTH the black exterior background and the entire purple rounded pill backdrop behind the ghost. Keep ONLY ghost plus orange parcel and small yellow exclamation accents as a clean genuinely transparent PNG cutout. Same composition with entire subject visible, no added symbols or text, tightly framed with small transparent padding. Background pill will be drawn separately by app and recolored by user theme.

## ตรวจสอบ

FloatingChecks 27 ข้อ รวมซ่อน/แสดงรายการหมดอายุ โดยไม่ลบต้นฉบับและไม่แทนที่ preview ล่าสุดด้วยงานเก่า Build release และ lintRelease; ทดสอบ Android เพิ่มสถานะซ่อนในหน้าประวัติ ปุ่มยืนยัน การแตะยกเลิกนอกปุ่ม และตัวเลือกโปรไฟล์ใหม่

ผลตรวจ emulator-5582: OverlayChecks 76, FilterChecks 7, HistoryChecks 8, UiChecks 54, ProfileChecks 28 และ ProfileUiChecks 35 ผ่านทั้งหมด ตรวจภาพปุ่มซ้าย/ขวา หน้าตั้งค่าปุ่มลอย และหน้าการยืนยันหยุดแล้ว Pure Java: FloatingChecks 27 และ ParserChecks 33 ผ่าน Build release ลงนามกุญแจเดิมและ lintRelease ผ่าน (0 errors; warnings เดิมเรื่องไอคอน/รูปแบบ custom view/ข้อความ/RTL) ไม่มีการ push หรือเผยแพร่ GitHub Release ในรอบนี้
