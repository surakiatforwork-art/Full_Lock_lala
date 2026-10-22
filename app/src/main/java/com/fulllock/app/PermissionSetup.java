package com.fulllock.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;

final class PermissionSetup {
    static boolean reading(Context c){
        String value=Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");
        if(value==null)return false;
        for(String entry:value.split(":")){ComponentName component=ComponentName.unflattenFromString(entry);if(component!=null&&component.getPackageName().equals(c.getPackageName()))return true;}return false;
    }
    static boolean battery(Context c){return c.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(c.getPackageName());}
    static boolean ready(Context c,int step){switch(step){case 0:return reading(c);case 1:return Settings.canDrawOverlays(c);case 2:return ForegroundWatcher.allowed(c);case 3:return c.getSystemService(NotificationManager.class).areNotificationsEnabled();default:return battery(c);}}
    static final String[] TITLES={"อ่านแจ้งเตือนงาน","แสดงปุ่มลอย","ซ่อนปุ่มเมื่อเปิด Lalamove","แจ้งเตือนจาก Full Lock","ทำงานเบื้องหลัง"};
    static final String[] DETAILS={"อ่านเฉพาะแจ้งเตือนงานจาก Lalamove","แสดงงานล่าสุดเหนือแอปอื่น","ใช้ข้อมูลการใช้งานเพื่อรู้ว่ากำลังเปิด Lalamove","แตะเปิดงานได้ หากงานยังไม่เปิดเอง","ตั้งค่าแบตเตอรี่ให้แอปทำงานต่อได้"};
    static void open(Activity activity,int step){
        Intent intent;
        switch(step){
            case 0:intent=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);break;
            case 1:intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+activity.getPackageName()));break;
            case 2:intent=new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);break;
            case 3:
                if(Build.VERSION.SDK_INT>=33&&activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);return;}
                intent=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,activity.getPackageName());break;
            default:intent=new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);break;
        }
        try{activity.startActivity(intent);}catch(ActivityNotFoundException e){activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+activity.getPackageName())));}
    }
}
