package com.matrix.launcher;
import android.content.pm.ApplicationInfo;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppManagementTest {
    @Test public void ordinaryInstalledAppCanRequestUninstall(){assertTrue(AppManagement.uninstallable(0,false));}
    @Test public void preinstalledAppsAndTheirUpdatesAreProtected(){assertFalse(AppManagement.uninstallable(ApplicationInfo.FLAG_SYSTEM,false));assertFalse(AppManagement.uninstallable(ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,false));}
    @Test public void launcherCannotRequestItsOwnUninstall(){assertFalse(AppManagement.uninstallable(0,true));}
}
