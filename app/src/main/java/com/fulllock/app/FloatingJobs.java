package com.fulllock.app;
import java.util.*;

/** Session-only list. A timed preview never marks jobs read. */
final class FloatingJobs {
    enum Mode { COLLAPSED, PREVIEW, PINNED }
    static final class Item {
        final String id,key,title,text;
        final long postTime;
        final JobParser.Job job;
        final boolean matches;
        boolean available;
        Item(String key,long postTime,String title,String text,JobParser.Job job,boolean matches,boolean available) {
            this.key=key;this.postTime=postTime;this.title=title;this.text=text;this.job=job;this.matches=matches;this.available=available;
            id=key+"\n"+title+"\n"+text;
        }
    }
    final List<Item> items=new ArrayList<>();
    int unread;
    Mode mode=Mode.COLLAPSED;
    void put(Item item,boolean visible) {
        boolean existing=items.removeIf(old->old.id.equals(item.id));
        items.add(0,item);if(items.size()>5)items.remove(items.size()-1);
        if(mode==Mode.PINNED)unread=0;
        else {if(!existing)unread++;if(visible)mode=Mode.PREVIEW;}
    }
    void open(){mode=Mode.PINNED;unread=0;}
    void collapse(){mode=Mode.COLLAPSED;}
    void expire(){if(mode==Mode.PREVIEW)collapse();}
    void reset(){items.clear();unread=0;collapse();}
    void removed(String key,long time){for(Item item:items)if(item.key.equals(key)&&item.postTime<=time)item.available=false;}
}
