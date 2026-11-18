package com.matrix.launcher;

import com.matrix.launcher.core.PackageNames;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

/** User-requested management only: a system confirmation, never silent package removal. */
final class AppManagement {
    private AppManagement(){}
    static boolean uninstallable(int flags,boolean self){return !self && (flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))==0;}
    static boolean canUninstall(Context context,String pkg){
        if(!PackageNames.validPackage(pkg))return false;
        try{return uninstallable(context.getPackageManager().getApplicationInfo(pkg,0).flags,context.getPackageName().equals(pkg));}
        catch(PackageManager.NameNotFoundException unavailable){return false;}
    }
    static Intent intent(Context context,String pkg){
        if(!PackageNames.validPackage(pkg))return null;
        if(canUninstall(context,pkg)){
            Intent request=new Intent(Intent.ACTION_UNINSTALL_PACKAGE,Uri.fromParts("package",pkg,null));
            ResolveInfo handler=context.getPackageManager().resolveActivity(request,PackageManager.MATCH_DEFAULT_ONLY|PackageManager.MATCH_SYSTEM_ONLY);
            if(handler!=null){request.setComponent(new ComponentName(handler.activityInfo.packageName,handler.activityInfo.name));return request;}
        }
        return SystemSettings.appInfo(context,pkg);
    }
}
