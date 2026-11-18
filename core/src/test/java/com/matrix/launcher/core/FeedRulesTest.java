package com.matrix.launcher.core;


import org.junit.Test;
import static org.junit.Assert.*;

public class FeedRulesTest {
    @Test public void providerPosterGeometryRecognizesOnlyPortraitContractValues() {
        for (int value : new int[]{4, 5, 6}) assertTrue(FeedRules.portraitAspect(value));
        for (int value : new int[]{-1, 0, 1, 2, 3, 7, 99}) assertFalse(FeedRules.portraitAspect(value));
    }
    @Test public void recognizesPreviewContinueChannels() {
        assertTrue(FeedRules.continuing("Continue Watching"));
        assertTrue(FeedRules.continuing("Resume watching"));
        assertFalse(FeedRules.continuing("Library Movies"));
    }
    @Test public void ignoresFinishedAndStaleItemsButRetainsUndatedPreviewItems() {
        long now = 1800000000000L;
        assertFalse(FeedRules.useful(990,1000,now,now));
        assertFalse(FeedRules.useful(100,1000,now-FeedRules.MAX_AGE_MS-1,now));
        assertTrue(FeedRules.useful(100,1000,0,now));
        assertTrue(FeedRules.useful(100,1000,now,now));
    }
    @Test public void ordersRecentRowsFirstAndUsesWeightsForUndatedPreviews() {
        assertTrue(FeedRules.compare(200,0,100,99)<0);
        assertTrue(FeedRules.compare(0,10,0,5)<0);
        assertTrue(FeedRules.compare(1,0,0,100)<0);
        assertEquals(0,FeedRules.compare(0,5,0,5));
    }
}
