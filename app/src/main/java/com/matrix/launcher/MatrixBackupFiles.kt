package com.matrix.launcher

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.UUID

/** App-owned storage only. Call on IO; constructor does no filesystem work. */
internal class MatrixBackupFiles(private val external: File?, private val internal: File) {
    companion object { const val MAX_FILES = 32 }

    private fun roots(): List<File> =
        listOfNotNull(external?.let { File(it, "backups") }, File(internal, "backups")).distinct()

    fun save(json: String, write: (File, ByteArray) -> Unit = { file, bytes ->
        file.outputStream().use { it.write(bytes); it.fd.sync() }
    }): File {
        val bytes = json.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MatrixBackup.MAX_BYTES) { "Backup exceeds size limit" }
        MatrixBackup.decode(json).getOrThrow()
        val directory = roots().firstOrNull { it.isDirectory || it.mkdirs() }
            ?: throw IOException("Cannot create backup directory")
        val target = File(directory, MatrixBackup.defaultFileName().removeSuffix(".json") +
            "-" + UUID.randomUUID() + ".json")
        val staged = File.createTempFile("matrix-backup-", ".tmp", directory)
        try {
            write(staged, bytes)
            if (!staged.renameTo(target)) throw IOException("Cannot finish backup")
            return target.absoluteFile
        } finally { staged.delete() }
    }

    /** Bound retained entries while iterating; never allocate the entire directory listing. */
    fun list(): List<File> {
        val order = compareByDescending<File> { it.name }.thenBy { it.absolutePath }
        val files = java.util.TreeSet(order)
        for (root in roots()) {
            if (!root.exists()) continue
            Files.newDirectoryStream(root.toPath(), "*.json").use { entries ->
                for (path in entries) {
                    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue
                    files.add(path.toFile().absoluteFile)
                    if (files.size > MAX_FILES) files.pollLast()
                }
            }
        }
        return files.toList()
    }

    fun read(file: File): MatrixBackupData {
        require(file.extension == "json" && roots().any { file.canonicalFile.parentFile == it.canonicalFile }) {
            "Not a local backup"
        }
        require(Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) { "Backup is unavailable" }
        return file.inputStream().use { MatrixBackup.decode(BoundedInput.readText(it, MatrixBackup.MAX_BYTES)).getOrThrow() }
    }
}
