package com.fulllock.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** One small window, owned and removed by the notification listener. */
final class FloatingJobView {
    private final Context context;
    private final WindowManager wm;
    private LinearLayout card;
    private TextView heading, detail, status, hint;
    private WindowManager.LayoutParams params;
    private final Runnable open;
    private float downX,downY;
    private int startX,startY;
    private boolean dragged;
    FloatingJobView(Context context,Runnable open) {
        this.context=context;this.open=open;wm=context.getSystemService(WindowManager.class);
    }
    private int dp(float n) {return (int)(context.getResources().getDisplayMetrics().density*n+.5f);}
    private TextView text(int size,boolean bold) {
        TextView t=new TextView(context);t.setTextColor(Color.WHITE);t.setTextSize(size);
        if(bold)t.setTypeface(null,Typeface.BOLD);
        card.addView(t,new LinearLayout.LayoutParams(-1,-2));return t;
    }
    boolean visible() { return card!=null; }
    void show() {
        if(card!=null || !Settings.canDrawOverlays(context))return;
        card=new LinearLayout(context);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(11),dp(16),dp(11));card.setElevation(dp(8));
        GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xFFF37832,0xFFE94A19});
        background.setCornerRadius(dp(25));card.setBackground(background);
        heading=text(14,true);detail=text(12,false);status=text(11,true);hint=text(10,false);hint.setTextColor(0xFFFFE6D8);
        heading.setMaxLines(3);detail.setMaxLines(4);status.setMaxLines(2);
        for(TextView t:new TextView[]{heading,detail,status})t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        heading.setText("FULL LOCK · รอแจ้งเตือนงาน");detail.setText("ยังไม่มีงานใหม่หลังเริ่มทำงาน");status.setText("");hint.setText("ลากเพื่อย้ายตำแหน่ง");
        int width=Math.min(dp(285),context.getResources().getDisplayMetrics().widthPixels-dp(24));
        params=new WindowManager.LayoutParams(width,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.LEFT;
        params.x=Store.prefs(context).getInt("floatX",dp(12));params.y=Store.prefs(context).getInt("floatY",dp(160));
        card.setContentDescription("งานล่าสุดจาก Lalamove ลากเพื่อย้าย แตะเพื่อเปิดงาน");
        card.setOnClickListener(v->open.run());
        card.setOnTouchListener((v,event)->{
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX=event.getRawX();downY=event.getRawY();startX=params.x;startY=params.y;dragged=false;return true;
                case MotionEvent.ACTION_MOVE:
                    float dx=event.getRawX()-downX,dy=event.getRawY()-downY;
                    if(Math.hypot(dx,dy)>ViewConfiguration.get(context).getScaledTouchSlop())dragged=true;
                    if(dragged){params.x=startX+Math.round(dx);params.y=startY+Math.round(dy);clamp();update();}return true;
                case MotionEvent.ACTION_UP:
                    if(!dragged)v.performClick();
                    else Store.prefs(context).edit().putInt("floatX",params.x).putInt("floatY",params.y).apply();
                    return true;
                case MotionEvent.ACTION_CANCEL:return true;
                default:return false;
            }
        });
        clamp();
        try{wm.addView(card,params);card.post(()->{if(card!=null){clamp();update();}});}
        catch(SecurityException | WindowManager.BadTokenException e){card=null;}
    }
    private void clamp() {
        var display=context.getResources().getDisplayMetrics();
        params.x=Math.max(0,Math.min(params.x,display.widthPixels-params.width));
        params.y=Math.max(0,Math.min(params.y,display.heightPixels-(card==null?dp(160):Math.max(card.getHeight(),dp(160)))-dp(60)));
    }
    private void update(){if(card!=null)try{wm.updateViewLayout(card,params);}catch(IllegalArgumentException | SecurityException e){hide();}}
    void latest(String title,String raw,JobParser.Job job,boolean clickable) {
        show();if(card==null)return;
        heading.setText(title);
        detail.setText(job==null?raw:job.type()+" · "+job.category()+"\nราคางาน ฿"+job.fare()+"  + ทิป ฿"+job.tip()+"\nรวม ฿"+job.fare().add(job.tip()));
        hint.setText(clickable?"แตะเปิดงานนี้ · ลากเพื่อย้าย":"ไม่มีคำสั่งเปิดงาน · ลากเพื่อย้าย");
        card.post(()->{if(card!=null){clamp();update();}});
    }
    void result(boolean worked,String reason){if(card!=null)status.setText((worked?"✓ ":"• ")+Store.shortReason(reason));}
    void unavailable(){if(card!=null){status.setText("แจ้งเตือนนี้ถูกลบแล้ว");hint.setText("รอแจ้งเตือนงานใหม่ · ลากเพื่อย้าย");}}
    void hide(){if(card!=null){try{wm.removeViewImmediate(card);}catch(IllegalArgumentException ignored){}card=null;}}
}
