package com.matrix.launcher.core;

import org.junit.Test;
import static org.junit.Assert.*;
public class HomeCardSizingTest {
    @Test public void additionalFavoritesScrollInsteadOfShrinkingEveryTile(){
        int columns=HomeCardSizing.dockColumns(5),width=HomeCardSizing.slotWidth(1728,columns,24);
        assertEquals(columns,HomeCardSizing.dockColumns(40));
        assertEquals(width,HomeCardSizing.slotWidth(1728,HomeCardSizing.dockColumns(40),24));
        assertTrue((width+24)*6-24>1728); // Five apps plus Edit favorites require horizontal travel.
    }
    @Test public void validContentPreferencesKeepTheirSize() {
        for(int size=3;size<=6;size++) assertEquals(size,HomeCardSizing.contentColumns(size));
    }
    @Test public void dockFitsMeasuredWidthsWithoutOverflow() {
        // Seven favorite apps plus the restored Edit favorites tile.
        for(int available:new int[]{640,864,1280,1728,2560})for(int count=1;count<=8;count++){
            int width=HomeCardSizing.slotWidth(available,count,24);
            int used=width*count+24*(count-1);
            assertTrue(used<=available);
            assertTrue(available-used<count);
        }
    }
    @Test public void invalidContentPreferenceIsBounded() {
        assertEquals(3,HomeCardSizing.contentColumns(0));
        assertEquals(6,HomeCardSizing.contentColumns(99));
    }
}
