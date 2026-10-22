package com.fulllock.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.service.notification.*;
import java.util.*;

public class JobListener extends NotificationListenerService {
    static volatile boolean connected;
    static final String DRIVER = "com.lalamove.global.driver.sea";
    private final Map<String,String> seen = new LinkedHashMap<>();
    private long lastAttempt;
    @Override public void onListenerConnected() { connected=true; }
    @Override public void onListenerDisconnected() { connected=false; }
    @Override public void onDestroy() { connected=false; super.onDestroy(); }
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (!DRIVER.equals(sbn.getPackageName())) return;
        Store.prefs(this).edit().putLong("lastReceived",System.currentTimeMillis()).apply();
        Notification n = sbn.getNotification();
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        String title = String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TITLE,""));
        String regular = String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TEXT,""));
        CharSequence big = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        String text = big == null || big.toString().isBlank() ? regular : big.toString();
        String fingerprint = title + "\n" + text;
        if (fingerprint.equals(seen.get(sbn.getKey()))) return;
        seen.put(sbn.getKey(),fingerprint);
        if (seen.size()>200) seen.remove(seen.keySet().iterator().next());
        if (!Store.prefs(this).getBoolean("enabled",false)) { Store.log(this,title,text,false,"ผู้ใช้ปิดการทำงานอยู่"); return; }
        JobParser.Job job = JobParser.parse(title,text);
        if (job == null && !text.equals(regular)) {
            job=JobParser.parse(title,regular);
            if(job!=null) text=regular;
        }
        if (job == null) { Store.log(this,title,text,false,"อ่านข้อมูลงานไม่ได้: ไม่พบเส้นทาง ประเภทงาน หรือราคาในรูปแบบที่รองรับ"); return; }
        String rejection = Store.rejection(this,job);
        if (rejection != null) { Store.log(this,title,text,false,rejection); return; }
        if (n.contentIntent == null) { Store.log(this,title,text,false,"ตรงเงื่อนไข แต่แจ้งเตือนไม่มีคำสั่งเปิดหน้างาน"); return; }
        long now = SystemClock.elapsedRealtime();
        int cooldown = Store.prefs(this).getInt("cooldown",15);
        if (lastAttempt != 0 && now-lastAttempt < cooldown*1000L) {
            long remaining=(cooldown*1000L-(now-lastAttempt)+999)/1000;
            Store.log(this,title,text,false,"ตรงเงื่อนไข แต่พักการเปิดงานซ้อนอยู่ เหลือประมาณ "+remaining+" วินาที (ตั้งไว้ "+cooldown+" วินาที)"); return;
        }
        lastAttempt = now;
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                ActivityOptions opts = ActivityOptions.makeBasic();
                opts.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                n.contentIntent.send(this,0,null,null,null,null,opts.toBundle());
            } else n.contentIntent.send();
            Store.log(this,title,text,true,"ตรงเงื่อนไขทั้งหมด · ส่งคำสั่งเปิดแล้ว\nยังไม่ยืนยันว่าหน้างานเปิดสำเร็จ ผู้ใช้ปัดรับงานเอง");
            offerManual(n.contentIntent,title);
        } catch (PendingIntent.CanceledException | SecurityException e) {
            Store.log(this,title,text,false,"ตรงเงื่อนไข แต่เปิดไม่ได้: " + (e instanceof PendingIntent.CanceledException?"คำสั่งในแจ้งเตือนถูกยกเลิกหรือหมดอายุ":"ระบบไม่อนุญาตให้เรียกคำสั่งเปิด")+" ("+e.getClass().getSimpleName()+")");
            offerManual(n.contentIntent,title);
        }
    }
    private void offerManual(PendingIntent intent,String title) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("matched","งานตรงเงื่อนไข",NotificationManager.IMPORTANCE_DEFAULT));
        // A user tap is a reliable fallback if Android blocks a background launch.
        try { nm.notify(41,new Notification.Builder(this,"matched").setSmallIcon(com.fulllock.app.R.drawable.ic_lock)
            .setContentTitle("งานตรงเงื่อนไข · แตะเพื่อเปิดหากยังไม่เปิด")
            .setContentText(title).setContentIntent(intent).setAutoCancel(true).build());
        } catch(SecurityException ignored) { }
    }
}
