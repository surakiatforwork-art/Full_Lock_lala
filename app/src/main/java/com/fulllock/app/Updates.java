package com.fulllock.app;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Fetches public metadata only. Job contents are never uploaded. */
final class Updates {
    static final String METADATA="https://lalamove-helper.vercel.app/web/downloads/version.json";
    static final String DOWNLOAD="https://lalamove-helper.vercel.app/";
    static final int NOTIFICATION=42;
    private static final long INTERVAL=15*60*1000L;
    private static final AtomicBoolean checking=new AtomicBoolean();
    private static final Handler main=new Handler(Looper.getMainLooper());
    static int compare(String left,String right){
        String[] a=parts(left),b=parts(right);for(int i=0;i<3;i++){int n=Integer.compare(Integer.parseInt(a[i]),Integer.parseInt(b[i]));if(n!=0)return n;}return 0;
    }
    private static String[] parts(String version){if(version==null||!version.matches("v?[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}"))throw new IllegalArgumentException("Invalid release version");return version.replaceFirst("^v","").split("\\.");}
    static String parse(String json)throws Exception{
        JSONObject data=new JSONObject(json);String version=data.getString("version");parts(version);
        if(!data.getString("sha256").matches("[a-fA-F0-9]{64}")||!(data.get("bytes") instanceof Number)||data.getLong("bytes")<=0||data.getLong("bytes")>100*1024*1024)throw new IllegalArgumentException("Invalid release metadata");
        return version.replaceFirst("^v","");
    }
    static void check(Context source){
        Context c=source.getApplicationContext();announce(c);var prefs=Store.prefs(c);long now=System.currentTimeMillis(),last=prefs.getLong("updateLastCheck",0);
        if(now>=last&&now-last<INTERVAL||!checking.compareAndSet(false,true))return;
        prefs.edit().putLong("updateLastCheck",now).apply();
        new Thread(()->{String version=null;try{version=parse(fetch());}catch(Exception ignored){}finally{String found=version;main.post(()->{try{if(found!=null)accept(c,found);}finally{checking.set(false);}});}},"FullLock-update").start();
    }
    private static String fetch()throws Exception{
        HttpURLConnection connection=(HttpURLConnection)new URL(METADATA).openConnection();connection.setConnectTimeout(4000);connection.setReadTimeout(4000);connection.setInstanceFollowRedirects(false);connection.setUseCaches(false);connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("Cache-Control","no-cache");
        try{if(connection.getResponseCode()!=200)throw new IOException("Release unavailable");try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[1024];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>4096)throw new IOException("Metadata too large");out.write(buffer,0,n);}return out.toString(StandardCharsets.UTF_8.name());}}finally{connection.disconnect();}
    }
    static boolean blocked(Context c){try{return compare(Store.prefs(c).getString("updateAvailable",""),BuildConfig.VERSION_NAME)>0;}catch(IllegalArgumentException ignored){return false;}}
    static void accept(Context c,String version){
        parts(version);if(compare(version,BuildConfig.VERSION_NAME)<=0)return;
        var prefs=Store.prefs(c);String cached=prefs.getString("updateAvailable","");try{if(compare(cached,version)>0)version=cached;}catch(IllegalArgumentException ignored){}
        prefs.edit().putString("updateAvailable",version).putBoolean("enabled",false).apply();JobListener.refreshFloating();announce(c);
    }
    private static void announce(Context c){
        var prefs=Store.prefs(c);String version=prefs.getString("updateAvailable","");try{if(compare(version,BuildConfig.VERSION_NAME)<=0){c.getSystemService(NotificationManager.class).cancel(NOTIFICATION);return;}}catch(IllegalArgumentException ignored){return;}
        c.getSystemService(NotificationManager.class).cancel(41);
        if(prefs.getBoolean("enabled",false))prefs.edit().putBoolean("enabled",false).apply();
        if(MainActivity.showUpdate(version))return;
        try{if(compare(version,prefs.getString("updateAnnounced",""))<=0)return;}catch(IllegalArgumentException ignored){}
        boolean shown=false;
        if(!shown){NotificationManager manager=c.getSystemService(NotificationManager.class);if(!manager.areNotificationsEnabled())return;manager.createNotificationChannel(new NotificationChannel("updates","อัปเดตแอป",NotificationManager.IMPORTANCE_DEFAULT));if(manager.getNotificationChannel("updates").getImportance()==NotificationManager.IMPORTANCE_NONE)return;
            PendingIntent download=PendingIntent.getActivity(c,NOTIFICATION,new Intent(Intent.ACTION_VIEW,Uri.parse(DOWNLOAD)),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            try{manager.notify(NOTIFICATION,new Notification.Builder(c,"updates").setSmallIcon(R.drawable.ic_lock).setContentTitle("มี Full Lock รุ่นใหม่ พร้อมอัปเดต").setContentText("เวอร์ชัน "+version+" · แตะเพื่อดาวน์โหลด").setContentIntent(download).addAction(new Notification.Action.Builder(null,"ดาวน์โหลด",download).build()).setAutoCancel(true).build());shown=true;}catch(SecurityException ignored){}
        }
        if(shown)prefs.edit().putString("updateAnnounced",version).apply();
    }
}
