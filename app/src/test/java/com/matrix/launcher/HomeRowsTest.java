package com.matrix.launcher;

import org.junit.Test;
import java.util.*;
import android.content.SharedPreferences;
import static org.junit.Assert.*;

public class HomeRowsTest {
    @Test public void hidingLimitingAndReorderingDoNotMutateProviderData(){
        TvFeed.Result feed=new TvFeed.Result();TvFeed.Row hidden=new TvFeed.Row("hidden","hidden");feed.rows.add(hidden);
        TvFeed.Row row=new TvFeed.Row("List","list");feed.rows.add(row);
        for(int i=0;i<8;i++){TvFeed.Item item=new TvFeed.Item();item.pkg="SIMKL";item.intent="title:"+i;row.items.add(item);}
        TvFeed.Item more=new TvFeed.Item();more.pkg="SIMKL";more.intent="simkl-list:42";row.items.add(more);
        Map<String,Object> values=new HashMap<>();values.put("hidden_rows",Collections.singleton("hidden"));values.put("row_limit:SIMKL",4);values.put("item_order:list","[\"title:7\"]");
        SharedPreferences prefs=(SharedPreferences)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.class},(proxy,method,args)->values.getOrDefault(args[0],args[1]));
        List<TvFeed.Row> shown=HomeRows.visible(feed,prefs);
        assertEquals(1,shown.size());assertEquals(4,shown.get(0).items.size());
        assertEquals("title:7",shown.get(0).items.get(0).intent);assertSame(more,shown.get(0).items.get(3));
        assertEquals(9,row.items.size());assertEquals("title:0",row.items.get(0).intent);assertEquals(2,feed.rows.size());
    }
    @Test public void pinnedRowsLeadAndNewRowsKeepProviderOrder(){
        List<String> original=Arrays.asList("new1","b","a","new2");
        assertEquals(Arrays.asList("a","b","new1","new2"),HomeRows.sorted(original,Arrays.asList("a","removed","b"),v->v));
        assertEquals(Arrays.asList("new1","b","a","new2"),original);
    }
    @Test public void recoveryMatchesExactServiceAndPreservesUserOptOut(){
        String component="com.matrix.launcher/com.matrix.launcher.HomeKeyService";
        assertTrue(HomeRecovery.selected("other/service:"+component,component));
        assertFalse(HomeRecovery.selected(null,component));
        assertFalse(HomeRecovery.selected("other/service",component));
        assertFalse(HomeRecovery.selected(component+"Extra",component));
    }
    @Test public void editorAndHomeReopenTheSavedOrderIncludingHiddenRows(){
        TvFeed.Result feed=new TvFeed.Result();
        TvFeed.Row a=new TvFeed.Row("A","a"), b=new TvFeed.Row("B","b"), c=new TvFeed.Row("C","c");
        feed.rows.addAll(Arrays.asList(a,b,c));
        Map<String,Object> values=new HashMap<>();
        values.put("row_order","[\"c\",\"b\",\"a\"]");values.put("hidden_rows",Collections.singleton("b"));
        SharedPreferences prefs=(SharedPreferences)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.class},(proxy,method,args)->values.getOrDefault(args[0],args[1]));
        assertEquals(Arrays.asList(c,b,a),HomeRows.orderedRows(feed,prefs));
        List<TvFeed.Row> visible=HomeRows.visible(feed,prefs);
        assertEquals("c",visible.get(0).key);assertEquals("a",visible.get(1).key);
        assertEquals(Arrays.asList(a,b,c),feed.rows);
    }
}
