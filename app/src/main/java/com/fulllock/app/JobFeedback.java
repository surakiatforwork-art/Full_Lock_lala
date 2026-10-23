package com.fulllock.app;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.*;
import android.view.*;
import android.widget.*;

final class JobFeedback {
    static void unavailable(Activity activity,View row){
        float dp=activity.getResources().getDisplayMetrics().density;
        ObjectAnimator shake=ObjectAnimator.ofFloat(row,"translationX",0,-9*dp,9*dp,-7*dp,7*dp,0);
        shake.setDuration(420);shake.start();
        android.os.Vibrator vibrator=activity.getSystemService(android.os.Vibrator.class);
        if(vibrator!=null&&vibrator.hasVibrator())vibrator.vibrate(VibrationEffect.createOneShot(90,VibrationEffect.DEFAULT_AMPLITUDE));
        FrameLayout decor=(FrameLayout)activity.getWindow().getDecorView();
        View previous=decor.findViewWithTag("job-unavailable");if(previous!=null)decor.removeView(previous);
        TextView banner=new TextView(activity);banner.setTag("job-unavailable");banner.setText("เปิดงานไม่ได้แล้ว");banner.setTextSize(16);banner.setTextColor(AppTheme.ink(activity));banner.setGravity(Gravity.CENTER);banner.setPadding((int)(20*dp),(int)(14*dp),(int)(20*dp),(int)(14*dp));banner.setBackground(AppTheme.background(AppTheme.surface(activity),(int)(12*dp)));banner.setElevation(12*dp);banner.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);params.topMargin=(int)(65*dp);decor.addView(banner,params);banner.setAlpha(0);banner.animate().alpha(1).setDuration(150).start();banner.postDelayed(()->banner.animate().alpha(0).setDuration(180).withEndAction(()->decor.removeView(banner)).start(),2200);
    }
}
