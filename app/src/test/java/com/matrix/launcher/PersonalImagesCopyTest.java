package com.matrix.launcher;

import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class PersonalImagesCopyTest {
    @Test public void cancellationDuringReadDoesNotWriteReturnedBytes() throws Exception {
        File target = File.createTempFile("matrix-copy", ".tmp");
        AtomicBoolean active = new AtomicBoolean(true);
        InputStream in = new ByteArrayInputStream(new byte[32]) {
            @Override public synchronized int read(byte[] bytes, int offset, int length) {
                int count = super.read(bytes, offset, length);
                active.set(false);
                return count;
            }
        };
        try {
            assertThrows(InterruptedIOException.class, () -> PersonalImages.copy(in, target, 1024, active::get));
            assertEquals(0, target.length());
        } finally { Files.deleteIfExists(target.toPath()); }
    }

    @Test public void boundedCopyAcceptsExactLimitAndRejectsExtraBytes() throws Exception {
        File target = File.createTempFile("matrix-copy", ".tmp");
        try {
            PersonalImages.copy(new ByteArrayInputStream(new byte[32]), target, 32);
            assertEquals(32, target.length());
            assertThrows(IOException.class, () -> PersonalImages.copy(new ByteArrayInputStream(new byte[33]), target, 32));
            assertEquals(0, target.length());
        } finally { Files.deleteIfExists(target.toPath()); }
    }
}
