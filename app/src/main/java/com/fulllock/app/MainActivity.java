package com.fulllock.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.math.BigDecimal;
import org.json.*;

public class MainActivity extends Activity {
    static volatile boolean foreground;
    private enum Page { FILTERS, HISTORY, PERMISSIONS, THEME }
    private Page page=Page.FILTERS;
    private int accent,ink,muted;
    private LinearLayout root,body;
    private FrameLayout frame;
    private View drawer;
    private boolean unwantedExpanded;
    private String pendingExportId;
    private static final int IMPORT_PROFILE=201,EXPORT_PROFILE=202;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener updates=(prefs,key)->{
        if((page==Page.HISTORY && "history".equals(key)) || "themeColor".equals(key) || "invertText".equals(key))render();
    };
    private android.content.SharedPreferences p(){return Store.prefs(this);}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        if(!AppIntegrity.trusted(this)){new AlertDialog.Builder(this).setTitle("เปิดแอปนี้ไม่ได้").setMessage("โปรดติดตั้ง Full Lock จากผู้พัฒนาอีกครั้ง").setCancelable(false).setPositiveButton("ปิด",(d,w)->finish()).show();return;}
        if(state!=null)pendingExportId=state.getString("pendingExportId");
        if(state!=null){try{page=Page.valueOf(state.getString("page","FILTERS"));}catch(IllegalArgumentException ignored){}}
        else if(!p().getBoolean("permissionsReviewed",false))page=Page.PERMISSIONS;
        p().registerOnSharedPreferenceChangeListener(updates);render();
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putString("page",page.name());state.putString("pendingExportId",pendingExportId);super.onSaveInstanceState(state);}
    @Override protected void onStart(){super.onStart();foreground=true;JobListener.refreshFloating();}
    @Override protected void onResume(){super.onResume();if(!AppIntegrity.trusted(this))return;reconnect();JobListener.refreshFloating();render();}
    @Override protected void onStop(){foreground=false;JobListener.refreshFloating();super.onStop();}
    @Override protected void onDestroy(){p().unregisterOnSharedPreferenceChangeListener(updates);super.onDestroy();}
    @Override public void onBackPressed(){if(drawer!=null){closeDrawer();return;}if(page!=Page.FILTERS){page=Page.FILTERS;render();return;}super.onBackPressed();}
    private TextView label(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
    private void space(LinearLayout l,int h){l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private void line(LinearLayout l){View v=new View(this);v.setBackgroundColor(AppTheme.divider(this));l.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
    private void render(){
        accent=AppTheme.accent(this);ink=AppTheme.ink(this);muted=AppTheme.muted(this);drawer=null;
        int systemFlags=AppTheme.inverted(this)?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        getWindow().getDecorView().setSystemUiVisibility(systemFlags);getWindow().setStatusBarColor(AppTheme.page(this));getWindow().setNavigationBarColor(AppTheme.surface(this));
        frame=new FrameLayout(this);frame.setBackgroundColor(AppTheme.page(this));root=column();root.setBackgroundColor(AppTheme.page(this));frame.addView(root);
        frame.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        LinearLayout header=column();header.setBackgroundColor(accent);pad(header,22,8,22,20);root.addView(header);
        int on=AppTheme.onAccent(this);LinearLayout toolbar=row();TextView menu=label("☰",28,on,false);pad(menu,0,0,17,0);menu.setContentDescription("เปิดเมนู");menu.setTag("open-menu");menu.setOnClickListener(v->showDrawer());toolbar.addView(menu);
        TextView brand=label("FULL LOCK",17,on,true);brand.setGravity(Gravity.CENTER_VERTICAL);brand.setLetterSpacing(.10f);toolbar.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));
        String status=p().getBoolean("enabled",false)?"● อัตโนมัติ":"● อ่านงาน";toolbar.addView(label(status,12,on,false));header.addView(toolbar);space(header,13);
        String title=page==Page.HISTORY?"ประวัติงาน":page==Page.THEME?"ธีม":page==Page.PERMISSIONS?(p().getBoolean("permissionsReviewed",false)?"การอนุญาตสิทธิ์":"เตรียมใช้งาน"):"ตัวกรองงาน";
        header.addView(label(title,27,on,true));space(header,4);
        String subtitle=page==Page.HISTORY?"ดูงานที่เข้าและผลการเลือก":page==Page.THEME?"เลือกสีในแบบของคุณ":page==Page.PERMISSIONS?"เลือกสิ่งที่ต้องการให้แอปช่วย":"เลือกงานที่ใช่ แล้วปัดรับด้วยตัวคุณเอง";
        header.addView(label(subtitle,14,on,false));
        if(page==Page.FILTERS){space(header,9);TextView profile=label("โปรไฟล์: "+Profiles.activeName()+" ▾",14,on,true);profile.setTag("profile-selector");profile.setMaxLines(2);profile.setEllipsize(android.text.TextUtils.TruncateAt.END);pad(profile,10,8,10,8);profile.setBackground(AppTheme.background(AppTheme.blend(accent,on,.12f),dp(10)));profile.setOnClickListener(v->showProfiles());header.addView(profile,new LinearLayout.LayoutParams(-1,-2));}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=column();pad(body,16,14,16,16);scroll.addView(body);
        switch(page){case HISTORY:historyBody();break;case THEME:themeBody();break;case PERMISSIONS:permissionsBody();break;default:settingsBody();}
        if(page==Page.FILTERS||page==Page.HISTORY)footer();
        setContentView(frame);
    }
    private void footer(){
        LinearLayout footer=column();footer.setBackgroundColor(AppTheme.surface(this));pad(footer,16,10,16,12);View border=new View(this);border.setBackgroundColor(accent);footer.addView(border,new LinearLayout.LayoutParams(-1,dp(2)));space(footer,9);
        footer.addView(label("อ่านงานตลอด · คุณปัดรับงานเอง",14,ink,true));space(footer,3);
        footer.addView(label(p().getBoolean("enabled",false)?"เปิดงานที่ตรงเงื่อนไขให้อัตโนมัติ":"ยังไม่เปิดงานให้ · แตะงานในปุ่มลอยเพื่อเปิดเอง",11,muted,false));space(footer,9);
        SwipeBar swipe=new SwipeBar(this,p().getBoolean("enabled",false),()->{
            boolean current=p().getBoolean("enabled",false);
            if(!current&&!PermissionSetup.reading(this)){page=Page.PERMISSIONS;render();Toast.makeText(this,"อนุญาตอ่านแจ้งเตือนก่อน",Toast.LENGTH_SHORT).show();return false;}
            p().edit().putBoolean("enabled",!current).apply();reconnect();render();return true;
        });footer.addView(swipe,new LinearLayout.LayoutParams(-1,dp(60)));root.addView(footer);
    }
    private LinearLayout card(String number,String title){
        LinearLayout outer=column();outer.setBackground(AppTheme.background(AppTheme.surface(this),dp(18)));outer.setClipToOutline(true);outer.setElevation(dp(2));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);body.addView(outer,lp);
        boolean band=page==Page.FILTERS&&title!=null;
        if(band){LinearLayout heading=column();pad(heading,17,10,17,10);heading.setBackgroundColor(accent);if(number!=null)heading.addView(label(number,11,AppTheme.onAccent(this),true));heading.addView(label(title,18,AppTheme.onAccent(this),true));outer.addView(heading);}
        LinearLayout content=column();pad(content,17,band?7:15,17,12);outer.addView(content);
        if(!band){if(number!=null){content.addView(label(number,11,accent,true));space(content,4);}if(title!=null){content.addView(label(title,19,ink,true));space(content,8);}}
        return content;
    }
    private void toggle(LinearLayout parent,String title,String subtitle,String key,boolean fallback){
        LinearLayout r=row();pad(r,0,9,0,9);LinearLayout texts=column();texts.addView(label(title,16,ink,true));if(subtitle!=null){space(texts,2);texts.addView(label(subtitle,12,muted,false));}r.addView(texts,new LinearLayout.LayoutParams(0,-2,1));
        Switch control=new Switch(this);control.setTag(key);control.setContentDescription(title);control.setThumbTintList(android.content.res.ColorStateList.valueOf(AppTheme.surface(this)));control.setTrackTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{accent,AppTheme.divider(this)}));control.setChecked(p().getBoolean(key,fallback));
        control.setOnCheckedChangeListener((v,checked)->{p().edit().putBoolean(key,checked).apply();if("floatingEnabled".equals(key)&&checked&&!Settings.canDrawOverlays(this)){page=Page.PERMISSIONS;render();}});r.addView(control);parent.addView(r);
    }
    private View item(LinearLayout parent,String title,String value,Runnable action){
        LinearLayout r=row();pad(r,0,13,0,13);r.addView(label(title,14,ink,false),new LinearLayout.LayoutParams(0,-2,1));TextView detail=label(value.isEmpty()?"›":value+"  ›",13,accent,false);detail.setGravity(Gravity.END);detail.setMaxWidth(dp(160));r.addView(detail);r.setOnClickListener(v->action.run());parent.addView(r);return r;
    }
    private void settingsBody(){
        LinearLayout c=card("01 / ราคา","฿ ช่วงราคาที่ต้องการ");toggle(c,"เลือกราคา","รับเฉพาะช่วงราคาที่ตั้งไว้","price",true);line(c);
        LinearLayout prices=row();priceBox(prices,"ต่ำสุด","min","100");priceBox(prices,"สูงสุด","max","2000");c.addView(prices);line(c);
        String[] formulas={"ราคางาน","ราคางาน + ทิป","หักค่าธรรมเนียม","หักค่าธรรมเนียม + ทิป"};
        item(c,"ราคาที่ใช้เทียบ",formulas[Math.max(0,Math.min(3,p().getInt("formula",0)))],()->choose("ราคาที่ใช้เทียบ","formula",formulas));item(c,"ค่าธรรมเนียม",p().getInt("fee",20)+"%",()->number("ค่าธรรมเนียม (%)","fee",20,0,100));
        c=card("02 / พื้นที่","⌾ เส้นทางที่ต้องการ");toggle(c,"เลือกต้นทาง",null,"originOn",false);item(c,"○ ต้นทาง",areaValue("origins"),()->area("ต้นทาง","origins"));line(c);toggle(c,"เลือกปลายทาง",null,"destOn",false);item(c,"● ปลายทาง",areaValue("destinations"),()->area("ปลายทาง","destinations"));
        line(c);LinearLayout excluded=column();excluded.setTag("excluded-routes");c.addView(excluded);
        TextView expand=label((unwantedExpanded?"▴ ":"▾ ")+"เส้นทางที่ไม่ต้องการ",15,accent,true);expand.setTag("excluded-toggle");pad(expand,0,13,0,13);excluded.addView(expand);
        LinearLayout blocked=column();blocked.setTag("excluded-settings");blocked.setVisibility(unwantedExpanded?View.VISIBLE:View.GONE);excluded.addView(blocked);expand.setOnClickListener(v->{unwantedExpanded=!unwantedExpanded;blocked.setVisibility(unwantedExpanded?View.VISIBLE:View.GONE);expand.setText((unwantedExpanded?"▴ ":"▾ ")+"เส้นทางที่ไม่ต้องการ");});
        blocked.addView(label("ข้ามพื้นที่เหล่านี้ แม้ตรงเส้นทางที่ต้องการ",12,muted,false));toggle(blocked,"ไม่รับต้นทาง",null,"excludeOriginOn",false);item(blocked,"○ ต้นทาง",areaValue("excludedOrigins"),()->area("ต้นทางที่ไม่ต้องการ","excludedOrigins"));line(blocked);toggle(blocked,"ไม่รับปลายทาง",null,"excludeDestOn",false);item(blocked,"● ปลายทาง",areaValue("excludedDestinations"),()->area("ปลายทางที่ไม่ต้องการ","excludedDestinations"));
        c=card("03 / งาน","▣ ประเภทงาน");String[] types={"ทั้งหมด","ขนส่งทันที","งานล่วงหน้า"};item(c,"เวลา",types[Math.max(0,Math.min(2,p().getInt("type",0)))],()->choose("ประเภทงาน","type",types));line(c);toggle(c,"ไม่รับงานเหมา","ดูจากราคางานที่กำหนดไว้","bulk",false);
        c=card(null,null);c.addView(label("ⓘ งานที่ตรงเงื่อนไขจะแสดง ✓",13,ink,true));space(c,5);c.addView(label("อ่านงานได้แม้ยังไม่เริ่มอัตโนมัติ\nค่าธรรมเนียมหักเฉพาะราคางาน ไม่หักทิป",12,muted,false));
    }
    private String areaValue(String key){String value=p().getString(key,"");return value.isBlank()?"ยังไม่เลือก":value;}
    private void priceBox(LinearLayout parent,String title,String key,String fallback){LinearLayout box=column();pad(box,8,14,8,14);box.addView(label(title,12,muted,false));space(box,4);box.addView(label("฿"+p().getString(key,fallback),26,ink,false));parent.addView(box,new LinearLayout.LayoutParams(0,-2,1));box.setOnClickListener(v->money(title,key,fallback));}
    private AlertDialog dialog(AlertDialog.Builder builder){AlertDialog d=builder.create();d.show();AppTheme.styleDialog(this,d);return d;}
    private EditText input(String value,int type){EditText e=new EditText(this);e.setText(value);e.setInputType(type);e.setSelectAllOnFocus(true);e.setTextColor(ink);pad(e,22,12,22,12);return e;}
    private void money(String title,String key,String fallback){
        EditText e=input(p().getString(key,fallback),8194);AlertDialog d=dialog(new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("บันทึก",null).setNegativeButton("ยกเลิก",null));
        d.getButton(-1).setOnClickListener(v->{try{BigDecimal n=new BigDecimal(e.getText().toString());BigDecimal other=new BigDecimal(p().getString(key.equals("min")?"max":"min",key.equals("min")?"2000":"100"));if(n.signum()<0||n.scale()>2||(key.equals("min")?n.compareTo(other)>0:n.compareTo(other)<0))throw new IllegalArgumentException();p().edit().putString(key,n.toPlainString()).apply();d.dismiss();render();}catch(Exception ex){e.setError("ตรวจช่วงราคาอีกครั้ง ใช้ทศนิยมได้ 2 ตำแหน่ง");}});
    }
    private void area(String title,String key){EditText e=input(p().getString(key,""),1);dialog(new AlertDialog.Builder(this).setTitle(title).setMessage("คั่นชื่อพื้นที่ด้วย , เช่น เสาธงหิน,บางพูด").setView(e).setPositiveButton("บันทึก",(d,w)->{p().edit().putString(key,e.getText().toString().trim()).apply();render();}).setNegativeButton("ยกเลิก",null));}
    private void number(String title,String key,int fallback,int min,int max){EditText e=input(""+p().getInt(key,fallback),2);AlertDialog d=dialog(new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("บันทึก",null).setNegativeButton("ยกเลิก",null));d.getButton(-1).setOnClickListener(v->{try{int n=Integer.parseInt(e.getText().toString());if(n<min||n>max)throw new IllegalArgumentException();p().edit().putInt(key,n).apply();d.dismiss();render();}catch(Exception ex){e.setError("เลือกค่า "+min+"–"+max);}});}
    private void choose(String title,String key,String[] options){dialog(new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(options,p().getInt(key,0),(d,w)->{p().edit().putInt(key,w).apply();d.dismiss();render();}).setNegativeButton("ยกเลิก",null));}
    private void permissionsBody(){
        boolean first=!p().getBoolean("permissionsReviewed",false);LinearLayout intro=card(null,first?"เริ่มจากการอ่านงาน":"ตั้งค่าสิทธิ์");intro.addView(label("แตะแต่ละข้อเพื่อเปิดการตั้งค่าของเครื่อง\nเลือกทำภายหลังได้ และกลับมาแก้จากเมนู ☰",13,muted,false));
        for(int i=0;i<PermissionSetup.TITLES.length;i++){
            final int step=i;LinearLayout c=card(String.format(java.util.Locale.ROOT,"%02d",i+1),PermissionSetup.TITLES[i]);c.addView(label(PermissionSetup.DETAILS[i],13,muted,false));space(c,7);
            item(c,PermissionSetup.ready(this,i)?"✓ พร้อมแล้ว":"ยังไม่เปิด",PermissionSetup.ready(this,i)?"จัดการ":"เปิดตั้งค่า",()->PermissionSetup.open(this,step));
        }
        LinearLayout c=card(null,null);TextView done=action(first?"เริ่มใช้งาน":"กลับไปตัวกรอง",()->{p().edit().putBoolean("permissionsReviewed",true).apply();page=Page.FILTERS;reconnect();render();});done.setTag("finish-permissions");c.addView(done,new LinearLayout.LayoutParams(-1,dp(48)));
        if(first){space(c,8);TextView skip=label("ไว้ภายหลัง",13,muted,false);skip.setGravity(Gravity.CENTER);pad(skip,0,10,0,10);skip.setOnClickListener(v->{p().edit().putBoolean("permissionsReviewed",true).apply();page=Page.FILTERS;render();});c.addView(skip);}
    }
    private TextView action(String text,Runnable click){TextView t=label(text,15,AppTheme.onAccent(this),true);t.setGravity(Gravity.CENTER);t.setBackground(AppTheme.background(accent,dp(8)));t.setOnClickListener(v->click.run());return t;}
    private void closeDrawer(){if(drawer!=null){frame.removeView(drawer);drawer=null;}}
    private void showDrawer(){
        if(drawer!=null)return;
        FrameLayout overlay=new FrameLayout(this);overlay.setBackgroundColor(0x88000000);frame.addView(overlay,new FrameLayout.LayoutParams(-1,-1));drawer=overlay;overlay.setOnClickListener(v->closeDrawer());
        ScrollView panel=new ScrollView(this);panel.setBackgroundColor(AppTheme.surface(this));overlay.addView(panel,new FrameLayout.LayoutParams(Math.min(dp(320),getResources().getDisplayMetrics().widthPixels-dp(36)),-1,Gravity.START));panel.setOnClickListener(v->{});
        LinearLayout nav=column();pad(nav,22,25,22,22);nav.setTag("drawer-content");panel.addView(nav);nav.addView(label("FULL LOCK",24,accent,true));space(nav,5);nav.addView(label("เลือกงานในแบบของคุณ",13,muted,false));space(nav,20);
        item(nav,"⌂ ตัวกรองงาน","",()->{page=Page.FILTERS;render();});item(nav,"◷ ประวัติงาน","",()->{page=Page.HISTORY;render();});line(nav);space(nav,8);
        toggle(nav,"ปุ่มลอย","ดูจำนวนงานและ 10 งานล่าสุด","floatingEnabled",true);transitionSettings(nav);space(nav,10);line(nav);space(nav,17);
        nav.addView(label("การอนุญาตสิทธิ์",16,ink,true));space(nav,8);
        for(int i=0;i<PermissionSetup.TITLES.length;i++){final int step=i;item(nav,PermissionSetup.TITLES[i],PermissionSetup.ready(this,i)?"✓":"ตั้งค่า",()->PermissionSetup.open(this,step));}
        item(nav,"ดูคำแนะนำการตั้งค่า","",()->{page=Page.PERMISSIONS;render();});space(nav,12);line(nav);space(nav,12);
        item(nav,"ลองตัวกรอง","",this::sample);item(nav,"ตรวจการทำงาน","",this::diagnostics);space(nav,15);nav.addView(label("Full Lock "+version(),11,muted,false));space(nav,3);nav.addView(label("Dev by PHANToM",10,muted,false));space(nav,14);line(nav);
        View theme=item(nav,"◐ ธีม","",()->{page=Page.THEME;render();});theme.setTag("theme-menu");
    }
    private void transitionSettings(LinearLayout nav){
        TextView expand=label("▾ ความเร็วการกาง–หุบ",13,muted,false);expand.setTag("transition-toggle");pad(expand,0,8,0,8);nav.addView(expand);
        LinearLayout controls=column();controls.setVisibility(View.GONE);controls.setTag("transition-settings");nav.addView(controls);expand.setOnClickListener(v->{boolean show=controls.getVisibility()!=View.VISIBLE;controls.setVisibility(show?View.VISIBLE:View.GONE);expand.setText((show?"▴ ":"▾ ")+"ความเร็วการกาง–หุบ");});
        int current=Math.max(0,Math.min(1000,p().getInt("transitionMs",200)));TextView value=label(String.format(java.util.Locale.US,"%.2f วินาที",current/1000f),12,ink,false);value.setTag("transition-value");controls.addView(value);
        SeekBar slider=new SeekBar(this);slider.setTag("transition-slider");slider.setContentDescription("เวลากางและหุบปุ่มลอย 0 ถึง 1 วินาที");slider.setMax(100);slider.setProgress(current/10);slider.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));slider.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));controls.addView(slider,new LinearLayout.LayoutParams(-1,dp(34)));slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int progress,boolean user){value.setText(String.format(java.util.Locale.US,"%.2f วินาที",progress/100f));if(user)p().edit().putInt("transitionMs",progress*10).apply();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});controls.addView(label("0.00 ทันที                         1.00 ช้า",10,muted,false));
    }
    private void showProfiles(){
        JSONArray list=Profiles.entries();String[] names=new String[list.length()];int selected=0;for(int i=0;i<list.length();i++){JSONObject item=list.optJSONObject(i);names[i]=item.optString("name");if(item.optString("id").equals(Profiles.active()))selected=i;}
        dialog(new AlertDialog.Builder(this).setTitle("เลือกโปรไฟล์").setSingleChoiceItems(names,selected,(d,which)->{d.dismiss();requestProfile(list.optJSONObject(which).optString("id"));}).setPositiveButton("เพิ่ม",(d,w)->nameProfile(null,null)).setNeutralButton("จัดการ",(d,w)->manageProfile()).setNegativeButton("นำเข้า .flp",(d,w)->importPicker()));
    }
    private void requestProfile(String id){
        if(id.equals(Profiles.active()))return;
        Runnable select=()->{Profiles.select(id);unwantedExpanded=false;render();};
        if(p().getBoolean("enabled",false))dialog(new AlertDialog.Builder(this).setTitle("เปลี่ยนโปรไฟล์?").setMessage("กำลังเปิดงานอัตโนมัติ เปลี่ยนเป็น “"+Profiles.get(id).optString("name")+"” หรือไม่?\nเงื่อนไขใหม่จะใช้กับงานที่เข้าหลังเปลี่ยน").setPositiveButton("เปลี่ยน",(d,w)->select.run()).setNegativeButton("ยกเลิก",null));else select.run();
    }
    private void manageProfile(){
        String id=Profiles.active();dialog(new AlertDialog.Builder(this).setTitle(Profiles.activeName()).setItems(new String[]{"เปลี่ยนชื่อ","ทำสำเนา","ส่งออก .flp","ลบโปรไฟล์"},(d,index)->{if(index==0)nameProfile(id,null);else if(index==1)nameProfile(null,id);else if(index==2)exportPicker(id);else deleteProfile(id);}).setNegativeButton("ปิด",null));
    }
    private void nameProfile(String rename,String copy){
        String initial=rename!=null?Profiles.get(rename).optString("name"):copy!=null?Profiles.get(copy).optString("name")+" สำเนา":"";EditText name=input(initial,1);name.setHint("ชื่อโปรไฟล์");name.setTag("profile-name-input");
        AlertDialog d=dialog(new AlertDialog.Builder(this).setTitle(rename!=null?"เปลี่ยนชื่อโปรไฟล์":copy!=null?"ทำสำเนาโปรไฟล์":"เพิ่มโปรไฟล์").setView(name).setPositiveButton("บันทึก",null).setNegativeButton("ยกเลิก",null));
        d.getButton(-1).setOnClickListener(v->{try{if(rename!=null){Profiles.rename(rename,name.getText().toString());d.dismiss();render();}else{String id=Profiles.create(name.getText().toString(),copy);d.dismiss();requestProfile(id);}}catch(IllegalArgumentException e){name.setError(e.getMessage());}});
    }
    private void deleteProfile(String id){
        if(Profiles.entries().length()==1){Toast.makeText(this,"ต้องมีอย่างน้อย 1 โปรไฟล์",Toast.LENGTH_SHORT).show();return;}
        String message="ลบ “"+Profiles.get(id).optString("name")+"” หรือไม่?\nแอปจะเลือกโปรไฟล์อื่นให้โดยอัตโนมัติ"+(p().getBoolean("enabled",false)?"\nเงื่อนไขใหม่จะใช้กับงานที่เข้าหลังลบ":"");
        dialog(new AlertDialog.Builder(this).setTitle("ลบโปรไฟล์?").setMessage(message).setPositiveButton("ลบ",(d,w)->{Profiles.delete(id);render();}).setNegativeButton("ยกเลิก",null));
    }
    private void importPicker(){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),IMPORT_PROFILE);}catch(ActivityNotFoundException e){Toast.makeText(this,"เปิดตัวเลือกไฟล์ไม่ได้",Toast.LENGTH_SHORT).show();}}
    private void exportPicker(String id){pendingExportId=id;String name=Profiles.get(id).optString("name").replaceAll("[^\\p{L}\\p{N}._ -]","_");try{startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE,name+".flp"),EXPORT_PROFILE);}catch(ActivityNotFoundException e){Toast.makeText(this,"เปิดตัวเลือกไฟล์ไม่ได้",Toast.LENGTH_SHORT).show();}}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        if(request==IMPORT_PROFILE)new Thread(()->{try(java.io.InputStream input=getContentResolver().openInputStream(uri)){if(input==null)throw new java.io.IOException();ProfileFile.Data imported=ProfileFile.decode(ProfileFile.read(input));runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())confirmImport(imported);});}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"นำเข้าไม่ได้ ไฟล์เสียหายหรือไม่รองรับ",Toast.LENGTH_LONG).show());}},"profile-import").start();
        if(request==EXPORT_PROFILE){JSONObject profile=Profiles.get(pendingExportId==null?Profiles.active():pendingExportId);pendingExportId=null;if(profile==null)return;ProfileFile.Data exported=Profiles.data(profile);new Thread(()->{try(java.io.OutputStream output=getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new java.io.IOException();output.write(ProfileFile.encode(exported));runOnUiThread(()->Toast.makeText(this,"ส่งออกโปรไฟล์แล้ว",Toast.LENGTH_SHORT).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"ส่งออกโปรไฟล์ไม่ได้",Toast.LENGTH_LONG).show());}},"profile-export").start();}
    }
    private void confirmImport(ProfileFile.Data imported){
        boolean duplicate=Profiles.named(imported.name)!=null;String message=duplicate?"โปรไฟล์นี้มีอยู่แล้ว ต้องการติดตั้งทับหรือไม่?\n“"+imported.name+"”":"ติดตั้งโปรไฟล์ “"+imported.name+"” และเลือกใช้งานหรือไม่?\nช่วงราคา ฿"+imported.values.get("min")+"–฿"+imported.values.get("max");
        if(p().getBoolean("enabled",false))message+="\nกำลังเปิดงานอัตโนมัติ เงื่อนไขใหม่จะใช้กับงานที่เข้าหลังติดตั้ง";
        dialog(new AlertDialog.Builder(this).setTitle(duplicate?"โปรไฟล์ชื่อซ้ำ":"นำเข้าโปรไฟล์").setMessage(message).setPositiveButton(duplicate?"ติดตั้งทับ":"ติดตั้ง",(d,w)->{try{Profiles.install(imported,duplicate);unwantedExpanded=false;render();Toast.makeText(this,"เลือกโปรไฟล์แล้ว",Toast.LENGTH_SHORT).show();}catch(IllegalArgumentException e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}).setNegativeButton("ยกเลิก",null));
    }
    private void themeBody(){
        LinearLayout note=card(null,null);note.addView(label("ธีมของโปรไฟล์: "+Profiles.activeName(),13,ink,true));
        LinearLayout c=card(null,"สีของแอป");LinearLayout preview=column();pad(preview,16,14,16,14);preview.setBackground(AppTheme.background(accent,dp(10)));preview.addView(label("FULL LOCK",18,AppTheme.onAccent(this),true));space(preview,4);preview.addView(label("ตัวอย่างสีที่เลือก",13,AppTheme.onAccent(this),false));c.addView(preview);space(c,15);
        String[] names={"ส้ม","น้ำเงิน","เขียว","ม่วง","ชมพู","ฟ้า","แดง","เทา"};int[] colors={AppTheme.DEFAULT,0xFF2563EB,0xFF16845A,0xFF7C3AED,0xFFDB2777,0xFF0891B2,0xFFDC2626,0xFF52525B};
        for(int r=0;r<2;r++){LinearLayout palette=row();for(int j=0;j<4;j++){int index=r*4+j;TextView swatch=label(names[index],12,AppTheme.onAccent(this),true);swatch.setGravity(Gravity.CENTER);swatch.setBackground(AppTheme.background(colors[index],dp(7)));swatch.setTag("theme-preset-"+index);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(43),1);lp.setMargins(dp(3),dp(3),dp(3),dp(3));palette.addView(swatch,lp);swatch.setOnClickListener(v->p().edit().putInt("themeColor",colors[index]).apply());}c.addView(palette);}
        item(c,"เลือกสีเอง","",this::colorPicker);
        c=card(null,"สีข้อความ");toggle(c,"สลับขาว–ดำ","สลับสีข้อความทั้งแอปและปุ่มลอย","invertText",false);space(c,4);c.addView(label("เครื่องหมาย ✓ ยังคงเป็นสีเขียว",12,muted,false));
        c=card(null,null);item(c,"คืนค่าสีเดิม","",()->dialog(new AlertDialog.Builder(this).setMessage("กลับไปใช้สีส้มและข้อความเดิม?").setPositiveButton("คืนค่า",(d,w)->p().edit().remove("themeColor").remove("invertText").apply()).setNegativeButton("ยกเลิก",null)));
    }
    private void colorPicker(){
        LinearLayout content=column();pad(content,20,10,20,10);float[] hsv=new float[3];Color.colorToHSV(accent,hsv);
        TextView preview=label("FULL LOCK",19,AppTheme.onAccent(this),true);preview.setGravity(Gravity.CENTER);content.addView(preview,new LinearLayout.LayoutParams(-1,dp(65)));space(content,10);
        Runnable update=()->{preview.setBackground(AppTheme.background(Color.HSVToColor(hsv),dp(8)));preview.setTextColor(AppTheme.onAccent(this));};update.run();String[] titles={"เฉดสี","ความสด","ความสว่าง"};
        for(int i=0;i<3;i++){final int index=i;content.addView(label(titles[i],13,ink,true));SeekBar slider=new SeekBar(this);slider.setMax(i==0?360:100);slider.setProgress(Math.round(hsv[i]*(i==0?1:100)));slider.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));slider.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));slider.setContentDescription(titles[i]);content.addView(slider,new LinearLayout.LayoutParams(-1,dp(44)));slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int value,boolean user){hsv[index]=index==0?value:value/100f;update.run();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});}
        dialog(new AlertDialog.Builder(this).setTitle("เลือกสีเอง").setView(content).setPositiveButton("ใช้สีนี้",(d,w)->p().edit().putInt("themeColor",Color.HSVToColor(hsv)).apply()).setNegativeButton("ยกเลิก",null));update.run();
    }
    private void sample(){dialog(new AlertDialog.Builder(this).setTitle("ลองงานตัวอย่าง").setItems(new String[]{"฿156.00 ไม่มีทิป","฿276.00 + ทิป ฿10.00","฿129.00 + ทิป ฿18.00"},(d,index)->{
        String[] routes={"[ขนส่งสินค้า] เสาธงหิน > วังใหม่","[ขนส่งสินค้า] บางคูรัด > หัวหมาก","[ขนส่งสินค้า] คลองพระอุดม > ปลายทางตัวอย่าง"};String[] amounts={"฿156.00","฿276.00 + ฿10.00","฿129.00 + ฿18.00"};JobParser.Job job=JobParser.parse(routes[index],"ขนส่งทันที "+amounts[index]+" รับงานขนส่ง!");String reason=Store.rejection(this,job);
        dialog(new AlertDialog.Builder(this).setTitle(reason==null?"✓ ตรงเงื่อนไข":"ไม่ตรงเงื่อนไข").setMessage(job.origin()+" → "+job.destination()+"\nราคางาน ฿"+job.fare()+" + ทิป ฿"+job.tip()+"\nใช้เทียบ ฿"+job.price(p().getInt("formula",0),p().getInt("fee",20))+(reason==null?"":"\n\n"+reason)+"\n\nเป็นตัวอย่าง ไม่เปิดงานจริง").setPositiveButton("ตกลง",null));
    }));}
    private String version(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){return "";}}
    private String state(boolean enabled){return enabled?"เปิด":"ปิด";}
    private void reconnect(){if(PermissionSetup.reading(this)&&!JobListener.connected)android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,JobListener.class));}
    private void diagnostics(){long last=p().getLong("lastReceived",0);String report="Full Lock "+version()+"\nเครื่อง: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid "+Build.VERSION.RELEASE+"\nเปิดงานอัตโนมัติ: "+state(p().getBoolean("enabled",false))+"\nอ่านแจ้งเตือน: "+(JobListener.connected?"พร้อม":"ยังไม่พร้อม")+"\nปุ่มลอย: "+state(p().getBoolean("floatingEnabled",true))+"\nแสดงปุ่มลอยได้: "+(Settings.canDrawOverlays(this)?"ได้":"ยังไม่ได้")+"\nซ่อนเมื่อเปิด Lalamove: "+(ForegroundWatcher.allowed(this)?"พร้อม":"ยังไม่ได้อนุญาต")+"\nงานเข้าล่าสุด: "+(last==0?"ยังไม่มี":java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(last)));try{JSONArray entries=new JSONArray(p().getString("history","[]"));if(entries.length()>0)report+="\nผลล่าสุด: "+Store.shortReason(entries.getJSONObject(0).getString("result"));}catch(JSONException ignored){}final String details=report;dialog(new AlertDialog.Builder(this).setTitle("ตรวจการทำงาน").setMessage(details).setPositiveButton("คัดลอก",(d,w)->getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Full Lock",details))).setNeutralButton("ลองเชื่อมต่อใหม่",(d,w)->{reconnect();Toast.makeText(this,"ลองอ่านงานอีกครั้งแล้ว",Toast.LENGTH_SHORT).show();}).setNegativeButton("ปิด",null));}
    private void historyBody(){
        try{JSONArray entries=new JSONArray(p().getString("history","[]"));if(entries.length()==0){LinearLayout c=card(null,"ยังไม่มีงานเข้า");c.addView(label("เมื่อมีงานใหม่ ผลการเลือกจะแสดงที่นี่",14,muted,false));}
            for(int i=0;i<entries.length();i++){JSONObject entry=entries.getJSONObject(i);LinearLayout c=card(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.MEDIUM).format(new java.util.Date(entry.getLong("time"))),entry.getString("title"));c.addView(label(entry.getString("text"),14,ink,false));space(c,10);boolean known=entry.has("worked"),opened=entry.optBoolean("worked",false);TextView badge=label(known?(opened?"✓ เรียกเปิดงานแล้ว":"− ไม่ได้เปิดงาน"):"บันทึกเดิม",14,opened?0xFF28A35B:muted,true);pad(badge,9,7,9,7);badge.setBackground(AppTheme.background(AppTheme.tint(this),dp(5)));c.addView(badge);space(c,7);c.addView(label(Store.shortReason(entry.getString("result")),13,ink,false));c.setTag("history-entry-"+i);c.setOnClickListener(v->{if(!JobListener.openHistory(entry))JobFeedback.unavailable(this,c);});}
            if(entries.length()>0){LinearLayout c=card(null,null);item(c,"ล้างประวัติ","",()->dialog(new AlertDialog.Builder(this).setMessage("ล้างประวัติงานทั้งหมด?").setPositiveButton("ล้าง",(d,w)->{p().edit().remove("history").apply();render();}).setNegativeButton("ยกเลิก",null)));}
        }catch(JSONException ignored){}
    }
}
