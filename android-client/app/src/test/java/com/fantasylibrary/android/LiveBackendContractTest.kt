package com.fantasylibrary.android

import com.fantasylibrary.android.data.network.AnimeItemDto
import com.fantasylibrary.android.data.network.ApiEnvelope
import com.fantasylibrary.android.data.network.EpisodeDto
import com.fantasylibrary.android.data.network.HealthDto
import com.fantasylibrary.android.data.network.PageDto
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * 真实后端契约测试（需要电脑上的 Spring Boot 正在运行）。
 *
 * 默认跳过；用下面的命令启用：
 *   .\gradlew.bat :app:testDebugUnitTest -PliveBackend=true --tests "*LiveBackendContractTest"
 *
 * 它验证的是：真实 Spring Boot + Oracle + MinIO 环境下，
 * 健康检查、分页、详情、分集、图片、Range、以及登录失败分支的实际行为。
 */
class LiveBackendContractTest {

    private val enabled = System.getProperty("liveBackend")?.toBoolean() == true
    private val baseUrl = (System.getProperty("liveBackendUrl") ?: "http://127.0.0.1:8080").trimEnd('/')

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    private fun get(path: String, configure: Request.Builder.() -> Unit = {}): Response {
        val request = Request.Builder()
            .url(baseUrl + path)
            .apply(configure)
            .build()
        return client.newCall(request).execute()
    }

    private fun jsonBody(response: Response): String = response.body?.string().orEmpty()

    @Test
    fun healthEndpointIsUp() {
        assumeTrue("需要 -PliveBackend=true 才会执行真实后端测试", enabled)
        get("/api/health").use { response ->
            val body = jsonBody(response)
            println("[live] GET /api/health -> ${response.code} $body")
            assertEquals(200, response.code)
            val health = json.decodeFromString(HealthDto.serializer(), body)
            assertEquals("UP", health.application)
            assertTrue("数据库应为 UP，实际 ${health.database}", health.database == "UP")
        }
    }

    @Test
    fun animePageReturnsRealOracleData() {
        assumeTrue(enabled)
        get("/api/anime?page=1&size=3").use { response ->
            val body = jsonBody(response)
            println("[live] GET /api/anime?page=1&size=3 -> ${response.code}")
            assertEquals(200, response.code)
            val envelope = json.decodeFromString<ApiEnvelope<PageDto<AnimeItemDto>>>(body)
            assertEquals("OK", envelope.code)
            val page = envelope.data!!
            println("[live] total=${page.total} totalPages=${page.totalPages} items=${page.items.size}")
            assertTrue(page.items.isNotEmpty())
            page.items.forEach { item ->
                println("[live]   #${item.work.id} ${item.work.title} cover=${item.work.cover?.url}")
                assertTrue(item.work.title.isNotBlank())
                val cover = item.work.cover?.url
                assertNotNull("作品 ${item.work.id} 缺少封面地址", cover)
                assertTrue("封面应为站内相对路径：$cover", cover!!.startsWith("/api/media/"))
            }
        }
    }

    @Test
    fun detailAndEpisodesExposePlaybackSource() {
        assumeTrue(enabled)
        val animePage = json.decodeFromString<ApiEnvelope<PageDto<AnimeItemDto>>>(
            get("/api/anime?page=1&size=50").use { jsonBody(it) },
        )
        val items = animePage.data?.items.orEmpty()
        assertTrue("动漫列表为空", items.isNotEmpty())

        // 找到真的带播放源的作品
        var found = false
        for (item in items) {
            val episodesBody = get("/api/works/${item.work.id}/episodes?allNodes=true").use { response ->
                assertEquals(200, response.code)
                jsonBody(response)
            }
            val episodes = json.decodeFromString<ApiEnvelope<List<EpisodeDto>>>(episodesBody).data.orEmpty()
            val source = episodes.firstOrNull { it.sources.isNotEmpty() }?.sources?.firstOrNull()
            if (source != null) {
                found = true
                println("[live] work=${item.work.id} episode=${episodes.first().number} source=${source.url} quality=${source.quality}")
                assertTrue("播放地址应为站内相对路径：${source.url}", source.url.startsWith("/api/media/playback/"))
                break
            }
        }
        assertTrue("所有动漫都没有播放源，无法验证视频链路", found)
    }

