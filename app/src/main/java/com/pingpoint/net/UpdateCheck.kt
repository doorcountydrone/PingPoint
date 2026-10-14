package com.pingpoint.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val versionName: String,
    val pageUrl: String,
)

object UpdateCheck {
    const val DOWNLOAD_PAGE = "https://doorcountydrone.github.io/PingPoint/"
    private const val LATEST_API =
        "https://api.github.com/repos/doorcountydrone/PingPoint/releases/latest"

    private val tagPattern = Regex(""""tag_name"\s*:\s*"([^"]+)"""")

    fun parseLatest(json: String, currentVersion: String): AppUpdate? {
        val tag = tagPattern.find(json)?.groupValues?.get(1) ?: return null
        val latest = AppVersion.normalize(tag)
        if (!AppVersion.isNewer(latest, currentVersion)) return null
        return AppUpdate(versionName = latest, pageUrl = DOWNLOAD_PAGE)
    }

    suspend fun check(currentVersion: String): AppUpdate? = withContext(Dispatchers.IO) {
        val connection = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "PingPoint")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseLatest(body, currentVersion)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
