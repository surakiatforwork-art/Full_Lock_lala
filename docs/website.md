# เว็บไซต์ Full Lock

หน้าเริ่มต้นคือ `index.html` ที่ root ของ branch main เป็นเว็บ static มีขั้นตอนคัดไฟล์สาธารณะไป `dist` ก่อน deploy

ปรับมือถือเป็นหลัก: landing แสดงภาพความละเอียดสูงพร้อมการเคลื่อนไหวเบา ๆ เส้นทางเคลื่อน แสงพื้นหลัง การเผยเนื้อหาเมื่อเลื่อน และปฏิกิริยาตอนแตะ มีแถบลัดดาวน์โหลด/สร้างโปรไฟล์ด้านล่างบนมือถือ รองรับ prefers-reduced-motion เพื่อปิดเอฟเฟกต์ และเอฟเฟกต์ตกแต่งหยุดหลังเล่นตามรอบที่กำหนด

ฟอร์มมือถือเรียงตั้งค่างาน → เลือกพื้นที่ → ดาวน์โหลด แผนที่โหลดเมื่อใกล้ส่วนสร้างโปรไฟล์ รายชื่อที่เลือกทยอยแสดง 60 พื้นที่แรกและกดดูเพิ่มได้ ไม่สร้างองค์ประกอบหลายพันชิ้นในครั้งเดียว

## เปิดทดสอบ / deploy

รัน `python -m http.server 8765 --bind 127.0.0.1` ที่ root แล้วเปิด http://127.0.0.1:8765 อย่าเปิด index.html ผ่าน file:// เพราะเบราว์เซอร์ต้องโหลดข้อมูลแผนที่

สำหรับ Vercel / Netlify ให้เชื่อมต่อ repo (private ได้ เมื่อให้ผู้ให้บริการเข้าถึง repo) แล้วใช้ไฟล์ vercel.json / netlify.toml ที่เตรียมไว้ **อย่า publish root ของ repo ทั้งชุด** เพราะมีซอร์ส Android ด้วย คำสั่งเตรียมเว็บคัดเฉพาะ index.html, web/ และ tools/flp-web-codec.mjs ไป dist ไม่ส่ง app/, .git, docs, workflow หรือเครื่องมือเผยแพร่ขึ้นเว็บ

ตั้งค่าผู้ให้บริการ: Framework = Other/None, Build command = `node tools/package-website.mjs`, Output / Publish directory = `dist` ไม่มี token หรือค่าลับที่ต้องตั้งในเว็บ มี .nojekyll สำหรับ static hosting ด้วย

ทุกปุ่มดาวน์โหลดชี้ไฟล์ `web/downloads/FullLock.apk` บนโดเมนเว็บเอง พร้อม download attribute ไม่พาไปหน้า GitHub และไม่เรียก GitHub API จากเบราว์เซอร์ Vercel/Netlify กำหนด Content-Disposition ให้โหลดไฟล์และตรวจ cache ใหม่ทุกครั้ง

ไฟล์ APK ที่เผยแพร่สาธารณะอยู่ใน Git ชื่อคงที่ version.json เก็บเฉพาะรุ่น ขนาด และ SHA256 ไม่มี token หรือ URL repo ไฟล์นี้เริ่มจาก v0.1.13 ที่ตรวจ checksum จาก Release แล้ว

### อัปเดต APK เมื่อมี Release ใหม่

Workflow `.github/workflows/sync-website-apk.yml` ทำงานเมื่อ publish Release (หรือ Run workflow เอง) เรียก `tools/sync-website-apk.py` เพื่อดึง **stable latest** ตรวจ SHA256SUMS และ GitHub asset digest แล้ว commit เฉพาะ web/downloads กลับ main โฮสต์ที่เชื่อม Git สามารถ deploy จาก commit ใหม่ได้ ไม่ต้องแก้ลิงก์ในหน้าเว็บ อัปโหลด APK และ SHA256SUMS ใน draft ให้ครบก่อน publish

GITHUB_TOKEN อยู่ใน workflow ฝั่งผู้เผยแพร่เท่านั้น ไม่ถูกส่งออกใน dist หาก branch protection หรือสิทธิ์ Actions ไม่อนุญาตให้ bot push งานจะล้มเหลวโดยเก็บไฟล์เดิมไว้ ให้รัน `python tools/sync-website-apk.py` จากเครื่องผู้พัฒนาที่ล็อกอิน gh แล้ว commit ตามปกติ

### Repo private กับเว็บไซต์สาธารณะ

