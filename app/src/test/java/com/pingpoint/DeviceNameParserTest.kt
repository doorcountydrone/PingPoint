package com.pingpoint

import com.pingpoint.net.DeviceNameParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceNameParserTest {
    @Test
    fun readsMdnsPointerName() {
        val packet = dnsPacket(
            owner = labels("4", "1", "168", "192", "in-addr", "arpa"),
            rdata = labels("Living-Room", "local"),
            type = 12,
        )
        assertEquals("Living-Room", DeviceNameParser.parsePtr(packet))
    }

    @Test
    fun readsNetbiosWorkstationName() {
        val packet = netbiosPacket("DESKTOP-TEST")
        assertEquals("DESKTOP-TEST", DeviceNameParser.parseNetbios(packet))
    }

    @Test
    fun cleansLocalSuffixAndRejectsAddresses() {
        assertEquals("printer", DeviceNameParser.clean("printer.local."))
        assertNull(DeviceNameParser.clean("192.168.1.1"))
        assertNull(DeviceNameParser.clean("localhost"))
        assertNull(DeviceNameParser.clean("   "))
    }

    private fun labels(vararg parts: String): ByteArray {
        val out = ArrayList<Byte>()
        for (part in parts) {
            out += part.length.toByte()
            out += part.toByteArray(Charsets.US_ASCII).toList()
        }
        out += 0
        return out.toByteArray()
    }

    private fun dnsPacket(owner: ByteArray, rdata: ByteArray, type: Int): ByteArray {
        val out = ArrayList<Byte>()
        out += bytes(0x00, 0x00, 0x84, 0x00)
        out += bytes(0x00, 0x00)
        out += bytes(0x00, 0x01)
        out += bytes(0x00, 0x00, 0x00, 0x00)
        out += owner.toList()
        out += bytes(0x00, type)
        out += bytes(0x00, 0x01)
        out += bytes(0x00, 0x00, 0x00, 0x78)
        out += bytes(rdata.size shr 8, rdata.size)
        out += rdata.toList()
        return out.toByteArray()
    }

    private fun netbiosPacket(workstation: String): ByteArray {
        val question = DeviceNameParser.netbiosQuery().copyOfRange(12, DeviceNameParser.netbiosQuery().size)
        val name = ByteArray(16)
        workstation.encodeToByteArray().copyInto(name, endIndex = minOf(15, workstation.length))
        for (index in workstation.length until 15) name[index] = ' '.code.toByte()
        val rdata = ByteArray(1 + 18)
        rdata[0] = 1
        name.copyInto(rdata, destinationOffset = 1)
        val out = ArrayList<Byte>()
        out += bytes(0x12, 0x34, 0x84, 0x00)
        out += bytes(0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00)
        out += question.toList()
        out += bytes(0xC0, 0x0C)
        out += bytes(0x00, 0x21, 0x00, 0x01)
        out += bytes(0x00, 0x00, 0x00, 0x00)
        out += bytes(0x00, rdata.size)
        out += rdata.toList()
        return out.toByteArray()
    }

    private fun bytes(vararg values: Int) = values.map { it.toByte() }
}
