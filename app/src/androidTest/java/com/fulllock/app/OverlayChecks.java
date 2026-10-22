package com.fulllock.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.service.notification.StatusBarNotification;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.Map;

/** Emulator integration checks; run with overlay and notification access granted. */
public class OverlayChecks extends Instrumentation {
    private int checks;
    private boolean preview;
    private void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
    private Object field(Object target,String name) throws Exception {
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    private JobListener listener() throws Exception {
        Field f=JobListener.class.getDeclaredField("instance");f.setAccessible(true);return (JobListener)f.get(null);
    }
    private StatusBarNotification sample(int id,String title,String text) {
        Notification n=new Notification.Builder(getTargetContext(),"test").setSmallIcon(com.fulllock.app.R.drawable.ic_lock).setContentTitle(title).setContentText(text).build();
        return new StatusBarNotification(JobListener.DRIVER,JobListener.DRIVER,id,null,10000,0,0,n,android.os.Process.myUserHandle(),System.currentTimeMillis());
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);preview=args!=null && "true".equals(args.getString("preview"));start();}
    @Override public void onStart(){
        Bundle result=new Bundle();Map<String,?>[] saved=new Map[1];int resultCode=Activity.RESULT_OK;
        try {
            for(int i=0;i<80 && !JobListener.connected;i++)Thread.sleep(250);
            runOnMainSync(()->{
                try {
                    JobListener service=listener();check(service!=null && JobListener.connected,"listener connected");
                    var p=Store.prefs(getTargetContext());saved[0]=p.getAll();
                    p.edit().putBoolean("enabled",false).apply();check(!JobListener.floatingVisible(),"hidden while stopped");
                    p.edit().putBoolean("enabled",true).putBoolean("price",true).putString("min","9999").putString("max","10000").putBoolean("originOn",false).putBoolean("destOn",false).putInt("type",0).putBoolean("bulk",false).apply();
                    check(JobListener.floatingVisible(),"visible when started");
                    StatusBarNotification first=sample(9001,"[ขนส่งสินค้า] บางคูรัด > หัวหมาก","ขนส่งทันที ฿276.00 + ฿10.00 รับงานขนส่ง!");
                    service.onNotificationPosted(first);
                    Object floating=field(service,"floating");
                    check(((TextView)field(floating,"heading")).getText().toString().contains("หัวหมาก"),"first title");
                    String detail=((TextView)field(floating,"detail")).getText().toString();
                    check(detail.contains("276.00")&&detail.contains("10.00")&&detail.contains("286.00"),"base tip total");
                    check(((TextView)field(floating,"status")).getText().toString().contains("ราคาต่ำไป"),"filter reason shown");
                    StatusBarNotification second=sample(9002,"[ขนส่งสินค้า] คลองพระอุดม > ปลายทางล่าสุด","ขนส่งทันที ฿129.00 + ฿18.00 รับงานขนส่ง!");
                    service.onNotificationPosted(second);
                    check(((TextView)field(floating,"heading")).getText().toString().contains("ปลายทางล่าสุด"),"latest replaces previous");
                    check(!((TextView)field(floating,"detail")).getText().toString().contains("276.00"),"previous price removed");
                    service.onNotificationRemoved(first);
                    check(((TextView)field(floating,"heading")).getText().toString().contains("ปลายทางล่าสุด"),"removing older item keeps latest");
                    service.onNotificationRemoved(second);
                    check(((TextView)field(floating,"status")).getText().toString().contains("ถูกลบ"),"removed latest cannot open");
                    p.edit().putBoolean("enabled",false).apply();check(!JobListener.floatingVisible(),"stop removes window");
                    p.edit().putBoolean("enabled",true).apply();check(JobListener.floatingVisible(),"restart shows window");
                    check(((TextView)field(floating,"heading")).getText().toString().contains("รอแจ้งเตือน"),"restart clears stale job");
                    p.edit().putBoolean("enabled",false).apply();
                    int[] taps={0};FloatingJobView touch=new FloatingJobView(getTargetContext(),()->taps[0]++);touch.show();
                    android.view.View touchCard=(android.view.View)field(touch,"card");
                    long when=android.os.SystemClock.uptimeMillis();
                    android.view.MotionEvent down=android.view.MotionEvent.obtain(when,when,0,30,30,0),up=android.view.MotionEvent.obtain(when,when+20,1,30,30,0);
                    touchCard.dispatchTouchEvent(down);touchCard.dispatchTouchEvent(up);down.recycle();up.recycle();check(taps[0]==1,"tap invokes open callback");
                    down=android.view.MotionEvent.obtain(when,when,0,30,30,0);
                    android.view.MotionEvent move=android.view.MotionEvent.obtain(when,when+20,2,160,160,0);up=android.view.MotionEvent.obtain(when,when+40,1,160,160,0);
                    touchCard.dispatchTouchEvent(down);touchCard.dispatchTouchEvent(move);touchCard.dispatchTouchEvent(up);down.recycle();move.recycle();up.recycle();
                    check(taps[0]==1,"drag does not open job");touch.hide();
                }catch(Exception e){throw new RuntimeException(e);}
            });
            if(preview){
                runOnMainSync(()->{try{Store.prefs(getTargetContext()).edit().putBoolean("enabled",true).apply();listener().onNotificationPosted(sample(9003,"[ขนส่งสินค้า] บางคูรัด > หัวหมาก","ขนส่งทันที ฿276.00 + ฿10.00 รับงานขนส่ง!"));}catch(Exception e){throw new RuntimeException(e);}});
                waitForIdleSync();Thread.sleep(800);
                android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();
                java.io.File output=new java.io.File(getTargetContext().getExternalFilesDir("validation"),"fulllock-floating.png");
                try(java.io.FileOutputStream stream=new java.io.FileOutputStream(output)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
            }
            result.putString("stream","OverlayChecks: "+checks+" checks passed\n");
        }catch(Throwable e){result.putString("stream","OverlayChecks failed: "+e+"\n");resultCode=Activity.RESULT_CANCELED;}
        finally{
            if(saved[0]!=null)runOnMainSync(()->{
                var edit=Store.prefs(getTargetContext()).edit().clear();
                for(var entry:saved[0].entrySet()){
                    Object value=entry.getValue();String key=entry.getKey();
                    if(value instanceof String)edit.putString(key,(String)value);
                    else if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);
                    else if(value instanceof Integer)edit.putInt(key,(Integer)value);
                    else if(value instanceof Long)edit.putLong(key,(Long)value);
                    else if(value instanceof Float)edit.putFloat(key,(Float)value);
                }edit.apply();JobListener.refreshFloating();
            });
        }
        finish(resultCode,result);
    }
}