Vercel/Netlify ต้องได้รับสิทธิ์อ่าน repo เพื่อ build แต่ผู้ดาวน์โหลดไม่ต้องมีบัญชี GitHub เพราะไฟล์ APK เสิร์ฟจากเว็บไซต์ หากเปลี่ยน visibility ให้ตรวจ Git integration ของโฮสต์แล้วยืนยันว่า deploy ล่าสุดสำเร็จ Repo ยังไม่ได้ถูกเปลี่ยนเป็น private โดยงานนี้ การซ่อนลิงก์ไม่ทำให้ repo ที่ยัง public เป็นความลับ และไม่เรียกคืนสำเนาที่มีคนดาวน์โหลดไว้แล้ว

GitHub Pages จาก repo private ขึ้นกับแผน GitHub (Free ใช้ public repo; private ต้อง Pro/Team/Enterprise ตามเงื่อนไข) ส่วน Release จาก repo private ต้องยืนยันสิทธิ์ จึงไม่ใช้เป็นปลายทางดาวน์โหลดสาธารณะโดยตรง

## การเลือกพื้นที่

เปลี่ยนโซน/จังหวัด/อำเภอเพื่อดูพื้นที่ แล้วกดเลือกทั้งพื้นที่นี้ หรือแตะเลือกแต่ละตำบล จากนั้นกำหนดเป็นหนึ่งในสี่รายการ รายชื่อค้นหาเป็นทางเลือกสำหรับการใช้คีย์บอร์ดหรือเมื่อแผนที่ไม่พร้อม

ขอบเขตการเลือกขยายเป็นแขวง/ตำบลทั้งหมดในขอบเขต รวมผลค้นหาถ้ามี ชื่อจังหวัดและอำเภอแสดงประกอบ แต่ไฟล์ส่งออกมีเพียงชื่อแขวง/ตำบลไม่ซ้ำ เพราะ Driver ไม่แสดงจังหวัดในแจ้งเตือน ชื่อเหมือนกันทุกตัวอักษรไม่แจ้งเตือน ชื่ออื่นที่มีชื่อที่เลือกเป็นส่วนหนึ่งจะเสนอให้ยกเว้นเฉพาะเมื่อผู้ใช้ยืนยัน ไม่มีการยกเว้นอัตโนมัติ

โซนใช้การแบ่งหกภาคสำหรับช่วยค้นหา ไม่ใช่แนวเขตการปกครอง กรุงเทพฯ และปริมณฑลเป็นกลุ่มเพิ่มเติมหกจังหวัด กรุงเทพฯ สมุทรปราการ นนทบุรี ปทุมธานี นครปฐม สมุทรสาคร

## ข้อมูล / สิทธิ์การใช้

- รายชื่อ GeoThai/data: CC BY 4.0, Copyright Pongsakorn Thipayanate; รายชื่อปัจจุบัน 77 จังหวัด 928 อำเภอ 7,436 แขวง/ตำบล เก็บ license และ NOTICE ไว้ใน web/data
- ขอบเขตประเทศ mapthai (MIT, Piyayut Chitchumnong), ต้นทาง RTSD/OCHA ปี 2019 ใช้เป็นแผนที่อ้างอิง ไม่ใช่รับรองแนวเขตปัจจุบัน
- แทนที่กรุงเทพฯ ทั้งชุดด้วยขอบเขต 180 แขวงจาก Traffy Data Viewer / Longdo เงื่อนไข https://traffy.longdo.com/about อนุญาตนำข้อมูล data/*.json ไปใช้โดยให้เครดิตและลิงก์ Traffy Fondue https://bangkok.traffy.in.th ซึ่งแสดงบนแผนที่และ footer
- บางนาเหนือใช้รหัส 104701 ในข้อมูล Traffy จับคู่กับรหัสรายชื่อ 104702 ด้วยชื่อที่ตรงกัน ท่าแฝกเปลี่ยนสังกัดอำเภอ จับคู่ด้วยชื่อและจังหวัดเดียวกัน ไม่มีการสร้างขอบเขตขึ้นเอง
- ลดจำนวนตำแหน่งทศนิยมเป็นห้าหลัก เก็บ Polygon/MultiPolygon และแยกตามจังหวัดเพื่อโหลดเมื่อใช้ รวม geometry ประมาณ 2.1 MB
- แผนที่พื้นหลัง OpenStreetMap ใช้ standard raster tiles พร้อมเครดิต ไม่มี prefetch/offline tiles
- Leaflet 1.9.4 BSD-2-Clause bundled ใน web/vendor/leaflet พร้อม LICENSE

