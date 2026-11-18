package com.matrix.launcher;

import android.content.Context;
import android.content.res.Resources;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Only platform/remote options used by this app. No Classic layout, animation or seasonal engine. */
final class RemoteConfig {
    // Serial reads; the worker exits after idle instead of keeping a thread alive for Home.
    private static final ThreadPoolExecutor IO = new ThreadPoolExecutor(0, 1, 15, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), task -> new Thread(task, "MatrixRemoteConfig"));
    /** Main-thread owner: replacement/cancellation rejects results after destruction or service unbinding. */
    static final class Loader {
        private volatile int generation;
        void load(Context context, Consumer<RemoteConfig> ready) {
            int request = ++generation;
            Context app = context.getApplicationContext();
            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
            IO.execute(() -> {
                if (request != generation) return;
                RemoteConfig value = RemoteConfig.load(app);
                main.post(() -> { if (request == generation) ready.accept(value); });
            });
        }
        void cancel() { generation++; }
    }
    static final String FILE_NAME = "config.json", SETTINGS_NAME = "launcher_settings";
    // Read only for legacy power-on redirect exclusions; Compose does not auto-launch a chosen app.
    static final String LEGACY_OPEN_ON_START = "open_on_start";
    final int[] inputKeycodes, settingsKeycodes;
    final List<String> appKeys;
    final String stockHomeRedirect, liveTvPackage;
    final boolean keyFilter, consumeVolumeKeys, gamepadPassthrough, restoreKeyServices, handleViewInputs,
        settingsKeyPanel, homeLongPressHome, powerOnHome, powerOnHomeAlways, bootToLiveTv, openOnStartChoice;

    private RemoteConfig(Resources resources, JSONObject json) {
        inputKeycodes = integers(json, "input_key_keycodes", resources.getIntArray(R.array.input_key_keycodes));
        settingsKeycodes = integers(json, "settings_key_keycodes", resources.getIntArray(R.array.settings_key_keycodes));
        appKeys = strings(json, "app_keys", resources.getStringArray(R.array.app_keys));
        stockHomeRedirect = text(json, "stock_home_redirect", resources.getString(R.string.stock_home_redirect));
        liveTvPackage = text(json, "live_tv_package", resources.getString(R.string.live_tv_package));
        keyFilter = flag(json, "key_filter", resources.getBoolean(R.bool.key_filter));
        consumeVolumeKeys = flag(json, "consume_volume_keys", resources.getBoolean(R.bool.consume_volume_keys));
        gamepadPassthrough = flag(json, "gamepad_passthrough", resources.getBoolean(R.bool.gamepad_passthrough));
        restoreKeyServices = flag(json, "restore_key_services", resources.getBoolean(R.bool.restore_key_services));
        handleViewInputs = flag(json, "handle_view_inputs", resources.getBoolean(R.bool.handle_view_inputs));
        settingsKeyPanel = flag(json, "settings_key_panel", resources.getBoolean(R.bool.settings_key_panel));
        homeLongPressHome = flag(json, "home_long_press_home", resources.getBoolean(R.bool.home_long_press_home));
        powerOnHome = flag(json, "power_on_home", resources.getBoolean(R.bool.power_on_home));
        powerOnHomeAlways = flag(json, "power_on_home_always", resources.getBoolean(R.bool.power_on_home_always));
        bootToLiveTv = flag(json, "boot_to_live_tv", resources.getBoolean(R.bool.boot_to_live_tv));
        openOnStartChoice = flag(json, "open_on_start_choice", resources.getBoolean(R.bool.open_on_start_choice));
    }
    static File dir(Context context) { return context.getExternalFilesDir(null); }
    static RemoteConfig load(Context context) {
        JSONObject overlay = new JSONObject();
        try {
            File directory = dir(context);
            File file = directory == null ? null : new File(directory, FILE_NAME);
            // Fail closed to built-ins for malformed/oversized overlays. No unbounded reads at service start.
            if (file != null && file.isFile() && file.length() <= 256 * 1024) {
                try (FileInputStream stream = new FileInputStream(file)) { overlay = new JSONObject(PersonalImages.text(stream, 256 * 1024)); }
                catch (Exception ignored) { /* Device defaults remain usable. */ }
            }
        } catch (SecurityException denied) { /* A removed storage grant leaves device defaults usable. */ }
        return new RemoteConfig(context.getResources(), overlay);
    }
    static boolean flag(JSONObject json, String key, boolean fallback) {
        Object value = json.opt(key); return value instanceof Boolean ? (Boolean) value : fallback;
    }
    static String text(JSONObject json, String key, String fallback) {
        Object value = json.opt(key); return value instanceof String ? ((String) value).trim() : fallback;
    }
    static int[] integers(JSONObject json, String key, int[] fallback) {
        JSONArray array = json.optJSONArray(key); if (array == null) return fallback;
        int[] result = new int[array.length()];
        for (int i = 0; i < result.length; i++) {
            Object value = array.opt(i);
            if (!(value instanceof Number) || ((Number) value).doubleValue() != ((Number) value).intValue() || ((Number) value).intValue() <= 0) return fallback;
            result[i] = ((Number) value).intValue();
        }
        return result;
    }
    static List<String> strings(JSONObject json, String key, String[] fallback) {
        JSONArray array = json.optJSONArray(key); List<String> result = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) { if (!(array.opt(i) instanceof String)) return new ArrayList<>(java.util.Arrays.asList(fallback)); result.add(array.optString(i)); }
        } else java.util.Collections.addAll(result, fallback);
        return result;
    }
}
