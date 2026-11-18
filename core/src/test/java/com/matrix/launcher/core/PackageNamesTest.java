package com.matrix.launcher.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class PackageNamesTest {
    @Test public void scopedIntentsRejectMalformedPackageNames() {
        for(String invalid:new String[]{"", ".app", "app.", "a..b", "1app", "a/b", "a:b", "com app"}) assertFalse(invalid,PackageNames.validPackage(invalid));
        assertFalse(PackageNames.validPackage(null));
        assertTrue(PackageNames.validPackage("com.example.tv_2"));
    }
}
