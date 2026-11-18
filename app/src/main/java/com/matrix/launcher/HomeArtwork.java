package com.matrix.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.LruCache;

/** Shared, bounded app artwork cache; keeps banners proportional and icons square. */
final class HomeArtwork {
    static final class Art {
        final Bitmap bitmap;
        final int accent;
        Art(Bitmap bitmap, int accent) { this.bitmap = bitmap; this.accent = accent; }
    }
    private final Context context;
    private int generation;
    private final LruCache<String, Art> cache = new LruCache<String, Art>(8 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Art art) { return art.bitmap.getByteCount(); }
    };
    HomeArtwork(Context context) { this.context = context.getApplicationContext(); }
    /** Memory-only; safe to seed visible artwork without a placeholder frame or disk/package I/O. */
    Art peek(String pkg) { synchronized (cache) { return cache.get(pkg); } }
    synchronized Art get(String pkg) {
        final int requestedGeneration;
        synchronized (cache) {
            Art hit = cache.get(pkg);
            if (hit != null) return hit;
            requestedGeneration = generation;
        }
        try {
            PackageManager pm = context.getPackageManager();
            Drawable drawable = null;
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setPackage(pkg);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                int id = info.activityInfo.banner != 0 ? info.activityInfo.banner : info.activityInfo.applicationInfo.banner;
                if (id != 0) {
                    Resources resources = pm.getResourcesForApplication(info.activityInfo.applicationInfo);
                    drawable = resources.getDrawableForDensity(id, Math.max(DisplayMetrics.DENSITY_XHIGH,
                            context.getResources().getDisplayMetrics().densityDpi), null);
                }
                if (drawable != null) break;
            }
            if (drawable == null) drawable = pm.getApplicationBanner(pkg);
            boolean banner = drawable != null && drawable.getIntrinsicWidth() >= drawable.getIntrinsicHeight() * 1.4f;
            if (!banner) drawable = pm.getApplicationIcon(pkg);
            int accent = ArtworkColor.vividColor(drawable);
            // Cache a proportional 480 x 270 banner; square icons retain a centered square image.
            Bitmap bitmap = Bitmap.createBitmap(480, 270, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(context.getColor(R.color.home_artwork_fallback));
            int width = Math.max(1, drawable.getIntrinsicWidth()), height = Math.max(1, drawable.getIntrinsicHeight());
            float scale = Math.min((banner ? 480f : 176f) / width, (banner ? 270f : 176f) / height);
            int w = Math.round(width * scale), h = Math.round(height * scale);
            Rect previous = new Rect(drawable.getBounds());
            drawable.setBounds((480-w)/2, (270-h)/2, (480+w)/2, (270+h)/2);
            drawable.draw(canvas);
            drawable.setBounds(previous);
            Art art = new Art(bitmap, accent);
            synchronized (cache) {
                if (requestedGeneration == generation) cache.put(pkg, art);
            }
            return art;
        } catch (Exception ignored) { return null; }
    }
    // Invalidation from a package broadcast must not wait for a drawable decode on an IO worker.
    void clear() { synchronized (cache) { generation++; cache.evictAll(); } }
}
