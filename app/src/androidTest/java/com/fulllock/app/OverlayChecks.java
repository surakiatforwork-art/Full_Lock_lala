package com.fulllock.app;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.*;

/** Emulator-only checks. No tests or synthetic notifications are shipped in the app APK. */
public class OverlayChecks extends Instrumentation {
    private int checks;
    private boolean preview,extraOnly,profilesOnly,profileModelOnly;
    private FloatingJobView view;
    private Map<String,?> saved;
    private BroadcastReceiver probe;
    private int sends;
    private void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
    private Object field(Object target,String name){try{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(Exception e){throw new RuntimeException(e);}}
    private JobListener service(){try{Field f=JobListener.class.getDeclaredField("instance");f.setAccessible(true);return (JobListener)f.get(null);}catch(Exception e){throw new RuntimeException(e);}}
    private void main(Runnable work){Throwable[] failure={null};runOnMainSync(()->{try{work.run();}catch(Throwable e){failure[0]=e;}});if(failure[0]!=null)throw new RuntimeException(failure[0]);}
    private FloatingJobs.Item item(int n){String title="[ขนส่งสินค้า] ต้นทาง "+n+" > ปลายทาง "+n;String text=(n%3==0?"งานล่วงหน้า":"ขนส่งทันที")+" ฿276.00 + ฿10.00 รับงานขนส่ง!";return new FloatingJobs.Item("key"+n,n,title,text,JobParser.parse(title,text),n%2==0,true);}
    private LinearLayout rows(){return (LinearLayout)field(view,"rows");}
    private void tap(float x,float y){long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,0,x,y,0),up=MotionEvent.obtain(now,now+60,1,x,y,0);getUiAutomation().injectInputEvent(down,true);getUiAutomation().injectInputEvent(up,true);down.recycle();up.recycle();waitForIdleSync();}
    private void tap(View target){int[] point=new int[2];main(()->{target.getLocationOnScreen(point);point[0]+=target.getWidth()/2;point[1]+=target.getHeight()/2;});tap(point[0],point[1]);}
    private android.service.notification.StatusBarNotification notification(int id,String title,String text,int flags,PendingIntent intent){Notification n=new Notification.Builder(getTargetContext(),"probe").setSmallIcon(com.fulllock.app.R.drawable.ic_lock).setContentTitle(title).setContentText(text).setContentIntent(intent).build();n.flags|=flags;return new android.service.notification.StatusBarNotification(JobListener.DRIVER,JobListener.DRIVER,id,null,10000,0,0,n,android.os.Process.myUserHandle(),System.currentTimeMillis());}
    @android.annotation.TargetApi(33)
    private void monitorChecks() throws Exception {
        main(()->{
            var c=getTargetContext();var p=Store.prefs(c);view.hide();MainActivity.foreground=false;
            p.edit().putBoolean("enabled",false).putBoolean("floatingEnabled",true).putBoolean("price",true).putString("min","100").putString("max","2000").putBoolean("originOn",false).putBoolean("destOn",false).putInt("type",0).putBoolean("bulk",false).remove("history").apply();
            check(JobListener.floatingVisible(),"FAB works with auto-open off");
            probe=new BroadcastReceiver(){public void onReceive(Context context,Intent intent){sends++;}};
            c.registerReceiver(probe,new IntentFilter("com.fulllock.app.OPEN_PROBE"),Context.RECEIVER_NOT_EXPORTED);
            PendingIntent intent=PendingIntent.getBroadcast(c,6001,new Intent("com.fulllock.app.OPEN_PROBE").setPackage(c.getPackageName()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            service().onNotificationPosted(notification(6001,"[ขนส่งสินค้า] A > B","ขนส่งทันที ฿276.00 + ฿10.00 รับงานขนส่ง!",0,intent));
            FloatingJobView live=(FloatingJobView)field(service(),"floating");
            check(live.model.items.get(0).matches,"matched tick calculated with auto-open off");check(live.model.unread==1,"job counted with auto-open off");
            try{check(new org.json.JSONArray(p.getString("history","[]")).getJSONObject(0).getString("result").contains("ยังไม่เปิดอัตโนมัติ"),"history explains auto-off");}catch(Exception e){throw new RuntimeException(e);}
            String history=p.getString("history","[]");long received=p.getLong("lastReceived",0);
            service().onNotificationPosted(notification(6002,"ประกาศ","รับโบนัส ฿500",0,null));
            service().onNotificationPosted(notification(6003,"GPS กำลังทำงาน","กำลังติดตามตำแหน่ง",Notification.FLAG_ONGOING_EVENT,null));
            service().onNotificationPosted(notification(6004,"[ขนส่งสินค้า] A > B","ขนส่งทันที ฿156.00 รับงานขนส่ง!",Notification.FLAG_ONGOING_EVENT,intent));
            service().onNotificationPosted(notification(6005,"[ขนส่งสินค้า] A > B","ขนส่งทันที ฿156.00 รับงานขนส่ง!",Notification.FLAG_GROUP_SUMMARY,intent));
            check(live.model.unread==1,"non-jobs not counted");check(history.equals(p.getString("history","[]")),"non-jobs not stored");check(received==p.getLong("lastReceived",0),"non-jobs do not change last-job time");
            service().onNotificationPosted(notification(6006,"[ขนส่งสินค้า] A > B","ขนส่งทันที ฿50.00 รับงานขนส่ง!",0,intent));
            check(!live.model.items.get(0).matches,"price calculated when auto-off");
            p.edit().putBoolean("floatingEnabled",false).apply();check(!JobListener.floatingVisible(),"FAB switch hides independently");
            service().onNotificationPosted(notification(6007,"[ขนส่งสินค้า] C > D","ขนส่งทันที ฿180.00 รับงานขนส่ง!",0,intent));
            check(live.model.unread==3,"reads jobs with FAB and auto-open off");
            p.edit().putBoolean("floatingEnabled",true).apply();check(JobListener.floatingVisible()&&live.model.unread==3,"FAB restores unread count");
            check(sends==0,"auto-off sends nothing");
            p.edit().putBoolean("enabled",true).apply();check(live.model.unread==3,"auto switch preserves unread");
            service().onNotificationPosted(notification(6008,"[ขนส่งสินค้า] E > F","ขนส่งทันที ฿190.00 รับงานขนส่ง!",0,intent));
            service().onNotificationPosted(notification(6010,"[ขนส่งสินค้า] I > J","ขนส่งทันที ฿191.00 รับงานขนส่ง!",0,intent));
        });
        waitForIdleSync();Thread.sleep(400);
        main(()->{check(sends==2,"matching jobs open immediately without cooldown");Store.prefs(getTargetContext()).edit().putBoolean("enabled",false).apply();
            PendingIntent intent=PendingIntent.getBroadcast(getTargetContext(),6001,new Intent("com.fulllock.app.OPEN_PROBE").setPackage(getTargetContext().getPackageName()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            service().onNotificationPosted(notification(6009,"[ขนส่งสินค้า] G > H","ขนส่งทันที ฿195.00 รับงานขนส่ง!",0,intent));
        });waitForIdleSync();Thread.sleep(400);
        main(()->{check(sends==2,"stopping auto prevents subsequent open");
            android.service.notification.StatusBarNotification target=null;
            for(var n:getTargetContext().getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==41)target=n;
            check(target!=null,"manual fallback notification active");
            try{java.lang.reflect.Method method=JobListener.class.getDeclaredMethod("openFloatingJob",FloatingJobs.Item.class);method.setAccessible(true);method.invoke(service(),new FloatingJobs.Item(target.getKey(),target.getPostTime(),"manual","manual",null,true,true));}catch(Exception e){throw new RuntimeException(e);}
        });waitForIdleSync();Thread.sleep(400);main(()->check(sends==3,"manual open works with auto-off"));
    }
    private void capture(String name)throws Exception {
        if(!preview)return;waitForIdleSync();Thread.sleep(450);
        android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();
        java.io.File output=new java.io.File(getTargetContext().getExternalFilesDir("validation"),name);
        try(java.io.FileOutputStream stream=new java.io.FileOutputStream(output)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
    }
    private void shell(String command)throws Exception {
        try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);java.io.InputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){byte[] buffer=new byte[1024];while(input.read(buffer)!=-1){} }
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);preview=args!=null&&"true".equals(args.getString("preview"));extraOnly=args!=null&&"true".equals(args.getString("extraOnly"));profilesOnly=args!=null&&"true".equals(args.getString("profilesOnly"));profileModelOnly=args!=null&&"true".equals(args.getString("profileModelOnly"));start();}
    private void runProfiles(){Bundle result=new Bundle();int code=Activity.RESULT_OK;Map<String,?> saved=Store.prefs(getTargetContext()).getAll();try{int model=new ProfileChecks(this).run();int ui=profileModelOnly?0:new ProfileUiChecks(this).run();result.putString("stream","ProfileChecks: "+model+" checks passed\nProfileUiChecks: "+ui+" checks passed\n");}catch(Throwable e){code=Activity.RESULT_CANCELED;result.putString("stream","Profile checks failed: "+e+"\n");}finally{runOnMainSync(()->{var edit=Store.prefs(getTargetContext()).edit().clear();for(var entry:saved.entrySet()){Object value=entry.getValue();if(value instanceof String)edit.putString(entry.getKey(),(String)value);else if(value instanceof Boolean)edit.putBoolean(entry.getKey(),(Boolean)value);else if(value instanceof Integer)edit.putInt(entry.getKey(),(Integer)value);else if(value instanceof Long)edit.putLong(entry.getKey(),(Long)value);else if(value instanceof Float)edit.putFloat(entry.getKey(),(Float)value);}edit.apply();});}finish(code,result);}
    @Override public void onStart(){
        if(profilesOnly){runProfiles();return;}
        Bundle result=new Bundle();int code=Activity.RESULT_OK;List<String> selected=new ArrayList<>();
        try{
            main(()->android.service.notification.NotificationListenerService.requestRebind(new ComponentName(getTargetContext(),JobListener.class)));
            // Instrumentation force-stops its target. Reset the emulator's stale listener binding.
            if(!JobListener.connected){shell("cmd notification disallow_listener com.fulllock.app/com.fulllock.app.JobListener");shell("cmd notification allow_listener com.fulllock.app/com.fulllock.app.JobListener");}
            for(int i=0;i<80&&!JobListener.connected;i++)Thread.sleep(250);
            main(()->{
                check(service()!=null&&JobListener.connected,"service connected");var p=Store.prefs(getTargetContext());saved=p.getAll();p.edit().putBoolean("showUnavailableFloating",true).putInt("fee",20).putInt("transitionMs",200).putBoolean("enabled",false).putBoolean("floatingEnabled",false).putInt("floatX",8).putInt("floatY",260).apply();
                check(!JobListener.floatingVisible(),"FAB switch hides service window");
                view=new FloatingJobView(getTargetContext(),entry->selected.add(entry.id));view.show();check(view.visible(),"FAB visible");
                WindowManager.LayoutParams fp=(WindowManager.LayoutParams)field(view,"fabParams");check(fp.width<=Math.round(80*getTargetContext().getResources().getDisplayMetrics().density),"compact 80dp mascot button");
                for(int i=1;i<=5;i++)view.offer(item(i));
                check(view.model.unread==5,"five unread");check(rows().getChildCount()==1,"automatic preview one item");
                check(view.model.mode==FloatingJobs.Mode.PREVIEW,"preview mode");
                LinearLayout previewRow=(LinearLayout)rows().getChildAt(0),previewRoute=(LinearLayout)previewRow.getChildAt(1);check(((TextView)previewRoute.getChildAt(1)).getText().toString().equals("฿228.80"),"new job preview shows net including tip fee");
            });
            if(!extraOnly){
            Thread.sleep(3400);
            main(()->{check(view.model.mode==FloatingJobs.Mode.COLLAPSED,"preview closes after 3 seconds");check(view.model.unread==5,"preview does not mark read");view.toggle();check(view.model.unread==0,"opening FAB marks read");check(rows().getChildCount()==5,"pinned list five items");view.offer(item(6));check(view.model.mode==FloatingJobs.Mode.PINNED,"new item preserves pinned");check(view.model.items.get(0).id.equals(item(6).id),"newest first");check(rows().getChildCount()==6,"six jobs retained");check(view.model.unread==0,"visible new item read");
                LinearLayout row=(LinearLayout)rows().getChildAt(0);LinearLayout route=(LinearLayout)row.getChildAt(1);
                check(((TextView)route.getChildAt(0)).getTypeface().isBold(),"route bold");check(((TextView)row.getChildAt(0)).getTypeface().isBold(),"type bold");check(((TextView)row.getChildAt(2)).getTypeface().isBold(),"fare tip bold");check(route.getChildCount()==3&&((TextView)route.getChildAt(2)).getText().toString().equals("✓"),"matching checkmark follows net");check(row.getChildCount()==3,"no rejection reason in panel");
                TextView net=(TextView)route.getChildAt(1);check(net.getText().toString().equals("฿228.80")&&net.getTypeface().isBold(),"list net amount bold and correct");check(net.getTextSize()>((TextView)row.getChildAt(2)).getTextSize(),"net larger than fare and tip");
            });
            capture("fulllock-fab-left.png");Thread.sleep(3200);
            View[] targetRow=new View[1];main(()->targetRow[0]=rows().getChildAt(0));tap(targetRow[0]);main(()->{check(selected.size()==1,"real tap opens row callback");check(view.model.mode==FloatingJobs.Mode.PINNED,"inside tap does not collapse panel");selected.clear();});
            tap(getTargetContext().getResources().getDisplayMetrics().widthPixels-20,getTargetContext().getResources().getDisplayMetrics().heightPixels/2);
            main(()->{check(view.model.mode==FloatingJobs.Mode.COLLAPSED,"outside tap collapses panel");view.toggle();});
            main(()->{
                check(view.model.mode==FloatingJobs.Mode.PINNED,"manual list does not auto-close");
                rows().getChildAt(4).performClick();check(selected.size()==1&&selected.get(0).equals(item(2).id),"click selected row not latest");
                view.removed("key2",2);rows().getChildAt(4).performClick();check(selected.size()==1,"removed notification cannot open");
                WindowManager.LayoutParams fp=(WindowManager.LayoutParams)field(view,"fabParams"),pp=(WindowManager.LayoutParams)field(view,"panelParams");check(pp.x>fp.x,"left FAB expands right");
                View fab=(View)field(view,"fab");long now=SystemClock.uptimeMillis();int width=getTargetContext().getResources().getDisplayMetrics().widthPixels;
                MotionEvent d=MotionEvent.obtain(now,now,0,10,10,0),m=MotionEvent.obtain(now,now+20,2,width,60,0),u=MotionEvent.obtain(now,now+40,1,width,60,0);fab.dispatchTouchEvent(d);fab.dispatchTouchEvent(m);fab.dispatchTouchEvent(u);d.recycle();m.recycle();u.recycle();
                check(selected.size()==1,"drag does not open item");check(pp.x<fp.x,"right FAB expands left");check(view.model.mode==FloatingJobs.Mode.PINNED,"drag preserves expanded panel");
            });
            capture("fulllock-fab-right.png");
            main(()->{view.toggle();view.offer(item(7));check(view.model.unread==1,"new unread after closing");view.hide();check(!view.visible(),"hide removes windows");check(view.model.unread==1,"hide retains unread");view.show();check(view.model.mode==FloatingJobs.Mode.COLLAPSED,"show returns compact");});
            capture("fulllock-fab-counter.png");
            }
            main(()->{for(int i=11;i<=21;i++)view.offer(item(i));view.toggle();check(rows().getChildCount()==10,"ten retained rows");});
            waitForIdleSync();Thread.sleep(600);
            main(()->{LinearLayout panel=(LinearLayout)field(view,"panel");ScrollView scroll=(ScrollView)panel.getChildAt(0);check(panel.getChildCount()==1,"no header or close button");check(((android.graphics.drawable.ColorDrawable)panel.getBackground()).getColor()==android.graphics.Color.TRANSPARENT,"transparent panel");float ratio=scroll.getHeight()/(float)(rows().getChildAt(0).getHeight()+Math.round(4*getTargetContext().getResources().getDisplayMetrics().density));check(ratio>3.4f&&ratio<3.6f,"3.5-row viewport");LinearLayout first=(LinearLayout)rows().getChildAt(0);check(((android.graphics.drawable.ColorDrawable)first.getChildAt(0).getBackground()).getColor()==AppTheme.jobColor(getTargetContext(),item(21).job),"advance color complementary");LinearLayout route=(LinearLayout)((LinearLayout)rows().getChildAt(1)).getChildAt(1);check(((TextView)route.getChildAt(2)).getTextSize()/getTargetContext().getResources().getDisplayMetrics().scaledDensity>=28,"large matching mark");scroll.fullScroll(View.FOCUS_DOWN);});
            waitForIdleSync();Thread.sleep(500);main(()->{check(((ScrollView)field(view,"scroll")).getScrollY()>0,"ten rows scroll");view.offer(item(22));check(((ScrollView)field(view,"scroll")).getScrollY()==0,"new job scrolls to newest");});
            main(()->{view.removed("key22",22);check(rows().getChildCount()==10,"expired rows shown by default");check(rows().getChildAt(0).getAlpha()<1f,"expired row faded");Store.prefs(getTargetContext()).edit().putBoolean("showUnavailableFloating",false).apply();view.refreshTheme();check(rows().getChildCount()==9,"toggle hides expired row");check(view.model.items.size()==10,"toggle retains original history");Store.prefs(getTargetContext()).edit().putBoolean("showUnavailableFloating",true).apply();view.refreshTheme();check(rows().getChildCount()==10,"toggle restores expired row");});
            capture("fulllock-ten-jobs.png");
            main(()->{view.toggle();check(field(view,"panel")!=null,"collapse retains panel during transition");});Thread.sleep(650);
            main(()->{check(field(view,"panel")==null,"collapse removes panel after transition");view.toggle();view.toggle();view.toggle();});Thread.sleep(650);
            main(()->check(view.model.mode==FloatingJobs.Mode.PINNED&&field(view,"panel")!=null,"rapid reverse preserves expanded panel"));
            if(!extraOnly){
            main(()->{
                view.hide();MainActivity.foreground=true;MainActivity.historyForeground=false;Store.prefs(getTargetContext()).edit().putBoolean("enabled",false).putBoolean("floatingEnabled",true).apply();JobListener.refreshFloating();check(JobListener.floatingVisible(),"shown in Full Lock filters");MainActivity.historyForeground=true;JobListener.refreshFloating();check(!JobListener.floatingVisible(),"hidden in Full Lock history");MainActivity.historyForeground=false;
                MainActivity.foreground=false;JobListener.refreshFloating();check(JobListener.floatingVisible(),"shown outside Full Lock");
                Object watcher=field(service(),"foregroundWatcher");Set<String> driver=(Set<String>)field(watcher,"driverActivities");driver.add("driver-foreground-test");JobListener.refreshFloating();check(!JobListener.floatingVisible(),"hidden with Driver foreground state");driver.clear();JobListener.refreshFloating();check(JobListener.floatingVisible(),"shown after leaving Driver");
                Store.prefs(getTargetContext()).edit().putBoolean("enabled",false).apply();check(JobListener.floatingVisible(),"auto-off keeps FAB");
            });
            monitorChecks();
            int filterCount=new FilterChecks(getTargetContext()).run();
            int historyCount=new HistoryChecks(this).run();
            int uiCount=new UiChecks(this,preview).run(); result.putString("stream","OverlayChecks: "+checks+" checks passed\nFilterChecks: "+filterCount+" checks passed\nHistoryChecks: "+historyCount+" checks passed\nUiChecks: "+uiCount+" checks passed\n");
            }else result.putString("stream","PanelChecks: "+checks+" checks passed\n");
        }catch(Throwable e){code=Activity.RESULT_CANCELED;result.putString("stream","OverlayChecks failed: "+e+"\n");}
        finally{main(()->{if(view!=null)view.hide();if(probe!=null)getTargetContext().unregisterReceiver(probe);getTargetContext().getSystemService(NotificationManager.class).cancel(41);MainActivity.foreground=false;MainActivity.historyForeground=false;if(saved!=null){var edit=Store.prefs(getTargetContext()).edit().clear();for(var entry:saved.entrySet()){Object v=entry.getValue();String k=entry.getKey();if(v instanceof String)edit.putString(k,(String)v);else if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);else if(v instanceof Integer)edit.putInt(k,(Integer)v);else if(v instanceof Long)edit.putLong(k,(Long)v);else if(v instanceof Float)edit.putFloat(k,(Float)v);}edit.apply();JobListener.refreshFloating();}});}
        finish(code,result);
    }
}
