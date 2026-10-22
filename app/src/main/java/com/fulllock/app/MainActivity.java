package com.fulllock.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.math.BigDecimal;

public class MainActivity extends Activity {
    private final int orange=0xFFE94A19, ink=0xFF26272C, muted=0xFF81838B;
    private LinearLayout root, body, header;
    private FrameLayout frame;
    private View drawer;
    private boolean history=false;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener updates =
        (preferences,key) -> { if(history && "history".equals(key)) render(); };
    private android.content.SharedPreferences p() { return Store.prefs(this); }
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(orange); getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        p().registerOnSharedPreferenceChangeListener(updates);
        render();
    }
    @Override protected void onResume() { super.onResume(); render(); }
    @Override protected void onDestroy() { p().unregisterOnSharedPreferenceChangeListener(updates); super.onDestroy(); }
    private GradientDrawable bg(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView label(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setFontFeatureSettings("kern"); if(bold)t.setTypeface(null,Typeface.BOLD);return t;
    }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l; }
    private void pad(View v,int a,int b,int c,int d) { v.setPadding(dp(a),dp(b),dp(c),dp(d)); }
    private void space(LinearLayout l,int h) { View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h))); }
    private void line(LinearLayout l) { View v=new View(this);v.setBackgroundColor(0xFFF0F0F2);l.addView(v,new LinearLayout.LayoutParams(-1,dp(1))); }
    private void render() {
        frame=new FrameLayout(this);root=column();root.setBackgroundColor(0xFFF4F4F6);frame.addView(root);
        // Insets keep controls clear of Android 15 status and gesture bars.
        frame.setOnApplyWindowInsetsListener((v,insets)-> { v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets; });
        header=column();header.setBackgroundColor(orange);pad(header,22,8,22,22);root.addView(header);
        LinearLayout toolbar=row();TextView menu=label("☰",30,Color.WHITE,false);pad(menu,0,0,18,0);menu.setContentDescription("เปิดเมนูสิทธิ์และการตั้งค่า");menu.setOnClickListener(v->showDrawer());toolbar.addView(menu);
        TextView brand=label("FULL LOCK",17,Color.WHITE,true);brand.setLetterSpacing(.12f);toolbar.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView status=label(p().getBoolean("enabled",false)?"●  เปิดอยู่":"●  ปิดอยู่",12,Color.WHITE,false);toolbar.addView(status);header.addView(toolbar);
        space(header,15);header.addView(label(history?"ประวัติแจ้งเตือน":"ตั้งค่าการรับงาน",27,Color.WHITE,true));
        space(header,3);header.addView(label(history?"ติดตามผลการกรองและการเปิดงาน":"เลือกงานที่ใช่ แล้วปัดรับด้วยตัวคุณเอง",14,0xFFFFE2D7,false));
        space(header,14);LinearLayout chips=row();chip(chips,"▣  Lalamove Driver");chip(chips,"เปิดจากแจ้งเตือน");header.addView(chips);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=column();pad(body,16,14,16,16);scroll.addView(body);
        if(history) historyBody();else settingsBody();
        LinearLayout footer=column();footer.setBackgroundColor(Color.WHITE);pad(footer,18,12,18,14);View border=new View(this);border.setBackgroundColor(orange);footer.addView(border,new LinearLayout.LayoutParams(-1,dp(2)));space(footer,10);
        footer.addView(label("คุณเป็นคนปัดรับงานเอง",15,ink,true));space(footer,2);footer.addView(label("แอปช่วยกรองและเปิดงานที่ตรงเงื่อนไข",12,muted,false));space(footer,10);
        TextView start=label(p().getBoolean("enabled",false)?"■     หยุดการทำงาน":"→     เริ่มทำงาน",19,Color.WHITE,true);start.setGravity(Gravity.CENTER);start.setBackground(bg(orange,10));footer.addView(start,new LinearLayout.LayoutParams(-1,dp(54)));
        start.setOnClickListener(v->{boolean enabled=p().getBoolean("enabled",false);if(!enabled&&!access()){new AlertDialog.Builder(this).setTitle("อนุญาตการอ่านแจ้งเตือนก่อน").setMessage("เลือก Full Lock แล้วเปิดสิทธิ์การเข้าถึงการแจ้งเตือน").setPositiveButton("เปิดการตั้งค่า",(d,w)->open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)).setNegativeButton("ยกเลิก",null).show();return;}p().edit().putBoolean("enabled",!enabled).apply();if(!enabled)reconnect();render();});
        root.addView(footer);setContentView(frame);
    }
    private void chip(LinearLayout parent,String text) { TextView t=label(text,12,ink,false);pad(t,10,6,10,6);t.setBackground(bg(0xFFFFF3EE,5));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=dp(8);parent.addView(t,lp); }
    private LinearLayout card(String kicker,String title) {
        LinearLayout c=column();pad(c,18,16,18,12);c.setBackground(bg(Color.WHITE,8));c.setElevation(dp(1));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);body.addView(c,lp);
        if(kicker!=null){TextView k=label(kicker,11,orange,true);k.setLetterSpacing(.08f);c.addView(k);space(c,5);}
        if(title!=null){c.addView(label(title,19,ink,true));space(c,9);}return c;
    }
    private void toggle(LinearLayout c,String title,String subtitle,String key,boolean def) {
        LinearLayout r=row();pad(r,0,5,0,8);LinearLayout text=column();text.addView(label(title,16,ink,true));if(subtitle!=null){space(text,2);text.addView(label(subtitle,12,muted,false));}r.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        Switch s=new Switch(this);s.setButtonTintList(null);s.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));s.setTrackTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{orange,0xFFD9DADF}));s.setContentDescription(title);s.setChecked(p().getBoolean(key,def));s.setOnCheckedChangeListener((v,checked)->p().edit().putBoolean(key,checked).apply());r.addView(s);c.addView(r);
    }
    private void item(LinearLayout c,String title,String value,Runnable click) {
        LinearLayout r=row();pad(r,0,13,0,13);r.addView(label(title,14,ink,false),new LinearLayout.LayoutParams(0,-2,1));TextView val=label(value+"  ›",14,orange,false);val.setGravity(Gravity.END);val.setMaxWidth(dp(185));r.addView(val);r.setOnClickListener(v->click.run());c.addView(r);
    }
    private void settingsBody() {
        LinearLayout c=card("01  /  ราคางาน","฿  กำหนดช่วงราคา");toggle(c,"กรองราคางาน","เปิดเฉพาะงานในช่วงราคาที่กำหนด","price",true);line(c);
        LinearLayout prices=row();priceBox(prices,"ราคาต่ำสุด","min","100");priceBox(prices,"ราคาสูงสุด","max","2000");c.addView(prices);line(c);
        String[] formulas={"ราคาอย่างเดียว","ราคา + ทิป","ราคาหักค่าธรรมเนียม","ราคาหักค่าธรรมเนียม + ทิป"};
        item(c,"สูตรคำนวณ",formulas[p().getInt("formula",0)],()->choose("สูตรคำนวณราคา","formula",formulas));
        item(c,"ค่าธรรมเนียม",p().getInt("fee",20)+"%",()->number("ค่าธรรมเนียม (%)","fee",20,0,100));
        c=card("02  /  เส้นทาง","⌾  พื้นที่ที่ต้องการ");toggle(c,"ล็อกต้นทาง",null,"originOn",false);
        item(c,"○  ต้นทาง",areaValue("origins"),()->area("ต้นทาง","origins"));line(c);toggle(c,"ล็อกปลายทาง",null,"destOn",false);
        item(c,"●  ปลายทาง",areaValue("destinations"),()->area("ปลายทาง","destinations"));
        c=card("03  /  ตัวเลือกงาน","▣  รูปแบบงาน");String[] types={"ทั้งหมด","ขนส่งทันที","งานล่วงหน้า"};item(c,"ประเภทงาน",types[p().getInt("type",0)],()->choose("ประเภทงาน","type",types));line(c);
        toggle(c,"บล็อกงานเหมา","อ้างอิงรายการราคาในสคริปต์เดิม","bulk",false);line(c);
        item(c,"พักการเปิดงานซ้อน",p().getInt("cooldown",15)+" วินาที",()->number("พักการเปิดงานซ้อน (วินาที)","cooldown",15,3,300));
        c=card(null,null);c.addView(label("ⓘ  ไม่ต้องดึงแถบแจ้งเตือนลง",14,ink,true));space(c,5);c.addView(label("เก็บแจ้งเตือนเดิมไว้ และเปิดผ่านคำสั่งของแจ้งเตือนโดยตรง หากระบบไม่เปิด ให้แตะการแจ้งเตือนจาก Full Lock",12,muted,false));space(c,6);c.addView(label("รองรับราคา + ทิป เช่น ฿276.00 + ฿10.00 โดยหักค่าธรรมเนียมเฉพาะราคางาน",12,orange,false));
    }
    private String areaValue(String key) { String s=p().getString(key,"");return s.isBlank()?"ยังไม่กำหนด":s; }
    private void priceBox(LinearLayout parent,String title,String key,String def) {
        LinearLayout box=column();pad(box,10,14,10,14);box.addView(label(title,12,muted,false));space(box,4);box.addView(label("฿"+p().getString(key,def),27,ink,false));
        parent.addView(box,new LinearLayout.LayoutParams(0,-2,1));box.setOnClickListener(v->money(title,key,def));
    }
    private EditText input(String value,int type) { EditText e=new EditText(this);e.setText(value);e.setInputType(type);e.setSelectAllOnFocus(true);pad(e,22,15,22,15);return e; }
    private void money(String title,String key,String def) {
        EditText e=input(p().getString(key,def),8194);AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("บันทึก",null).setNegativeButton("ยกเลิก",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{try{BigDecimal n=new BigDecimal(e.getText().toString());BigDecimal other=new BigDecimal(p().getString(key.equals("min")?"max":"min",key.equals("min")?"2000":"100"));if(n.signum()<0||n.scale()>2|| (key.equals("min")?n.compareTo(other)>0:n.compareTo(other)<0))throw new Exception();p().edit().putString(key,n.toPlainString()).apply();d.dismiss();render();}catch(Exception ex){e.setError("ตรวจช่วงราคาและทศนิยมไม่เกิน 2 ตำแหน่ง");}}));d.show();
    }
    private void area(String title,String key) { EditText e=input(p().getString(key,""),1);new AlertDialog.Builder(this).setTitle(title).setMessage("คั่นชื่อพื้นที่ด้วย , เช่น เสาธงหิน,บางพูด").setView(e).setPositiveButton("บันทึก",(d,w)->{p().edit().putString(key,e.getText().toString().trim()).apply();render();}).setNegativeButton("ยกเลิก",null).show(); }
    private void number(String title,String key,int def,int min,int max) {
        EditText e=input(""+p().getInt(key,def),2);AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("บันทึก",null).setNegativeButton("ยกเลิก",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{try{int n=Integer.parseInt(e.getText().toString());if(n<min||n>max)throw new Exception();p().edit().putInt(key,n).apply();d.dismiss();render();}catch(Exception ex){e.setError("ระบุค่า "+min+"–"+max);}}));d.show();
    }
    private void choose(String title,String key,String[] options) { new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(options,p().getInt(key,0),(d,w)->{p().edit().putInt(key,w).apply();d.dismiss();render();}).setNegativeButton("ยกเลิก",null).show(); }
    private boolean access() { String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");if(enabled==null)return false;for(String entry:enabled.split(":")){ComponentName n=ComponentName.unflattenFromString(entry);if(n!=null&&n.getPackageName().equals(getPackageName()))return true;}return false; }
    private void open(String action) { try { startActivity(new Intent(action)); }catch(ActivityNotFoundException e){Toast.makeText(this,"เครื่องนี้ไม่มีหน้าตั้งค่านี้",Toast.LENGTH_LONG).show();} }
    private void showDrawer() {
        FrameLayout overlay=new FrameLayout(this);overlay.setBackgroundColor(0x88000000);frame.addView(overlay,new FrameLayout.LayoutParams(-1,-1));drawer=overlay;overlay.setOnClickListener(v->frame.removeView(overlay));
        ScrollView panel=new ScrollView(this);panel.setBackgroundColor(Color.WHITE);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(310),-1,Gravity.LEFT);overlay.addView(panel,lp);panel.setOnClickListener(v->{});
        LinearLayout nav=column();pad(nav,22,28,22,24);panel.addView(nav);nav.addView(label("FULL LOCK",24,orange,true));space(nav,5);nav.addView(label("จัดการแอปและการอนุญาตสิทธิ์",13,muted,false));space(nav,28);
        item(nav,"⌂  ตั้งค่าตัวกรอง","",()->{history=false;render();});item(nav,"◷  ประวัติแจ้งเตือน","",()->{history=true;render();});line(nav);space(nav,20);nav.addView(label("การอนุญาตสิทธิ์",16,ink,true));space(nav,12);
        item(nav,"อ่านแจ้งเตือน",access()?"อนุญาตแล้ว":"ยังไม่อนุญาต",()->open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        boolean canNotify=getSystemService(NotificationManager.class).areNotificationsEnabled();
        item(nav,"แจ้งเตือนงานที่พบ",canNotify?"อนุญาตแล้ว":"ยังไม่อนุญาต",()->{if(Build.VERSION.SDK_INT>=33&&!canNotify)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));});
        item(nav,"แสดงเหนือแอปอื่น",Settings.canDrawOverlays(this)?"อนุญาตแล้ว":"ตัวเลือกเสริม",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        nav.addView(label("สิทธิ์เสริมอาจช่วยให้เปิดหน้าจากเบื้องหลังได้ ขึ้นอยู่กับ Android และมือถือ",12,muted,false));
        item(nav,"การใช้แบตเตอรี่","เปิดตั้งค่า",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        space(nav,16);line(nav);space(nav,16);nav.addView(label("ทดสอบตัวกรอง",16,ink,true));item(nav,"แจ้งเตือนตัวอย่าง","ทดสอบ",()->sample());item(nav,"ตรวจการทำงาน","ตรวจสอบ",()->diagnostics());
        space(nav,22);nav.addView(label("Full Lock 0.1.3 · รุ่นทดสอบ\nทำงานแยกจาก Automagic\nไม่ปัดรับงานแทนผู้ใช้",12,muted,false));
    }
    private void sample() {
        new AlertDialog.Builder(this).setTitle("เลือกแจ้งเตือนตัวอย่าง").setItems(new String[]{"ไม่มีทิป · ฿156.00","มีทิป · ฿276.00 + ฿10.00","มีทิป · ฿129.00 + ฿18.00"},(dialog,index)->{
            String[] titles={"[ขนส่งสินค้า] เสาธงหิน > วังใหม่","[ขนส่งสินค้า] บางคูรัด > หัวหมาก","[ขนส่งสินค้า] คลองพระอุดม > ปลายทางตัวอย่าง"};
            String[] amounts={"฿156.00","฿276.00 + ฿10.00","฿129.00 + ฿18.00"};
            JobParser.Job j=JobParser.parse(titles[index],"ขนส่งทันที "+amounts[index]+" รับงานขนส่ง!");String r=Store.rejection(this,j);
            new AlertDialog.Builder(this).setTitle("ผลทดสอบตัวกรอง").setMessage(j.origin()+" → "+j.destination()+"\nราคางาน ฿"+j.fare()+"\nทิป ฿"+j.tip()+"\nราคาตามสูตร ฿"+j.price(p().getInt("formula",0),p().getInt("fee",20))+"\n\n"+(r==null?"ผ่านเงื่อนไข":r)+"\n\nข้อมูลตัวอย่าง ไม่เปิด Lalamove").setPositiveButton("ตกลง",null).show();
        }).show();
    }
    private void reconnect() {
        if(access() && !JobListener.connected) android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,JobListener.class));
    }
    private void diagnostics() {
        long last=p().getLong("lastReceived",0);
        String report="Full Lock 0.1.3\nเครื่อง: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid "+Build.VERSION.RELEASE+" ("+Build.VERSION.SDK_INT+")\nเปิดทำงาน: "+p().getBoolean("enabled",false)+"\nสิทธิ์อ่านแจ้งเตือน: "+access()+"\nตัวอ่านเชื่อมต่อ: "+JobListener.connected+"\nแจ้งเตือนของแอป: "+getSystemService(NotificationManager.class).areNotificationsEnabled()+"\nแสดงเหนือแอปอื่น: "+Settings.canDrawOverlays(this)+"\nได้รับจาก Lalamove ล่าสุด: "+(last==0?"ยังไม่เคย":java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(last)));
        try {JSONArray entries=new JSONArray(p().getString("history","[]"));if(entries.length()>0)report+="\nผลล่าสุด: "+entries.getJSONObject(0).getString("result");}catch(JSONException ignored){}
        final String details=report;
        new AlertDialog.Builder(this).setTitle("ตรวจการทำงาน").setMessage(details).setPositiveButton("คัดลอกข้อมูล",(d,w)->getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Full Lock diagnostics",details))).setNeutralButton("เชื่อมต่อตัวอ่านอีกครั้ง",(d,w)->{reconnect();Toast.makeText(this,"ส่งคำขอแล้ว ตรวจสถานะอีกครั้ง",Toast.LENGTH_LONG).show();}).setNegativeButton("ปิด",null).show();
    }
    private void historyBody() {
        try{JSONArray entries=new JSONArray(p().getString("history","[]"));if(entries.length()==0){LinearLayout c=card(null,"ยังไม่มีแจ้งเตือน");c.addView(label("เปิดสิทธิ์อ่านแจ้งเตือนและกดเริ่มทำงาน\nเมื่อมีงานเข้า ผลการตรวจจะแสดงที่นี่",14,muted,false));}
            for(int i=0;i<entries.length();i++){
                JSONObject e=entries.getJSONObject(i);LinearLayout c=card(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.MEDIUM).format(new java.util.Date(e.getLong("time"))),e.getString("title"));
                c.addView(label(e.getString("text"),14,ink,false));space(c,12);
                boolean known=e.has("worked"), worked=e.optBoolean("worked",false);
                int color=known?(worked?0xFF237A46:0xFFB44426):muted;
                TextView badge=label(known?(worked?"✓  ทำงาน · ส่งคำสั่งเปิดแล้ว":"−  ไม่ทำงาน · ไม่ส่งคำสั่งเปิด"):"ประวัติรุ่นก่อน · ดูผลด้านล่าง",14,color,true);
                pad(badge,10,7,10,7);badge.setBackground(bg(known?(worked?0xFFEAF6EE:0xFFFFF0E9):0xFFF1F1F3,5));c.addView(badge);space(c,9);
                c.addView(label(Store.shortReason(e.getString("result")),13,ink,false));
            }
            if(entries.length()>0){LinearLayout c=card(null,null);item(c,"ล้างประวัติ","",()->new AlertDialog.Builder(this).setMessage("ล้างประวัติแจ้งเตือนทั้งหมด?").setPositiveButton("ล้าง",(d,w)->{p().edit().remove("history").apply();render();}).setNegativeButton("ยกเลิก",null).show());}
        }catch(JSONException ignored){}
    }
}
