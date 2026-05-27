package com.example.clickdevice

import com.example.clickdevice.activity.ScriptRunConfig
import org.junit.Assert.*
import org.junit.Test

class ScriptRunConfigTest {

    @Test
    fun defaultValues_areCorrect() {
        val config = ScriptRunConfig()
        assertEquals(1000, config.interval)
        assertEquals(1, config.count)
        assertEquals(1.0, config.speed, 0.001)
        assertFalse(config.checkAppChange)
    }

    @Test
    fun copy_modifiesOnlySpecifiedField() {
        val original = ScriptRunConfig()
        val modified = original.copy(interval = 2000)

        assertEquals(2000, modified.interval)
        assertEquals(original.count, modified.count)
        assertEquals(original.speed, modified.speed, 0.001)
        assertEquals(original.checkAppChange, modified.checkAppChange)
    }

    @Test
    fun copy_allFields_modifiesAll() {
        val config = ScriptRunConfig().copy(
            interval = 500,
            count = 10,
            speed = 2.5,
            checkAppChange = true
        )
        assertEquals(500, config.interval)
        assertEquals(10, config.count)
        assertEquals(2.5, config.speed, 0.001)
        assertTrue(config.checkAppChange)
    }

    @Test
    fun equality_sameValues_areEqual() {
        val a = ScriptRunConfig(interval = 500, count = 3)
        val b = ScriptRunConfig(interval = 500, count = 3)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equality_differentValues_areNotEqual() {
        val a = ScriptRunConfig(interval = 500)
        val b = ScriptRunConfig(interval = 1000)
        assertNotEquals(a, b)
    }
}
