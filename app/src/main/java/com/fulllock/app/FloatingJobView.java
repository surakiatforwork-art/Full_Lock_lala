package com.fulllock.app;
import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;

/** A draggable unread counter and a separate, side-opening recent-jobs panel. */
final class FloatingJobView {
    final FloatingJobs model=new FloatingJobs();
    private final Context context;
    private final WindowManager wm;
    private final Consumer<FloatingJobs.Item> open;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView fab;
    private LinearLayout panel,rows;
    private WindowManager.LayoutParams fabParams,panelParams;
    private float downX,downY;
    private int startX,startY;
    private boolean dragged;
    private final Runnable timeout=()->{model.expire();render();};
    FloatingJobView(Context context,Consumer<FloatingJobs.Item> open){this.context=context;this.open=open;wm=context.getSystemService(WindowManager.class);}
    private int dp(float n){return (int)(n*context.getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,boolean bold){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    boolean visible(){return fab!=null;}
    void show(){
        if(fab!=null || !Settings.canDrawOverlays(context))return;
        fab=text("0",17,true);fab.setGravity(Gravity.CENTER);fab.setBackground(background(0xFFF16527,24));fab.setElevation(dp(6));
        fabParams=params(dp(48));fabParams.height=dp(48);
        fabParams.x=Store.prefs(context).getInt("floatX",dp(8));fabParams.y=Store.prefs(context).getInt("floatY",dp(180));clampFab();
        fab.setOnClickListener(v->toggle());
        fab.setOnTouchListener((v,e)->{
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startX=fabParams.x;startY=fabParams.y;dragged=false;return true;
                case MotionEvent.ACTION_MOVE:
                    float dx=e.getRawX()-downX,dy=e.getRawY()-downY;
                    if(Math.hypot(dx,dy)>ViewConfiguration.get(context).getScaledTouchSlop())dragged=true;
                    if(dragged){fabParams.x=startX+Math.round(dx);fabParams.y=startY+Math.round(dy);clampFab();updateFab();positionPanel();}return true;
                case MotionEvent.ACTION_UP:
                    if(!dragged)v.performClick();else{fabParams.x=onLeft()?dp(8):screenWidth()-dp(56);clampFab();updateFab();positionPanel();Store.prefs(context).edit().putInt("floatX",fabParams.x).putInt("floatY",fabParams.y).apply();}return true;
                case MotionEvent.ACTION_CANCEL:return true;
                default:return false;
            }
        });
        try{wm.addView(fab,fabParams);render();}catch(SecurityException | WindowManager.BadTokenException e){fab=null;}
    }
    private WindowManager.LayoutParams params(int width){WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;return p;}
    private int screenWidth(){return context.getResources().getDisplayMetrics().widthPixels;}
    private int screenHeight(){return context.getResources().getDisplayMetrics().heightPixels;}
    private boolean onLeft(){return fabParams.x+dp(24)<screenWidth()/2;}
    private void clampFab(){fabParams.x=Math.max(0,Math.min(fabParams.x,screenWidth()-dp(48)));fabParams.y=Math.max(0,Math.min(fabParams.y,screenHeight()-dp(110)));}
    private void updateFab(){if(fab!=null)try{wm.updateViewLayout(fab,fabParams);}catch(IllegalArgumentException | SecurityException e){hide();}}
    void offer(FloatingJobs.Item item){model.put(item,visible());handler.removeCallbacks(timeout);render();if(model.mode==FloatingJobs.Mode.PREVIEW)handler.postDelayed(timeout,3000);}
    void toggle(){handler.removeCallbacks(timeout);if(model.mode==FloatingJobs.Mode.PINNED)model.collapse();else model.open();render();}
    void removed(String key,long time){model.removed(key,time);render();}
    void reset(){handler.removeCallbacks(timeout);model.reset();render();}
    private void render(){
        if(fab==null)return;
        fab.setText(model.unread>99?"99+":String.valueOf(model.unread));fab.setContentDescription("งานยังไม่เปิดดู "+model.unread+" รายการ แตะดูงานล่าสุด ลากเพื่อย้าย");
        if(model.mode==FloatingJobs.Mode.COLLAPSED){removePanel();return;}
        if(panel==null){
            panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(7),dp(5),dp(7),dp(5));panel.setBackground(background(0xFFF16527,14));panel.setElevation(dp(6));
            TextView close=text("งานล่าสุด                         ×",10,false);close.setPadding(dp(3),dp(3),dp(3),dp(5));close.setOnClickListener(v->{handler.removeCallbacks(timeout);model.collapse();render();});panel.addView(close);
            ScrollView scroll=new ScrollView(context);scroll.setVerticalScrollBarEnabled(false);
            rows=new LinearLayout(context);rows.setOrientation(LinearLayout.VERTICAL);rows.setClipChildren(false);
            LayoutTransition transition=new LayoutTransition();transition.enableTransitionType(LayoutTransition.CHANGING);
            AnimatorSet appear=new AnimatorSet();appear.playTogether(ObjectAnimator.ofFloat(null,"alpha",0f,1f),ObjectAnimator.ofFloat(null,"translationY",-dp(10),0f));transition.setAnimator(LayoutTransition.APPEARING,appear);
            AnimatorSet disappear=new AnimatorSet();disappear.playTogether(ObjectAnimator.ofFloat(null,"alpha",1f,0f),ObjectAnimator.ofFloat(null,"translationY",0f,dp(32)));transition.setAnimator(LayoutTransition.DISAPPEARING,disappear);transition.setDuration(220);rows.setLayoutTransition(transition);
            scroll.addView(rows);panel.addView(scroll,new LinearLayout.LayoutParams(-1,-2));panelParams=params(Math.min(dp(205),screenWidth()-dp(70)));
            try{wm.addView(panel,panelParams);}catch(SecurityException | WindowManager.BadTokenException e){panel=null;rows=null;return;}
        }
        List<FloatingJobs.Item> desired=model.mode==FloatingJobs.Mode.PREVIEW?model.items.subList(0,Math.min(1,model.items.size())):model.items;
        Set<String> ids=new HashSet<>();for(var item:desired)ids.add(item.id);
        for(int i=rows.getChildCount()-1;i>=0;i--){View child=rows.getChildAt(i);if(!ids.contains(child.getTag()))rows.removeView(child);}
        if(desired.isEmpty()){
            if(rows.getChildCount()==0){TextView waiting=text("รอแจ้งเตือนงานใหม่",11,false);waiting.setTag("waiting");rows.addView(waiting);}
        }else for(int i=0;i<desired.size();i++){
            FloatingJobs.Item item=desired.get(i);View existing=rows.findViewWithTag(item.id);
            if(existing!=null){int index=rows.indexOfChild(existing);if(index!=i){rows.removeView(existing);rows.addView(existing,i);}bind((LinearLayout)existing,item);}
            else {LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.VERTICAL);row.setTag(item.id);row.setPadding(dp(5),dp(5),dp(5),dp(5));row.setBackground(background(0x19FFFFFF,7));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(4);rows.addView(row,i,lp);bind(row,item);}
        }
        rows.post(this::positionPanel);
    }
    private void bind(LinearLayout row,FloatingJobs.Item item){
        row.removeAllViews();LinearLayout route=new LinearLayout(context);route.setGravity(Gravity.CENTER_VERTICAL);
        String title=item.job==null?item.title:item.job.origin()+" → "+item.job.destination();
        TextView path=text(title,11,true);path.setMaxLines(2);path.setEllipsize(android.text.TextUtils.TruncateAt.END);route.addView(path,new LinearLayout.LayoutParams(0,-2,1));
        if(item.matches){TextView mark=text("✓",15,true);mark.setTextColor(0xFF9CFFC3);mark.setContentDescription("ตรงเงื่อนไข");route.addView(mark);}row.addView(route);
        TextView type=text(item.job==null?item.text:item.job.type(),10,true);type.setMaxLines(2);type.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(type);
        if(item.job!=null)row.addView(text("฿"+item.job.fare()+" + ฿"+item.job.tip(),11,true));
        row.setAlpha(item.available?1f:.55f);row.setContentDescription(title+(item.available?" แตะเปิดงาน":" แจ้งเตือนหมดอายุ"));
        row.setOnClickListener(v->{if(item.available)open.accept(item);else Toast.makeText(context,"แจ้งเตือนนี้ไม่อยู่แล้ว",Toast.LENGTH_SHORT).show();});
    }
    private void positionPanel(){if(panel==null || fab==null)return;panelParams.x=onLeft()?fabParams.x+dp(54):fabParams.x-panelParams.width-dp(6);panelParams.x=Math.max(0,Math.min(panelParams.x,screenWidth()-panelParams.width));int limit=screenHeight()-dp(100);panel.measure(View.MeasureSpec.makeMeasureSpec(panelParams.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(limit,View.MeasureSpec.AT_MOST));panelParams.height=Math.min(panel.getMeasuredHeight(),limit);panelParams.y=Math.max(0,Math.min(fabParams.y,screenHeight()-panelParams.height-dp(70)));try{wm.updateViewLayout(panel,panelParams);}catch(IllegalArgumentException | SecurityException e){removePanel();}}
    private void removePanel(){if(panel!=null){try{wm.removeViewImmediate(panel);}catch(IllegalArgumentException ignored){}panel=null;rows=null;}}
    void hide(){handler.removeCallbacks(timeout);model.collapse();removePanel();if(fab!=null){try{wm.removeViewImmediate(fab);}catch(IllegalArgumentException ignored){}fab=null;}}
}
