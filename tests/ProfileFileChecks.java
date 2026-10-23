import com.fulllock.app.ProfileFile;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public class ProfileFileChecks {
    static int count;
    interface Work{void run()throws Exception;}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);count++;}
    static void reject(Work work,String why)throws Exception{try{work.run();throw new AssertionError(why);}catch(IOException|IllegalArgumentException expected){count++;}}
    static byte[] container(byte[] raw)throws Exception{ByteArrayOutputStream zip=new ByteArrayOutputStream();try(DeflaterOutputStream out=new DeflaterOutputStream(zip)){out.write(raw);}CRC32 crc=new CRC32();crc.update(raw);ByteArrayOutputStream file=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(file);out.writeBytes("FLP-PHTM");out.writeByte(1);out.writeInt(zip.size());out.writeInt(raw.length);out.writeInt((int)crc.getValue());out.write(zip.toByteArray());return file.toByteArray();}
    public static void main(String[] args)throws Exception {
        Map<String,Object> values=new LinkedHashMap<>(ProfileFile.DEFAULTS);values.put("origins","บางพูด,คลองสาม");values.put("excludedDestinations","คลองสามประเวศ");values.put("themeColor",0xFF2563EB);values.put("invertText",true);values.put("serviceMask",2);
        ProfileFile.Data profile=new ProfileFile.Data("กรุงเทพ / กลางคืน",values);byte[] bytes=ProfileFile.encode(profile);ProfileFile.Data decoded=ProfileFile.decode(bytes);
        check(new ProfileFile.Data("เก่า",Map.of("serviceMask",3)).values.get("serviceMask").equals(1),"old both-services profile migrates to goods");
        check(decoded.name.equals(profile.name),"Thai name round trip");check(decoded.values.equals(values),"all fields round trip");check(bytes.length<1024,"compact preset");
        check(ProfileFile.decode(ProfileFile.encode(new ProfileFile.Data("ย่อ",Map.of()))).values.equals(ProfileFile.DEFAULTS),"omitted defaults");
        reject(()->new ProfileFile.Data(" ",values),"empty name");reject(()->new ProfileFile.Data("a".repeat(61),values),"long name");reject(()->new ProfileFile.Data("bad\nname",values),"control name");
        Map<String,Object> bad=new HashMap<>(values);bad.put("min","500");bad.put("max","100");reject(()->new ProfileFile.Data("bad",bad),"inverted range");
        for(var entry:Map.of("fee",101,"formula",4,"type",3,"serviceMask",0,"themeColor",0x002563EB).entrySet()){Map<String,Object> m=new HashMap<>(values);m.put(entry.getKey(),entry.getValue());reject(()->new ProfileFile.Data("bad",m),"bad range or color");}
        for(String amount:new String[]{"-1","NaN","1e3","1.001"}){Map<String,Object> m=new HashMap<>(values);m.put("min",amount);reject(()->new ProfileFile.Data("bad",m),"invalid money");}
        Map<String,Object> wrong=new HashMap<>(values);wrong.put("price","true");reject(()->new ProfileFile.Data("bad",wrong),"wrong type");
        Map<String,Object> unknown=new HashMap<>(values);unknown.put("enabled",true);reject(()->new ProfileFile.Data("bad",unknown),"global state cannot enter profile");
        Map<String,Object> huge=new HashMap<>(values);huge.put("origins","ก".repeat(6000));reject(()->ProfileFile.encode(new ProfileFile.Data("bad",huge)),"oversized exported area list");check(new ProfileFile.Data("legacy",huge).values.get("origins").equals(huge.get("origins")),"legacy long lists remain usable locally");
        byte[] rawV2;try(InflaterInputStream zin=new InflaterInputStream(new ByteArrayInputStream(bytes,21,bytes.length-21))){rawV2=zin.readAllBytes();}
        byte[] rawV1=Arrays.copyOf(rawV2,rawV2.length-10);rawV1[1]--;ProfileFile.Data legacy=ProfileFile.decode(container(rawV1));
        check(legacy.values.get("serviceMask").equals(1),"old FLP defaults to goods only");check(legacy.values.get("type").equals(values.get("type")),"old time selection preserved");check(bytes[8]==2,"new files explicitly require FLP v2");
        reject(()->ProfileFile.decode(new byte[0]),"empty file");reject(()->ProfileFile.decode(Arrays.copyOf(bytes,bytes.length-1)),"truncation");reject(()->ProfileFile.decode(Arrays.copyOf(bytes,bytes.length+1)),"trailing bytes");
        byte[] changed=bytes.clone();changed[0]=0;reject(()->ProfileFile.decode(changed),"wrong magic");byte[] version=bytes.clone();version[8]=3;reject(()->ProfileFile.decode(version),"future format");byte[] checksum=bytes.clone();checksum[20]^=1;reject(()->ProfileFile.decode(checksum),"CRC corruption");
        byte[] excess=bytes.clone();excess[13]=0x7f;reject(()->ProfileFile.decode(excess),"oversized declared payload");
        reject(()->ProfileFile.decode(container(new byte[]{0,1,1,1,0,0,0,2,(byte)0xc0,(byte)0xaf})),"invalid UTF8");
        reject(()->ProfileFile.decode(container(new byte[]{0,1,10,2,0,0,0,1,2})),"non-boolean flag");
        reject(()->ProfileFile.decode(container(new byte[]{0,2,1,1,0,0,0,1,65,1,1,0,0,0,1,66})),"duplicate fields");
        reject(()->ProfileFile.read(new ByteArrayInputStream(new byte[ProfileFile.MAX_FILE+1])),"bounded stream read");
        Path output=Path.of("app/build/profile-fixtures");Files.createDirectories(output);Files.write(output.resolve("java.flp"),bytes);
        if(args.length>0){ProfileFile.Data web=ProfileFile.decode(Files.readAllBytes(Path.of(args[0])));check(web.name.equals("จากเว็บไซต์"),"independent web encoder");check(web.values.get("origins").equals("บางพูด,คลองสาม"),"web Thai routes");check(web.values.get("serviceMask").equals(2)&&web.values.get("type").equals(2),"web passenger and scheduled selections preserved");check(web.values.get("themeColor").equals(ProfileFile.DEFAULTS.get("themeColor")),"web defaults match app");}
        System.out.println("ProfileFileChecks: "+count+" checks passed; sample "+bytes.length+" bytes");
    }
}
