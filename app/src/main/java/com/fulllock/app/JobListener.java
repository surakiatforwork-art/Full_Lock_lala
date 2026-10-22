package com.fulllock.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.service.notification.*;
import java.util.*;

public class JobListener extends NotificationListenerService {
    static volatile boolean connected;
    private static JobListener instance;
    private FloatingJobView floating;
    private StatusBarNotification latest;
    private JobParser.Job latestJob;
    private String latestText="", latestResult="";
    private boolean latestWorked;
    private final Map<String,String> seen = new LinkedHashMap<>();
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener changes=(p,key)->{
        if("enabled".equals(key)) { latest=null;latestJob=null;latestResult="";seen.clear();lastAttempt=0;syncFloating(); }
    };
    static final String DRIVER = "com.lalamove.global.driver.sea";
    private long lastAttempt;
    @Override public void onCreate() {
        super.onCreate();instance=this;
        floating=new FloatingJobView(this,this::openFloatingJob);
        Store.prefs(this).registerOnSharedPreferenceChangeListener(changes);
    }
    static void refreshFloating() { if(instance!=null)instance.syncFloating(); }
    static boolean floatingVisible(){return instance!=null && instance.floating!=null && instance.floating.visible();}
    private void syncFloating() {
        if(!connected || !Store.prefs(this).getBoolean("enabled",false) || !android.provider.Settings.canDrawOverlays(this)) {floating.hide();return;}
        floating.show();
        if(latest!=null){floating.latest(String.valueOf(latest.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE,"")),latestText,latestJob,latest.getNotification().contentIntent!=null);floating.result(latestWorked,latestResult);}
    }
    @Override public void onListenerConnected() { connected=true;syncFloating(); }
    @Override public void onListenerDisconnected() { connected=false;floating.hide();latest=null; }
    @Override public void onDestroy() {
        connected=false;Store.prefs(this).unregisterOnSharedPreferenceChangeListener(changes);
        floating.hide();latest=null;if(instance==this)instance=null;super.onDestroy();
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if(latest!=null && latest.getKey().equals(sbn.getKey()) && sbn.getPostTime()>=latest.getPostTime()) {
            latest=null;floating.unavailable();
        }
    }
    private void record(String title,String text,boolean worked,String reason) {
        Store.log(this,title,text,worked,reason);
        latestWorked=worked;latestResult=reason;
        if(Store.prefs(this).getBoolean("enabled",false))floating.result(worked,reason);
    }
    private void openFloatingJob() {
        if(!connected || !Store.prefs(this).getBoolean("enabled",false))return;
        if(latest==null){android.widget.Toast.makeText(this,"ยังไม่มีแจ้งเตือนงานที่เปิดได้",android.widget.Toast.LENGTH_SHORT).show();return;}
        try {
            StatusBarNotification target=null;
            StatusBarNotification[] activeNotifications=getActiveNotifications();
            if(activeNotifications==null){floating.result(false,"ตัวอ่านยังไม่เชื่อมต่อ");return;}
            for(StatusBarNotification active:activeNotifications)
                if(active.getKey().equals(latest.getKey()) && active.getPostTime()==latest.getPostTime()) {target=active;break;}
            if(target==null){latest=null;floating.unavailable();return;}
            PendingIntent intent=target.getNotification().contentIntent;
            String title=String.valueOf(target.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE,""));
            if(intent==null){record(title,latestText,false,"ไม่มีคำสั่งเปิดงาน");return;}
            sendOpen(intent);
            record(title,latestText,true,"แตะปุ่มลอย · ส่งคำสั่งเปิดแล้ว");
        }catch(PendingIntent.CanceledException e){floating.unavailable();latest=null;}
        catch(SecurityException e){floating.result(false,"ระบบไม่อนุญาตให้เปิดงาน");}
    }
    private void sendOpen(PendingIntent intent) throws PendingIntent.CanceledException {
        if(Build.VERSION.SDK_INT>=34){ActivityOptions opts=ActivityOptions.makeBasic();opts.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);intent.send(this,0,null,null,null,null,opts.toBundle());}
        else intent.send();
    }
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
        if (!Store.prefs(this).getBoolean("enabled",false)) { record(title,text,false,"ผู้ใช้ปิดการทำงานอยู่"); return; }
        JobParser.Job job = JobParser.parse(title,text);
        if (job == null && !text.equals(regular)) {
            job=JobParser.parse(title,regular);
            if(job!=null) text=regular;
        }
        latest=sbn;latestJob=job;latestText=text;latestWorked=false;latestResult="กำลังตรวจเงื่อนไข";syncFloating();
        if (job == null) { record(title,text,false,"อ่านข้อมูลงานไม่ได้: ไม่พบเส้นทาง ประเภทงาน หรือราคาในรูปแบบที่รองรับ"); return; }
        String rejection = Store.rejection(this,job);
        if (rejection != null) { record(title,text,false,rejection); return; }
        if (n.contentIntent == null) { record(title,text,false,"ตรงเงื่อนไข แต่แจ้งเตือนไม่มีคำสั่งเปิดหน้างาน"); return; }
        long now = SystemClock.elapsedRealtime();
        int cooldown = Store.prefs(this).getInt("cooldown",15);
        if (lastAttempt != 0 && now-lastAttempt < cooldown*1000L) {
            long remaining=(cooldown*1000L-(now-lastAttempt)+999)/1000;
            record(title,text,false,"ตรงเงื่อนไข แต่พักการเปิดงานซ้อนอยู่ เหลือประมาณ "+remaining+" วินาที (ตั้งไว้ "+cooldown+" วินาที)"); return;
        }
        lastAttempt = now;
        try {
            sendOpen(n.contentIntent);
            record(title,text,true,"ตรงเงื่อนไขทั้งหมด · ส่งคำสั่งเปิดแล้ว\nยังไม่ยืนยันว่าหน้างานเปิดสำเร็จ ผู้ใช้ปัดรับงานเอง");
            offerManual(n.contentIntent,title);
        } catch (PendingIntent.CanceledException | SecurityException e) {
            record(title,text,false,"ตรงเงื่อนไข แต่เปิดไม่ได้: " + (e instanceof PendingIntent.CanceledException?"คำสั่งในแจ้งเตือนถูกยกเลิกหรือหมดอายุ":"ระบบไม่อนุญาตให้เรียกคำสั่งเปิด")+" ("+e.getClass().getSimpleName()+")");
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
