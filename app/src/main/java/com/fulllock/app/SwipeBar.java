package com.fulllock.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.util.function.BooleanSupplier;

/** Start on release at the endpoint. Stopping requires a separate confirmation tap. */
final class SwipeBar extends FrameLayout {
    private TextView handle;
    private final BooleanSupplier commit;
    private final boolean enabled;
    private float downX;
    private boolean tracking,feedbackGiven,confirming;
    SwipeBar(Context c,boolean enabled,BooleanSupplier commit){
        super(c);this.enabled=enabled;this.commit=commit;setTag("swipe-bar");setPadding(dp(6),dp(6),dp(6),dp(6));setFocusable(true);normal();
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value){TextView t=new TextView(getContext());t.setText(value);t.setTextColor(AppTheme.onAccent(getContext()));t.setTextSize(17);t.setTypeface(null,Typeface.BOLD);t.setGravity(Gravity.CENTER);return t;}
    private void normal(){
        confirming=false;removeAllViews();android.graphics.drawable.GradientDrawable background=AppTheme.background(enabled?AppTheme.page(getContext()):AppTheme.accent(getContext()),dp(17));if(enabled)background.setStroke(dp(2),AppTheme.accent(getContext()));setBackground(background);setContentDescription(enabled?"แตะเพื่อหยุด":"ปัดเพื่อเริ่ม");
        TextView label=text(enabled?"แตะเพื่อหยุด":"ปัดเพื่อเริ่ม  ››");if(enabled)label.setTextColor(AppTheme.ink(getContext()));if(!enabled)label.setPadding(dp(50),0,0,0);addView(label,new LayoutParams(-1,-1));
        setOnClickListener(enabled?v->confirmStop():null);
        if(!enabled){handle=text("→");handle.setTextSize(28);handle.setTextColor(AppTheme.accent(getContext()));handle.setBackground(AppTheme.background(AppTheme.surface(getContext()),dp(12)));handle.setElevation(dp(2));addView(handle,new LayoutParams(dp(48),-1,Gravity.START));}
    }
    private void confirmStop(){
        confirming=true;removeAllViews();setOnClickListener(null);setContentDescription("แตะยืนยันทางขวาเพื่อหยุด แตะที่อื่นเพื่อยกเลิก");
        LinearLayout choices=new LinearLayout(getContext());choices.setGravity(Gravity.CENTER_VERTICAL);addView(choices,new LayoutParams(-1,-1));
        TextView hint=text("หยุดทำงานหรือไม่?");hint.setTextColor(AppTheme.ink(getContext()));choices.addView(hint,new LinearLayout.LayoutParams(0,-1,1));hint.setOnClickListener(v->cancelStop());
        TextView confirm=text("ยืนยัน");confirm.setTag("stop-confirm");confirm.setTextSize(14);confirm.setBackground(AppTheme.background(AppTheme.accent(getContext()),dp(11)));confirm.setElevation(dp(3));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(90),-1);cp.setMargins(dp(8),dp(2),dp(3),dp(3));choices.addView(confirm,cp);confirm.setOnClickListener(v->{confirming=false;commit.getAsBoolean();});
    }
    void cancelStop(){if(confirming)normal();}
    boolean isConfirming(){return confirming;}
    boolean confirmationContains(float x,float y){View confirm=findViewWithTag("stop-confirm");if(confirm==null)return false;int[] at=new int[2];confirm.getLocationOnScreen(at);return x>=at[0]&&x<at[0]+confirm.getWidth()&&y>=at[1]&&y<at[1]+confirm.getHeight();}
    @Override protected void onDetachedFromWindow(){cancelStop();super.onDetachedFromWindow();}
    private float travel(){return Math.max(0,getWidth()-getPaddingLeft()-getPaddingRight()-handle.getWidth());}
    private boolean atEnd(){return travel()>0&&handle.getTranslationX()>=travel()-dp(2);}
    private void reset(){handle.animate().translationX(0).setDuration(150).start();tracking=false;getParent().requestDisallowInterceptTouchEvent(false);}
    private void endpoint(){if(feedbackGiven)return;feedbackGiven=true;Vibrator vibrator=getContext().getSystemService(Vibrator.class);if(vibrator!=null&&vibrator.hasVibrator())vibrator.vibrate(VibrationEffect.createOneShot(35,VibrationEffect.DEFAULT_AMPLITUDE));Splash splash=new Splash(getContext());splash.setTag("start-splash");addView(splash,new LayoutParams(-1,-1));ValueAnimator animation=ValueAnimator.ofFloat(0,1);animation.setDuration(340);animation.addUpdateListener(a->{splash.progress=(float)a.getAnimatedValue();splash.invalidate();});animation.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){removeView(splash);}});animation.start();}
    @Override public boolean onTouchEvent(MotionEvent e){
        if(enabled)return super.onTouchEvent(e);
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                if(e.getX()>getPaddingLeft()+handle.getWidth()+dp(8))return false;
                handle.animate().cancel();handle.setTranslationX(0);downX=e.getX();tracking=true;feedbackGiven=false;getParent().requestDisallowInterceptTouchEvent(true);return true;
            case MotionEvent.ACTION_MOVE:if(!tracking)return false;handle.setTranslationX(Math.max(0,Math.min(travel(),e.getX()-downX)));if(atEnd())endpoint();return true;
            case MotionEvent.ACTION_UP:
                if(!tracking)return false;handle.setTranslationX(Math.max(0,Math.min(travel(),e.getX()-downX)));boolean complete=atEnd();if(complete)endpoint();reset();if(complete)commit.getAsBoolean();return true;
            case MotionEvent.ACTION_CANCEL:reset();return true;
            default:return super.onTouchEvent(e);
        }
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);if(!enabled){info.setClassName(SeekBar.class.getName());info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,"ปัดเพื่อเริ่ม"));}}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(!enabled&&action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)return commit.getAsBoolean();return super.performAccessibilityAction(action,args);}
    private final class Splash extends View {
        float progress;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Splash(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){paint.setColor(AppTheme.onAccent(getContext()));paint.setAlpha((int)(180*(1-progress)));float x=getWidth()-dp(27),y=getHeight()/2f;for(int i=0;i<7;i++){double angle=Math.PI*2*i/7;float radius=dp(5+24*progress);canvas.drawCircle(x+(float)Math.cos(angle)*radius,y+(float)Math.sin(angle)*radius,dp(2.5f)*(1-progress*.7f),paint);}}
    }
}
