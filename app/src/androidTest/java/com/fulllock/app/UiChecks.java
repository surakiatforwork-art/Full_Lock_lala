package com.fulllock.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Exercise first-run setup, menu placement, swipe gestures, and persisted appearance. */
final class UiChecks {
    private final Instrumentation instrumentation;
    private final boolean screenshots;
    private MainActivity activity;
    private int count;
    UiChecks(Instrumentation i,boolean screenshots){instrumentation=i;this.screenshots=screenshots;}
    private void check(boolean value,String why){if(!value)throw new AssertionError(why);count++;}
    private void main(Runnable work){Throwable[] problem={null};instrumentation.runOnMainSync(()->{try{work.run();}catch(Throwable e){problem[0]=e;}});if(problem[0]!=null)throw new RuntimeException(problem[0]);}
    private View root(){return activity.getWindow().getDecorView();}
    private TextView findText(View root,String text){if(root instanceof TextView&&((TextView)root).getText().toString().equals(text))return (TextView)root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){TextView match=findText(group.getChildAt(i),text);if(match!=null)return match;}}return null;}
    private boolean textContains(View root,String text){if(root instanceof TextView&&((TextView)root).getText().toString().contains(text))return true;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)if(textContains(group.getChildAt(i),text))return true;}return false;}
    private void settle()throws Exception {instrumentation.waitForIdleSync();Thread.sleep(220);}
    private void click(String text){main(()->{TextView v=findText(root(),text);check(v!=null,"found "+text);View target=v;while(!target.isClickable()&&target.getParent() instanceof View)target=(View)target.getParent();target.performClick();});}
    private void menu(){main(()->root().findViewWithTag("open-menu").performClick());}
    private void gesture(SwipeBar bar,float fraction,boolean cancel){long now=SystemClock.uptimeMillis();float start=bar.getPaddingLeft()+24*activity.getResources().getDisplayMetrics().density;float end=start+(bar.getWidth()-bar.getPaddingLeft()-bar.getPaddingRight()-48*activity.getResources().getDisplayMetrics().density)*fraction;float y=bar.getHeight()/2f;for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,cancel?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(now,now+action*20,action,action==0?start:end,y,0);bar.dispatchTouchEvent(event);event.recycle();}}
    private void capture(String name)throws Exception{if(!screenshots)return;settle();android.graphics.Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();java.io.File output=new java.io.File(instrumentation.getTargetContext().getExternalFilesDir("validation"),name);try(java.io.FileOutputStream stream=new java.io.FileOutputStream(output)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();}
    int run()throws Exception {
        Context c=instrumentation.getTargetContext();
        main(()->Store.prefs(c).edit().putBoolean("permissionsReviewed",false).putBoolean("enabled",false).putBoolean("floatingEnabled",false).remove("themeColor").remove("invertText").apply());
        try{
            activity=(MainActivity)instrumentation.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            main(()->{check(findText(root(),"เตรียมใช้งาน")!=null,"first launch permissions");check(findText(root(),"อ่านแจ้งเตือนงาน")!=null,"first permission visible");check(root().findViewWithTag("swipe-bar")==null,"no auto start in setup");});capture("ui-permissions.png");
            main(()->root().findViewWithTag("finish-permissions").performClick());settle();
            main(()->{check(Store.prefs(c).getBoolean("permissionsReviewed",false),"setup remembered");check(findText(root(),"ตัวกรองงาน")!=null,"filter page");check(!textContains(root(),"สิทธิ์"),"no permissions on filters");check(root().findViewWithTag("floatingEnabled")==null,"no floating toggle on filters");check(!textContains(root(),"พักการเปิด"),"cooldown absent");check(!textContains(root(),"FAB"),"no developer label on filters");
                SwipeBar swipe=(SwipeBar)root().findViewWithTag("swipe-bar");check(swipe.getWidth()>0,"swipe measured");gesture(swipe,0,false);check(!Store.prefs(c).getBoolean("enabled",false),"tap does not start");gesture(swipe,.25f,false);check(!Store.prefs(c).getBoolean("enabled",false),"partial swipe does not start");gesture(swipe,1,true);check(!Store.prefs(c).getBoolean("enabled",false),"cancelled swipe does not start");gesture(swipe,1,false);check(Store.prefs(c).getBoolean("enabled",false),"full swipe starts");});settle();
            main(()->{SwipeBar swipe=(SwipeBar)root().findViewWithTag("swipe-bar");gesture(swipe,1,false);check(!Store.prefs(c).getBoolean("enabled",false),"full swipe stops");});settle();capture("ui-filters.png");
            menu();settle();
            main(()->{View toggle=root().findViewWithTag("floatingEnabled");TextView permission=findText(root(),"การอนุญาตสิทธิ์");LinearLayout nav=(LinearLayout)root().findViewWithTag("drawer-content");check(toggle!=null,"floating switch in menu");check(findText(root(),"Dev by PHANToM")!=null,"developer credit");check(nav.indexOfChild((View)toggle.getParent())<nav.indexOfChild(permission),"floating switch above permission title");check(nav.getChildAt(nav.getChildCount()-1).getTag().equals("theme-menu"),"theme last in menu");check(!textContains(nav,"FAB"),"menu plain language");});capture("ui-menu.png");
            click("◷ ประวัติงาน");settle();main(()->{check(!textContains(root(),"สิทธิ์"),"no permissions on history");check(!textContains(root(),"Exception"),"history hides exception names");View entry=root().findViewWithTag("history-entry-0");check(entry!=null,"history entry exists");entry.performClick();check(root().findViewWithTag("job-unavailable")!=null,"unavailable history shows OSD");});
            capture("ui-history-feedback.png");menu();click("◐ ธีม");settle();main(()->root().findViewWithTag("theme-preset-1").performClick());settle();
            main(()->{check(AppTheme.accent(c)==0xFF2563EB,"blue chosen");Switch invert=(Switch)root().findViewWithTag("invertText");invert.setChecked(true);});settle();
            main(()->{check(AppTheme.onAccent(c)==Color.BLACK,"white text switches to black");check(AppTheme.ink(c)==Color.WHITE,"dark text switches to white");check(findText(root(),"สีของแอป").getCurrentTextColor()==Color.WHITE,"page text updated");check(findText(root(),"FULL LOCK").getCurrentTextColor()==Color.BLACK,"header text updated");});capture("ui-theme.png");
            click("เลือกสีเอง");settle();capture("ui-color-picker.png");instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();
            main(()->activity.finish());settle();
            activity=(MainActivity)instrumentation.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            main(()->{check(findText(root(),"ตัวกรองงาน")!=null,"relaunch skips first setup");check(AppTheme.accent(c)==0xFF2563EB&&AppTheme.inverted(c),"theme persisted after relaunch");check(findText(root(),"FULL LOCK").getCurrentTextColor()==Color.BLACK,"relaunch theme applied");});
        }finally{if(activity!=null){main(()->activity.finish());settle();}}
        return count;
    }
}
