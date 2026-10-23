package com.fulllock.app;
public class FloatingChecks {
    static int count;
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);count++;}
    static FloatingJobs.Item item(int n){return new FloatingJobs.Item("key"+n,n,"ต้นทาง"+n,"ข้อความ"+n,null,true,true);}
    public static void main(String[] args){
        FloatingJobs m=new FloatingJobs();check(m.unread==0,"initial count");check(m.mode==FloatingJobs.Mode.COLLAPSED,"initial collapsed");
        m.put(item(1),true);check(m.unread==1,"first unread");check(m.mode==FloatingJobs.Mode.PREVIEW,"auto preview");m.expire();check(m.mode==FloatingJobs.Mode.COLLAPSED,"expire");check(m.unread==1,"preview keeps unread");
        m.put(item(1),true);check(m.unread==1,"duplicate not counted");for(int i=2;i<=12;i++)m.put(item(i),true);check(m.items.size()==10,"ten max");check(m.unread==12,"count includes overflow unread");check(m.items.get(0).id.equals(item(12).id),"newest at top");check(m.items.get(9).id.equals(item(3).id),"oldest retained");
        m.open();check(m.unread==0,"mark read");check(m.mode==FloatingJobs.Mode.PINNED,"pinned");m.put(item(9),true);check(m.unread==0,"incoming while reading");m.expire();check(m.mode==FloatingJobs.Mode.PINNED,"pinned ignores timer");
        m.removed("key9",8);check(m.items.get(0).available,"older removal ignored");m.removed("key9",9);check(!m.items.get(0).available,"current removal disables");check(m.items.get(1).available,"other jobs unaffected");
        m.collapse();m.put(item(20),false);check(m.unread==1,"hidden unread");check(m.mode==FloatingJobs.Mode.COLLAPSED,"hidden never previews");m.reset();check(m.items.isEmpty()&&m.unread==0,"reset session");
        System.out.println("FloatingChecks: "+count+" checks passed");
    }
}
