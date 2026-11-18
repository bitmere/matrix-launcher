package com.matrix.launcher;
import org.junit.Test;
import java.nio.file.*;
import java.io.File;
import static org.junit.Assert.*;
public class WallpaperRetentionTest {
    @Test public void importsDoNotEvictTheCurrentlySelectedWallpaper()throws Exception {
        Path directory=Files.createTempDirectory("matrix-wallpaper-test");
        try {
            File selected=Files.write(directory.resolve("current.jpg"),new byte[]{1}).toFile();selected.setLastModified(1000);
            File older=Files.write(directory.resolve("older.jpg"),new byte[]{1}).toFile();older.setLastModified(2000);
            File newest=Files.write(directory.resolve("newest.jpg"),new byte[]{1}).toFile();newest.setLastModified(3000);
            PersonalWallpapers.cap(directory.toFile(),2,selected.getAbsolutePath());
            assertTrue(selected.exists());assertFalse(older.exists());assertTrue(newest.exists());
        } finally { for(File file:directory.toFile().listFiles())file.delete();Files.delete(directory); }
    }
}
