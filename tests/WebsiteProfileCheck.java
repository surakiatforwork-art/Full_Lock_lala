import com.fulllock.app.ProfileFile;
import java.nio.file.*;
public class WebsiteProfileCheck {
    public static void main(String[] args) throws Exception {
        var profile=ProfileFile.decode(Files.readAllBytes(Path.of(args[0])));
        if(!profile.name.equals("แผนที่ประเทศไทย") || !profile.values.get("origins").equals("คลองสาม") || !profile.values.get("excludedOrigins").equals("คลองสามประเวศ") || !profile.values.get("originOn").equals(true) || !profile.values.get("excludeOriginOn").equals(true))throw new AssertionError("Website routes must reach the Android decoder unchanged");
        System.out.println("Website profile accepted by Android Java decoder: Thai names, enabled routes and exclusions preserved");
    }
}
