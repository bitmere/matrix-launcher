package com.matrix.launcher;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import java.io.*;
import java.util.*;

final class PersonalWallpapers {
    private final Context context;
    private final SharedPreferences prefs;
    final File local, downloaded, legacy;
    private boolean initialized;
    PersonalWallpapers(Context context, SharedPreferences prefs) {
        this.context = context.getApplicationContext(); this.prefs = prefs;
        File external = context.getExternalFilesDir(null);
        File base = new File(external != null ? external : context.getFilesDir(), "wallpapers");
        local = new File(base, "local"); downloaded = new File(base, "commons");
        // Read-only compatibility path: keep previously downloaded pictures without moving or deleting them.
        legacy = new File(base, "reddit");
    }
    void initialize() {
        if (initialized) return;
        local.mkdirs(); downloaded.mkdirs();
        File[] interrupted = downloaded.listFiles((directory, name) -> name.endsWith(".part"));
        if (interrupted != null) for (File file : interrupted) file.delete();
        migratePreferences(prefs);
        initialized = true;
    }
    static void migratePreferences(SharedPreferences prefs) {
        if (prefs.getBoolean("wallpaper_reddit_removed", false)) return;
        SharedPreferences.Editor edit = prefs.edit();
        for (String key : new String[]{"reddit_client", "reddit_refresh", "reddit_enabled", "reddit_attempt", "subreddits"}) edit.remove(key);
        if ("reddit".equals(prefs.getString("wallpaper_mode", "both"))) edit.putString("wallpaper_mode", "both");
        edit.putBoolean("wallpaper_reddit_removed", true).apply();
    }
    List<File> images() {
        initialize();
        List<File> images = new ArrayList<>();
        String mode = prefs.getString("wallpaper_mode", "both");
        if (mode.equals("off")) return images;
        if (!mode.equals("downloaded")) { add(images, local); add(images, legacy); }
        if (!mode.equals("local")) add(images, downloaded);
        return images;
    }
    List<File> collection() {
        initialize();List<File> files = new ArrayList<>();
        add(files,local);add(files,legacy);add(files,downloaded);return files;
    }
    boolean select(String path) {
        File selected = null;for(File file:collection())if(file.getAbsolutePath().equals(path))selected=file;
        if(selected==null)return false;
        boolean visible=false;for(File file:images())if(file.equals(selected))visible=true;
        SharedPreferences.Editor edit=prefs.edit();
        if(!visible)edit.putString("wallpaper_mode","both");
        edit.putString("wallpaper_current",path).putLong("wallpaper_changed",System.currentTimeMillis()).apply();
        return true;
    }
    private static void add(List<File> list, File dir) {
        File[] files = dir.listFiles((d,n) -> n.endsWith(".jpg"));
        if (files != null) { Arrays.sort(files); list.addAll(Arrays.asList(files)); }
    }
    synchronized File current(boolean next) {
        List<File> files = images();
        if (files.isEmpty()) return null;
        String old = prefs.getString("wallpaper_current", "");
        File selected = null; for (File f : files) if (f.getAbsolutePath().equals(old)) selected = f;
        long due = prefs.getLong("wallpaper_changed", 0) + prefs.getInt("rotation_minutes", 30) * 60000L;
        if (selected != null && !next && System.currentTimeMillis() < due) return selected;
        List<File> choices = new ArrayList<>(files); if (choices.size() > 1) choices.remove(selected);
        selected = choices.get(new Random().nextInt(choices.size()));
        prefs.edit().putString("wallpaper_current", selected.getAbsolutePath())
                .putLong("wallpaper_changed", System.currentTimeMillis()).apply();
        return selected;
    }
    int importImages(List<Uri> uris, java.util.function.BooleanSupplier active) {
        initialize();
        int imported = 0;
        for (Uri uri : uris.subList(0, Math.min(20, uris.size()))) {
            if (!active.getAsBoolean()) break;
            File temp = null;
            try {
                temp = File.createTempFile("wallpaper", ".part", context.getCacheDir());
                try (InputStream in = context.getContentResolver().openInputStream(uri)) { PersonalImages.copy(in, temp, 10 * 1024 * 1024, active); }
                if (!active.getAsBoolean()) break;
                File target = new File(local, "local-" + System.nanoTime() + ".jpg");
                saveImage(temp, target); imported++;
            } catch (Exception ignored) { /* One bad image must not lose the existing collection. */ }
            finally { if (temp != null) temp.delete(); }
        }
        cap(local, 20, prefs.getString("wallpaper_current", ""));
        return imported;
    }
    static void saveImage(File source, File target) throws IOException {
        Bitmap bitmap = PersonalImages.decode(source, 1920, 1080);
        if (bitmap == null) throw new IOException("Not a supported image");
        Bitmap scaled = bitmap;
        if (bitmap.getWidth() > 1920 || bitmap.getHeight() > 1080) {
            float ratio = Math.min(1920f / bitmap.getWidth(), 1080f / bitmap.getHeight());
            scaled = Bitmap.createScaledBitmap(bitmap, Math.max(1,(int)(bitmap.getWidth()*ratio)), Math.max(1,(int)(bitmap.getHeight()*ratio)), true);
        }
        File part = new File(target.getPath() + ".part");
        try {
            try (OutputStream out = new FileOutputStream(part)) {
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)) throw new IOException("Cannot encode image");
            }
            if (!part.renameTo(target)) throw new IOException("Cannot store image");
        } finally { part.delete(); if (scaled != bitmap) scaled.recycle(); bitmap.recycle(); }
    }
    static void cap(File directory, int limit, String selected) {
        File[] files = directory.listFiles((d,n) -> n.endsWith(".jpg"));
        if (files == null) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int remove = files.length - limit;
        for (File file : files) {
            if (remove <= 0) break;
            if (!file.getAbsolutePath().equals(selected) && file.delete()) remove--;
        }
    }
}
