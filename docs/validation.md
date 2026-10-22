# ตรวจรุ่น 0.1.0 — 2026-10-22

- `gradlew.bat :app:assembleDebug :app:lintDebug`: ผ่าน, lint 0 errors / 3 warnings (target SDK 35, backup configuration และกำหนดเมนูด้านซ้ายตามคำขอ)
- `ParserChecks`: ผ่าน 11 ข้อ รวมทศนิยม หลักพัน สูตรค่าธรรมเนียม พื้นที่ และการปฏิเสธรูปแบบทิปที่ยังไม่ยืนยัน
- ติดตั้ง debug APK และเปิดบน emulator Android API 36 ได้ ตรวจภาพหน้าตั้งค่าและเมนูสิทธิ์ด้านซ้ายแล้ว
- กดทดสอบแจ้งเตือนตัวอย่างจากเมนู แสดงเส้นทาง เสาธงหิน → วังใหม่ ราคา 156.00 และผลผ่านเงื่อนไขได้
- ไม่พบ crash ใน crash log ระหว่างตรวจ
- Emulator ตอบสนองช้า มี launch timeout จาก `am start -W` ก่อนหน้าจอเปิดได้ภายหลัง จึงไม่ใช่ผลวัดความเร็วบนมือถือจริง
- ยังไม่ได้ทดสอบแจ้งเตือนจาก Lalamove จริง, สิทธิ์จากผู้ใช้จริง, การเปิด contentIntent ใน foreground/background/lock screen หรือการยืนยันว่าหน้างานเฉพาะรายการเปิดได้

ภาพประกอบ: `fulllock-ui.png`, `fulllock-menu.png`, `fulllock-sample.png`

## รุ่น 0.1.1

- Build และ lint ผ่าน; ParserChecks ผ่าน 21 ข้อ รวมสองภาพทิปและสูตรทั้งสี่
- เพิ่ม fallback ไปอ่าน EXTRA_TEXT ถ้า EXTRA_BIG_TEXT ว่างหรือแยกไม่ได้
- เพิ่ม diagnostics และขอ rebind ตัวอ่านเมื่อกดเริ่มทำงานโดยมีสิทธิ์แล้วแต่ service ยังไม่เชื่อมต่อ
- ยังไม่ได้ยืนยันสาเหตุบนเครื่องผู้ใช้ หรือทดสอบเปิดงานจริง การแก้ parser ไม่ใช่หลักฐานว่าการเปิดจากเบื้องหลังทำงานแล้ว
- ภาพ UI ในโฟลเดอร์นี้มาจาก 0.1.0 ยังไม่ใช่ภาพเมนู diagnostics ใหม่

## รุ่น 0.1.2

- Build และ lint ผ่าน หลังเพิ่มสถานะ worked ในประวัติและปรับเหตุผลการไม่เปิดงาน
- รวบรวมเหตุผลของตัวกรองทุกข้อที่ไม่ผ่าน พร้อมราคาและค่าที่ตั้งไว้ ณ เวลาตรวจ
- ประวัติเก่าไม่มี worked จะคงผลเดิมและระบุว่าเป็นประวัติรุ่นก่อน
- บันทึกแจ้งเตือน Lalamove เมื่อปิดสวิตช์ด้วยเหตุผล “ผู้ใช้ปิดการทำงานอยู่” (ยังข้าม summary และข้อความซ้ำใน key เดิม)
- สถานะ worked=true หมายถึง PendingIntent.send() ไม่โยน exception เท่านั้น ไม่ยืนยันว่าหน้างานเปิดจริง
- ยังไม่ได้ตรวจ UI ประวัติใหม่บน emulator หรือการเปิดหน้างานจริงในรุ่นนี้

## รุ่น 0.1.3

- ย่อเหตุผลตัวกรองตามคำขอ รวมประวัติรุ่นเก่าเฉพาะตอนแสดงผล
- ไม่เปลี่ยนเกณฑ์ราคา พื้นที่ หรือการตรวจราคางานเหมาแบบตรงรายการ
- Build และ lint ผ่าน; ไม่เพิ่มการทดสอบใหม่สำหรับการเปลี่ยนข้อความ

## รุ่น 0.1.4 — ปุ่มลอยนอกแอป

- Build, Android test APK และ lint ผ่าน (0 errors / 7 warnings)
- ParserChecks ผ่าน 21 ข้อ
- OverlayChecks บน emulator API 36 ผ่าน 15 ข้อ: หยุดแล้วซ่อน เริ่มแล้วแสดง ราคาแยกทิป/รวม เหตุผล งานล่าสุดแทนเก่า ลบงานเก่าไม่กระทบงานใหม่ ลบล่าสุดแล้วเปิดไม่ได้ เริ่มใหม่ไม่ค้างงานเก่า แตะเรียก callback และลากไม่เรียกเปิด
- ทดสอบด้วย StatusBarNotification จำลองที่ส่งเข้าตัวอ่านบน emulator ไม่ใช่แจ้งเตือนจริงจาก Lalamove
- ตรวจภาพ `fulllock-floating.png` บนหน้า Home ของ emulator: สีส้ม ทรงมุมมน แสดงข้อมูลเส้นทาง ราคา ทิป รวม และเหตุผล ตัวอย่างไม่มี contentIntent จึงแสดงว่าไม่มีคำสั่งเปิดงาน
- การแตะตรวจว่า notification key และ postTime ยังตรงกับแจ้งเตือนที่ active ก่อนเรียก contentIntent ไม่ใช้คำสั่งจากรายการที่ถูกลบ
- ยังต้องตรวจการเปิดหน้างานจริงจากปุ่มลอยกับ Lalamove บนมือถือผู้ใช้

รันบน emulator ที่อนุญาต notification access และ overlay แล้ว:

```powershell
./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.fulllock.app.test/com.fulllock.app.OverlayChecks
```

เพิ่ม `-e preview true` ก่อนชื่อ runner เพื่อบันทึกภาพทดสอบใน external files ของแอป ข้อมูลตั้งค่าและประวัติก่อนทดสอบจะถูกคืนค่าตอนจบ runner
