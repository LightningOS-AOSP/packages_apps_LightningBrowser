package com.lightning.browser.ui.browser

import android.util.Log
import android.webkit.CookieManager
import com.lightning.browser.ui.settings.DnsProvider
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket

/**
 * Real DNS-over-HTTPS. Resolves hosts through the selected provider using
 * the RFC 8484 wire format (`application/dns-message` over GET), then
 * re-establishes the page request directly to the resolved address while
 * keeping the real host name for TLS SNI and certificate verification.
 */
object DohResolver {

    private const val TAG = "LightningDoH"
    private class CacheEntry(val addresses: List<String>, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 300_000L
    private const val MAX_ANSWERS = 16

    private val BOOTSTRAP_IPS = mapOf(
        "cloudflare-dns.com" to listOf("1.1.1.1", "1.0.0.1"),
        "dns.google" to listOf("8.8.8.8", "8.8.4.4"),
        "dns.quad9.net" to listOf("9.9.9.9", "149.112.112.112"),
    )

    fun endpointFor(provider: DnsProvider, customUrl: String): String? = when (provider) {
        DnsProvider.OFF -> null
        DnsProvider.CLOUDFLARE -> "https://cloudflare-dns.com/dns-query"
        DnsProvider.GOOGLE -> "https://dns.google/dns-query"
        DnsProvider.QUAD9 -> "https://dns.quad9.net/dns-query"
        DnsProvider.CUSTOM -> customUrl.trim().ifBlank { null }?.let {
            if (it.startsWith("https://", ignoreCase = true)) it else "https://${it.removePrefix("http://")}"
        }
    }

    fun addressesFor(host: String, endpoint: String): List<String> {
        cache[host]?.let { entry -> if (entry.expiresAt > System.currentTimeMillis()) return entry.addresses }
        val resolved = runCatching { query(endpoint, host) }.getOrDefault(emptyList())
        if (resolved.isNotEmpty()) {
            cache[host] = CacheEntry(resolved, System.currentTimeMillis() + CACHE_TTL_MS)
            Log.d(TAG, "resolved $host -> ${resolved.joinToString(", ")} via DoH")
        } else {
            Log.w(TAG, "no answers for $host")
        }
        return resolved
    }

    private fun query(endpoint: String, host: String): List<String> {
        val addresses = mutableListOf<String>()
        val separator = if (endpoint.contains('?')) '&' else '?'
        val endpointHost = runCatching { URL(endpoint).host }.getOrNull()
        val bootstrap = endpointHost?.let { BOOTSTRAP_IPS[it] }.orEmpty()
        for (type in listOf(1, 28)) { // A, AAAA
            val message = question(host, type)
            val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(message)
            val queryUrl = "$endpoint${separator}dns=$encoded"
            var body = queryViaUrlConnection(queryUrl, host, type)
            if (body == null && bootstrap.isNotEmpty()) {
                for (ip in bootstrap) {
                    val resp = PinnedHttp.fetchOnce(
                        url = queryUrl,
                        ip = ip,
                        method = "GET",
                        userAgent = "Lightning",
                        headers = mapOf("Accept" to "application/dns-message"),
                    )
                    if (resp != null && resp.statusCode in 200..299 && resp.body.isNotEmpty()) {
                        body = resp.body
                        break
                    }
                }
            }
            if (body != null) {
                addresses.addAll(parseAnswers(body))
            }
        }
        return addresses
    }

    private fun queryViaUrlConnection(queryUrl: String, host: String, type: Int): ByteArray? {
        val connection = (URL(queryUrl).openConnection() as? HttpURLConnection)
            ?: run { Log.e(TAG, "openConnection failed for $host"); return null }
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/dns-message")
            connection.setRequestProperty("User-Agent", "Lightning")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            val code = connection.responseCode
            if (code !in 200..299) {
                Log.e(TAG, "$queryUrl http $code for $host")
                null
            } else {
                connection.inputStream.use { it.readBytes() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DoH query failed for $host type=$type", e)
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun question(host: String, type: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0)
        out.write(1)
        out.write(0x01)
        out.write(0x00)
        out.write(0)
        out.write(1)
        out.write(0)
        out.write(0)
        out.write(0)
        out.write(0)
        out.write(0)
        out.write(0)
        host.split('.').forEach { label ->
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0)
        out.write((type ushr 8) and 0xff)
        out.write(type and 0xff)
        out.write(0)
        out.write(1)
        return out.toByteArray()
    }

    private fun parseAnswers(data: ByteArray): List<String> {
        if (data.size < 12) return emptyList()
        val answerCount = ((data[6].toInt() and 0xff) shl 8) or (data[7].toInt() and 0xff)
        if (answerCount == 0) return emptyList()
        var pos = skipName(data, 12)
        pos += 4
        val out = mutableListOf<String>()
        repeat(minOf(answerCount, MAX_ANSWERS)) {
            pos = skipName(data, pos)
            if (pos + 10 > data.size) return out
            val type = ((data[pos].toInt() and 0xff) shl 8) or (data[pos + 1].toInt() and 0xff)
            pos += 8
            val rdLength = ((data[pos].toInt() and 0xff) shl 8) or (data[pos + 1].toInt() and 0xff)
            pos += 2
            if (pos + rdLength > data.size) return out
            when (type) {
                1 -> if (rdLength == 4) {
                    out.add(
                        "${data[pos].toInt() and 0xff}.${data[pos + 1].toInt() and 0xff}." +
                            "${data[pos + 2].toInt() and 0xff}.${data[pos + 3].toInt() and 0xff}",
                    )
                }
                28 -> if (rdLength == 16) {
                    out.add(ipv6(data, pos))
                }
            }
            pos += rdLength
        }
        return out
    }

    private fun skipName(data: ByteArray, start: Int): Int {
        var pos = start
        while (pos < data.size) {
            val length = data[pos].toInt() and 0xff
            if (length == 0) return pos + 1
            if (length and 0xc0 == 0xc0) return pos + 2
            pos += 1 + length
        }
        return data.size
    }

    private fun ipv6(data: ByteArray, offset: Int): String =
        buildString {
            for (i in 0 until 16 step 2) {
                if (i > 0) append(':')
                val value = ((data[offset + i].toInt() and 0xff) shl 8) or (data[offset + i + 1].toInt() and 0xff)
                append(value.toString(16))
            }
        }
}

/** Minimal HTTPS client that talks to a pinned IP with the real SNI host. */
object PinnedHttp {

    private const val MAX_ADDRESSES = 4
    private const val MAX_REDIRECTS = 5
    private val DROPPED_HEADERS = setOf(
        "transfer-encoding",
        "content-length",
        "content-encoding",
        "connection",
        "set-cookie",
    )

    class Response(
        val statusCode: Int,
        val headers: Map<String, String>,
        val body: ByteArray,
    )

    fun get(
        url: String,
        dnsEndpoint: String,
        userAgent: String,
        headers: Map<String, String>,
        method: String = "GET",
        followRedirects: Boolean = true,
    ): Response? {
        var currentUrl = url
        var currentMethod = method
        val requestHeaders = LinkedHashMap(headers)
        val maxHops = if (followRedirects) MAX_REDIRECTS + 1 else 1
        repeat(maxHops) {
            val parsed = runCatching { URL(currentUrl) }.getOrNull() ?: return null
            if (!parsed.protocol.equals("https", ignoreCase = true)) return null
            val host = parsed.host ?: return null
            val addresses = DohResolver.addressesFor(host, dnsEndpoint).take(MAX_ADDRESSES)
            var hopResponse: Response? = null
            for (ip in addresses) {
                Log.d("LightningDoH", "fetch $currentMethod $currentUrl -> $host@$ip")
                hopResponse = fetchOnce(currentUrl, ip, currentMethod, userAgent, requestHeaders)
                if (hopResponse != null) break
            }
            val response = hopResponse ?: return null
            if (response.statusCode !in 300..399 || !followRedirects) return response
            val location = response.headers["location"] ?: return null
            val nextUrl = runCatching { URL(parsed, location).toString() }.getOrNull() ?: return null
            currentUrl = nextUrl
            if (response.statusCode == 303 && !currentMethod.equals("HEAD", ignoreCase = true)) {
                currentMethod = "GET"
            }
            CookieManager.getInstance().getCookie(currentUrl)?.let { cookie ->
                requestHeaders["Cookie"] = cookie
            } ?: requestHeaders.remove("Cookie")
        }
        return null
    }

    internal fun fetchOnce(
        url: String,
        ip: String,
        method: String,
        userAgent: String,
        headers: Map<String, String>,
    ): Response? {
        val parsed = runCatching { URL(url) }.getOrNull() ?: return null
        val host = parsed.host ?: return null
        val port = if (parsed.port == -1) 443 else parsed.port
        val path = parsed.path.ifEmpty { "/" } + (parsed.query?.let { "?$it" } ?: "")
        val raw = Socket()
        try {
            raw.connect(InetSocketAddress(ip, port), 10_000)
            raw.soTimeout = 15_000
        } catch (e: IOException) {
            Log.w("LightningDoH", "connect failed $host@$ip: ${e.message}")
            runCatching { raw.close() }
            return null
        }
        val sslContext = runCatching {
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, null, null)
            ctx
        }.getOrNull() ?: return null
        var tls: SSLSocket? = null
        return try {
            tls = sslContext.socketFactory.createSocket(raw, host, port, true) as SSLSocket
            tls.startHandshake()
            val verifier = HttpsURLConnection.getDefaultHostnameVerifier()
            if (!verifier.verify(host, tls.session)) {
                throw SSLPeerUnverifiedException("Certificate does not match $host")
            }
            val request = buildString {
                append(method).append(' ').append(path).append(" HTTP/1.1\r\n")
                append("Host: ").append(host).append(if (port == 443) "" else ":$port").append("\r\n")
                append("Connection: close\r\n")
                append("User-Agent: ").append(userAgent).append("\r\n")
                append("Accept-Encoding: identity\r\n")
                headers.forEach { (key, value) ->
                    if (key.equals("host", true) || key.equals("connection", true) ||
                        key.equals("user-agent", true) || key.equals("accept-encoding", true)
                    ) {
                        return@forEach
                    }
                    append(key).append(": ").append(value).append("\r\n")
                }
                append("\r\n")
            }
            tls.outputStream.write(request.toByteArray(Charsets.UTF_8))
            tls.outputStream.flush()
            val input = BufferedInputStream(tls.inputStream)
            val statusLine = readLine(input) ?: return null
            val parts = statusLine.split(" ")
            val code = parts.getOrNull(1)?.toIntOrNull() ?: return null
            val cookies = mutableListOf<String>()
            val responseHeaders = readHeaders(input, cookies)
            val cookieManager = CookieManager.getInstance()
            cookies.forEach { cookie -> cookieManager.setCookie(url, cookie) }
            val rawBody = if (method.equals("HEAD", ignoreCase = true) || code == 204 || code == 304 || code in 300..399) {
                ByteArray(0)
            } else {
                readBody(input, responseHeaders, 64 * 1024 * 1024)
            }
            val encoding = responseHeaders["content-encoding"]?.trim()?.lowercase()
            val body = when (encoding) {
                null, "", "identity" -> rawBody
                "gzip", "x-gzip" -> GZIPInputStream(ByteArrayInputStream(rawBody)).use { it.readBytes() }
                "deflate" -> InflaterInputStream(ByteArrayInputStream(rawBody)).use { it.readBytes() }
                else -> {
                    Log.w("LightningDoH", "unsupported content-encoding '$encoding' for $host")
                    return null
                }
            }
            Response(code, responseHeaders.filterKeys { it !in DROPPED_HEADERS }, body)
        } catch (e: Exception) {
            Log.w("LightningDoH", "fetchOnce failed $host@$ip: ${e.message}")
            null
        } finally {
            runCatching { tls?.close() }
        }
    }

    private fun readLine(input: InputStream): String? {
        val line = ByteArrayOutputStream()
        while (true) {
            val byte = input.read()
            if (byte == -1) return if (line.size() > 0) line.toString("ISO-8859-1") else null
            if (byte == '\n'.code) {
                return line.toString("ISO-8859-1").trimEnd('\r')
            }
            line.write(byte)
        }
    }

    private fun readHeaders(input: InputStream, cookies: MutableList<String>): Map<String, String> {
        val headers = LinkedHashMap<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon > 0) {
                val key = line.substring(0, colon).trim().lowercase()
                val value = line.substring(colon + 1).trim()
                if (key == "set-cookie") cookies.add(value)
                if (!headers.containsKey(key)) headers[key] = value
            }
        }
        return headers
    }

