package com.matrix.launcher;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** One bounded 12x12 sample; shared by app accents and two-color media backgrounds. */
final class ArtworkColor {
    private static final int SIZE = 12;
    static int vividColor(Drawable live) { return choose(sample(live), 0, SIZE); }
    static int[] ambientColors(Drawable live) {
        int[] pixels = sample(live);
        return new int[]{choose(pixels, 0, SIZE / 2), choose(pixels, SIZE / 2, SIZE)};
    }
    private static int[] sample(Drawable live) {
        if (live == null) return new int[0];
        Drawable drawable = live.getConstantState() != null ? live.getConstantState().newDrawable().mutate() : live;
        Rect saved = new Rect(drawable.getBounds());
        Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        try {
            drawable.setBounds(0, 0, SIZE, SIZE);
            drawable.draw(new Canvas(bitmap));
            int[] pixels = new int[SIZE * SIZE];
            bitmap.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE);
            return pixels;
        } finally { drawable.setBounds(saved); bitmap.recycle(); }
    }
    private static int choose(int[] pixels, int from, int to) {
        if (pixels.length == 0) return 0xff444444;
        int best = 0xff444444, count = 0;
        float score = -1f;
        long r = 0, g = 0, b = 0;
        for (int y = 0; y < SIZE; y++) for (int x = from; x < to; x++) {
            int color = pixels[y * SIZE + x];
            if (Color.alpha(color) < 128) continue;
            int red = Color.red(color), green = Color.green(color), blue = Color.blue(color);
            r += red; g += green; b += blue; count++;
            int max = Math.max(red, Math.max(green, blue)), min = Math.min(red, Math.min(green, blue));
            float candidate = (max == 0 ? 0f : (max - min) / (float) max) * Math.min(1f, max / 255f * 1.4f);
            if (candidate > score) { score = candidate; best = color; }
        }
        if (count == 0) return best;
        float[] hsv = new float[3];
        Color.colorToHSV(score < .25f ? Color.rgb((int)(r/count), (int)(g/count), (int)(b/count)) : best, hsv);
        hsv[1] = Math.min(1f, hsv[1] * 1.1f);
        hsv[2] = Math.max(score < .25f ? .55f : .5f, hsv[2]);
        return Color.HSVToColor(hsv);
    }
}
