package com.fulllock.app;

import android.content.Context;
import android.graphics.Typeface;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.util.function.BooleanSupplier;

/** Commit only when a drag from the handle reaches the far end; taps never toggle. */
final class SwipeBar extends FrameLayout {
    private final TextView handle;
    private final BooleanSupplier commit;
    private float downX;
    private boolean tracking;
    SwipeBar(Context c,boolean enabled,BooleanSupplier commit){
        super(c);this.commit=commit;setTag("swipe-bar");setContentDescription(enabled?"ปัดเพื่อหยุด":"ปัดเพื่อเริ่ม");
        setBackground(AppTheme.background(AppTheme.accent(c),dp(12)));setPadding(dp(6),dp(6),dp(6),dp(6));
        TextView label=new TextView(c);label.setText(enabled?"ปัดเพื่อหยุด  ››":"ปัดเพื่อเริ่ม  ››");label.setTextColor(AppTheme.onAccent(c));label.setTextSize(17);label.setTypeface(null,Typeface.BOLD);label.setGravity(Gravity.CENTER);label.setPadding(dp(50),0,dp(5),0);addView(label,new LayoutParams(-1,-1));
        handle=new TextView(c);handle.setText("→");handle.setTextSize(28);handle.setGravity(Gravity.CENTER);handle.setTextColor(AppTheme.accent(c));handle.setBackground(AppTheme.background(AppTheme.surface(c),dp(8)));handle.setElevation(dp(2));addView(handle,new LayoutParams(dp(48),-1,Gravity.START));setFocusable(true);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private float travel(){return Math.max(0,getWidth()-getPaddingLeft()-getPaddingRight()-handle.getWidth());}
    private void reset(){handle.animate().translationX(0).setDuration(150).start();tracking=false;}
    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                if(e.getX()>getPaddingLeft()+handle.getWidth()+dp(8))return false;
                handle.animate().cancel();handle.setTranslationX(0);downX=e.getX();tracking=true;getParent().requestDisallowInterceptTouchEvent(true);return true;
            case MotionEvent.ACTION_MOVE:if(!tracking)return false;handle.setTranslationX(Math.max(0,Math.min(travel(),e.getX()-downX)));return true;
            case MotionEvent.ACTION_UP:
                if(!tracking)return false;boolean complete=travel()>0&&handle.getTranslationX()>=travel()*.85f;tracking=false;getParent().requestDisallowInterceptTouchEvent(false);
                if(complete){commit.getAsBoolean();}reset();return true;
            case MotionEvent.ACTION_CANCEL:reset();return true;
            default:return super.onTouchEvent(e);
        }
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName(SeekBar.class.getName());info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,"ปัดเพื่อยืนยัน"));}
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)return commit.getAsBoolean();return super.performAccessibilityAction(action,args);}
}
