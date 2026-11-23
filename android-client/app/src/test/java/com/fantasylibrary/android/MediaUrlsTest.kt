package com.fantasylibrary.android

import com.fantasylibrary.android.data.network.MediaUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlsTest {

    private val baseUrl = "http://10.0.2.2:8080"

    @Test
    fun `站内相对路径会拼接到服务器地址`() {
        val check = MediaUrls.resolve(
            "/api/media/catalog/uploads/abc.webp",
            baseUrl,
            allowCleartext = true,
        )
        assertEquals(MediaUrls.Verdict.RESOLVED_RELATIVE, check.verdict)
        assertEquals("http://10.0.2.2:8080/api/media/catalog/uploads/abc.webp", check.resolved)
        assertTrue(check.ok)
    }

    @Test
    fun `回环地址被拒绝`() {
        val check = MediaUrls.resolve("http://127.0.0.1:9000/bucket/a.jpg", baseUrl, true)
        assertEquals(MediaUrls.Verdict.REJECTED, check.verdict)
        assertNotNull(check.reason)
        assertTrue(check.reason!!.contains("回环"))
        assertFalse(check.ok)
    }

    @Test
    fun `localhost 被拒绝`() {
        val check = MediaUrls.resolve("http://localhost:9000/bucket/a.jpg", baseUrl, true)
        assertEquals(MediaUrls.Verdict.REJECTED, check.verdict)
    }

    @Test
    fun `Docker 内部服务名被拒绝`() {
        val check = MediaUrls.resolve("http://minio:9000/bucket/a.jpg", baseUrl, true)
        assertEquals(MediaUrls.Verdict.REJECTED, check.verdict)
        assertTrue(check.reason!!.contains("Docker"))
    }

    @Test
    fun `预签名地址保持原样`() {
        val presigned =
            "https://s3.example.com/bucket/video.mp4?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=deadbeef&X-Amz-Expires=3600"
        val check = MediaUrls.resolve(presigned, baseUrl, allowCleartext = true)
        assertEquals(MediaUrls.Verdict.PRESIGNED, check.verdict)
        assertEquals(presigned, check.resolved)
        assertTrue(check.isPresigned)
    }

    @Test
    fun `Release 构建拒绝明文 HTTP`() {
        val check = MediaUrls.resolve("http://192.168.1.20:8080/a.jpg", baseUrl, allowCleartext = false)
        assertEquals(MediaUrls.Verdict.REJECTED, check.verdict)
        assertTrue(check.reason!!.contains("HTTPS"))
    }

    @Test
    fun `协议相对地址按基础协议补全`() {
        val check = MediaUrls.resolve("//cdn.example.com/a.png", "https://lib.example.com", true)
        assertEquals(MediaUrls.Verdict.ABSOLUTE, check.verdict)
        assertEquals("https://cdn.example.com/a.png", check.resolved)
    }

    @Test
    fun `空地址被拒绝`() {
        val check = MediaUrls.resolve(null, baseUrl, true)
        assertEquals(MediaUrls.Verdict.REJECTED, check.verdict)
        assertFalse(check.ok)
    }

    @Test
    fun `未配置服务器地址时相对路径无法解析`() {
        val check = MediaUrls.resolve("/api/media/catalog/a.jpg", "", true)
        assertEquals(MediaUrls.Verdict.REJECTED, check.verdict)
        assertTrue(check.reason!!.contains("服务器地址"))
    }

    @Test
    fun `originOf 去掉路径与末尾斜杠`() {
        assertEquals("http://192.168.1.20:8080", MediaUrls.originOf("http://192.168.1.20:8080/"))
        assertEquals("http://192.168.1.20:8080", MediaUrls.originOf("http://192.168.1.20:8080/foo/bar"))
        assertEquals(null, MediaUrls.originOf(""))
    }

    @Test
    fun `hostOf 与 portOf 兼容 ipv6`() {
        assertEquals("[::1]", MediaUrls.hostOf("http://[::1]:8080/api"))
        assertEquals(8080, MediaUrls.portOf("http://[::1]:8080/api"))
        assertEquals("192.168.1.20", MediaUrls.hostOf("http://192.168.1.20/api"))
        assertEquals(9090, MediaUrls.portOf("http://192.168.1.20:9090/api"))
    }

    @Test
    fun `显示用地址会折叠超长 query`() {
        val long = "https://example.com/a.mp4?X-Amz-Signature=" + "a".repeat(200)
        val shown = MediaUrls.displayUrl(long, maxLength = 60)
        assertTrue(shown.length < long.length)
        assertTrue(shown.contains("预签名"))
    }
}
