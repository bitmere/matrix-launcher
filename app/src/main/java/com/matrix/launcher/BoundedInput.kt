package com.matrix.launcher

import java.io.File
import java.io.InputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Bounds actual bytes, including providers that do not report a length. */
internal object BoundedInput {
    fun readText(input: InputStream, limit: Int): String {
        val output = java.io.ByteArrayOutputStream(minOf(limit, 8192))
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer, 0, minOf(buffer.size, limit - output.size() + 1))
            if (count < 0) break
            if (output.size() + count > limit) throw IOException("Document exceeds size limit")
            output.write(buffer, 0, count)
        }
        return output.toString("UTF-8")
    }

    /** Keep the existing file intact until a complete sibling has reached disk. */
    fun replace(file: File, text: String) {
        val staged = File.createTempFile(file.name, ".tmp", file.parentFile)
        try {
            staged.outputStream().use { it.write(text.toByteArray(Charsets.UTF_8)); it.fd.sync() }
            Files.move(staged.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { staged.delete() }
    }
}
