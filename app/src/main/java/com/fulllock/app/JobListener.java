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
    private ForegroundWatcher foregroundWatcher;
    private final Map<String,String> seen = new LinkedHashMap<>();
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener changes=(p,key)->{
        if("floatingEnabled".equals(key)) { updateWatcher();syncFloating(); }
        if("themeColor".equals(key)||"invertText".equals(key))floating.refreshTheme();
    };
    static final String DRIVER = "com.lalamove.global.driver.sea";
    @Override public void onCreate() {
        super.onCreate();if(!AppIntegrity.trusted(this)){stopSelf();return;}instance=this;
        floating=new FloatingJobView(this,this::openFloatingJob);
        foregroundWatcher=new ForegroundWatcher(this,this::syncFloating);
        Store.prefs(this).registerOnSharedPreferenceChangeListener(changes);
    }
    static void refreshFloating() { if(instance!=null)instance.syncFloating(); }
    static boolean floatingVisible(){return instance!=null && instance.floating!=null && instance.floating.visible();}
    private void syncFloating() {
        if(!connected || !Store.prefs(this).getBoolean("floatingEnabled",true) || !android.provider.Settings.canDrawOverlays(this) || foregroundWatcher.blocked()) {floating.hide();return;}
        floating.show();
    }
    private void updateWatcher(){if(connected && Store.prefs(this).getBoolean("floatingEnabled",true)){foregroundWatcher.refresh();foregroundWatcher.start();}else foregroundWatcher.stop();}
    @Override public void onListenerConnected() { if(floating==null)return;connected=true;updateWatcher();syncFloating(); }
    @Override public void onListenerDisconnected() { connected=false;if(foregroundWatcher!=null)foregroundWatcher.stop();if(floating!=null)floating.hide(); }
    @Override public void onDestroy() {
        connected=false;Store.prefs(this).unregisterOnSharedPreferenceChangeListener(changes);
        if(foregroundWatcher!=null)foregroundWatcher.stop();if(floating!=null)floating.hide();if(instance==this)instance=null;super.onDestroy();
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if(floating==null)return;
        seen.remove(sbn.getKey());
        floating.removed(sbn.getKey(),sbn.getPostTime());
    }
    private void record(String title,String text,boolean worked,String reason) {
        Store.log(this,title,text,worked,reason);
    }
    private void openFloatingJob(FloatingJobs.Item item) {
        if(!connected)return;
        try {
            StatusBarNotification target=null;
            StatusBarNotification[] activeNotifications=getActiveNotifications();
            if(activeNotifications==null){toast("ยังอ่านงานไม่ได้ ลองใหม่อีกครั้ง");return;}
            for(StatusBarNotification active:activeNotifications)
                if(active.getKey().equals(item.key) && active.getPostTime()==item.postTime) {target=active;break;}
            if(target==null){floating.removed(item.key,item.postTime);toast("แจ้งเตือนนี้ไม่อยู่แล้ว");return;}
            PendingIntent intent=target.getNotification().contentIntent;
            String title=String.valueOf(target.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE,""));
            if(intent==null){record(title,item.text,false,"เปิดงานนี้ไม่ได้");toast("เปิดงานนี้ไม่ได้");return;}
            sendOpen(intent);
            record(title,item.text,true,"เปิดจากปุ่มลอยแล้ว");
        }catch(PendingIntent.CanceledException e){floating.removed(item.key,item.postTime);toast("งานนี้หมดอายุแล้ว");}
        catch(SecurityException e){toast("ระบบไม่อนุญาตให้เปิดงาน");}
    }
    private void toast(String message){android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_SHORT).show();}
    private void sendOpen(PendingIntent intent) throws PendingIntent.CanceledException {
        if(Build.VERSION.SDK_INT>=34){ActivityOptions opts=ActivityOptions.makeBasic();opts.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);intent.send(this,0,null,null,null,null,opts.toBundle());}
        else intent.send();
    }
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if(floating==null)return;
        if (!DRIVER.equals(sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        boolean ongoing=(n.flags & Notification.FLAG_ONGOING_EVENT)!=0, summary=(n.flags & Notification.FLAG_GROUP_SUMMARY)!=0;
        if(ongoing || summary)return;
        String title = String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TITLE,""));
        String regular = String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TEXT,""));
        CharSequence big = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        String text = big == null || big.toString().isBlank() ? regular : big.toString();
        JobParser.Job job=JobParser.notificationJob(title,text,ongoing,summary);
        if(job==null && !text.equals(regular)){job=JobParser.notificationJob(title,regular,ongoing,summary);if(job!=null)text=regular;}
        // Only real job offers enter history, counters, filtering, or the floating list.
        if(job==null)return;
        String fingerprint = title + "\n" + text;
        if (fingerprint.equals(seen.get(sbn.getKey()))) return;
        seen.put(sbn.getKey(),fingerprint);
        if (seen.size()>200) seen.remove(seen.keySet().iterator().next());
        Store.prefs(this).edit().putLong("lastReceived",System.currentTimeMillis()).apply();
        String rejection=Store.rejection(this,job);
        syncFloating();floating.offer(new FloatingJobs.Item(sbn.getKey(),sbn.getPostTime(),title,text,job,rejection==null,n.contentIntent!=null));
        if (rejection != null) { record(title,text,false,rejection); return; }
        if(!Store.prefs(this).getBoolean("enabled",false)){record(title,text,false,"ตรงเงื่อนไข / ยังไม่เปิดอัตโนมัติ");return;}
        if (n.contentIntent == null) { record(title,text,false,"ตรงเงื่อนไข / เปิดงานนี้ไม่ได้"); return; }
        try {
            sendOpen(n.contentIntent);
            record(title,text,true,"ตรงเงื่อนไข / เรียกเปิดงานแล้ว");
            offerManual(n.contentIntent,title);
        } catch (PendingIntent.CanceledException | SecurityException e) {
            record(title,text,false,e instanceof PendingIntent.CanceledException?"งานหมดอายุแล้ว":"ระบบไม่อนุญาตให้เปิดงาน");
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
