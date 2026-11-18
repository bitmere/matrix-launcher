package com.matrix.launcher.core;

/** Validates package-scoped intents without depending on an app-library UI. */
public final class PackageNames {
    public static boolean validPackage(String pkg) {
        if (pkg == null || pkg.isEmpty() || pkg.length() > 255) return false;
        boolean first = true;
        for (int i = 0; i < pkg.length(); i++) {
            char c = pkg.charAt(i);
            if (c == '.') {
                if (first) return false;
                first = true;
            } else {
                boolean letter = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
                if (!letter && (first || c < '0' || c > '9')) return false;
                first = false;
            }
        }
        return !first;
    }
}
