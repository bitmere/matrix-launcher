package com.matrix.launcher;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class ThemeFilesTest {
    @Test public void onlyTheConsumedRemoteOverlayIsAllowed() {
        assertEquals("config.json", ThemeFiles.path(Collections.singletonList("config.json")));
        for (String retired : new String[]{"wallpaper.jpg", "theme_revision", "wallpapers", "captions.txt", ".."})
            assertNull(ThemeFiles.path(Collections.singletonList(retired)));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "dusk.jpg")));
        assertNull(ThemeFiles.path(Arrays.asList("..", "config.json")));
        assertNull(ThemeFiles.path(Collections.emptyList()));
        assertNull(ThemeFiles.path(null));
    }
}