    @Test
    fun coverImageIsServedByMinioThroughBackend() {
        assumeTrue(enabled)
        val page = json.decodeFromString<ApiEnvelope<PageDto<AnimeItemDto>>>(
            get("/api/anime?page=1&size=1").use { jsonBody(it) },
        )
        val coverPath = page.data?.items?.firstOrNull()?.work?.cover?.url
        assertNotNull(coverPath)
        get(coverPath!!).use { response ->
            val contentType = response.header("Content-Type")
            val length = response.header("Content-Length")
            println("[live] GET $coverPath -> ${response.code} type=$contentType length=$length")
            assertEquals(200, response.code)
            assertTrue("Content-Type 应为图片：$contentType", contentType?.startsWith("image/") == true)
            val bytes = response.body?.bytes() ?: ByteArray(0)
            assertTrue("图片内容过小：${bytes.size}", bytes.size > 1024)
        }
    }

    @Test
    fun videoSupportsHttpRange() {
        assumeTrue(enabled)
        val page = json.decodeFromString<ApiEnvelope<PageDto<AnimeItemDto>>>(
            get("/api/anime?page=1&size=50").use { jsonBody(it) },
        )
        var mediaPath: String? = null
        outer@ for (item in page.data?.items.orEmpty()) {
            val episodes = json.decodeFromString<ApiEnvelope<List<EpisodeDto>>>(
                get("/api/works/${item.work.id}/episodes?allNodes=true").use { jsonBody(it) },
            ).data.orEmpty()
            val source = episodes.firstOrNull { it.sources.isNotEmpty() }?.sources?.firstOrNull()
            if (source != null) {
                mediaPath = source.url
                break@outer
            }
        }
        assertNotNull("没有找到可播放的视频地址", mediaPath)

        get(mediaPath!!) {
            header("Range", "bytes=0-1")
        }.use { response ->
            val contentRange = response.header("Content-Range")
            val acceptRanges = response.header("Accept-Ranges")
            println("[live] RANGE $mediaPath -> ${response.code} Content-Range=$contentRange Accept-Ranges=$acceptRanges")
            assertEquals("应返回 206 Partial Content", 206, response.code)
            assertNotNull(contentRange)
            assertTrue("Content-Range 应包含总长度：$contentRange", contentRange!!.contains("/"))
            assertTrue("Accept-Ranges 应为 bytes", acceptRanges.equals("bytes", ignoreCase = true))
        }
    }

    @Test
    fun loginWithWrongPasswordFailsHonestly() {
        assumeTrue(enabled)
        val cookies = mutableMapOf<String, String>()

        fun cookieHeader(): String = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

        fun absorb(response: Response) {
            response.headers("Set-Cookie").forEach { raw ->
                val pair = raw.substringBefore(';')
                val name = pair.substringBefore('=')
                val value = pair.substringAfter('=', "")
                if (name.isNotBlank()) cookies[name] = value
            }
        }

        val csrfBody = get("/api/auth/csrf") {
            header("Accept", "application/json")
        }.use { response ->
            absorb(response)
            assertEquals(200, response.code)
            jsonBody(response)
        }
        println("[live] GET /api/auth/csrf -> $csrfBody cookies=${cookies.keys}")

        val proof = json.decodeFromString<com.fantasylibrary.android.data.network.ApiEnvelope<com.fantasylibrary.android.data.network.CsrfProofDto>>(csrfBody).data
        assertNotNull(proof)
        assertTrue(proof!!.token.isNotBlank())

        val loginRequest = Request.Builder()
            .url("$baseUrl/api/auth/login")
            .header("Content-Type", "application/json")
            .header("Cookie", cookieHeader())
            .header(proof.headerName, proof.token)
            .post(
                """{"username":"android_probe_no_such_user","password":"definitely-wrong-123"}"""
                    .toRequestBody("application/json".toMediaType()),
            )
            .build()

        client.newCall(loginRequest).execute().use { response ->
            val body = jsonBody(response)
            println("[live] POST /api/auth/login (wrong password) -> ${response.code} $body")
            assertEquals("错误密码必须返回 401，不能假成功", 401, response.code)
            assertTrue("应返回 INVALID_CREDENTIALS：$body", body.contains("INVALID_CREDENTIALS"))
        }
    }
}
