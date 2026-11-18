package com.matrix.launcher.core;


import org.junit.Test;
import java.time.Instant;
import static org.junit.Assert.*;

public class SimklRequestPolicyTest {
    private long time(String value){return Instant.parse(value).toEpochMilli();}
    @Test public void absentHeadersAreUnknownNotZero(){
        SimklRequestPolicy p=new SimklRequestPolicy();p.observe(100,200,"",null,null,null);
        assertEquals(-1,p.remaining);assertFalse(p.blocked(100));
    }
    @Test public void lastSuccessfulRequestBlocksUntilEasternMidnight(){
        SimklRequestPolicy p=new SimklRequestPolicy();long now=time("2026-07-01T12:00:00Z");
        p.observe(now,200,"","1000","0",null);
        assertEquals(time("2026-07-02T04:00:00Z"),p.blockedUntil);
        assertTrue(p.blocked(now));assertFalse(p.blocked(p.blockedUntil));
    }
    @Test public void quotaRetryAfterIsSecondsNotEpoch(){
        SimklRequestPolicy p=new SimklRequestPolicy();p.observe(10000,429,"user_limit_exceeded","1000","0","60");
        assertEquals(70000,p.blockedUntil);
    }
    @Test public void burstIgnoresDailyRetryHeader(){
        SimklRequestPolicy p=new SimklRequestPolicy();p.observe(10000,429,"rate_limit","1000","25","45000");
        assertEquals(11500,p.blockedUntil);assertEquals(25,p.remaining);
    }
    @Test public void resetUsesTimezoneAcrossDst(){
        assertEquals(time("2026-11-02T05:00:00Z"),SimklRequestPolicy.nextReset(time("2026-11-01T04:30:00Z")));
        assertEquals(time("2026-03-09T04:00:00Z"),SimklRequestPolicy.nextReset(time("2026-03-08T05:30:00Z")));
    }
    @Test public void networkFailureDoesNotShortenQuotaCooldown(){
        SimklRequestPolicy p=new SimklRequestPolicy();p.observe(10000,429,"user_limit_exceeded","1000","0","120");
        p.networkFailure(11000);assertEquals(130000,p.blockedUntil);
    }
    @Test public void exemptResponseDoesNotEraseLastKnownAllowance(){
        SimklRequestPolicy p=new SimklRequestPolicy();p.observe(100,200,"","1000","45",null);
        p.observe(200,200,"",null,null,null);assertEquals(45,p.remaining);assertEquals(100,p.reportedAt);
    }
}
