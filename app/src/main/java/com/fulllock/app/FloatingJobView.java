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
    private ScrollView scroll;
    private boolean closing;
    private long slideMs(){return Math.max(0,Math.min(1000,Store.prefs(context).getInt("transitionMs",200)));}
    void refreshMotion(){if(rows!=null&&rows.getLayoutTransition()!=null)rows.getLayoutTransition().setDuration(slideMs());}
    private int rowHeight(){return dp(72*Math.max(1,context.getResources().getConfiguration().fontScale));}
    private WindowManager.LayoutParams fabParams,panelParams;
    private float downX,downY;
    private int startX,startY;
    private boolean dragged;
    private final Runnable timeout=()->{model.expire();render();};
    private final class OutsideFab extends TextView {
        private final Bitmap mascot;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final RectF imageBounds=new RectF();
        private final Paint.FontMetrics counterMetrics=new Paint.FontMetrics();
        OutsideFab(){super(FloatingJobView.this.context);BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=4;mascot=BitmapFactory.decodeResource(getResources(),R.drawable.floating_phantom,options);paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
        @Override protected void onDraw(Canvas canvas){
            float scale=Math.min(getWidth()/(float)mascot.getWidth(),getHeight()/(float)mascot.getHeight()),width=mascot.getWidth()*scale,height=mascot.getHeight()*scale,left=(getWidth()-width)/2,top=(getHeight()-height)/2;
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);imageBounds.set(left,top,left+width,top+height);canvas.drawBitmap(mascot,null,imageBounds,paint);
            String count=getText().toString();paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(19));float maximum=width*.24f;if(paint.measureText(count)>maximum)paint.setTextSize(paint.getTextSize()*maximum/paint.measureText(count));
            float x=left+width*.775f,y=top+height*.785f;paint.getFontMetrics(counterMetrics);float baseline=y-(counterMetrics.ascent+counterMetrics.descent)/2;
            canvas.save();canvas.rotate(-8,x,y);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.4f));paint.setColor(0xFF7C2B0B);canvas.drawText(count,x,baseline,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(0xFFFFFAEF);canvas.drawText(count,x,baseline,paint);canvas.restore();
        }
        @Override public boolean dispatchTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)MainActivity.cancelPendingStop();if(e.getActionMasked()==MotionEvent.ACTION_OUTSIDE){outside(e);return true;}return super.dispatchTouchEvent(e);}
    }
    private final class OutsidePanel extends LinearLayout {
        OutsidePanel(){super(FloatingJobView.this.context);}
        @Override public boolean dispatchTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)MainActivity.cancelPendingStop();if(e.getActionMasked()==MotionEvent.ACTION_OUTSIDE){outside(e);return true;}return super.dispatchTouchEvent(e);}
    }
    private boolean contains(View view,float x,float y){if(view==null)return false;int[] location=new int[2];view.getLocationOnScreen(location);return x>=location[0] && x<location[0]+view.getWidth() && y>=location[1] && y<location[1]+view.getHeight();}
    private void outside(MotionEvent event){if(model.mode!=FloatingJobs.Mode.COLLAPSED && !contains(fab,event.getRawX(),event.getRawY()) && !contains(panel,event.getRawX(),event.getRawY()))collapse();}
    private void collapse(){handler.removeCallbacks(timeout);model.collapse();render();}
    FloatingJobView(Context context,Consumer<FloatingJobs.Item> open){this.context=context;this.open=open;wm=context.getSystemService(WindowManager.class);}
    private int dp(float n){return (int)(n*context.getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,boolean bold){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(AppTheme.onAccent(context));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    void refreshTheme(){if(fab==null)return;fab.setTextColor(AppTheme.onAccent(context));fab.setBackground(background(AppTheme.accent(context),24));render();}
    boolean visible(){return fab!=null;}
    void show(){
        if(fab!=null || !Settings.canDrawOverlays(context))return;
        fab=new OutsideFab();fab.setTextSize(17);fab.setTextColor(AppTheme.onAccent(context));fab.setTypeface(null,Typeface.BOLD);fab.setGravity(Gravity.CENTER);fab.setBackground(background(AppTheme.accent(context),24));fab.setElevation(dp(6));
        fabParams=params(dp(80));fabParams.height=dp(56);
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
                    if(!dragged)v.performClick();else{fabParams.x=onLeft()?dp(8):screenWidth()-fabParams.width-dp(8);clampFab();updateFab();positionPanel();Store.prefs(context).edit().putInt("floatX",fabParams.x).putInt("floatY",fabParams.y).apply();}return true;
                case MotionEvent.ACTION_CANCEL:return true;
                default:return false;
            }
        });
        try{wm.addView(fab,fabParams);render();}catch(SecurityException | WindowManager.BadTokenException e){fab=null;}
    }
    private WindowManager.LayoutParams params(int width){WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;return p;}
    private int screenWidth(){return context.getResources().getDisplayMetrics().widthPixels;}
    private int screenHeight(){return context.getResources().getDisplayMetrics().heightPixels;}
    private boolean onLeft(){return fabParams.x+fabParams.width/2<screenWidth()/2;}
    private void clampFab(){fabParams.x=Math.max(0,Math.min(fabParams.x,screenWidth()-fabParams.width));fabParams.y=Math.max(0,Math.min(fabParams.y,screenHeight()-dp(110)));}
    private void updateFab(){if(fab!=null)try{wm.updateViewLayout(fab,fabParams);}catch(IllegalArgumentException | SecurityException e){hide();}}
    void offer(FloatingJobs.Item item){model.put(item,visible());handler.removeCallbacks(timeout);render();if(scroll!=null)scroll.scrollTo(0,0);if(model.mode==FloatingJobs.Mode.PREVIEW)handler.postDelayed(timeout,3000);}
    void toggle(){handler.removeCallbacks(timeout);if(model.mode==FloatingJobs.Mode.PINNED)model.collapse();else model.open();render();}
    void removed(String key,long time){model.removed(key,time);render();}
    void reset(){handler.removeCallbacks(timeout);model.reset();render();}
    private void render(){
        if(fab==null)return;
        fab.setText(model.unread>99?"99+":String.valueOf(model.unread));fab.setContentDescription("งานยังไม่เปิดดู "+model.unread+" รายการ แตะดูงานล่าสุด ลากเพื่อย้าย");
        if(model.mode==FloatingJobs.Mode.COLLAPSED){closePanel();return;}
        boolean opening=panel==null||closing;
        if(closing&&panel!=null){panel.animate().cancel();closing=false;panel.animate().alpha(1).translationX(0).setDuration(slideMs()).start();}
        if(panel==null){
            panel=new OutsidePanel();panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(7),dp(5),dp(7),dp(5));panel.setBackgroundColor(Color.TRANSPARENT);
            scroll=new ScrollView(context);scroll.setVerticalScrollBarEnabled(true);scroll.setFillViewport(false);
            rows=new LinearLayout(context);rows.setOrientation(LinearLayout.VERTICAL);rows.setClipChildren(false);
            LayoutTransition transition=new LayoutTransition();transition.enableTransitionType(LayoutTransition.CHANGING);
            AnimatorSet appear=new AnimatorSet();appear.playTogether(ObjectAnimator.ofFloat(null,"alpha",0f,1f),ObjectAnimator.ofFloat(null,"translationY",-dp(10),0f));transition.setAnimator(LayoutTransition.APPEARING,appear);
            AnimatorSet disappear=new AnimatorSet();disappear.playTogether(ObjectAnimator.ofFloat(null,"alpha",1f,0f),ObjectAnimator.ofFloat(null,"translationY",0f,dp(32)));transition.setAnimator(LayoutTransition.DISAPPEARING,disappear);transition.setDuration(slideMs());rows.setLayoutTransition(transition);
            scroll.addView(rows);panel.addView(scroll,new LinearLayout.LayoutParams(-1,-2));panelParams=params(Math.min(dp(260),screenWidth()-fabParams.width-dp(22)));
            panelParams.x=Math.max(0,Math.min(onLeft()?fabParams.x+fabParams.width+dp(6):fabParams.x-panelParams.width-dp(6),screenWidth()-panelParams.width));panelParams.y=fabParams.y;
            try{panel.setAlpha(0);panel.setTranslationX(onLeft()?-dp(45):dp(45));wm.addView(panel,panelParams);panel.animate().alpha(1).translationX(0).setDuration(slideMs()).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}catch(SecurityException | WindowManager.BadTokenException e){panel=null;rows=null;return;}
        }
        boolean showUnavailable=Store.prefs(context).getBoolean("showUnavailableFloating",true);
        List<FloatingJobs.Item> desired=model.displayed(showUnavailable,model.mode==FloatingJobs.Mode.PREVIEW);
        Set<String> ids=new HashSet<>();for(var item:desired)ids.add(item.id);
        for(int i=rows.getChildCount()-1;i>=0;i--){View child=rows.getChildAt(i);if(!ids.contains(child.getTag()))rows.removeView(child);}
        if(desired.isEmpty()){
            if(rows.getChildCount()==0){TextView waiting=text(model.items.isEmpty()?"รอแจ้งเตือนงานใหม่":"ไม่มีงานที่เปิดได้",11,false);waiting.setTextColor(AppTheme.ink(context));waiting.setPadding(dp(10),dp(8),dp(10),dp(8));waiting.setBackground(background(AppTheme.surface(context),18));waiting.setTag("waiting");rows.addView(waiting);}
        }else for(int i=0;i<desired.size();i++){
            FloatingJobs.Item item=desired.get(i);View existing=rows.findViewWithTag(item.id);
            if(existing!=null){int index=rows.indexOfChild(existing);if(index!=i){rows.removeView(existing);rows.addView(existing,i);}bind((LinearLayout)existing,item,opening);}
            else {LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.VERTICAL);row.setTag(item.id);row.setClipToOutline(true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,rowHeight());lp.bottomMargin=dp(4);rows.addView(row,i,lp);bind(row,item,true);}
        }
        rows.post(this::positionPanel);
    }
    private void bind(LinearLayout row,FloatingJobs.Item item,boolean animateMark){
        row.removeAllViews();row.setGravity(Gravity.TOP);row.setBackground(background(AppTheme.surface(context),18));
        TextView type=text(item.job==null?"งานขนส่ง":item.job.type(),9,true);type.setTextColor(AppTheme.onAccent(context));type.setPadding(dp(8),dp(2),dp(8),dp(2));type.setBackgroundColor(AppTheme.jobColor(context,item.job));row.addView(type,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout route=new LinearLayout(context);route.setGravity(Gravity.CENTER_VERTICAL);route.setPadding(dp(8),0,dp(8),0);
        String title=item.job==null?item.title:item.job.origin()+" → "+item.job.destination();
        TextView path=text(title,11,true);path.setTextColor(AppTheme.ink(context));path.setMaxLines(2);path.setEllipsize(android.text.TextUtils.TruncateAt.END);route.addView(path,new LinearLayout.LayoutParams(0,-2,1));
        if(item.job!=null){
            String amount="฿"+item.job.overlayNet(Store.prefs(context).getInt("fee",20)).toPlainString();
            TextView net=text(amount,18,true);net.setTextColor(AppTheme.ink(context));net.setSingleLine(true);net.setMaxWidth(dp(112));net.setAutoSizeTextTypeUniformWithConfiguration(10,18,1,android.util.TypedValue.COMPLEX_UNIT_SP);net.setPadding(dp(4),0,dp(4),0);net.setContentDescription("สุทธิ "+amount+" บาท");route.addView(net);
        }
        if(item.matches){TextView mark=text("✓",28,true);mark.setTextColor(0xFF1FA65B);mark.setContentDescription("ตรงเงื่อนไข");route.addView(mark);if(animateMark)blink(mark);}row.addView(route,new LinearLayout.LayoutParams(-1,0,1));
        TextView price=text(item.job==null?item.text:"฿"+item.job.fare()+" + ฿"+item.job.tip(),11,true);price.setTextColor(AppTheme.ink(context));price.setPadding(dp(8),0,dp(8),dp(4));price.setMaxLines(1);price.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(price);
        row.setAlpha(item.available?1f:.55f);row.setContentDescription(title+(item.available?" แตะเปิดงาน":" แจ้งเตือนหมดอายุ"));
        row.setOnClickListener(v->{if(item.available)open.accept(item);else Toast.makeText(context,"แจ้งเตือนนี้ไม่อยู่แล้ว",Toast.LENGTH_SHORT).show();});
    }
    private void blink(TextView mark){
        ObjectAnimator blink=ObjectAnimator.ofFloat(mark,View.ALPHA,1f,.25f);blink.setDuration(125);blink.setRepeatCount(5);blink.setRepeatMode(ValueAnimator.REVERSE);
        mark.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){blink.cancel();v.setAlpha(1f);v.removeOnAttachStateChangeListener(this);}});
        blink.start();
    }
    private void positionPanel(){
        if(panel==null || fab==null)return;
        panelParams.x=onLeft()?fabParams.x+fabParams.width+dp(6):fabParams.x-panelParams.width-dp(6);
        panelParams.x=Math.max(0,Math.min(panelParams.x,screenWidth()-panelParams.width));
        int available=screenHeight()-dp(110);
        int count=rows.getChildCount();
        int content=count==0?rowHeight():count*(rowHeight()+dp(4));
        int viewport=model.mode==FloatingJobs.Mode.PREVIEW?rowHeight()+dp(4):Math.min(content,Math.round((rowHeight()+dp(4))*3.5f));
        scroll.getLayoutParams().height=Math.max(1,Math.min(viewport,available-dp(10)));
        panelParams.height=scroll.getLayoutParams().height+dp(10);
        panelParams.y=Math.max(0,Math.min(fabParams.y,screenHeight()-panelParams.height-dp(70)));
        try{wm.updateViewLayout(panel,panelParams);}catch(IllegalArgumentException | SecurityException e){removePanel();}
    }
    private void closePanel(){if(panel==null||closing)return;if(slideMs()==0){removePanel();return;}closing=true;panel.animate().cancel();panel.animate().alpha(0).translationX(onLeft()?-dp(45):dp(45)).setDuration(slideMs()).withEndAction(()->{if(model.mode==FloatingJobs.Mode.COLLAPSED)removePanel();}).start();}
    private void removePanel(){if(panel!=null){panel.animate().cancel();try{wm.removeViewImmediate(panel);}catch(IllegalArgumentException ignored){}panel=null;rows=null;scroll=null;}closing=false;}
    void hide(){handler.removeCallbacks(timeout);model.collapse();removePanel();if(fab!=null){try{wm.removeViewImmediate(fab);}catch(IllegalArgumentException ignored){}fab=null;}}
}
