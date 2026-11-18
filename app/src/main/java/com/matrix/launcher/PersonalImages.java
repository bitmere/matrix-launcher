package com.matrix.launcher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/** Bounded image decoding and HTTPS transfer helpers shared by active providers. */
final class PersonalImages {
    private PersonalImages() {}
    static Bitmap decode(File file, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0 || options.outWidth > 20000 || options.outHeight > 20000) return null;
        options.inSampleSize = 1;
        while (options.outWidth / options.inSampleSize > width * 2 || options.outHeight / options.inSampleSize > height * 2)
            options.inSampleSize *= 2;
        options.inJustDecodeBounds = false; options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
    static HttpURLConnection connect(String url, String bearer) throws IOException {
        URL endpoint = new URL(url);
        if (!"https".equals(endpoint.getProtocol())) throw new IOException("HTTPS required");
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
        connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "MatrixLauncher (Android TV)");
        if (bearer != null) connection.setRequestProperty("Authorization", "Bearer " + bearer);
        return connection;
    }
    static void copy(InputStream in, File target, int limit) throws IOException {
        copy(in, target, limit, () -> true);
    }
    static void copy(InputStream in, File target, int limit, java.util.function.BooleanSupplier active) throws IOException {
        if (in == null) throw new IOException("Cannot open image");
        try (OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192]; int count, total = 0;
            while (true) {
                if (!active.getAsBoolean()) throw new InterruptedIOException("Image import cancelled");
                count = in.read(buffer);
                if (!active.getAsBoolean()) throw new InterruptedIOException("Image import cancelled");
                if (count == -1) break;
                total += count;
                if (total > limit) throw new IOException("Image too large");
                out.write(buffer, 0, count);
            }
        }
    }
    static String text(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[4096]; int n;
        while ((n = in.read(b)) != -1) { if (out.size() + n > limit) throw new IOException("Response too large"); out.write(b, 0, n); }
        return out.toString("UTF-8");
    }
    static String hash(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
        StringBuilder s = new StringBuilder(); for (byte b : bytes) s.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return s.toString();
    }
}
