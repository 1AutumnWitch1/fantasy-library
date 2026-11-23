package com.fantasylibrary.android

import com.fantasylibrary.android.data.network.FailureKind
import com.fantasylibrary.android.data.network.Failures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class FailuresTest {

    @Test
    fun `404 被归类为接口不存在`() {
        val failure = Failures.fromHttp(404, """{"code":"ROUTE_NOT_FOUND","message":"请求地址不存在"}""")
        assertEquals(FailureKind.NOT_FOUND, failure.kind)
        assertEquals("ROUTE_NOT_FOUND", failure.code)
        assertEquals(404, failure.httpStatus)
    }

    @Test
    fun `401 且 INVALID_CREDENTIALS 被归类为认证失败`() {
        val failure = Failures.fromHttp(401, """{"code":"INVALID_CREDENTIALS","message":"账号名或密码不正确"}""")
        assertEquals(FailureKind.UNAUTHORIZED, failure.kind)
        assertEquals("用户名或密码错误", failure.message)
    }

    @Test
    fun `401 LOGIN_REQUIRED 有明确提示`() {
        val failure = Failures.fromHttp(401, """{"code":"LOGIN_REQUIRED","message":"请先登录"}""")
        assertEquals(FailureKind.UNAUTHORIZED, failure.kind)
        assertEquals("需要登录后才能访问该接口", failure.message)
    }

    @Test
    fun `503 数据库不可用被单独归类`() {
        val failure = Failures.fromHttp(503, """{"code":"DATABASE_UNAVAILABLE","message":"馆藏数据库暂时不可用"}""")
        assertEquals(FailureKind.DATABASE, failure.kind)
        assertTrue(failure.retryable)
    }

    @Test
    fun `503 MinIO 不可用被归类为媒体存储`() {
        val failure = Failures.fromHttp(503, """{"code":"MEDIA_STORAGE_UNAVAILABLE","message":"存储不可用"}""")
        assertEquals(FailureKind.MEDIA_STORAGE, failure.kind)
    }

    @Test
    fun `403 被归类为请求被拒绝`() {
        val failure = Failures.fromHttp(403, """{"code":"FORBIDDEN","message":"请求未通过安全校验，请重试"}""")
        assertEquals(FailureKind.FORBIDDEN, failure.kind)
    }

    @Test
    fun `无法解析主机`() {
        assertEquals(FailureKind.DNS, Failures.fromThrowable(UnknownHostException("nope")).kind)
    }

    @Test
    fun `超时`() {
        assertEquals(FailureKind.TIMEOUT, Failures.fromThrowable(SocketTimeoutException("timeout")).kind)
    }

    @Test
    fun `连接被拒绝`() {
        assertEquals(FailureKind.CONNECT, Failures.fromThrowable(ConnectException("refused")).kind)
    }

    @Test
    fun `HTML 错误页不会让解析崩溃`() {
        val failure = Failures.fromHttp(500, "<html><body>500</body></html>")
        assertEquals(FailureKind.SERVER_ERROR, failure.kind)
        assertNotNull(failure.message)
    }

    @Test
    fun `空响应体也能给出可读信息`() {
        val failure = Failures.fromHttp(502, null)
        assertEquals(FailureKind.SERVER_ERROR, failure.kind)
        assertTrue(failure.message.contains("502"))
    }
}
