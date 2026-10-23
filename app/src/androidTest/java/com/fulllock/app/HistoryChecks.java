package com.fulllock.app;
import android.app.*;
import android.content.*;
import android.os.*;
import android.service.notification.StatusBarNotification;
import org.json.*;
import java.lang.reflect.*;

final class HistoryChecks {
    final Instrumentation test; int count; volatile int sends;
    HistoryChecks(Instrumentation test){this.test=test;}
    void check(boolean ok,String message){if(!ok)throw new AssertionError(message);count++;}
    StatusBarNotification notification(PendingIntent intent,long time,String title){Notification n=new Notification.Builder(test.getTargetContext(),"probe").setSmallIcon(R.drawable.ic_lock).setContentTitle(title).setContentText("ขนส่งทันที ฿156.00 รับงานขนส่ง!").setContentIntent(intent).build();return new StatusBarNotification(JobListener.DRIVER,JobListener.DRIVER,7001,null,10000,0,0,n,android.os.Process.myUserHandle(),time);}
    int run()throws Exception {
        Context c=test.getTargetContext();Field f=JobListener.class.getDeclaredField("instance");f.setAccessible(true);JobListener service=(JobListener)f.get(null);
        Method open=JobListener.class.getDeclaredMethod("openHistory",JSONObject.class,StatusBarNotification[].class);open.setAccessible(true);
        BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){sends++;}};c.registerReceiver(receiver,new IntentFilter("com.fulllock.app.HISTORY_PROBE"),Context.RECEIVER_NOT_EXPORTED);
        PendingIntent intent=PendingIntent.getBroadcast(c,7001,new Intent("com.fulllock.app.HISTORY_PROBE").setPackage(c.getPackageName()),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        try{
            String title="[ขนส่งสินค้า] A > B";StatusBarNotification n=notification(intent,123456,title);
            JSONObject entry=new JSONObject().put("notificationKey",n.getKey()).put("notificationTime",n.getPostTime()).put("title",title).put("text","ขนส่งทันที ฿156.00 รับงานขนส่ง!");
            check((boolean)open.invoke(service,entry,new StatusBarNotification[]{n}),"exact active notification opens");
            for(int i=0;i<30&&sends==0;i++)Thread.sleep(100);check(sends==1,"exact pending intent sent");
            check(!(boolean)open.invoke(service,entry,new StatusBarNotification[0]),"removed notification cannot open");
            check(!(boolean)open.invoke(service,entry,new StatusBarNotification[]{notification(intent,123457,title)}),"reused key cannot open new job");
            check(!(boolean)open.invoke(service,entry,new StatusBarNotification[]{notification(intent,123456,"changed")}),"changed content cannot open");
            check(!(boolean)open.invoke(service,new JSONObject().put("title",title),new StatusBarNotification[]{n}),"legacy text-only history cannot open");
            intent.cancel();check(!(boolean)open.invoke(service,entry,new StatusBarNotification[]{n}),"cancelled pending intent fails safely");
            check(sends==1,"failed attempts never send another job");
        }finally{intent.cancel();c.unregisterReceiver(receiver);}
        return count;
    }
}
