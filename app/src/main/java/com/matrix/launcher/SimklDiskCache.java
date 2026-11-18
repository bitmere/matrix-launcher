package com.matrix.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

/** Bounded, replaceable snapshots. Tokens live in preferences and are never evicted here. */
final class SimklDiskCache {
    static final long MAX_BYTES = 96L * 1024 * 1024;
    static final int MAX_FILES = 128, ENTRY_BYTES = 8 * 1024 * 1024;
    private SimklDiskCache() { }

    static synchronized void prepare(File directory, Set<String> retained) {
        File[] invalid = directory.listFiles(file -> file.getName().endsWith(".part") ||
                (file.getName().endsWith(".json") && file.length() > ENTRY_BYTES));
        if (invalid != null) for (File file : invalid) file.delete();
        trim(directory, MAX_BYTES, MAX_FILES, retained);
    }

    static synchronized boolean write(File directory, String key, byte[] bytes, Set<String> retained) throws IOException {
        if (bytes.length > ENTRY_BYTES) return false;
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create SIMKL cache");
        File target = new File(directory, key + ".json");
        java.util.HashSet<String> keep = new java.util.HashSet<>(retained);
        keep.add(key);
        // Reserve replacement space before touching the existing good snapshot.
        if (!trim(directory, MAX_BYTES - bytes.length + (target.isFile() ? target.length() : 0),
                MAX_FILES - (target.isFile() ? 0 : 1), keep)) return false;
        File part = File.createTempFile("simkl-", ".part", directory);
        try {
            Files.write(part.toPath(), bytes);
            try { Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        } finally { part.delete(); }
        return true;
    }

    /** Oldest unpinned pages go first; five metadata/library snapshots and four pinned previews fit below the budget. */
    static synchronized boolean trim(File directory, long budget, int count, Set<String> retained) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return true;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        long bytes = 0; int entries = files.length;
        for (File file : files) bytes += file.length();
        for (File file : files) {
            if (bytes <= budget && entries <= count) return true;
            String key = file.getName().substring(0, file.getName().length() - 5);
            if (retained.contains(key)) continue;
            long size = file.length();
            if (file.delete()) { bytes -= size; entries--; }
        }
        return bytes <= budget && entries <= count;
    }
}
