package com.matrix.launcher;

import android.content.ComponentName;
import android.content.Context;

/** Persisted platform identities are not branding: retain approvals across namespace-only updates. */
final class MatrixComponents {
    static final String HOME_SERVICE = "com.gathin.tvhome.HomeKeyService";
    static ComponentName homeService(Context context) { return new ComponentName(context.getPackageName(), HOME_SERVICE); }
    private MatrixComponents() { }
}
