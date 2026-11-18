package com.matrix.launcher.core;

import java.time.Instant;
import java.time.ZoneId;

/** Server-reported allowance and shared cooldown; never estimates usage by counting calls. */
public final class SimklRequestPolicy {
    public int limit=-1,remaining=-1;
    public long reportedAt,blockedUntil;
    public String reason="";

    public static long nextReset(long now){
        return Instant.ofEpochMilli(now).atZone(ZoneId.of("America/New_York"))
                .toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli();
    }
    public boolean blocked(long now){return now<blockedUntil;}
    public void observe(long now,int status,String error,String quota,String left,String retry){
        int count=number(left),maximum=number(quota);
        if(count>=0 && maximum>0){remaining=count;limit=maximum;reportedAt=now;}
        if(status==429){
            boolean daily=error.equals("user_limit_exceeded") || error.equals("app_limit_exceeded");
            long seconds=number(retry);
            blockedUntil=daily?(seconds>0?now+Math.min(seconds,172800L)*1000:nextReset(now)):now+1500;
            reason=daily?error:"rate_limit";
        }else if(status>=200 && status<300 && count==0 && maximum>0){
            blockedUntil=nextReset(now);reason="user_limit_exceeded";
        }
    }
    public void networkFailure(long now){
        if(!blocked(now)){blockedUntil=now+60000;reason="connection_unavailable";}
    }
    private static int number(String value){
        try{return Math.max(-1,Integer.parseInt(value));}catch(Exception ignored){return -1;}
    }
}
