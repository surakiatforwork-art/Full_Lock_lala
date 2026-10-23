package com.fulllock.app;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;
import java.util.*;

final class ProfileUiChecks {
    final Instrumentation test;MainActivity activity;int count;
    ProfileUiChecks(Instrumentation test){this.test=test;}
    void check(boolean ok,String why){if(!ok)throw new AssertionError(why);count++;}
    void settle()throws Exception{test.waitForIdleSync();Thread.sleep(250);}
    AccessibilityNodeInfo find(AccessibilityNodeInfo root,String text,String klass){if(root==null)return null;if((text==null||text.contentEquals(root.getText()==null?"":root.getText()))&&(klass==null||klass.contentEquals(root.getClassName())))return root;for(int i=0;i<root.getChildCount();i++){AccessibilityNodeInfo match=find(root.getChild(i),text,klass);if(match!=null)return match;}return null;}
    AccessibilityNodeInfo node(String text)throws Exception{for(int i=0;i<30;i++){AccessibilityNodeInfo found=find(test.getUiAutomation().getRootInActiveWindow(),text,null);if(found!=null)return found;Thread.sleep(100);}throw new AssertionError("Missing UI: "+text);}
    void click(String text)throws Exception{AccessibilityNodeInfo n=node(text);while(!n.isClickable()&&n.getParent()!=null)n=n.getParent();check(n.performAction(AccessibilityNodeInfo.ACTION_CLICK),"click "+text);settle();}
    void name(String value)throws Exception{AccessibilityNodeInfo field=find(test.getUiAutomation().getRootInActiveWindow(),null,"android.widget.EditText");check(field!=null,"profile name input");Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);check(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args),"enter profile name");}
    void selector()throws Exception{test.runOnMainSync(()->activity.getWindow().getDecorView().findViewWithTag("profile-selector").performClick());settle();}
    void manage()throws Exception{selector();click("จัดการ");}
    void importFile(File file)throws Exception{test.runOnMainSync(()->activity.onActivityResult(201,Activity.RESULT_OK,new Intent().setData(android.net.Uri.fromFile(file))));settle();}
    void capture(String name)throws Exception{settle();android.graphics.Bitmap bitmap=test.getUiAutomation().takeScreenshot();File output=new File(test.getTargetContext().getExternalFilesDir("validation"),name);try(FileOutputStream out=new FileOutputStream(output)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
    int run()throws Exception {
        Context c=test.getTargetContext();test.runOnMainSync(()->Store.prefs(c).edit().putBoolean("permissionsReviewed",true).putBoolean("enabled",false).apply());
        try{
            activity=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();check(activity.getWindow().getDecorView().findViewWithTag("profile-selector")!=null,"selector in filter header");
            selector();click("เพิ่ม");name("เที่ยวเช้า");click("บันทึก");check(Profiles.activeName().equals("เที่ยวเช้า"),"new profile selected");
            manage();click("เปลี่ยนชื่อ");name("เที่ยวเย็น");click("บันทึก");check(Profiles.activeName().equals("เที่ยวเย็น"),"rename active profile");String evening=Profiles.active();
            manage();click("ทำสำเนา");name("เที่ยวเสาร์");click("บันทึก");check(Profiles.activeName().equals("เที่ยวเสาร์"),"copy selected");String saturday=Profiles.active();
            test.runOnMainSync(()->Store.prefs(c).edit().putBoolean("enabled",true).apply());selector();click("เที่ยวเย็น");check(Profiles.active().equals(saturday),"live switch waits for confirmation");click("ยกเลิก");check(Profiles.active().equals(saturday),"cancel switch retains profile");selector();click("เที่ยวเย็น");click("เปลี่ยน");check(Profiles.active().equals(evening)&&Store.prefs(c).getBoolean("enabled",false),"confirmed switch keeps auto-open on");
            File imported=new File(c.getCacheDir(),"import-profile.flp");try(FileOutputStream out=new FileOutputStream(imported)){out.write(ProfileFile.encode(new ProfileFile.Data("เที่ยวเย็น",Map.of("origins","บางพูด,คลองสาม"))));}
            String before=Profiles.entries().toString();importFile(imported);node("โปรไฟล์ชื่อซ้ำ");capture("ui-profile-overwrite.png");click("ยกเลิก");check(before.equals(Profiles.entries().toString()),"cancel overwrite changes nothing");int size=Profiles.entries().length();importFile(imported);click("ติดตั้งทับ");check(Profiles.active().equals(evening)&&Profiles.entries().length()==size,"UI overwrite retains ID and count");check(Store.prefs(c).getString("origins","").equals("บางพูด,คลองสาม"),"UI overwrite applies file");
            File exported=new File(c.getCacheDir(),"export-profile.flp");test.runOnMainSync(()->activity.onActivityResult(202,Activity.RESULT_OK,new Intent().setData(android.net.Uri.fromFile(exported))));for(int i=0;i<50&&(!exported.exists()||exported.length()==0);i++)Thread.sleep(100);ProfileFile.Data roundTrip;try(FileInputStream in=new FileInputStream(exported)){roundTrip=ProfileFile.decode(ProfileFile.read(in));}check(roundTrip.name.equals("เที่ยวเย็น")&&roundTrip.values.get("origins").equals("บางพูด,คลองสาม"),"SAF-result export writes usable FLP");
            capture("ui-profile-header.png");manage();click("ลบโปรไฟล์");click("ลบ");check(!Profiles.active().equals(evening)&&Profiles.get(Profiles.active())!=null,"UI delete auto-selects another");
            imported.delete();exported.delete();
        }finally{if(activity!=null){test.runOnMainSync(()->activity.finish());settle();}}
        return count;
    }
}
