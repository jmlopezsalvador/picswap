package com.picswap

import com.picswap.domain.PhotoFiles
import com.picswap.domain.PhotoRepository
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhotoRepositoryTest {
    private class MemoryFiles : PhotoFiles {
        val m = LinkedHashMap<String, ByteArray>()
        override fun list() = m.keys.toList()
        override fun read(name: String) = m[name]
        override fun write(name: String, bytes: ByteArray) { m[name] = bytes }
        override fun delete(name: String) { m.remove(name) }
    }

    @Test fun listsNewestFirst() {
        var now = 1000L
        val repo = PhotoRepository(MemoryFiles(), { now })
        val a = repo.add(byteArrayOf(1))
        now = 5000L
        val b = repo.add(byteArrayOf(2))
        now = 3000L
        val c = repo.add(byteArrayOf(3))
        assertEquals(listOf(b, c, a), repo.ids())
    }

    @Test fun sameInstantDoesNotOverwrite() {
        val repo = PhotoRepository(MemoryFiles(), { 42L })
        val a = repo.add(byteArrayOf(1))
        val b = repo.add(byteArrayOf(2))
        assertEquals(2, repo.ids().size)
        assertContentEquals(byteArrayOf(1), repo.bytes(a))
        assertContentEquals(byteArrayOf(2), repo.bytes(b))
    }

    @Test fun removeDeletesOnlyThatPhoto() {
        var now = 1L
        val repo = PhotoRepository(MemoryFiles(), { now++ })
        val a = repo.add(byteArrayOf(1))
        val b = repo.add(byteArrayOf(2))
        repo.remove(a)
        assertEquals(listOf(b), repo.ids())
        assertNull(repo.bytes(a))
    }

    @Test fun cutoutKeepsItsColorAndRestoreBringsBackTheOriginal() {
        val files = MemoryFiles()
        val repo = PhotoRepository(files, { 7L })
        val id = repo.add(byteArrayOf(1))
        assertContentEquals(byteArrayOf(1), repo.displayBytes(id))
        assertEquals(emptyMap(), repo.bgColors())
        repo.saveCutout(id, byteArrayOf(9), 0xFF00B140L)
        assertContentEquals(byteArrayOf(9), repo.displayBytes(id))
        assertEquals(mapOf(id to 0xFF00B140L), repo.bgColors())
        assertEquals(listOf(id), repo.ids()) // el .png y el .bg no cuentan como fotos aparte
        repo.restoreOriginal(id)
        assertContentEquals(byteArrayOf(1), repo.displayBytes(id))
        assertEquals(emptyMap(), repo.bgColors())
    }

    @Test fun removeDeletesEverythingOfThePhoto() {
        val files = MemoryFiles()
        val repo = PhotoRepository(files, { 7L })
        val id = repo.add(byteArrayOf(1))
        repo.saveCutout(id, byteArrayOf(9), 0xFF000000L)
        repo.remove(id)
        assertEquals(0, files.m.size)
    }

    @Test fun metaKeepsTheDefaultViewAndOldFilesStillLoad() {
        val files = MemoryFiles()
        val repo = PhotoRepository(files, { 9L })
        val id = repo.add(byteArrayOf(1))
        repo.saveCutout(id, byteArrayOf(2), com.picswap.domain.PhotoMeta(0xFF00B140L, 2.5f, 0.1f, -0.2f))
        assertEquals(com.picswap.domain.PhotoMeta(0xFF00B140L, 2.5f, 0.1f, -0.2f), repo.metas()[id])
        // Un fichero del formato antiguo (solo el color) sigue valiendo, con la vista normal.
        files.write(id.removeSuffix(".jpg") + ".bg", "4278190080".encodeToByteArray())
        assertEquals(com.picswap.domain.PhotoMeta(0xFF000000L), repo.metas()[id])
    }

    @Test fun metaKeepsTheEditSettingsToReopenTheEditor() {
        val repo = PhotoRepository(MemoryFiles(), { 11L })
        val id = repo.add(byteArrayOf(1))
        val meta = com.picswap.domain.PhotoMeta(
            0xFF000000L, 1.5f, 0.1f, 0.2f,
            tolerance = 55, feather = 20, featherAmount = 80, refColor = -123456, seedX = 0.25f, seedY = 0.75f,
        )
        repo.saveCutout(id, byteArrayOf(2), meta)
        assertEquals(meta, repo.metas()[id])
        // Sin cuentagotas: los campos nulos se guardan y se recuperan como nulos.
        val plain = com.picswap.domain.PhotoMeta(0xFFFFFFFFL, tolerance = 30, feather = 5, featherAmount = 100)
        repo.saveCutout(id, byteArrayOf(2), plain)
        assertEquals(plain, repo.metas()[id])
    }
}