    private fun readBody(input: InputStream, headers: Map<String, String>, cap: Long): ByteArray {
        return try {
            val transfer = headers["transfer-encoding"]?.lowercase()
            if (transfer != null && transfer.contains("chunked")) {
                readChunked(input, cap)
            } else {
                val length = headers["content-length"]?.trim()?.toLongOrNull()
                readExact(input, length ?: Long.MAX_VALUE, cap)
            }
        } catch (_: Exception) {
            ByteArray(0)
        }
    }

    private fun readChunked(input: InputStream, cap: Long): ByteArray {
        val out = ByteArrayOutputStream()
        while (true) {
            val sizeLine = readLine(input)?.trim()?.substringBefore(';') ?: break
            val size = sizeLine.toLongOrNull(16) ?: break
            if (size == 0L) break
            if (out.size() + size > cap) break
            copyN(input, out, size)
            readLine(input)
        }
        return out.toByteArray()
    }

    private fun readExact(input: InputStream, length: Long, cap: Long): ByteArray {
        val out = ByteArrayOutputStream()
        var remaining = length
        val buffer = ByteArray(8192)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read == -1) break
            if (out.size() + read > cap) return ByteArray(0)
            out.write(buffer, 0, read)
            remaining -= read
        }
        return out.toByteArray()
    }

    private fun copyN(input: InputStream, out: ByteArrayOutputStream, count: Long) {
        val buffer = ByteArray(minOf(count, 8192L).toInt())
        var remaining = count
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read == -1) return
            out.write(buffer, 0, read)
            remaining -= read
        }
    }
}