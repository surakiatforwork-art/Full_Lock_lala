package com.fulllock.app;
import android.app.*;
import android.content.*;
import java.util.*;

final class UpdateChecks {
    final Instrumentation test;int count;
    UpdateChecks(Instrumentation test){this.test=test;}
    void check(boolean ok,String why){if(!ok)throw new AssertionError(why);count++;}
    interface Work{void run()throws Exception;}
    void reject(Work w,String why)throws Exception{try{w.run();throw new AssertionError(why);}catch(IllegalArgumentException|org.json.JSONException expected){count++;}}
    String json(String version,String hash,String bytes){return "{\"version\":\""+version+"\",\"sha256\":\""+hash+"\",\"bytes\":"+bytes+"}";}
    boolean notification(){for(var n:test.getTargetContext().getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==Updates.NOTIFICATION)return true;return false;}
    int run()throws Exception{
        Context c=test.getTargetContext();var prefs=Store.prefs(c);Map<String,?> saved=prefs.getAll();MainActivity activity=null;
        try{
            check(Updates.compare("v0.1.10","0.1.9")>0,"numeric patch comparison");check(Updates.compare("0.2.0","0.1.999")>0,"minor comparison");check(Updates.compare("1.0.0","0.99.99")>0,"major comparison");check(Updates.compare("v0.1.22","0.1.22")==0,"prefix equality");
            String hash="a".repeat(64);check(Updates.parse(json("v999999.0.1",hash,"1234")).equals("999999.0.1"),"production metadata layout accepted");
            reject(()->Updates.parse(json("1.0.0-beta",hash,"1234")),"prerelease rejected");reject(()->Updates.parse(json("1.0.0","bad","1234")),"invalid checksum rejected");reject(()->Updates.parse(json("1.0.0",hash,"0")),"invalid size rejected");reject(()->Updates.parse("{}"),"missing metadata rejected");
            test.runOnMainSync(()->{prefs.edit().remove("updateAvailable").remove("updateAnnounced").putLong("updateLastCheck",System.currentTimeMillis()).putBoolean("permissionsReviewed",true).apply();Updates.accept(c,BuildConfig.VERSION_NAME);});check(!notification(),"installed version does not notify");
            test.runOnMainSync(()->{prefs.edit().putBoolean("enabled",true).apply();Updates.accept(c,"999999.0.1");});check(notification(),"newer version creates notification");check(Updates.blocked(c),"confirmed new version blocks old app");check(!prefs.getBoolean("enabled",true),"update requirement stops auto-open");check(!JobListener.openHistory(new org.json.JSONObject()),"update requirement blocks manual history open");
            test.runOnMainSync(()->Updates.accept(c,"999998.0.1"));check(prefs.getString("updateAvailable","").equals("999999.0.1"),"older metadata cannot lower cached requirement");
            test.runOnMainSync(()->c.getSystemService(NotificationManager.class).cancel(Updates.NOTIFICATION));
            activity=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));test.waitForIdleSync();
            MainActivity first=activity;java.lang.reflect.Field field=MainActivity.class.getDeclaredField("mandatoryUpdate");field.setAccessible(true);AlertDialog dialog=(AlertDialog)field.get(first);check(dialog!=null&&dialog.isShowing(),"cached requirement shows popup even after notification was announced");check(dialog.getButton(AlertDialog.BUTTON_NEGATIVE)==null||dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getVisibility()!=android.view.View.VISIBLE,"no later button");check(dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString().equals("ไปดาวน์โหลด"),"single download confirmation");
            test.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);test.waitForIdleSync();check(dialog.isShowing(),"back cannot dismiss mandatory update");
            new ProfileUiChecks(test).capture("ui-update-required.png");
            IntentFilter browserFilter=new IntentFilter(Intent.ACTION_VIEW);browserFilter.addDataScheme("https");browserFilter.addCategory(Intent.CATEGORY_BROWSABLE);Instrumentation.ActivityMonitor browser=test.addMonitor(browserFilter,new Instrumentation.ActivityResult(Activity.RESULT_OK,null),true);
            try{test.runOnMainSync(()->dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());check(browser.getHits()==1,"confirmation launches browser");check(Updates.DOWNLOAD.equals("https://lalamove-helper.vercel.app/"),"destination is website download page");check(dialog.isShowing(),"confirmation does not unlock old app");}finally{test.removeMonitor(browser);}
            test.runOnMainSync(first::finish);test.waitForIdleSync();activity=null;
            activity=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));test.waitForIdleSync();check(((AlertDialog)field.get(activity)).isShowing(),"relaunch while offline still requires update");MainActivity second=activity;test.runOnMainSync(second::finish);test.waitForIdleSync();activity=null;
            test.runOnMainSync(()->prefs.edit().putString("updateAvailable",BuildConfig.VERSION_NAME).apply());check(!Updates.blocked(c),"installed matching version clears gate");test.runOnMainSync(()->prefs.edit().putString("updateAvailable","broken").apply());check(!Updates.blocked(c),"invalid cached metadata never locks app");
        }finally{
            if(activity!=null){MainActivity a=activity;test.runOnMainSync(a::finish);test.waitForIdleSync();}
            test.runOnMainSync(()->{c.getSystemService(NotificationManager.class).cancel(Updates.NOTIFICATION);var edit=prefs.edit().clear();for(var entry:saved.entrySet()){Object v=entry.getValue();String k=entry.getKey();if(v instanceof String)edit.putString(k,(String)v);else if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);else if(v instanceof Integer)edit.putInt(k,(Integer)v);else if(v instanceof Long)edit.putLong(k,(Long)v);else if(v instanceof Float)edit.putFloat(k,(Float)v);}edit.apply();});
        }
        return count;
    }
}
