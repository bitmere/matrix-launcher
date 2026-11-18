package com.matrix.launcher;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SimklDiskCacheTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private File snapshot(File dir, String key, long age) throws Exception {
        File file = new File(dir,key+".json"); Files.write(file.toPath(),new byte[4]);
        assertTrue(file.setLastModified(age)); return file;
    }
    @Test public void evictionKeepsLibraryMetadataAndPinnedPreviewAndRemovesOldestPages() throws Exception {
        File dir=temp.newFolder();
        File library=snapshot(dir,"library-shows",1000), pinned=snapshot(dir,"list-1-1",2000);
        File old=snapshot(dir,"list-1-2",3000), recent=snapshot(dir,"list-2-2",4000);
        assertTrue(SimklDiskCache.trim(dir,12,3,new HashSet<>(java.util.Arrays.asList("library-shows","list-1-1"))));
        assertTrue(library.exists()); assertTrue(pinned.exists()); assertTrue(recent.exists()); assertFalse(old.exists());
    }
    @Test public void countBudgetAndProtectedOverflowCannotDeleteRetainedSnapshots() throws Exception {
        File dir=temp.newFolder();File kept=snapshot(dir,"lists",1000);snapshot(dir,"list-2-2",2000);
        assertTrue(SimklDiskCache.trim(dir,100,1,Collections.singleton("lists")));
        assertEquals(1,dir.listFiles().length);
        assertFalse(SimklDiskCache.trim(dir,1,1,Collections.singleton("lists")));assertTrue(kept.exists());
    }
    @Test public void atomicReplacementAndOversizedWritePreserveTheLastGoodSnapshot() throws Exception {
        File dir=temp.newFolder();Set<String> retained=Collections.singleton("lists");
        assertTrue(SimklDiskCache.write(dir,"lists","first".getBytes(java.nio.charset.StandardCharsets.UTF_8),retained));
        assertTrue(SimklDiskCache.write(dir,"lists","second".getBytes(java.nio.charset.StandardCharsets.UTF_8),retained));
        assertFalse(SimklDiskCache.write(dir,"lists",new byte[SimklDiskCache.ENTRY_BYTES+1],retained));
        assertEquals("second",new String(Files.readAllBytes(new File(dir,"lists.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(1,dir.listFiles().length);
    }
    @Test public void prepareRemovesInterruptedTransfersWithoutRemovingUnrelatedFiles() throws Exception {
        File dir=temp.newFolder();File part=new File(dir,"simkl-old.part");assertTrue(part.createNewFile());
        File other=new File(dir,"keep.txt");assertTrue(other.createNewFile());
        File invalid=new File(dir,"library-shows.json");
        try(java.io.RandomAccessFile file=new java.io.RandomAccessFile(invalid,"rw")){file.setLength(SimklDiskCache.ENTRY_BYTES+1L);}
        SimklDiskCache.prepare(dir,Collections.singleton("library-shows"));
        assertFalse(part.exists());assertTrue(other.exists());assertFalse(invalid.exists());
    }
}
