package com.fulllock.app;
import android.content.Context;

final class FilterChecks {
    final Context c;int count;
    FilterChecks(Context c){this.c=c;}
    void check(boolean value,String message){if(!value)throw new AssertionError(message);count++;}
    JobParser.Job job(String origin,String destination){return JobParser.parse("[ขนส่งสินค้า] "+origin+" > "+destination,"ขนส่งทันที ฿156.00 รับงานขนส่ง!");}
    int run(){
        var p=Store.prefs(c);
        p.edit().putBoolean("price",false).putBoolean("originOn",false).putBoolean("destOn",true).putString("destinations","คลองสาม").putInt("type",0).putBoolean("bulk",false).putBoolean("excludeOriginOn",false).putBoolean("excludeDestOn",true).putString("excludedDestinations","คลองสามประเวศ").apply();
        check(Store.rejection(c,job("บางพูด","คลองสาม"))==null,"wanted exact area accepted");
        check("ปลายทางที่ไม่ต้องการ".equals(Store.rejection(c,job("บางพูด","คลองสามประเวศ"))),"exclusion overrides positive substring");
        check("ปลายทางไม่ตรง".equals(Store.rejection(c,job("บางพูด","บางนา"))),"wanted filter still applies");
        p.edit().putBoolean("excludeDestOn",false).apply();check(Store.rejection(c,job("บางพูด","คลองสามประเวศ"))==null,"disabled exclusion does not block");
        p.edit().putBoolean("excludeOriginOn",true).putString("excludedOrigins","บางนา, บางพูด").apply();check("ต้นทางที่ไม่ต้องการ".equals(Store.rejection(c,job("บางพูด","คลองสาม"))),"origin exclusion list");
        p.edit().putString("excludedOrigins","").apply();check(Store.rejection(c,job("บางพูด","คลองสาม"))==null,"empty exclusion blocks nothing");
        p.edit().putBoolean("excludeOriginOn",false).putBoolean("excludeDestOn",true).putString("excludedDestinations","คลองสามประเวศ").putBoolean("destOn",false).apply();check("ปลายทางที่ไม่ต้องการ".equals(Store.rejection(c,job("บางพูด","คลองสามประเวศ"))),"exclusions work without positive filter");
        p.edit().putBoolean("excludeOriginOn",false).putBoolean("excludeDestOn",false).putBoolean("destOn",false).putBoolean("price",false).putInt("serviceMask",1).putInt("type",0).apply();
        JobParser.Job passenger=JobParser.parse("[รับ-ส่งผู้โดยสาร] บางกระสอ > บางกระสอ","ขนส่งทันที ฿48.00 รับงานขนส่ง!");
        check("ประเภทงานไม่ตรง".equals(Store.rejection(c,passenger)),"legacy goods selection rejects passenger");
        p.edit().putInt("serviceMask",2).apply();check(Store.rejection(c,passenger)==null,"passenger selection accepts passenger");check("ประเภทงานไม่ตรง".equals(Store.rejection(c,job("A","B"))),"passenger only rejects goods");
        p.edit().putInt("serviceMask",3).apply();check("ประเภทงานไม่ตรง".equals(Store.rejection(c,passenger))&&Store.rejection(c,job("A","B"))==null,"legacy both-services value defaults to goods");
        p.edit().putInt("type",2).apply();check("ประเภทงานไม่ตรง".equals(Store.rejection(c,passenger)),"time still applies to passenger");
        p.edit().putInt("type",0).putInt("serviceMask",1).putBoolean("price",true).apply();return count;
    }
}
