package com.matrix.launcher;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Malformed overlays must preserve usable remote settings instead of changing key capture. */
public class RemoteConfigTest {
    @Test public void malformedFlagsAndKeysKeepDeviceDefaults() throws Exception {
        JSONObject invalid = new JSONObject("{\"key_filter\":\"false\",\"input_key_keycodes\":[178,-1],\"stock_home_redirect\":false}");
        assertTrue(RemoteConfig.flag(invalid,"key_filter",true));
        assertArrayEquals(new int[]{178},RemoteConfig.integers(invalid,"input_key_keycodes",new int[]{178}));
        assertEquals("stock.launcher",RemoteConfig.text(invalid,"stock_home_redirect","stock.launcher"));
    }
    @Test public void validOverlayCanClearMappingsAndDisableFiltering() throws Exception {
        JSONObject valid = new JSONObject("{\"key_filter\":false,\"input_key_keycodes\":[],\"app_keys\":[]}");
        assertFalse(RemoteConfig.flag(valid,"key_filter",true));
        assertArrayEquals(new int[0],RemoteConfig.integers(valid,"input_key_keycodes",new int[]{178}));
        assertTrue(RemoteConfig.strings(valid,"app_keys",new String[]{"178=inputs"}).isEmpty());
    }
}
