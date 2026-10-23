package com.fulllock.app;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Shared palette for the activity, dialogs, swipe control, and floating windows. */
final class AppTheme {
    static final int DEFAULT=0xFFE94A19;
    static int accent(Context c){return Store.prefs(c).getInt("themeColor",DEFAULT)|0xFF000000;}
    static int jobColor(Context c,JobParser.Job job){int color=accent(c);if(job!=null&&"งานล่วงหน้า".equals(job.type())){float[] hsv=new float[3];Color.colorToHSV(color,hsv);hsv[0]=(hsv[0]+180)%360;return Color.HSVToColor(hsv);}return color;}
    static boolean inverted(Context c){return Store.prefs(c).getBoolean("invertText",false);}
    static int ink(Context c){return inverted(c)?Color.WHITE:0xFF26272C;}
    static int muted(Context c){return inverted(c)?0xFFBDC1CA:0xFF777B85;}
    static int onAccent(Context c){return inverted(c)?Color.BLACK:Color.WHITE;}
    static int surface(Context c){return inverted(c)?0xFF24272E:Color.WHITE;}
    static int page(Context c){return inverted(c)?0xFF14171C:0xFFF4F4F6;}
    static int divider(Context c){return inverted(c)?0xFF3B3E46:0xFFEDEEF1;}
    static int tint(Context c){return blend(surface(c),accent(c),.11f);}
    static int blend(int base,int color,float amount){return Color.rgb(Math.round(Color.red(base)*(1-amount)+Color.red(color)*amount),Math.round(Color.green(base)*(1-amount)+Color.green(color)*amount),Math.round(Color.blue(base)*(1-amount)+Color.blue(color)*amount));}
    static GradientDrawable background(int color,int radiusPx){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radiusPx);return d;}
    static void styleDialog(Context c,AlertDialog dialog){
        if(dialog.getWindow()==null)return;
        dialog.getWindow().setBackgroundDrawable(background(surface(c),Math.round(14*c.getResources().getDisplayMetrics().density)));
        styleTree(c,dialog.getWindow().getDecorView());
        for(int which:new int[]{-1,-2,-3}){Button b=dialog.getButton(which);if(b!=null)b.setTextColor(accent(c));}
    }
    private static void styleTree(Context c,View v){
        if(v instanceof TextView){TextView t=(TextView)v;t.setTextColor(ink(c));t.setHintTextColor(muted(c));}
        if(v instanceof EditText)v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent(c)));
        if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)styleTree(c,group.getChildAt(i));}
    }
}
