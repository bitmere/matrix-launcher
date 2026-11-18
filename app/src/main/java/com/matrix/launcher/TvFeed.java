package com.matrix.launcher;

import com.matrix.launcher.core.FeedRules;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads only rows Android makes visible with READ_TV_LISTINGS. Never queries another app's database. */
final class TvFeed {
    static final class Item {
        String title, poster, intent, pkg, metadata, mediaType, summary;
        String simklReference = "";
        long position, duration, engaged, weight;
        boolean portrait;
    }
    static final class Row {
        final String title,key;
        final List<Item> items = new ArrayList<>();
        boolean hasMore;
        Row(String name) {this(name,name);}
        Row(String name,String key) {title=name;this.key=key;}
    }
    static final class Result {
        final List<Row> rows = new ArrayList<>();
        String status = "";
    }
    private static final class Channel {
        String title, pkg, provider;
    }

    static Result read(Context context, Set<String> sources) {
        Result result = new Result();
        if(sources.isEmpty()) {
            result.status="Choose apps in Settings → Home channels to show their shared rows.";
            return result;
        }
        Map<String,Row> continuing = new LinkedHashMap<>();
        Map<Long, Channel> channels = new HashMap<>();
        LinkedHashMap<Long, Row> previews = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        try {
            // A selection is forbidden to a non-privileged TV reader: filter after querying.
            try (Cursor c = query(context, "channel")) {
                int scanned = 0;
                while (c != null && c.moveToNext() && scanned++ < 500) {
                    String pkg = string(c, "package_name");
                    if (!sources.contains(pkg)) continue;
                    Channel ch = new Channel();
                    ch.pkg = pkg; ch.title = string(c, "display_name"); ch.provider = string(c,"internal_provider_id");
                    channels.put(number(c, "_id"), ch);
                }
            }
            try (Cursor c = query(context, "watch_next_program")) {
                int scanned = 0;
                while (c != null && c.moveToNext() && scanned++ < 1000) {
                    Item item = item(c);
                    if (!sources.contains(item.pkg) || !valid(item)) continue;
                    // Nuvio beta.9 publishes its canonical queue as a preview channel, not Watch Next.
                    if (item.pkg.equals("com.nuvio.tv")) continue;
                    if (!FeedRules.useful(item.position, item.duration,
                            number(c, "last_engagement_time_utc_millis"), System.currentTimeMillis())) continue;
                    Row next = continuing.get(item.pkg);
                    if(next==null){next=new Row(appName(context,item.pkg)+" · Continue watching",item.pkg+":continue");continuing.put(item.pkg,next);}
                    add(next, item, seen);
                }
            }
            try (Cursor c = query(context, "preview_program")) {
                int scanned = 0;
                while (c != null && c.moveToNext() && scanned++ < 1500) {
                    long id = number(c, "channel_id");
                    Channel ch = channels.get(id);
                    if (ch == null) continue;
                    Item item = item(c);
                    item.pkg = ch.pkg;
                    if (!valid(item)) continue;
                    if (FeedRules.continuing(ch.title)) {
                        if(ch.pkg.equals("com.nuvio.tv") && !ch.pkg.equals(ch.provider))continue;
                        Row next=continuing.get(ch.pkg);
                        if(next==null){next=new Row(appName(context,ch.pkg)+" · Continue watching",ch.pkg+":continue");continuing.put(ch.pkg,next);}
                        if (FeedRules.useful(item.position, item.duration, 0, System.currentTimeMillis())) add(next, item, seen);
                    } else {
                        Row row = previews.get(id);
                        if (row == null && previews.size() < FeedRules.MAX_CHANNELS) {
                            row = new Row(appName(context, ch.pkg) + " · " + ch.title,ch.pkg+":"+id);
                            previews.put(id, row);
                        }
                        // Deduplicate within each channel; different catalogues can legitimately overlap.
                        // Retain the best bounded set even when better candidates arrive late.
                        if (row != null) {
                            boolean duplicate = false;
                            for (Item old : row.items) if (old.intent.equals(item.intent)) duplicate = true;
                            if (!duplicate) best(row, item);
                        }
                    }
                }
            }
            Row nuvio=continuing.get("com.nuvio.tv");if(nuvio!=null && !nuvio.items.isEmpty())result.rows.add(nuvio);
            for(Map.Entry<String,Row> entry:continuing.entrySet())if(!entry.getKey().equals("com.nuvio.tv") && !entry.getValue().items.isEmpty())result.rows.add(entry.getValue());
            for (Row row : previews.values()) if (!row.items.isEmpty()) result.rows.add(row);
            result.status = result.rows.isEmpty()
                    ? "No shared rows yet. Enable channels in your selected apps; only published, searchable items are visible."
                    : "From your apps’ shared Android TV channels";
        } catch (SecurityException e) {
            result.status = "Allow TV listings in Home settings to read your apps’ shared rows.";
        } catch (Exception e) {
            result.status = "TV channel provider unavailable. App shortcuts still work.";
        }
        return result;
    }

    private static void add(Row row, Item item, Set<String> seen) {
        if (seen.add(item.pkg + "|" + item.intent)) best(row, item);
    }
    static void best(Row row, Item item) {
        row.items.add(item);
        row.items.sort((a,b) -> FeedRules.compare(a.engaged, a.weight, b.engaged, b.weight));
        if (row.items.size() > FeedRules.MAX_ITEMS) row.items.remove(row.items.size()-1);
    }
    private static boolean valid(Item item) { return !item.title.isEmpty() && !item.intent.isEmpty(); }
    private static Cursor query(Context c, String table) {
        return c.getContentResolver().query(Uri.parse("content://android.media.tv/" + table), null, null, null, null);
    }
    private static Item item(Cursor c) {
        Item i = new Item();
        i.title = string(c, "title"); i.poster = string(c, "poster_art_uri");
        i.intent = string(c, "intent_uri"); i.pkg = string(c, "package_name");
        i.position = number(c, "last_playback_position_millis"); i.duration = number(c, "duration_millis");
        i.engaged = number(c, "last_engagement_time_utc_millis"); i.weight = number(c, "weight");
        i.portrait = FeedRules.portraitAspect((int) number(c, "poster_art_aspect_ratio"));
        return i;
    }
    private static String string(Cursor c, String key) {
        int index = c.getColumnIndex(key);
        return index < 0 || c.isNull(index) ? "" : c.getString(index);
    }
    private static long number(Cursor c, String key) {
        int index = c.getColumnIndex(key);
        return index < 0 || c.isNull(index) ? 0 : c.getLong(index);
    }
    /** Bounded label cache: one PackageManager Binder call per package, not one per row per Home load. */
    private static final java.util.Map<String, String> APP_NAMES =
            java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String, String>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, String> e) { return size() > 128; }
            });

    /** Cleared with the app list on a package broadcast, so a renamed or updated app is re-read. */
    static void clearAppNameCache() { APP_NAMES.clear(); }

    static String appName(Context c, String pkg) {
        String cached = APP_NAMES.get(pkg);
        if (cached != null) return cached;
        try {
            String label = c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(pkg, 0)).toString();
            APP_NAMES.put(pkg, label);
            return label;
        }
        catch (Exception e) { return pkg; }
    }
}
