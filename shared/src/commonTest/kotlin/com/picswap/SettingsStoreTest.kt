package com.picswap

import com.picswap.domain.DragMode
import com.picswap.domain.DragSettings
import com.picswap.ui.AppSettings
import com.picswap.ui.DEFAULT_PRESETS
import com.picswap.ui.KeyValueStore
import com.picswap.ui.SettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsStoreTest {
    private class MemoryStore : KeyValueStore {
        val m = HashMap<String, Any>()
        override fun getString(key: String) = m[key] as String?
        override fun getBoolean(key: String, default: Boolean) = m[key] as Boolean? ?: default
        override fun getLong(key: String, default: Long) = m[key] as Long? ?: default
        override fun putString(key: String, value: String) { m[key] = value }
        override fun putBoolean(key: String, value: Boolean) { m[key] = value }
        override fun putLong(key: String, value: Long) { m[key] = value }
    }

    @Test fun defaultBackgroundIsPureBlack() {
        assertEquals(0xFF000000L, SettingsStore(MemoryStore()).load().lastColor)
    }

    @Test fun savesAndLoadsSettings() {
        val store = SettingsStore(MemoryStore())
        store.save(AppSettings(lastColor = 0xFFFFFFFFL))
        assertEquals(0xFFFFFFFFL, store.load().lastColor)
    }

    @Test fun presetsAndDragSettingsAreStored() {
        val store = SettingsStore(MemoryStore())
        val defaults = store.load()
        assertEquals(DEFAULT_PRESETS, defaults.presets)
        assertEquals(DragMode.Drop, defaults.drag.mode)
        store.save(
            defaults.copy(
                presets = listOf(0xFF8B5A2BL, 0xFFFFFFFFL, 0xFF00B140L, 0xFF0047BBL),
                drag = DragSettings(DragMode.Follow, edgeOnly = true, edgeDp = 120),
            ),
        )
        val loaded = store.load()
        assertEquals(0xFF8B5A2BL, loaded.presets[0])
        assertEquals(DragSettings(DragMode.Follow, true, 120), loaded.drag)
    }

    @Test fun featherAndGuideAreStored() {
        val store = SettingsStore(MemoryStore())
        assertEquals(10, store.load().lastFeather)
        assertEquals(true, store.load().showGuide)
        store.save(store.load().copy(lastFeather = 55, showGuide = false))
        assertEquals(55, store.load().lastFeather)
        assertEquals(false, store.load().showGuide)
    }

    @Test fun photoTipsAreOnByDefaultAndStored() {
        val store = SettingsStore(MemoryStore())
        assertEquals(true, store.load().showPhotoTips)
        store.save(store.load().copy(showPhotoTips = false))
        assertEquals(false, store.load().showPhotoTips)
    }
}
