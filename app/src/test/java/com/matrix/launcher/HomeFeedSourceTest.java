package com.matrix.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/** JVM-only, no network: parallel feed assembly (D-13) keeps today's order
 * and isolates a failing section to the status message. */
public final class HomeFeedSourceTest {

    @Test public void outOfOrderArrivalsAndAFailingSectionKeepAssemblyOrder() throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put("simkl_sections", new HashSet<>(Arrays.asList("shows:watching", "movies:plantowatch", "anime:watching")));
        values.put("simkl_pins", new HashSet<>(Collections.singletonList("42")));
        TvFeed.Result result = new TvFeed.Result();
        result.rows.add(new TvFeed.Row("Nuvio · Continue watching"));

        HomeFeedSource.append(result, new MapPreferences(values), new SlowFirstFetcher());

        // Sections in LIBRARY order, then pins, inserted after the Nuvio
        // rows — even though shows:watching finished last (300 ms sleep)
        // and anime:watching failed outright.
        assertEquals(4, result.rows.size());
        assertEquals("Nuvio · Continue watching", result.rows.get(0).title);
        assertEquals("SIMKL · Watching TV", result.rows.get(1).title);
        assertEquals("SIMKL · Plan to watch movies", result.rows.get(2).title);
        assertEquals("SIMKL · My pin", result.rows.get(3).title);
        // The pin actually assembled its content (1 item + "See all").
        assertEquals(2, result.rows.get(3).items.size());
        assertEquals("Valid", result.rows.get(3).items.get(0).title);
        // The failure is isolated to the status message, exactly as before.
        assertTrue(result.status.contains("Some lists could not be refreshed"));
    }

    /** Slowest section first, one failing section, one pin. */
    private static final class SlowFirstFetcher implements HomeFeedSource.FeedFetcher {
        @Override public TvFeed.Row library(String type, String status) throws Exception {
            if ("shows".equals(type) && "watching".equals(status)) {
                Thread.sleep(300);
                return new TvFeed.Row("shows-watching");
            }
            if ("anime".equals(type) && "watching".equals(status)) {
                throw new IOException("simkl down");
            }
            return new TvFeed.Row(type + "-" + status);
        }

        @Override public JSONObject list(String id, int page) throws Exception {
            return new JSONObject()
                    .put("name", "My pin")
                    .put("media_type", "movies")
                    .put("items", new JSONArray()
                            .put(new JSONObject()
                                    .put("title", "Valid")
                                    .put("endpoint_type", "movies")
                                    .put("ids", new JSONObject().put("imdb", "tt42"))));
        }
    }

    /** Minimal test-only SharedPreferences backed by a map. Only
     * getStringSet is used by HomeFeedSource.append; the rest return
     * defaults. Production code never uses this. */
    private static final class MapPreferences implements SharedPreferences {
        private final Map<String, Object> values;
        MapPreferences(Map<String, Object> values) { this.values = values; }
        @Override public Map<String, ?> getAll() { return new HashMap<>(values); }
        @Override public String getString(String key, String defValue) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : defValue;
        }
        @Override @SuppressWarnings("unchecked") public Set<String> getStringSet(String key, Set<String> defValues) {
            Object value = values.get(key);
            return value instanceof Set ? (Set<String>) value : defValues;
        }
        @Override public int getInt(String key, int defValue) {
            Object value = values.get(key);
            return value instanceof Integer ? (Integer) value : defValue;
        }
        @Override public long getLong(String key, long defValue) {
            Object value = values.get(key);
            return value instanceof Long ? (Long) value : defValue;
        }
        @Override public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            return value instanceof Float ? (Float) value : defValue;
        }
        @Override public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defValue;
        }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public Editor edit() { return null; }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    }
}
