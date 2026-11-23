package com.fantasylibrary.android.data.network

/**
 * 服务器地址的解析与校验（纯 Kotlin，可在 JVM 单元测试中直接验证）。
 *
 * 允许用户直接粘贴 `http://192.168.1.20:8080`，也允许分开填写主机与端口；
 * 组合结果会即时显示，避免“保存了但实际不是我想要的地址”。
 */
data class ServerAddress(
    val scheme: String,
    val host: String,
    val port: Int,
) {
    /** 不带末尾斜杠，便于拼接 `api/...`。 */
    val baseUrl: String get() = "$scheme://$host:$port"

    val isCleartext: Boolean get() = scheme.equals("http", ignoreCase = true)

    companion object {
        const val DEFAULT_HTTP_PORT = 8080
        const val DEFAULT_HTTPS_PORT = 443

        private val HOST_PATTERN = Regex("^[A-Za-z0-9]([A-Za-z0-9._-]{0,251}[A-Za-z0-9])?$")
        private val IPV6_PATTERN = Regex("^\\[[0-9A-Fa-f:.]+]$")

        /**
         * @param schemeInput "http" / "https"，为空时按 allowCleartext 选择
         * @param hostInput   主机，可包含完整 URL
         * @param portInput   端口，可空
         */
        fun parse(
            schemeInput: String,
            hostInput: String,
            portInput: String,
            allowCleartext: Boolean,
        ): ParseResult {
            val rawHost = hostInput.trim()
            if (rawHost.isEmpty()) {
                return ParseResult.Invalid("请填写服务器地址（IP 或域名）")
            }
            if (rawHost.any { it.isWhitespace() }) {
                return ParseResult.Invalid("服务器地址不能包含空格")
            }

            var scheme = schemeInput.trim().lowercase().ifEmpty {
                if (allowCleartext) "http" else "https"
            }
            var hostPart = rawHost
            var portPart = portInput.trim()

            // 允许直接粘贴完整地址
            if (hostPart.contains("://")) {
                val idx = hostPart.indexOf("://")
                scheme = hostPart.substring(0, idx).lowercase()
                hostPart = hostPart.substring(idx + 3)
            }

            if (scheme != "http" && scheme != "https") {
                return ParseResult.Invalid("协议只能是 http 或 https，当前为：$scheme")
            }

            hostPart = hostPart.substringBefore('/').substringBefore('?').substringBefore('#')

            // 剥离 userInfo
            if (hostPart.contains('@')) {
                return ParseResult.Invalid("服务器地址中不应包含账号密码")
            }

            // 从主机部分拆出端口（IPv6 用方括号）
            if (hostPart.startsWith("[")) {
                val end = hostPart.indexOf(']')
                if (end <= 0) return ParseResult.Invalid("IPv6 地址缺少右方括号")
                val rest = hostPart.substring(end + 1)
                if (rest.startsWith(":")) portPart = rest.substring(1)
                hostPart = hostPart.substring(0, end + 1)
            } else if (hostPart.count { it == ':' } == 1) {
                val maybePort = hostPart.substringAfter(':')
                if (maybePort.all { it.isDigit() }) {
                    hostPart = hostPart.substringBefore(':')
                    if (portPart.isEmpty()) portPart = maybePort
                }
            }

            if (hostPart.isEmpty()) {
                return ParseResult.Invalid("请填写服务器地址（IP 或域名）")
            }

            val isIpv6 = IPV6_PATTERN.matches(hostPart)
            if (!isIpv6 && !HOST_PATTERN.matches(hostPart)) {
                return ParseResult.Invalid("主机名格式不正确：$hostPart（不要带 http://、路径或空格）")
            }

            val port = if (portPart.isEmpty()) {
                if (scheme == "https") DEFAULT_HTTPS_PORT else DEFAULT_HTTP_PORT
            } else {
                portPart.toIntOrNull() ?: return ParseResult.Invalid("端口必须是数字：$portPart")
            }
            if (port !in 1..65535) {
                return ParseResult.Invalid("端口必须在 1–65535 之间，当前为 $port")
            }

            val warnings = buildList {
                if (scheme == "http" && !allowCleartext) {
                    add("Release 构建禁止明文 HTTP，请改用 https://")
                }
                val lowerHost = hostPart.lowercase()
                if (lowerHost in setOf("localhost", "127.0.0.1", "[::1]", "::1", "0.0.0.0")) {
                    add("回环地址在真机上指向手机自身；真机请填电脑的局域网 IP，模拟器可用 10.0.2.2")
                }
                if (lowerHost in setOf("minio", "oracle", "backend", "frontend", "host.docker.internal")) {
                    add("这是 Docker 内部服务名，手机无法解析")
                }
            }

            if (scheme == "http" && !allowCleartext) {
                return ParseResult.Invalid("Release 构建只允许 HTTPS，已拒绝明文地址")
            }

            return ParseResult.Ok(ServerAddress(scheme, hostPart, port), warnings)
        }
    }

    sealed interface ParseResult {
        data class Ok(val address: ServerAddress, val warnings: List<String> = emptyList()) : ParseResult
        data class Invalid(val message: String) : ParseResult
    }
}
