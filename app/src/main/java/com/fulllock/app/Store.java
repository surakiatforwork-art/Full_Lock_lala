package com.fulllock.app;

import android.content.*;
import org.json.*;
import java.math.BigDecimal;

final class Store {
    static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("settings", Context.MODE_PRIVATE); }
    static String rejection(Context c, JobParser.Job job) {
        var p = prefs(c);
        if (p.getBoolean("price", true)) {
            BigDecimal value = job.price(p.getInt("formula", 0), p.getInt("fee", 20));
            if (value.compareTo(new BigDecimal(p.getString("min", "100"))) < 0) return "ราคาต่ำกว่าที่ตั้งไว้";
            if (value.compareTo(new BigDecimal(p.getString("max", "2000"))) > 0) return "ราคาสูงกว่าที่ตั้งไว้";
        }
        if (p.getBoolean("originOn", false) && !JobParser.areaMatches(job.origin(), p.getString("origins", ""))) return "ต้นทางไม่ตรงเงื่อนไข";
        if (p.getBoolean("destOn", false) && !JobParser.areaMatches(job.destination(), p.getString("destinations", ""))) return "ปลายทางไม่ตรงเงื่อนไข";
        int type = p.getInt("type", 0);
        if (type == 1 && !job.type().equals("ขนส่งทันที") || type == 2 && !job.type().equals("งานล่วงหน้า")) return "ประเภทงานไม่ตรงเงื่อนไข";
        if (p.getBoolean("bulk", false)) for (int amount : new int[]{475,600,650,900,735,1000,1150})
            if (job.fare().compareTo(BigDecimal.valueOf(amount)) == 0) return "ราคาตรงรายการบล็อกงานเหมา";
        return null;
    }
    static synchronized void log(Context c, String title, String text, String result) {
        try {
            JSONArray old = new JSONArray(prefs(c).getString("history", "[]")), next = new JSONArray();
            next.put(new JSONObject().put("title",title).put("text",text).put("result",result).put("time",System.currentTimeMillis()));
            for (int i=0;i<Math.min(old.length(),29);i++) next.put(old.get(i));
            prefs(c).edit().putString("history",next.toString()).apply();
        } catch(JSONException ignored) { }
    }
}
