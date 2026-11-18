package com.matrix.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public class MatrixComponentCompatibilityTest {
    @Test public void selectedServiceClassesRemainStableWhileImplementationsMoveToMatrix() {
        assertEquals("com.gathin.tvhome.HomeKeyService", MatrixComponents.HOME_SERVICE);
        assertEquals(HomeKeyService.class, com.gathin.tvhome.HomeKeyService.class.getSuperclass());
        assertEquals(InputKeyService.class, com.gathin.tvhome.InputKeyService.class.getSuperclass());
        assertEquals(GuardRevival.class, com.gathin.tvhome.GuardRevival.class.getSuperclass());
        assertEquals(ClockDream.class, com.gathin.tvhome.ClockDream.class.getSuperclass());
    }
}
