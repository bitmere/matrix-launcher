package com.matrix.launcher;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HomeRequestTest {
    @org.junit.Test public void recreationPreservesHistoryUnlessHomeWasRequested() {
        org.junit.Assert.assertFalse(HomeRequest.resetOnCreate(true, "android.intent.action.MAIN", false));
        org.junit.Assert.assertTrue(HomeRequest.resetOnCreate(false, null, false));
        org.junit.Assert.assertTrue(HomeRequest.resetOnCreate(true, null, true));
        org.junit.Assert.assertTrue(HomeRequest.resetOnCreate(true, "com.android.tv.action.VIEW_INPUTS", false));
    }
    @Test public void savedLegacyRemoteActionsRemainAcceptedAfterNamespaceMigration(){
        assertEquals(HomeRequest.Page.APPS,HomeRequest.page("com.gathin.tvhome.action.APP_LIBRARY"));
        assertEquals(HomeRequest.Page.SETTINGS,HomeRequest.page("com.gathin.tvhome.action.LAUNCHER_SETTINGS"));
    }
    @Test public void existingRemoteActionsSelectMatrixPages(){
        assertEquals(HomeRequest.Page.INPUTS,HomeRequest.page("com.android.tv.action.VIEW_INPUTS"));
        assertEquals(HomeRequest.Page.APPS,HomeRequest.page("com.matrix.launcher.action.APP_LIBRARY"));
        assertEquals(HomeRequest.Page.SETTINGS,HomeRequest.page("com.matrix.launcher.action.LAUNCHER_SETTINGS"));
    }
    @Test public void consumedHomeRequestDoesNotResetRecreatedNavigation(){
        org.junit.Assert.assertFalse(HomeRequest.resetOnCreate(true, null, false));
        org.junit.Assert.assertTrue(HomeRequest.resetOnCreate(true, "android.intent.action.MAIN", true));
    }
    @Test public void ordinaryHomeAndUnknownActionsDoNotOpenPanels(){
        assertEquals(HomeRequest.Page.NONE,HomeRequest.page(null));
        assertEquals(HomeRequest.Page.NONE,HomeRequest.page("android.intent.action.MAIN"));
        assertEquals(HomeRequest.Page.NONE,HomeRequest.page("arbitrary.external.action"));
    }
}
