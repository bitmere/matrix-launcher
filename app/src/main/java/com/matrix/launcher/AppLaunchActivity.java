package com.matrix.launcher;

import com.matrix.launcher.core.PackageNames;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/** Handles market://launch?id=<package> (Android TV Remote "launch app" by package name), which the
 *  stock launcher normally serves: opens the app's TV launch intent, then the phone launch intent. */
// Invisible market:// routing endpoint; it does not implement a splash screen.
@android.annotation.SuppressLint("CustomSplashScreen")
public class AppLaunchActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Uri data = getIntent().getData();
            String pkg = data == null || !data.isHierarchical() || !"market".equals(data.getScheme()) || !"launch".equals(data.getHost()) ? null : data.getQueryParameter("id");
            if (PackageNames.validPackage(pkg)) {
                Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                if (launch == null) launch = getPackageManager().getLaunchIntentForPackage(pkg);
                if (launch != null) startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } catch (ActivityNotFoundException | SecurityException | IllegalArgumentException ignored) {
        } finally {
            finish();
        }
    }
}
