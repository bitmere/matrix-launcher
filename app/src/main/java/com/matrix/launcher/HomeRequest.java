package com.matrix.launcher;

import android.content.Context;
import android.content.Intent;

/** Explicit, one-shot navigation into Matrix; never forwards a caller's extras or component. */
final class HomeRequest {
    static final String ACTION_LAUNCHER_SETTINGS = "com.matrix.launcher.action.LAUNCHER_SETTINGS";
    static final String ACTION_APP_LIBRARY = "com.matrix.launcher.action.APP_LIBRARY";
    static final String ACTION_VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS";
    enum Page { NONE, INPUTS, APPS, SETTINGS }
    private HomeRequest() {}
    static Page page(String action) {
        if (ACTION_VIEW_INPUTS.equals(action)) return Page.INPUTS;
        if (ACTION_APP_LIBRARY.equals(action) || "com.gathin.tvhome.action.APP_LIBRARY".equals(action)) return Page.APPS;
        if (ACTION_LAUNCHER_SETTINGS.equals(action) || "com.gathin.tvhome.action.LAUNCHER_SETTINGS".equals(action)) return Page.SETTINGS;
        return Page.NONE;
    }
    /** System recreation keeps saved navigation; an actual Home/remote request wins over it. */
    static boolean resetOnCreate(boolean saved, String action, boolean homeCategory) {
        return !saved || homeCategory || page(action) != Page.NONE;
    }
    static Intent intent(Context context, String action) {
        return new Intent(context, ComposeHomeActivity.class).setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
