package com.fulllock.app;

import android.content.*;
import org.json.*;
import java.util.*;

/** Flat settings are the active snapshot. Switching replaces them in one preferences edit. */
final class Profiles {
    static final String LIST="profiles",ACTIVE="activeProfile";
    private static SharedPreferences prefs;
    private static final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)->{if(ProfileFile.DEFAULTS.containsKey(key))saveActive();};
    static synchronized void initialize(Context context){
        SharedPreferences settings=Store.prefs(context);
        if(prefs==settings)return;
        if(prefs!=null)prefs.unregisterOnSharedPreferenceChangeListener(listener);prefs=settings;
        JSONArray list=entries();String id=prefs.getString(ACTIVE,"");
        if(list.length()==0){id=UUID.randomUUID().toString();list.put(entry(id,new ProfileFile.Data("โปรไฟล์เริ่มต้น",snapshot())));prefs.edit().putString(LIST,list.toString()).putString(ACTIVE,id).apply();}
        else if(find(list,id)<0){id=list.optJSONObject(0).optString("id");apply(list,id,data(list.optJSONObject(0)));}
        if(prefs.getInt("serviceMask",1)==3)prefs.edit().putInt("serviceMask",1).apply();saveActive();prefs.registerOnSharedPreferenceChangeListener(listener);
    }
    static synchronized JSONArray entries(){try{return new JSONArray(prefs.getString(LIST,"[]"));}catch(Exception e){return new JSONArray();}}
    static String active(){return prefs.getString(ACTIVE,"");}
    static String activeName(){JSONObject profile=get(active());return profile==null?"โปรไฟล์เริ่มต้น":profile.optString("name");}
    static JSONObject get(String id){JSONArray list=entries();int index=find(list,id);return index<0?null:list.optJSONObject(index);}
    private static int find(JSONArray list,String id){for(int i=0;i<list.length();i++)if(id.equals(list.optJSONObject(i).optString("id")))return i;return -1;}
    static String named(String name){String normalized=ProfileFile.name(name);JSONArray list=entries();for(int i=0;i<list.length();i++){JSONObject item=list.optJSONObject(i);if(normalized.equalsIgnoreCase(item.optString("name")))return item.optString("id");}return null;}
    static Map<String,Object> snapshot(){Map<String,Object> values=new LinkedHashMap<>();Map<String,?> all=prefs.getAll();for(var field:ProfileFile.DEFAULTS.entrySet())values.put(field.getKey(),all.containsKey(field.getKey())?all.get(field.getKey()):field.getValue());return values;}
    static ProfileFile.Data data(JSONObject item){try{Map<String,Object> values=new LinkedHashMap<>();JSONObject filters=item.getJSONObject("values");for(String key:ProfileFile.DEFAULTS.keySet())if(filters.has(key))values.put(key,filters.get(key));return new ProfileFile.Data(item.getString("name"),values);}catch(JSONException e){throw new IllegalArgumentException("โปรไฟล์ไม่ถูกต้อง",e);}}
    private static JSONObject entry(String id,ProfileFile.Data data){try{return new JSONObject().put("id",id).put("name",data.name).put("values",new JSONObject(data.values));}catch(JSONException e){throw new IllegalArgumentException(e);}}
    private static synchronized void saveActive(){if(prefs==null)return;JSONArray list=entries();int index=find(list,active());if(index<0)return;JSONObject current=list.optJSONObject(index);try{list.put(index,entry(active(),new ProfileFile.Data(current.optString("name"),snapshot())));prefs.edit().putString(LIST,list.toString()).apply();}catch(IllegalArgumentException|JSONException ignored){}}
    private static void apply(JSONArray list,String id,ProfileFile.Data data){SharedPreferences.Editor edit=prefs.edit().putString(LIST,list.toString()).putString(ACTIVE,id);for(var field:data.values.entrySet()){Object value=field.getValue();if(value instanceof Boolean)edit.putBoolean(field.getKey(),(Boolean)value);else if(value instanceof Integer)edit.putInt(field.getKey(),(Integer)value);else edit.putString(field.getKey(),(String)value);}edit.apply();}
    static synchronized void select(String id){saveActive();JSONArray list=entries();int index=find(list,id);if(index<0)throw new IllegalArgumentException("ไม่พบโปรไฟล์");apply(list,id,data(list.optJSONObject(index)));}
    static synchronized String create(String name,String source){if(named(name)!=null)throw new IllegalArgumentException("ชื่อนี้มีอยู่แล้ว");JSONArray list=entries();if(list.length()>=50)throw new IllegalArgumentException("เก็บได้สูงสุด 50 โปรไฟล์");String id=UUID.randomUUID().toString();Map<String,Object> values=source==null?ProfileFile.DEFAULTS:data(get(source)).values;list.put(entry(id,new ProfileFile.Data(name,values)));prefs.edit().putString(LIST,list.toString()).apply();return id;}
    static synchronized void rename(String id,String name){String duplicate=named(name);if(duplicate!=null&&!duplicate.equals(id))throw new IllegalArgumentException("ชื่อนี้มีอยู่แล้ว");JSONArray list=entries();int index=find(list,id);if(index<0)throw new IllegalArgumentException("ไม่พบโปรไฟล์");try{JSONObject item=list.getJSONObject(index);list.put(index,entry(id,new ProfileFile.Data(name,data(item).values)));prefs.edit().putString(LIST,list.toString()).apply();}catch(JSONException e){throw new IllegalArgumentException(e);}}
    static synchronized void delete(String id){JSONArray list=entries();if(list.length()<=1)throw new IllegalArgumentException("ต้องมีอย่างน้อย 1 โปรไฟล์");int index=find(list,id);if(index<0)return;list.remove(index);if(active().equals(id)){JSONObject next=list.optJSONObject(Math.min(index,list.length()-1));apply(list,next.optString("id"),data(next));}else prefs.edit().putString(LIST,list.toString()).apply();}
    static synchronized void deleteMany(Set<String> selected){
        if(selected.isEmpty())throw new IllegalArgumentException("เลือกโปรไฟล์ที่ต้องการลบก่อน");
        JSONArray current=entries(),remaining=new JSONArray();Set<String> found=new HashSet<>();
        for(int i=0;i<current.length();i++){JSONObject item=current.optJSONObject(i);String id=item.optString("id");if(selected.contains(id))found.add(id);else remaining.put(item);}
        if(!found.equals(selected))throw new IllegalArgumentException("รายการโปรไฟล์เปลี่ยนแล้ว กรุณาเลือกใหม่");
        if(remaining.length()==0)throw new IllegalArgumentException("ต้องเหลืออย่างน้อย 1 โปรไฟล์");
        if(selected.contains(active())){JSONObject next=remaining.optJSONObject(0);apply(remaining,next.optString("id"),data(next));}
        else prefs.edit().putString(LIST,remaining.toString()).apply();
    }
    static synchronized String install(ProfileFile.Data imported,boolean overwrite){JSONArray list=entries();String id=named(imported.name);if(id!=null&&!overwrite)throw new IllegalArgumentException("โปรไฟล์นี้มีอยู่แล้ว");if(id==null){if(list.length()>=50)throw new IllegalArgumentException("เก็บได้สูงสุด 50 โปรไฟล์");id=UUID.randomUUID().toString();list.put(entry(id,imported));}else try{list.put(find(list,id),entry(id,imported));}catch(JSONException e){throw new IllegalArgumentException(e);}apply(list,id,imported);return id;}
}
