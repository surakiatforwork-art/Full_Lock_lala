package com.fulllock.app;
import android.app.*;
import android.app.usage.*;
import android.content.Context;
import android.os.*;
import java.util.*;

final class ForegroundWatcher {
    private final Context context;
    private final Runnable changed;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Set<String> driverActivities=new HashSet<>();
    private long cursor;
    private boolean running,hadPermission;
    ForegroundWatcher(Context c,Runnable changed){context=c;this.changed=changed;}
    static boolean allowed(Context c){return c.getSystemService(AppOpsManager.class).checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}
    private final Runnable tick=new Runnable(){public void run(){if(!running)return;refresh();changed.run();handler.postDelayed(this,750);}};
    void start(){if(running)return;running=true;cursor=0;handler.post(tick);}
    void stop(){running=false;handler.removeCallbacks(tick);driverActivities.clear();cursor=0;}
    void refresh(){
        boolean permission=allowed(context);
        if(!permission){driverActivities.clear();cursor=0;hadPermission=false;return;}
        if(!hadPermission)cursor=0;hadPermission=true;
        long now=System.currentTimeMillis();
        try {
            UsageEvents events=context.getSystemService(UsageStatsManager.class).queryEvents(cursor==0?now-86400000L:cursor-2000,now+1);
            if(events==null)return;
            UsageEvents.Event event=new UsageEvents.Event();
            while(events.hasNextEvent()){
                events.getNextEvent(event);
                if(!JobListener.DRIVER.equals(event.getPackageName()))continue;
                String activity=String.valueOf(event.getClassName());
                if(event.getEventType()==UsageEvents.Event.MOVE_TO_FOREGROUND)driverActivities.add(activity);
                else if(event.getEventType()==UsageEvents.Event.MOVE_TO_BACKGROUND || event.getEventType()==UsageEvents.Event.ACTIVITY_STOPPED)driverActivities.remove(activity);
            }
            cursor=now+1;
        }catch(SecurityException ignored){driverActivities.clear();cursor=0;}
    }
    boolean blocked(){return MainActivity.historyForeground || !driverActivities.isEmpty() || !context.getSystemService(PowerManager.class).isInteractive() || context.getSystemService(KeyguardManager.class).isKeyguardLocked();}
}
