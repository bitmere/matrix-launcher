package com.matrix.launcher.core;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ReorderSessionTest {
    @Test public void cancelRestoresTheWholeMoveAndDoesNotTouchOriginal(){
        List<String> original=Arrays.asList("a","b","c");ReorderSession<String> move=new ReorderSession<>(original);
        move.begin(1);move.move(-1);assertEquals(Arrays.asList("b","a","c"),move.items);
        move.move(20);assertEquals(2,move.moving);move.cancel();
        assertEquals(original,move.items);assertEquals(Arrays.asList("a","b","c"),original);assertEquals(-1,move.moving);
    }
    @Test public void confirmedOrderBecomesTheNextCancelBaseline(){
        ReorderSession<String> move=new ReorderSession<>(Arrays.asList("a","b","c"));
        move.begin(2);move.move(-10);move.confirm();assertEquals(Arrays.asList("c","a","b"),move.items);
        move.begin(0);move.move(1);move.cancel();assertEquals(Arrays.asList("c","a","b"),move.items);
        move.begin(-1);move.move(1);assertEquals(-1,move.moving);
    }
    @Test public void unchangedAndRoundTripMovesDoNotNeedSaving(){
        ReorderSession<String> move=new ReorderSession<>(Arrays.asList("a","b"));
        assertFalse(move.move(1));move.begin(0);assertFalse(move.move(-1));assertFalse(move.confirm());
        move.begin(0);assertTrue(move.move(1));assertTrue(move.move(-1));assertFalse(move.confirm());
        move.begin(0);assertTrue(move.move(1));assertTrue(move.confirm());
    }
}
