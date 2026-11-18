package com.matrix.launcher;

/** Stable section IDs and bounded previews shared by Home and its section settings. */
final class HomeSections {
    static final int PREVIEW_ITEMS=6;
    static final String[][] LIBRARY={
        {"shows:watching","Watching TV"},
        {"movies:plantowatch","Plan to watch movies"},
        {"shows:plantowatch","Plan to watch TV"},
        {"anime:watching","Watching anime"},
        {"anime:plantowatch","Plan to watch anime"},
        {"shows:hold","TV on hold"},
        {"anime:hold","Anime on hold"},
        {"movies:completed","Watched movies"},
        {"shows:completed","Completed TV"},
        {"anime:completed","Completed anime"}
    };
    static TvFeed.Row preview(String title,TvFeed.Row full,String destination) {
        TvFeed.Row row=new TvFeed.Row(title,destination);
        for(int i=0;i<Math.min(PREVIEW_ITEMS,full.items.size());i++)row.items.add(full.items.get(i));
        TvFeed.Item more=new TvFeed.Item();more.title="See all";more.pkg="SIMKL";
        more.portrait=full.items.isEmpty() || full.items.get(0).portrait;
        more.poster="";
        more.intent=destination;row.items.add(more);return row;
    }
}
