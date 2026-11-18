package com.matrix.launcher;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Event-driven repair for firmware that retains a selected helper but clears the master switch. */
public final class HomeRecovery extends BroadcastReceiver {
    static final String KEY = "startup_recovery";
    private static boolean checking;
    static boolean enabled(Context context) {
        return context.createDeviceProtectedStorageContext().getSharedPreferences("matrix_boot",0).getBoolean(KEY,false);
    }
    // The boot recovery flag must be on disk before a reboot.
    @android.annotation.SuppressLint("ApplySharedPref")
    static void enable(Context context, boolean value) {
        context.createDeviceProtectedStorageContext().getSharedPreferences("matrix_boot",0).edit().putBoolean(KEY,value).commit();
    }
    static boolean selected(String services, String component) {
        if(services==null)return false;
        for(String service:services.split(":"))if(component.equals(service))return true;
        return false;
    }
    static void repair(Context context) {
        try {
        if(!enabled(context) || context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)!=PackageManager.PERMISSION_GRANTED)return;
        String list=Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        ComponentName mine=MatrixComponents.homeService(context);
        // Never add a removed service: explicitly switching Matrix off remains authoritative.
        if(!selected(list,mine.flattenToString()) && !selected(list,mine.flattenToShortString()))return;
        if(Settings.Secure.getInt(context.getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,0)==0)
            Settings.Secure.putInt(context.getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,1);
        }catch(SecurityException ignored){/* A revoked ADB grant must not prevent Home opening. */}
    }
    // Enabled/selected is not proof that vendor firmware actually connected the service.
    static boolean bound(Context context) { return HomeKeyService.isConnected(); }
    /** One bounded check on foreground entry also handles vendor failure to rebind after an APK update. */
    static void verifyWhenVisible(Context context) {
        repair(context);
        if (enabled(context)) checkBinding(context, () -> { });
    }
    private static void checkBinding(Context context,Runnable complete){
        if(checking){complete.run();return;}checking=true;
        Context app=context.getApplicationContext();Handler main=new Handler(Looper.getMainLooper());
        main.postDelayed(()->{
            try{
                if(!enabled(app) || bound(app) || app.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)!=PackageManager.PERMISSION_GRANTED){checking=false;complete.run();return;}
                String before=Settings.Secure.getString(app.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                ComponentName mine=MatrixComponents.homeService(app);List<String> others=new ArrayList<>();boolean found=false;
                if(before!=null)for(String entry:before.split(":")){if(mine.equals(ComponentName.unflattenFromString(entry)))found=true;else if(!entry.isEmpty())others.add(entry);}
                if(!found){checking=false;complete.run();return;}
                String without=String.join(":",others);
                Settings.Secure.putString(app.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,without);
                main.postDelayed(()->{
                    try{
                        // A concurrent user edit wins; never overwrite another helper's settings.
                        if(enabled(app) && Objects.equals(without,Settings.Secure.getString(app.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES))){
                            Settings.Secure.putString(app.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,before);
                            Settings.Secure.putInt(app.getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,1);
                        }
                    }catch(RuntimeException ignored){/* Permission may be revoked during the repair. */}finally{checking=false;complete.run();}
                },600);
            }catch(RuntimeException ignored){checking=false;complete.run();}
        },1200);
    }
    @Override public void onReceive(Context context, Intent intent) {
        String action=intent.getAction();
        if(Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            PendingResult pending=goAsync();
            try {repair(context);checkBinding(context,pending::finish);}catch(RuntimeException ignored){pending.finish();}
        }
    }
}
