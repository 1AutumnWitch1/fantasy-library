package com.fantasylibrary.android

import com.fantasylibrary.android.data.network.ServerAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAddressTest {

    @Test
    fun `默认端口为 8080`() {
        val result = ServerAddress.parse("", "192.168.1.20", "", allowCleartext = true)
        assertTrue(result is ServerAddress.ParseResult.Ok)
        val address = (result as ServerAddress.ParseResult.Ok).address
        assertEquals("http://192.168.1.20:8080", address.baseUrl)
    }

    @Test
    fun `允许直接粘贴完整地址`() {
        val result = ServerAddress.parse("", "http://192.168.1.20:9090", "", true)
        val address = (result as ServerAddress.ParseResult.Ok).address
        assertEquals("http://192.168.1.20:9090", address.baseUrl)
    }

    @Test
    fun `端口字段优先于地址里的端口`() {
        val result = ServerAddress.parse("http", "192.168.1.20:9090", "8080", true)
        val address = (result as ServerAddress.ParseResult.Ok).address
        assertEquals("http://192.168.1.20:8080", address.baseUrl)
    }

    @Test
    fun `https 默认端口为 443`() {
        val result = ServerAddress.parse("https", "lib.example.com", "", true)
        val address = (result as ServerAddress.ParseResult.Ok).address
        assertEquals("https://lib.example.com:443", address.baseUrl)
    }

    @Test
    fun `空地址被拒绝`() {
        assertTrue(ServerAddress.parse("", "   ", "", true) is ServerAddress.ParseResult.Invalid)
    }

    @Test
    fun `非法端口被拒绝`() {
        assertTrue(ServerAddress.parse("http", "192.168.1.20", "70000", true) is ServerAddress.ParseResult.Invalid)
        assertTrue(ServerAddress.parse("http", "192.168.1.20", "abc", true) is ServerAddress.ParseResult.Invalid)
    }

    @Test
    fun `Release 构建不允许 http`() {
        assertTrue(
            ServerAddress.parse("http", "192.168.1.20", "8080", allowCleartext = false)
                is ServerAddress.ParseResult.Invalid,
        )
    }

    @Test
    fun `地址中不允许含账号密码`() {
        assertTrue(
            ServerAddress.parse("", "user:pass@192.168.1.20", "", true)
                is ServerAddress.ParseResult.Invalid,
        )
    }

    @Test
    fun `路径会被剥离`() {
        val result = ServerAddress.parse("", "192.168.1.20/api/anime", "", true)
        val address = (result as ServerAddress.ParseResult.Ok).address
        assertEquals("http://192.168.1.20:8080", address.baseUrl)
    }

    @Test
    fun `回环地址给出提示而不是直接拒绝`() {
        val result = ServerAddress.parse("", "127.0.0.1", "8080", true)
        val ok = result as ServerAddress.ParseResult.Ok
        assertEquals("http://127.0.0.1:8080", ok.address.baseUrl)
        assertTrue(ok.warnings.any { it.contains("回环") })
    }

    @Test
    fun `Docker 服务名给出提示`() {
        val result = ServerAddress.parse("", "minio", "9000", true)
        val ok = result as ServerAddress.ParseResult.Ok
        assertTrue(ok.warnings.any { it.contains("Docker") })
    }
}
