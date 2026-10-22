package com.fulllock.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.service.notification.*;
import java.util.*;

public class JobListener extends NotificationListenerService {
    static final String DRIVER = "com.lalamove.global.driver.sea";
    private final Map<String,String> seen = new LinkedHashMap<>();
    private long lastAttempt;
    @Override public void onListenerConnected() { Store.prefs(this).edit().putBoolean("connected",true).apply(); }
    @Override public void onListenerDisconnected() { Store.prefs(this).edit().putBoolean("connected",false).apply(); }
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (!DRIVER.equals(sbn.getPackageName()) || !Store.prefs(this).getBoolean("enabled",false)) return;
        Notification n = sbn.getNotification();
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        String title = String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TITLE,""));
        String text = String.valueOf(n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT,
                n.extras.getCharSequence(Notification.EXTRA_TEXT,"")));
        String fingerprint = title + "\n" + text;
        if (fingerprint.equals(seen.get(sbn.getKey()))) return;
        seen.put(sbn.getKey(),fingerprint);
        if (seen.size()>200) seen.remove(seen.keySet().iterator().next());
        JobParser.Job job = JobParser.parse(title,text);
        if (job == null) { Store.log(this,title,text,"ยังไม่รองรับรูปแบบนี้ · ไม่เปิดงาน"); return; }
        String rejection = Store.rejection(this,job);
        if (rejection != null) { Store.log(this,title,text,rejection); return; }
        if (n.contentIntent == null) { Store.log(this,title,text,"ตรงเงื่อนไข แต่ไม่มีคำสั่งเปิดงาน"); return; }
        long now = SystemClock.elapsedRealtime();
        int cooldown = Store.prefs(this).getInt("cooldown",15);
        if (lastAttempt != 0 && now-lastAttempt < cooldown*1000L) {
            Store.log(this,title,text,"ตรงเงื่อนไข · พักการเปิดงานซ้อน"); return;
        }
        lastAttempt = now;
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                ActivityOptions opts = ActivityOptions.makeBasic();
                opts.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                n.contentIntent.send(this,0,null,null,null,null,opts.toBundle());
            } else n.contentIntent.send();
            Store.log(this,title,text,"ส่งคำสั่งเปิดแล้ว · ยังไม่ยืนยันว่าหน้างานเปิด");
            offerManual(n.contentIntent,title);
        } catch (PendingIntent.CanceledException | SecurityException e) {
            Store.log(this,title,text,"เปิดไม่ได้ · " + e.getClass().getSimpleName());
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
