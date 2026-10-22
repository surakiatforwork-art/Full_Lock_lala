package com.fulllock.app;

import android.content.*;
import org.json.*;
import java.math.BigDecimal;

final class Store {
    static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("settings", Context.MODE_PRIVATE); }
    static String rejection(Context c, JobParser.Job job) {
        var p = prefs(c);
        java.util.List<String> reasons = new java.util.ArrayList<>();
        if (p.getBoolean("price", true)) {
            BigDecimal value = job.price(p.getInt("formula", 0), p.getInt("fee", 20));
            String min=p.getString("min","100"), max=p.getString("max","2000");
            if (value.compareTo(new BigDecimal(min)) < 0) reasons.add("ราคาต่ำไป");
            if (value.compareTo(new BigDecimal(max)) > 0) reasons.add("ราคาสูงไป");
        }
        if (p.getBoolean("originOn", false) && !JobParser.areaMatches(job.origin(), p.getString("origins", ""))) reasons.add("ต้นทางไม่ตรง");
        if (p.getBoolean("destOn", false) && !JobParser.areaMatches(job.destination(), p.getString("destinations", ""))) reasons.add("ปลายทางไม่ตรง");
        int type = p.getInt("type", 0);
        if (type == 1 && !job.type().equals("ขนส่งทันที") || type == 2 && !job.type().equals("งานล่วงหน้า")) reasons.add("ประเภทงานไม่ตรง");
        if (p.getBoolean("bulk", false)) for (int amount : new int[]{475,600,650,900,735,1000,1150})
            if (job.fare().compareTo(BigDecimal.valueOf(amount)) == 0) reasons.add("คาดว่าเป็นงานเหมา");
        return reasons.isEmpty()?null:String.join(" / ",reasons);
    }
    // Render previous versions' filter reasons concisely without modifying saved history.
    static String shortReason(String result) {
        java.util.List<String> lines=new java.util.ArrayList<>();
        for(String line:result.split("\n")) {
            if(line.startsWith("ยังไม่ยืนยันว่าหน้างานเปิด"))continue;
            if(line.contains("พักการเปิดงานซ้อน")){lines.add("บันทึกจากรุ่นก่อน");continue;}
            if(line.contains("CanceledException")){lines.add("งานหมดอายุแล้ว");continue;}
            if(line.contains("SecurityException")){lines.add("ระบบไม่อนุญาตให้เปิดงาน");continue;}
            if(line.contains("ส่งคำสั่งเปิดแล้ว")){lines.add("เรียกเปิดงานแล้ว");continue;}
            if(line.contains("ไม่มีคำสั่งเปิด")){lines.add("เปิดงานนี้ไม่ได้");continue;}
            if(line.contains("ปิดเปิดงานอัตโนมัติ")){lines.add("ตรงเงื่อนไข / ยังไม่เปิดอัตโนมัติ");continue;}
            if(line.startsWith("อ่านข้อมูลงานไม่ได้")){lines.add("อ่านงานนี้ไม่ได้");continue;}
            if(line.contains("ต่ำกว่าขั้นต่ำ") || line.equals("ราคาต่ำกว่าที่ตั้งไว้")) line="ราคาต่ำไป";
            else if(line.contains("สูงกว่าขั้นสูง") || line.equals("ราคาสูงกว่าที่ตั้งไว้")) line="ราคาสูงไป";
            else if(line.startsWith("ไม่ตรงเงื่อนไข: ต้นทาง") || line.equals("ต้นทางไม่ตรงเงื่อนไข")) line="ต้นทางไม่ตรง";
            else if(line.startsWith("ไม่ตรงเงื่อนไข: ปลายทาง") || line.equals("ปลายทางไม่ตรงเงื่อนไข")) line="ปลายทางไม่ตรง";
            else if(line.startsWith("เปิดบล็อกงานเหมา:") || line.equals("ราคาตรงรายการบล็อกงานเหมา")) line="คาดว่าเป็นงานเหมา";
            else if(line.startsWith("ไม่ตรงเงื่อนไข: เป็น") || line.equals("ประเภทงานไม่ตรงเงื่อนไข")) line="ประเภทงานไม่ตรง";
            lines.add(line);
        }
        return String.join(" / ",lines);
    }
    static synchronized void log(Context c, String title, String text, boolean worked, String result) {
        try {
            JSONArray old = new JSONArray(prefs(c).getString("history", "[]")), next = new JSONArray();
            next.put(new JSONObject().put("title",title).put("text",text).put("worked",worked).put("result",result).put("time",System.currentTimeMillis()));
            for (int i=0;i<Math.min(old.length(),29);i++) next.put(old.get(i));
            prefs(c).edit().putString("history",next.toString()).apply();
        } catch(JSONException ignored) { }
    }
}
