package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class MatrixBackupFilesTest {
    @get:Rule val temporary = TemporaryFolder()
    private val values = mapOf<String, Any>("list_columns" to 5, "ai_discovery_enabled" to true)
    private fun json() = MatrixBackup.encode(values, null)
    private fun store(external: File? = null) = MatrixBackupFiles(external, temporary.root)

    @Test fun localBackupRoundTripAppliesPreferencesAfterRestart() {
        val file = store().save(json())
        val data = store().read(file)
        val restored = mutableMapOf<String, Any>()
        assertEquals(2, MatrixBackup.apply(temporary.root, mapBackedPrefs(restored), data))
        assertEquals(values, restored)
        assertEquals(listOf(file), store().list())
    }

    @Test fun nullExternalParentUsesAbsoluteInternalBackupsDirectory() {
        val file = store().save(json())
        assertTrue(file.isAbsolute)
        assertEquals(File(temporary.root, "backups"), file.parentFile)
    }

    @Test fun externalStorageIsPreferredAndBothRootsRemainRestorable() {
        val external = temporary.newFolder("external")
        val internalFile = store().save(json())
        val externalFile = store(external).save(json())
        assertEquals(File(external, "backups"), externalFile.parentFile)
        assertEquals(setOf(internalFile, externalFile), store(external).list().toSet())
        assertEquals(values, store(external).read(internalFile).prefs)
    }

    @Test fun unavailableExternalDirectoryFallsBackToInternal() {
        val external = temporary.newFile("unmounted")
        val file = store(external).save(json())
        assertEquals(File(temporary.root, "backups"), file.parentFile)
    }

    @Test fun repeatedSaveNeverOverwritesExistingBackup() {
        val first = store().save(json())
        val second = store().save(json())
        assertNotEquals(first, second)
        assertEquals(json(), first.readText())
        assertEquals(2, store().list().size)
    }

    @Test fun malformedJsonIsRejectedOnSaveAndRestore() {
        assertTrue(runCatching { store().save("{broken") }.isFailure)
        val valid = store().save(json())
        valid.writeText("{broken")
        assertTrue(runCatching { store().read(valid) }.isFailure)
    }

    @Test fun oversizedJsonIsRejectedOnSaveAndBoundedRead() {
        val oversized = " ".repeat(MatrixBackup.MAX_BYTES + 1)
        assertTrue(runCatching { store().save(oversized) }.isFailure)
        val file = store().save(json())
        file.writeText(oversized)
        assertTrue(runCatching { store().read(file) }.exceptionOrNull() is IOException)
    }

    @Test fun fullStorageCleansTemporaryFileAndPreservesExistingBackup() {
        val existing = store().save(json())
        val failure = IOException("No space left on device")
        assertSame(failure, runCatching {
            store().save(json()) { file, _ -> file.writeText("partial"); throw failure }
        }.exceptionOrNull())
        assertEquals(listOf(existing), store().list())
        assertEquals(listOf(existing.name), existing.parentFile!!.list()!!.toList())
        assertEquals(json(), existing.readText())
    }

    @Test fun listIsBoundedDeterministicAndIgnoresDirectoriesAndTemporaryFiles() {
        val root = temporary.newFolder("backups")
        repeat(40) { File(root, "backup-%02d.json".format(it)).writeText(json()) }
        File(root, "ignored.tmp").writeText("partial")
        File(root, "folder.json").mkdir()
        val files = store().list()
        assertEquals(MatrixBackupFiles.MAX_FILES, files.size)
        assertEquals("backup-39.json", files.first().name)
        assertEquals(files, store().list())
        assertTrue(files.all { it.isFile && it.extension == "json" })
    }

    @Test fun restoreRejectsFilesOutsideBackupRoots() {
        val file = temporary.newFile("outside.json").apply { writeText(json()) }
        assertTrue(runCatching { store().read(file) }.isFailure)
    }

    @Test fun failedRenameLeavesNoPartialJson() {
        val saved = store().save(json())
        assertTrue(runCatching {
            store().save(json()) { staged, _ -> staged.delete() }
        }.exceptionOrNull() is IOException)
        assertEquals(listOf(saved), store().list())
        assertEquals(1, saved.parentFile!!.list()!!.size)
    }
}
