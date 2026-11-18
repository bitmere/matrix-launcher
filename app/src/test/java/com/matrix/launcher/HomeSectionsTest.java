package com.matrix.launcher;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class HomeSectionsTest {
    @Test public void previewKeepsOriginalOrderAndAlwaysOffersFullList() {
        TvFeed.Row full=new TvFeed.Row("Example");
        for(int i=0;i<30;i++){TvFeed.Item item=new TvFeed.Item();item.title="Title "+i;item.portrait=true;full.items.add(item);}
        TvFeed.Row row=HomeSections.preview("Example",full,"simkl-list:42");
        assertEquals(7,row.items.size());assertSame(full.items.get(5),row.items.get(5));
        assertEquals("simkl-list:42",row.items.get(6).intent);assertEquals(30,full.items.size());
        assertTrue("See all must match its portrait row",row.items.get(6).portrait);
    }
    @Test public void emptyListRemainsBrowsable() {
        TvFeed.Row row=HomeSections.preview("Empty",new TvFeed.Row("Empty"),"simkl-library:shows:watching");
        assertEquals(1,row.items.size());assertEquals("See all",row.items.get(0).title);
        assertTrue(row.items.get(0).portrait);
    }
    @Test public void libraryPagesFilterBeforeCountingAndDoNotLoseTitles()throws Exception {
        JSONArray entries=new JSONArray();
        for(int i=0;i<51;i++)entries.put(new JSONObject().put("status",i%2==0?"watching":"hold")
                .put("show",new JSONObject().put("title","Title "+i).put("year",2025)
                        .put("ids",new JSONObject().put("imdb","tt"+i))));
        TvFeed.Row first=SimklClient.libraryPage(entries,"shows","watching",1);
        TvFeed.Row second=SimklClient.libraryPage(entries,"shows","watching",2);
        assertEquals(24,first.items.size());assertTrue(first.hasMore);
        assertEquals(2,second.items.size());assertFalse(second.hasMore);
        assertEquals("Title 48",second.items.get(0).title);
    }
}
