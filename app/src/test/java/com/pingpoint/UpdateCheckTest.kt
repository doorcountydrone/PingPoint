package com.pingpoint

import com.pingpoint.net.AppVersion
import com.pingpoint.net.UpdateCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckTest {
    @Test
    fun comparesDottedVersions() {
        assertTrue(AppVersion.isNewer("1.1", "1.0"))
        assertTrue(AppVersion.isNewer("v2.0", "1.9"))
        assertTrue(AppVersion.isNewer("1.10", "1.9"))
        assertFalse(AppVersion.isNewer("1.0", "1.0"))
        assertFalse(AppVersion.isNewer("1.0", "1.1"))
    }

    @Test
    fun parsesANewerGithubRelease() {
        val json = """
            {"tag_name":"v1.2","html_url":"https://github.com/doorcountydrone/PingPoint/releases/tag/v1.2"}
        """.trimIndent()
        val update = UpdateCheck.parseLatest(json, "1.0")
        assertEquals("1.2", update?.versionName)
        assertEquals(UpdateCheck.DOWNLOAD_PAGE, update?.pageUrl)
    }

    @Test
    fun ignoresTheSameOrOlderRelease() {
        val json = """{"tag_name":"v1.0","html_url":"https://example.com"}"""
        assertNull(UpdateCheck.parseLatest(json, "1.0"))
        assertNull(UpdateCheck.parseLatest(json, "1.1"))
    }
}
