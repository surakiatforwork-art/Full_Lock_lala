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
            if (value.compareTo(new BigDecimal(min)) < 0) reasons.add("ไม่ตรงเงื่อนไข: ราคาตามสูตร "+money(value)+" บาท ต่ำกว่าขั้นต่ำ "+min+" บาท");
            if (value.compareTo(new BigDecimal(max)) > 0) reasons.add("ไม่ตรงเงื่อนไข: ราคาตามสูตร "+money(value)+" บาท สูงกว่าขั้นสูง "+max+" บาท");
        }
        if (p.getBoolean("originOn", false) && !JobParser.areaMatches(job.origin(), p.getString("origins", ""))) reasons.add("ไม่ตรงเงื่อนไข: ต้นทาง “"+job.origin()+"” ไม่อยู่ในพื้นที่ที่ตั้งไว้: "+area(p.getString("origins","")));
        if (p.getBoolean("destOn", false) && !JobParser.areaMatches(job.destination(), p.getString("destinations", ""))) reasons.add("ไม่ตรงเงื่อนไข: ปลายทาง “"+job.destination()+"” ไม่อยู่ในพื้นที่ที่ตั้งไว้: "+area(p.getString("destinations","")));
        int type = p.getInt("type", 0);
        if (type == 1 && !job.type().equals("ขนส่งทันที") || type == 2 && !job.type().equals("งานล่วงหน้า")) reasons.add("ไม่ตรงเงื่อนไข: เป็น"+job.type()+" แต่ตั้งไว้เฉพาะ"+(type==1?"ขนส่งทันที":"งานล่วงหน้า"));
        if (p.getBoolean("bulk", false)) for (int amount : new int[]{475,600,650,900,735,1000,1150})
            if (job.fare().compareTo(BigDecimal.valueOf(amount)) == 0) reasons.add("เปิดบล็อกงานเหมา: ราคางาน "+money(job.fare())+" บาท ตรงรายการที่บล็อกไว้");
        return reasons.isEmpty()?null:String.join("\n",reasons);
    }
    private static String money(BigDecimal n) { return n.setScale(2,java.math.RoundingMode.HALF_UP).toPlainString(); }
    private static String area(String s) { return s.isBlank()?"ยังไม่ได้กำหนดพื้นที่":s; }
    static synchronized void log(Context c, String title, String text, boolean worked, String result) {
        try {
            JSONArray old = new JSONArray(prefs(c).getString("history", "[]")), next = new JSONArray();
            next.put(new JSONObject().put("title",title).put("text",text).put("worked",worked).put("result",result).put("time",System.currentTimeMillis()));
            for (int i=0;i<Math.min(old.length(),29);i++) next.put(old.get(i));
            prefs(c).edit().putString("history",next.toString()).apply();
        } catch(JSONException ignored) { }
    }
}
