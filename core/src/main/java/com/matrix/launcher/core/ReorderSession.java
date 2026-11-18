package com.matrix.launcher.core;

import java.util.ArrayList;
import java.util.List;

/** Draft movement is reversible until confirmed; callers retain ownership of persisted order. */
public final class ReorderSession<T> {
    public final List<T> items;
    private List<T> before;
    public int moving=-1;
    public ReorderSession(List<T> values){items=new ArrayList<>(values);}
    public void begin(int index){if(index<0 || index>=items.size() || moving>=0)return;before=new ArrayList<>(items);moving=index;}
    public boolean move(int step){if(moving<0)return false;int next=Math.max(0,Math.min(items.size()-1,moving+step));if(next==moving)return false;T item=items.remove(moving);items.add(next,item);moving=next;return true;}
    public boolean confirm(){boolean changed=before!=null && !before.equals(items);moving=-1;before=null;return changed;}
    public void cancel(){if(before!=null){items.clear();items.addAll(before);}confirm();}
}
