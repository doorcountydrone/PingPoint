package com.pingpoint

import com.pingpoint.net.MacLabel
import com.pingpoint.net.MacVendor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MacVendorTest {
    private val sample = mapOf(
        "000393" to "Apple",
        "0000F0" to "Samsung",
        "000AEB" to "TP-Link",
        "3C5AB4" to "Google",
    )

    @Test
    fun readsTheMakerFromTheFirstThreeBytes() {
        assertEquals("Apple", MacVendor.maker("00:03:93:11:22:33", sample))
        assertEquals("Samsung", MacVendor.maker("00-00-F0-AA-BB-CC", sample))
        assertEquals("TP-Link", MacVendor.maker("00:0a:eb:01:02:03", sample))
        assertEquals("Google", MacVendor.maker("3C:5A:B4:00:00:01", sample))
    }

    @Test
    fun skipsRandomizedAndGroupAddresses() {
        assertEquals(MacLabel.Private, MacVendor.identify("02:03:93:11:22:33", sample))
        assertEquals(MacLabel.Private, MacVendor.identify("A6:11:22:33:44:55", sample))
        assertEquals(MacLabel.Unknown, MacVendor.identify("01:00:5E:00:00:01", sample))
        assertEquals(MacLabel.Unknown, MacVendor.identify("00:11:22:33:44:55", sample))
    }

    @Test
    fun shippedTableKnowsCommonMakers() {
        val file = File("src/main/assets/oui.txt")
        assertTrue(file.exists())
        val vendors = file.readLines().mapNotNull { line ->
            val tab = line.indexOf('\t')
            if (tab != 6) null else line.substring(0, 6) to line.substring(tab + 1)
        }.toMap()
        assertEquals("Apple", MacVendor.maker("00:03:93:00:00:01", vendors))
        assertEquals("Samsung", MacVendor.maker("00:00:F0:00:00:01", vendors))
        assertEquals("TP-Link", MacVendor.maker("00:0A:EB:00:00:01", vendors))
        assertEquals("Google", MacVendor.maker("3C:5A:B4:00:00:01", vendors))
    }
}
