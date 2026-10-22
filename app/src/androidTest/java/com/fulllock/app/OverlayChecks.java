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
    private boolean preview;
    private FloatingJobView view;
    private Map<String,?> saved;
    private void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
    private Object field(Object target,String name){try{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(Exception e){throw new RuntimeException(e);}}
    private JobListener service(){try{Field f=JobListener.class.getDeclaredField("instance");f.setAccessible(true);return (JobListener)f.get(null);}catch(Exception e){throw new RuntimeException(e);}}
    private void main(Runnable work){Throwable[] failure={null};runOnMainSync(()->{try{work.run();}catch(Throwable e){failure[0]=e;}});if(failure[0]!=null)throw new RuntimeException(failure[0]);}
    private FloatingJobs.Item item(int n){String title="[ขนส่งสินค้า] ต้นทาง "+n+" > ปลายทาง "+n;String text="ขนส่งทันที ฿276.00 + ฿10.00 รับงานขนส่ง!";return new FloatingJobs.Item("key"+n,n,title,text,JobParser.parse(title,text),n%2==0,true);}
    private LinearLayout rows(){return (LinearLayout)field(view,"rows");}
    private void capture(String name)throws Exception {
        if(!preview)return;waitForIdleSync();Thread.sleep(450);
        android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();
        java.io.File output=new java.io.File(getTargetContext().getExternalFilesDir("validation"),name);
        try(java.io.FileOutputStream stream=new java.io.FileOutputStream(output)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
    }
    private void shell(String command)throws Exception {
        try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);java.io.InputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){byte[] buffer=new byte[1024];while(input.read(buffer)!=-1){} }
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);preview=args!=null&&"true".equals(args.getString("preview"));start();}
    @Override public void onStart(){
        Bundle result=new Bundle();int code=Activity.RESULT_OK;List<String> selected=new ArrayList<>();
        try{
            main(()->android.service.notification.NotificationListenerService.requestRebind(new ComponentName(getTargetContext(),JobListener.class)));
            // Instrumentation force-stops its target. Reset the emulator's stale listener binding.
            if(!JobListener.connected){shell("cmd notification disallow_listener com.fulllock.app/com.fulllock.app.JobListener");shell("cmd notification allow_listener com.fulllock.app/com.fulllock.app.JobListener");}
            for(int i=0;i<80&&!JobListener.connected;i++)Thread.sleep(250);
            main(()->{
                check(service()!=null&&JobListener.connected,"service connected");var p=Store.prefs(getTargetContext());saved=p.getAll();p.edit().putBoolean("enabled",false).putInt("floatX",8).putInt("floatY",260).apply();
                check(!JobListener.floatingVisible(),"stopped service hidden");
                view=new FloatingJobView(getTargetContext(),entry->selected.add(entry.id));view.show();check(view.visible(),"FAB visible");
                WindowManager.LayoutParams fp=(WindowManager.LayoutParams)field(view,"fabParams");check(fp.width<=Math.round(48*getTargetContext().getResources().getDisplayMetrics().density),"compact 48dp FAB");
                for(int i=1;i<=5;i++)view.offer(item(i));
                check(view.model.unread==5,"five unread");check(rows().getChildCount()==1,"automatic preview one item");
                check(view.model.mode==FloatingJobs.Mode.PREVIEW,"preview mode");
            });
            Thread.sleep(3400);
            main(()->{check(view.model.mode==FloatingJobs.Mode.COLLAPSED,"preview closes after 3 seconds");check(view.model.unread==5,"preview does not mark read");view.toggle();check(view.model.unread==0,"opening FAB marks read");check(rows().getChildCount()==5,"pinned list five items");view.offer(item(6));check(view.model.mode==FloatingJobs.Mode.PINNED,"new item preserves pinned");check(view.model.items.get(0).id.equals(item(6).id),"newest first");check(rows().getChildCount()==5,"oldest removed");check(view.model.unread==0,"visible new item read");
                LinearLayout row=(LinearLayout)rows().getChildAt(0);LinearLayout route=(LinearLayout)row.getChildAt(0);
                check(((TextView)route.getChildAt(0)).getTypeface().isBold(),"route bold");check(((TextView)row.getChildAt(1)).getTypeface().isBold(),"type bold");check(((TextView)row.getChildAt(2)).getTypeface().isBold(),"fare tip bold");check(route.getChildCount()==2&&((TextView)route.getChildAt(1)).getText().toString().equals("✓"),"matching checkmark");check(row.getChildCount()==3,"no rejection reason in panel");
            });
            capture("fulllock-fab-left.png");Thread.sleep(3200);
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
            main(()->{
                view.hide();MainActivity.foreground=true;Store.prefs(getTargetContext()).edit().putBoolean("enabled",true).apply();JobListener.refreshFloating();check(!JobListener.floatingVisible(),"hidden in Full Lock");
                MainActivity.foreground=false;JobListener.refreshFloating();check(JobListener.floatingVisible(),"shown outside Full Lock");
                Object watcher=field(service(),"foregroundWatcher");Set<String> driver=(Set<String>)field(watcher,"driverActivities");driver.add("driver-foreground-test");JobListener.refreshFloating();check(!JobListener.floatingVisible(),"hidden with Driver foreground state");driver.clear();JobListener.refreshFloating();check(JobListener.floatingVisible(),"shown after leaving Driver");
                Store.prefs(getTargetContext()).edit().putBoolean("enabled",false).apply();check(!JobListener.floatingVisible(),"stop hides FAB");
            });
            result.putString("stream","OverlayChecks: "+checks+" checks passed\n");
        }catch(Throwable e){code=Activity.RESULT_CANCELED;result.putString("stream","OverlayChecks failed: "+e+"\n");}
        finally{main(()->{if(view!=null)view.hide();MainActivity.foreground=false;if(saved!=null){var edit=Store.prefs(getTargetContext()).edit().clear();for(var entry:saved.entrySet()){Object v=entry.getValue();String k=entry.getKey();if(v instanceof String)edit.putString(k,(String)v);else if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);else if(v instanceof Integer)edit.putInt(k,(Integer)v);else if(v instanceof Long)edit.putLong(k,(Long)v);else if(v instanceof Float)edit.putFloat(k,(Float)v);}edit.apply();JobListener.refreshFloating();}});}
        finish(code,result);
    }
}
