package com.matrix.launcher.core;

/** Only image streams explicitly shared by a content provider, never paths or remote URLs. */
public final class SharedImagePolicy {
    public static final int LIMIT = 20;
    public static boolean allowed(String mime, String uri, String ownAuthority) {
        if (mime == null || !mime.startsWith("image/") || uri == null || uri.length() > 8192) return false;
        try {
            java.net.URI value = new java.net.URI(uri);
            return "content".equals(value.getScheme()) && value.getAuthority() != null && !value.getAuthority().isEmpty()
                && value.getUserInfo() == null && value.getPort() == -1 && !value.getAuthority().equals(ownAuthority);
        } catch (Exception invalid) { return false; }
    }
    private SharedImagePolicy() { }
}
