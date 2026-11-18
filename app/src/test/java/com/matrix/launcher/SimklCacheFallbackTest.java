package com.matrix.launcher;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class SimklCacheFallbackTest {
    @Test public void bothPublicClientAndTokenAreRequiredBeforeConnectedReads(){
        assertFalse(SimklClient.configured("",""));
        assertFalse(SimklClient.configured("client",""));
        assertFalse(SimklClient.configured(" ","token"));
        assertFalse(SimklClient.configured(null,"token"));
        assertTrue(SimklClient.configured("client","token"));
    }
    @Test public void offlineQuotaAndServerFailureAllowCachedData(){
        assertTrue(SimklClient.canUseCache(new IOException("offline")));
        for(String code:new String[]{"rate_limit","user_limit_exceeded","app_limit_exceeded","connection_unavailable","http_503"})
            assertTrue(code,SimklClient.canUseCache(new SimklClient.ApiException(code)));
    }
    @Test public void authenticationAndPermissionFailuresStayVisible(){
        for(String code:new String[]{"invalid_grant","invalid_token","premium_only","oauth2_token_required","http_401","http_403","http_404"})
            assertFalse(code,SimklClient.canUseCache(new SimklClient.ApiException(code)));
    }
}
