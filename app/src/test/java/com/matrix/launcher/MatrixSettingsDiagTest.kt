package com.matrix.launcher

import org.junit.Assert.assertFalse
import org.junit.Test

class MatrixSettingsDiagTest {
    @Test fun diagnosticIsOffByDefault() {
        assertFalse(MatrixSettingsDiag.enabled)
    }

    @Test fun loggingWhenOffDoesNothing() {
        // The detail lambda must not even be evaluated, and the call must not
        // reach android.util.Log (a stub on the JVM test runtime).
        var evaluated = false
        MatrixSettingsDiag.log("test") { evaluated = true; "no-op" }
        assertFalse(evaluated)
    }
}
