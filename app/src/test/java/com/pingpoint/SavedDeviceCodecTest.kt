package com.pingpoint

import com.pingpoint.model.SavedDevice
import com.pingpoint.net.SavedDeviceCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedDeviceCodecTest {
    @Test
    fun roundTripsNameHostAndMac() {
        val devices = listOf(
            SavedDevice(name = "Living Room", host = "192.168.1.20", mac = "AA:BB:CC:DD:EE:FF"),
            SavedDevice(name = "Office PC", host = "office-pc.local", mac = null),
        )
        assertEquals(devices, SavedDeviceCodec.decode(SavedDeviceCodec.encode(devices)))
    }

    @Test
    fun keepsTabsAndNewlinesInsideTheName() {
        val device = SavedDevice(name = "Line\nTwo\tTab", host = "10.0.0.8", mac = "00-11-22-33-44-55")
        val restored = SavedDeviceCodec.decode(SavedDeviceCodec.encode(listOf(device))).single()
        assertEquals("Line\nTwo\tTab", restored.name)
        assertEquals("00:11:22:33:44:55", restored.mac)
    }

    @Test
    fun updatesAnExistingHostAndKeepsItsPlace() {
        val original = listOf(
            SavedDevice("Router", "192.168.1.1", "AA:BB:CC:DD:EE:01"),
            SavedDevice("Printer", "192.168.1.50", null),
        )
        val updated = SavedDeviceCodec.upsert(
            original,
            SavedDevice("Front router", "192.168.1.1", "AA:BB:CC:DD:EE:01"),
        )
        assertEquals(listOf("Front router", "Printer"), updated.map { it.name })
    }

    @Test
    fun rejectsAMacThatIsNotAnAddress() {
        assertNull(SavedDeviceCodec.normalizeMac("not-a-mac"))
        assertEquals("AA:BB:CC:DD:EE:FF", SavedDeviceCodec.normalizeMac("aa-bb-cc-dd-ee-ff"))
    }
}
