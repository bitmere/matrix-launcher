package com.matrix.launcher.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class SharedImagePolicyTest {
    @Test public void acceptsAChosenDownloaderImageWithoutBroadStorageAccess() {
        assertTrue(SharedImagePolicy.allowed("image/jpeg", "content://com.esaba.downloader.fileprovider/download/photo.jpg", "matrix.theme"));
    }
    @Test public void rejectsPathsRemoteLinksForeignPayloadsAndOurPrivateProvider() {
        for(String uri:new String[]{"file:///sdcard/Download/photo.jpg","https://example.com/photo.jpg","content:///photo","content://user@provider/photo","content://provider:12/photo","content://matrix.theme/config.json","bad uri"})
            assertFalse(uri, SharedImagePolicy.allowed("image/jpeg",uri,"matrix.theme"));
        assertFalse(SharedImagePolicy.allowed("application/json","content://provider/image","matrix.theme"));
        assertFalse(SharedImagePolicy.allowed(null,"content://provider/image","matrix.theme"));
        assertFalse(SharedImagePolicy.allowed("image/png",null,"matrix.theme"));
    }
}
