package com.matrix.launcher.core;


import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class SimklCachePolicyTest {
    @Test public void movieChangesDoNotInvalidateTvLibrary()throws Exception {
        JSONObject before=new JSONObject("{\"all\":\"1\",\"tv_shows\":{\"watching\":\"1\"},\"movies\":{\"plantowatch\":\"1\"}}");
        JSONObject after=new JSONObject("{\"all\":\"2\",\"tv_shows\":{\"watching\":\"1\"},\"movies\":{\"plantowatch\":\"2\"}}");
        assertEquals(SimklCachePolicy.libraryStamp(before,"shows"),SimklCachePolicy.libraryStamp(after,"shows"));
        assertNotEquals(SimklCachePolicy.libraryStamp(before,"movies"),SimklCachePolicy.libraryStamp(after,"movies"));
    }
    @Test public void oneListChangeLeavesOtherCachedListsValid()throws Exception {
        JSONObject oldActivity=new JSONObject("{\"custom_lists\":{\"all\":\"1\"}}");
        JSONObject newActivity=new JSONObject("{\"custom_lists\":{\"all\":\"2\"}}");
        JSONObject before=new JSONObject("{\"lists\":[{\"id\":12,\"updated_at\":\"1\"},{\"id\":13,\"updated_at\":\"1\"}]}");
        JSONObject after=new JSONObject("{\"lists\":[{\"id\":12,\"updated_at\":\"2\"},{\"id\":13,\"updated_at\":\"1\"}]}");
        assertEquals(SimklCachePolicy.listStamp(oldActivity,before,"13"),SimklCachePolicy.listStamp(newActivity,after,"13"));
        assertNotEquals(SimklCachePolicy.listStamp(oldActivity,before,"12"),SimklCachePolicy.listStamp(newActivity,after,"12"));
    }
    @Test public void missingTimestampFallsBackToActivityGate()throws Exception {
        JSONObject activity=new JSONObject("{\"all\":\"3\"}");
        assertEquals("3",SimklCachePolicy.libraryStamp(activity,"anime"));
        assertEquals("3",SimklCachePolicy.listStamp(activity,new JSONObject(),"12"));
    }
}
