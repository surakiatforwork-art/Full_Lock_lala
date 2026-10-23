import com.fulllock.app.JobParser;
import java.math.BigDecimal;

public class ParserChecks {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        var j=JobParser.parse("[ขนส่งสินค้า] เสาธงหิน > วังใหม่","ขนส่งทันที ฿156.00 รับงานขนส่ง!");
        check(j!=null,"sample parses");
        check(j.origin().equals("เสาธงหิน") && j.destination().equals("วังใหม่"),"route");
        check(j.fare().compareTo(new BigDecimal("156.00"))==0,"decimal price");
        check(j.price(2,20).compareTo(new BigDecimal("124.80"))==0,"fee");
        check(JobParser.parse("[ขนส่งสินค้า] A > B","งานล่วงหน้า ฿1,234.50!").fare().compareTo(new BigDecimal("1234.50"))==0,"thousands");
        check(JobParser.parse("[ขนส่งสินค้า] A > B","ขนส่งทันที ฿150.00 ทิป ฿20.00")==null,"unknown tips fail closed");
        check(JobParser.parse("[ขนส่งสินค้า] A > B","ขนส่งทันที ฿156.001!")==null,"invalid decimal");
        check(JobParser.parse("ข่าวสาร","ขนส่งทันที ฿156.00!")==null,"non-route");
        check(JobParser.parse("[ขนส่งสินค้า] A > B","฿156.00!")==null,"unknown type");
        check(JobParser.areaMatches("ตำบลเสาธงหิน","บางพูด, เสาธงหิน"),"area list");
        check(!JobParser.areaMatches("บางพูด",", ,"),"empty areas fail closed");
        var tipped=JobParser.parse("[ขนส่งสินค้า] บางคูรัด > หัวหมาก","ขนส่งทันที ฿276.00 + ฿10.00 รับงานขนส่ง!");
        check(tipped!=null,"tip sample parses");
        check(tipped.fare().compareTo(new BigDecimal("276"))==0,"tip base fare");
        check(tipped.tip().compareTo(new BigDecimal("10"))==0,"tip value");
        check(tipped.price(0,20).compareTo(new BigDecimal("276"))==0,"base formula");
        check(tipped.price(1,20).compareTo(new BigDecimal("286"))==0,"base plus tip");
        check(tipped.price(2,20).compareTo(new BigDecimal("220.80"))==0,"net base");
        check(tipped.price(3,20).compareTo(new BigDecimal("230.80"))==0,"net base plus full tip");
        var second=JobParser.parse("[ขนส่งสินค้า] คลองพระอุดม > สาทร","ขนส่งทันที ฿129.00 + ฿18.00 รับงานขนส่ง!");
        check(second.price(1,20).compareTo(new BigDecimal("147"))==0,"second sample total");
        check(JobParser.parse("[ขนส่งสินค้า] A > B","ขนส่งทันที ฿150 + ฿20 + ฿5")==null,"three amounts rejected");
        check(JobParser.parse("[ขนส่งสินค้า] A > B","ขนส่งทันที ฿150 / ฿20")==null,"ambiguous amounts rejected");
        String title="[ขนส่งสินค้า] บางคูรัด > หัวหมาก",body="ขนส่งทันที ฿276.00 + ฿10.00 รับงานขนส่ง!";
        check(JobParser.notificationJob(title,body,false,false)!=null,"real job eligible");
        check(JobParser.notificationJob(title,body,true,false)==null,"ongoing job-shaped notification ignored");
        check(JobParser.notificationJob(title,body,false,true)==null,"group summary ignored");
        check(JobParser.notificationJob("GPS กำลังทำงาน","กำลังติดตามตำแหน่ง",true,false)==null,"GPS ignored");
        check(JobParser.notificationJob("ประกาศ","ขนส่งทันที รับโบนัส ฿200",false,false)==null,"announcement ignored");
        check(JobParser.notificationJob(title,"ประกาศ ตัวอย่างงาน: "+body,false,false)==null,"announcement quoting job ignored");
        check(JobParser.notificationJob(title,"งานล่วงหน้า ฿156.00 รับงานขนส่ง!",false,false)!=null,"scheduled job eligible");
        check(JobParser.notificationJob(title,null,false,false)==null,"empty notification ignored");
        check(tipped.overlayNet(20).compareTo(new BigDecimal("230.80"))==0,"overlay deducts fee from fare only then adds tip");
        check(tipped.overlayNet(0).compareTo(new BigDecimal("286.00"))==0,"overlay no fee");
        check(tipped.overlayNet(100).compareTo(new BigDecimal("10.00"))==0,"overlay full fee");
        check(new JobParser.Job("","","","",new BigDecimal("1.01"),BigDecimal.ZERO,"","").overlayNet(50).equals(new BigDecimal("0.51")),"overlay money rounding");
        var driverSample=JobParser.parse("[ขนส่งสินค้า] อ้อมเกร็ด > บางตะไนย์","ขนส่งทันที ฿83.00 + ฿7.00 รับงานขนส่ง!");
        check(driverSample.overlayNet(20).equals(new BigDecimal("73.40")),"driver example: 83 minus 20 percent plus 7");
        check(driverSample.overlayNet(20).compareTo(driverSample.price(3,20))==0,"overlay agrees with net plus tip filter");
        System.out.println("ParserChecks: 35 checks passed");
    }
}
