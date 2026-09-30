package set.starlev.utils

import java.net.HttpURLConnection
import java.net.URI

/**
 * Единый HTTP-клиент мода — замена дублирования в ModUpdater / ModuleLoader / ModuleUpdater.
 * Таймауты и UA как в оригинальных реализациях, но в одном месте.
 */
object HttpUtils {
    private const val UA_MOD = "StarredHeltix-Mod"
    private const val UA_UPDATER = "StarredHeltix-Updater"
    private const val CONNECT_TIMEOUT = 5000
    private const val READ_TIMEOUT = 10000

    fun downloadBytes(url: String, userAgent: String = UA_MOD, connectTimeout: Int = CONNECT_TIMEOUT, readTimeout: Int = READ_TIMEOUT): ByteArray? {
        return try {
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = connectTimeout
            conn.readTimeout = readTimeout
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return null
            conn.inputStream.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    fun downloadString(url: String, userAgent: String = UA_MOD): String? {
        return downloadBytes(url, userAgent)?.toString(Charsets.UTF_8)
    }

    fun downloadJson(url: String, userAgent: String = UA_MOD): String? = downloadString(url, userAgent)
}