`web/data/provenance.json` บันทึก URL ที่ pin commit และ SHA256 ข้อมูลต้นทาง รัน `python tools/prepare-web-data.py` เพื่อ refresh (ต้องมี requests) การ refresh ดึง main ล่าสุดแล้ว pin commit ของรอบนั้น สคริปต์จะหยุดเมื่อจับคู่ขอบเขตไม่ได้ ให้ตรวจความเปลี่ยนแปลงก่อน commit

## โปรไฟล์และข้อจำกัด

ใช้ codec เดียวกับเอกสาร FLP เดิม ข้อมูลสร้างในเบราว์เซอร์ ไม่มีบัญชี ไม่มีการส่งโปรไฟล์ขึ้นเซิร์ฟเวอร์ การเปิดเว็บยังติดต่อ Google Fonts และ OpenStreetMap เพื่อแสดงผล

แอป v0.1.12 จำกัดแต่ละรายชื่อ 16 KiB เว็บตรวจและไม่ส่งออกหากเกิน เพื่อไม่สร้างไฟล์ที่แอปอ่านไม่ได้ เลือกทั้งภาค/ประเทศอาจเกินเพดาน ต้องลดพื้นที่จนกว่าจะมีแอปที่เพิ่มขนาดรองรับ ไม่ตัดชื่อเงียบ ๆ

ทดสอบ `node tests/website-checks.mjs` และทดสอบ FLP ข้าม Java/Node ตาม docs/website-and-flp-plan.md

ตรวจแพ็กเกจและ APK: `node tools/package-website.mjs` แล้ว `node tests/website-download-checks.mjs` ตรวจ hash, ขนาด, ลิงก์ทั้งสี่ และไม่มีซอร์ส/กุญแจใน dist

ทดสอบ direct download รอบนี้: เรียก APK จาก HTTP ของ dist ได้ 200 พร้อม SHA256 ตรง v0.1.13 กดปุ่มในเบราว์เซอร์แล้ว URL หน้ายังเป็นเว็บเดิม ไม่มีการเปิด GitHub ตรวจ app/build.gradle, .git/config และ tools/sync-website-apk.py จาก public server ได้ 404 ทั้งหมด (การตรวจ header เฉพาะ Vercel/Netlify ต้องตรวจอีกครั้งเมื่อ deploy จริง)

ไฟล์จากตัวสร้างเว็บ: หลังรันทดสอบ Node แล้ว รัน `javac -encoding UTF-8 -d app/build/profile-checks app/src/main/java/com/fulllock/app/ProfileFile.java tests/WebsiteProfileCheck.java` และ `java -cp app/build/profile-checks WebsiteProfileCheck app/build/profile-fixtures/website.flp`

ตรวจหน้าเว็บจริงด้วยเบราว์เซอร์ที่ 1366×900 และ 390×844: ค้นหาคลองสาม ยืนยันยกเว้นคลองสามประเวศ แตะ polygon เลือกคลองห้า เลือกอำเภอคลองหลวงครบ 7 ตำบล และสร้างไฟล์ผ่านปุ่มดาวน์โหลด หน้าจอมือถือไม่ล้นแนวนอน ตัวอ่าน Java ยอมรับไฟล์ที่สร้างจาก model/codec เว็บ (ไม่ใช่การทดสอบติดตั้งบนโทรศัพท์จริง) การจับ download event ของ in-app browser ไม่ส่งคืน path จึงตรวจการสร้างไฟล์จากสถานะบนจอร่วมกับทดสอบ codec ข้าม Java

ตรวจรอบก่อน push: 320×740, 390×844 และ 1366×900 ไม่ล้นแนวนอน ภาพโหลดครบ console ไม่มี JavaScript errors ฟอร์มมือถือส่งออกผ่าน Blob link ชื่อภาษาไทยได้ (ทดสอบจากเว็บ.flp) คำเตือนชื่อซ้อนและรายการยกเว้นทำงาน ภาพหน้าจออยู่ใน docs/website-preview/mobile-polished.png, mobile-320.png และ desktop-polished.png

ตั้งแต่ 0.1.21 ฟอร์มเลือกบริการ (ขนส่งสินค้า/รับ-ส่งผู้โดยสาร) และเวลาได้หลายรายการ ต้องเลือกอย่างน้อยอย่างละ 1 ตัวเลือก ส่งออก FLP v2 โดย serviceMask แยกจาก type ซึ่งยังเก็บเวลาเดิม นำเข้าไฟล์ v1 ในแอปรุ่นใหม่ได้ ใช้ขนส่งสินค้าเป็นค่าเริ่มต้น ส่วนแอปเก่าต้องอัปเดตก่อนใช้ไฟล์จากเว็บรุ่นนี้
