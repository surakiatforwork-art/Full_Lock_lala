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
        System.out.println("ParserChecks: 11 checks passed");
    }
}
