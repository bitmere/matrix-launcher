package com.matrix.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;

/** The standard settings intent, or, when nothing usable answers it, the first vendor settings entry
 *  from settings_fallback_components that is resolvable (Xiaomi China, TCL / FFALCON, Fire OS), else the
 *  AOSP TV settings app's main screen where it is available. */
final class SystemSettings {
    private SystemSettings() {}

    static Intent intent(Context context, String action) {
        PackageManager pm = context.getPackageManager();
        Intent standard = new Intent(action);
        ResolveInfo resolved = pm.resolveActivity(standard, PackageManager.MATCH_DEFAULT_ONLY);
        // Fire OS resolves the standard actions to a CTS compliance stub that opens nothing;
        // treat it as missing so the vendor fallback runs.
        if (resolved != null && !resolved.activityInfo.name.contains("CTSDummy")) return standard;
        Intent vendor = vendorIntent(context);
        if (vendor != null) return vendor;
        // Some firmware ships the AOSP TV settings app with no filter for the standard actions (HY300 Pro+
        // projector, rk3566, Android 14): open its main screen directly. Not a vendor entry, so the Settings
        // menu's "Native settings" row stays as it was.
        return available(pm, AOSP_TV_SETTINGS) ? new Intent().setComponent(AOSP_TV_SETTINGS) : standard;
    }

    private static final ComponentName AOSP_TV_SETTINGS =
            new ComponentName("com.android.tv.settings", "com.android.tv.settings.MainSettings");

    /** The first vendor settings entry from settings_fallback_components that is resolvable, or null
     *  when none is. China-market firmware keeps pages the AOSP TV settings has no row for -
     *  Xiaomi's Account & Security, where the ADB switch lives - in the vendor's own settings app;
     *  Retained as a fallback when standard Android settings actions are unavailable. */
    static Intent vendorIntent(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String value : context.getResources().getStringArray(R.array.settings_fallback_components)) {
            ComponentName component = ComponentName.unflattenFromString(value);
            if (component == null) continue;
            if (!available(pm, component)) continue;
            return new Intent().setComponent(component);
        }
        return null;
    }

    /** An installed package is insufficient when firmware renamed or disabled its settings activity. */
    private static boolean available(PackageManager pm, ComponentName component) {
        ResolveInfo resolved = pm.resolveActivity(new Intent().setComponent(component), 0);
        return resolved != null && resolved.activityInfo != null
                && resolved.activityInfo.enabled && resolved.activityInfo.applicationInfo.enabled
                && component.equals(new ComponentName(resolved.activityInfo.packageName, resolved.activityInfo.name));
    }

    /** The TV's App info page for pkg, or null where the firmware has none (CVTE's settings app, #1202). */
    static Intent appInfo(Context context, String pkg) {
        Intent info = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
        return context.getPackageManager().resolveActivity(info, PackageManager.MATCH_DEFAULT_ONLY) != null ? info : null;
    }
}
