package com.matrix.launcher;

import com.matrix.launcher.core.FeedRules;

import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.*;

/** Local presentation choices. Provider data and provider ordering are never modified. */
final class HomeRows {
    private HomeRows(){}
    static List<String> order(SharedPreferences prefs,String key){
        List<String> out=new ArrayList<>();
        try{JSONArray a=new JSONArray(prefs.getString(key,"[]"));for(int i=0;i<a.length();i++)out.add(a.getString(i));}catch(Exception ignored){}
        return out;
    }
    static <T> List<T> sorted(List<T> values,List<String> order,java.util.function.Function<T,String> key){
        if(order.isEmpty())return new ArrayList<>(values);
        Map<String,Integer> rank=new HashMap<>();for(int i=0;i<order.size();i++)rank.putIfAbsent(order.get(i),i);
        List<T> out=new ArrayList<>(values);out.sort(Comparator.comparingInt(value->rank.getOrDefault(key.apply(value),Integer.MAX_VALUE)));return out;
    }
    static List<TvFeed.Row> orderedRows(TvFeed.Result result,SharedPreferences prefs){
        return sorted(result.rows,order(prefs,"row_order"),r->r.key);
    }
    static List<TvFeed.Row> visible(TvFeed.Result result,SharedPreferences prefs){
        Set<String> hidden=prefs.getStringSet("hidden_rows",Collections.emptySet());List<TvFeed.Row> out=new ArrayList<>();
        for(TvFeed.Row row:orderedRows(result,prefs)){
            if(hidden.contains(row.key))continue;
            TvFeed.Row copy=new TvFeed.Row(row.title,row.key);copy.hasMore=row.hasMore;
            String pkg=row.items.isEmpty()?"":row.items.get(0).pkg;
            int limit=Math.max(1,Math.min(FeedRules.MAX_ITEMS,prefs.getInt("row_limit:"+pkg,12)));
            List<TvFeed.Item> items=sorted(row.items,order(prefs,"item_order:"+row.key),i->i.intent);
            TvFeed.Item more=null;
            for(Iterator<TvFeed.Item> it=items.iterator();it.hasNext();){TvFeed.Item item=it.next();if(item.intent.startsWith("simkl-list:") || item.intent.startsWith("simkl-library:")){more=item;it.remove();}}
            copy.items.addAll(items.subList(0,Math.min(more==null?limit:limit-1,items.size())));
            if(more!=null)copy.items.add(more);out.add(copy);
        }
        return out;
    }
}
