package com.fantasylibrary.android.data.network

/**
 * 媒体地址解析与安全检查（纯 Kotlin，可在 JVM 单元测试中直接验证）。
 *
 * 后端返回的封面/视频地址有两种形态：
 *   1. 站内相对路径，例如 `/api/media/catalog/uploads/xxx.jpg`、`/api/media/playback/xxx.mp4`
 *      —— 必须拼到用户配置的服务器地址上；
 *   2. 绝对地址（例如 MinIO 预签名 URL），带 query 参数，必须原样使用，不能重写。
 *
 * 同时拦截三类“在 Android 上一定打不开”的地址：
 *   - 回环地址（localhost / 127.0.0.1 / ::1）：在手机上指向手机自身；
 *   - Docker Compose 内部服务名（minio / oracle / backend / host.docker.internal 等）：
 *     只在 compose 网络内可解析；
 *   - Release 构建中的明文 HTTP 地址。
 */
object MediaUrls {

    enum class Verdict {
        /** 相对路径，已拼接到服务器地址 */
        RESOLVED_RELATIVE,

        /** 绝对地址，原样使用 */
        ABSOLUTE,

        /** MinIO / S3 预签名地址，原样使用 */
        PRESIGNED,

        /** 被拒绝 */
        REJECTED,
    }

    data class Check(
        val raw: String?,
        val resolved: String?,
        val verdict: Verdict,
        val reason: String? = null,
        val isPresigned: Boolean = false,
    ) {
        val ok: Boolean get() = verdict != Verdict.REJECTED && resolved != null
    }

    private val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "0.0.0.0", "::1", "[::1]", "127.0.0.1.nip.io")

    private val DOCKER_INTERNAL_HOSTS = setOf(
        "minio",
        "oracle",
        "backend",
        "frontend",
        "admin-frontend",
        "minio-init",
        "host.docker.internal",
        "gateway.docker.internal",
        "docker.for.win.localhost",
        "docker.for.mac.localhost",
    )

    private val PRESIGN_KEYS = listOf(
        "X-Amz-Signature",
        "X-Amz-Credential",
        "X-Goog-Signature",
        "Signature=",
        "AWSAccessKeyId",
    )

    /** 判断一个地址是否是预签名地址（仅用于 UI 展示，不参与校验决策）。 */
    fun isPresigned(url: String): Boolean = PRESIGN_KEYS.any { url.contains(it, ignoreCase = true) }

    /**
     * @param raw     后端返回的原始地址（可能为 null）
     * @param baseUrl 用户配置的服务器地址，例如 http://10.0.2.2:8080
     * @param allowCleartext 是否允许 http://（仅 Debug 为 true）
     */
    fun resolve(raw: String?, baseUrl: String, allowCleartext: Boolean): Check {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return Check(raw, null, Verdict.REJECTED, "接口没有返回媒体地址")
        }

        // 协议相对地址（//host/path）按当前服务器协议补全，但同样要做主机检查
        val normalized = when {
            trimmed.startsWith("//") -> {
                val scheme = schemeOf(baseUrl) ?: "http"
                "$scheme:$trimmed"
            }
            else -> trimmed
        }

        val isAbsolute = normalized.startsWith("http://", true) || normalized.startsWith("https://", true)

        if (!isAbsolute) {
            if (!normalized.startsWith("/")) {
                return Check(raw, null, Verdict.REJECTED, "无法识别的相对地址（不是以 / 开头）")
            }
            val origin = originOf(baseUrl)
                ?: return Check(raw, null, Verdict.REJECTED, "尚未配置有效的服务器地址，无法拼接相对路径")
            if (!allowCleartext && origin.startsWith("http://", true)) {
                return Check(raw, null, Verdict.REJECTED, "Release 版本只允许 HTTPS，已拒绝明文媒体地址")
            }
            return Check(raw, origin + normalized, Verdict.RESOLVED_RELATIVE)
        }

        val scheme = schemeOf(normalized)?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return Check(raw, null, Verdict.REJECTED, "不支持的协议：$scheme")
        }
        if (scheme == "http" && !allowCleartext) {
            return Check(raw, null, Verdict.REJECTED, "Release 版本只允许 HTTPS，已拒绝明文媒体地址")
        }

        val host = hostOf(normalized)
            ?: return Check(raw, null, Verdict.REJECTED, "无法解析地址中的主机名")

        val lowerHost = host.lowercase()
        if (lowerHost in LOOPBACK_HOSTS) {
            return Check(
                raw, null, Verdict.REJECTED,
                "地址指向回环主机 $host，手机 / 模拟器无法访问；应由后端返回可访问地址",
            )
        }
        if (lowerHost in DOCKER_INTERNAL_HOSTS) {
            return Check(
                raw, null, Verdict.REJECTED,
                "地址使用了 Docker 内部服务名 $host，只有容器网络内部才能解析",
            )
        }

        return Check(
            raw = raw,
            resolved = normalized,
            verdict = if (isPresigned(normalized)) Verdict.PRESIGNED else Verdict.ABSOLUTE,
            isPresigned = isPresigned(normalized),
        )
    }

    /** 取 baseUrl 的 scheme://host:port，去掉多余路径与末尾斜杠。 */
    fun originOf(baseUrl: String): String? {
        val value = baseUrl.trim()
        if (value.isEmpty()) return null
        val scheme = schemeOf(value) ?: return null
        if (scheme != "http" && scheme != "https") return null
        val withoutScheme = value.substringAfter("://")
        val authority = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        if (authority.isEmpty()) return null
        return "$scheme://$authority"
    }

    fun schemeOf(url: String): String? {
        val idx = url.indexOf("://")
        if (idx <= 0) return null
        val scheme = url.substring(0, idx)
        return if (scheme.all { it.isLetter() || it.isDigit() || it == '+' || it == '-' || it == '.' }) scheme else null
    }

    /** 取主机名（去掉 ipv6 方括号、端口、userInfo）。 */
    fun hostOf(url: String): String? {
        val idx = url.indexOf("://")
        if (idx <= 0) return null
        var authority = url.substring(idx + 3).substringBefore('/').substringBefore('?').substringBefore('#')
        authority = authority.substringAfterLast('@')
        if (authority.startsWith("[")) {
            val end = authority.indexOf(']')
            return if (end > 0) authority.substring(0, end + 1) else null
        }
        val host = authority.substringBefore(':')
        return host.ifEmpty { null }
    }

    fun portOf(url: String, default: Int = 8080): Int {
        val idx = url.indexOf("://")
        if (idx <= 0) return default
        var authority = url.substring(idx + 3).substringBefore('/').substringBefore('?').substringBefore('#')
        authority = authority.substringAfterLast('@')
        if (authority.startsWith("[")) {
            val end = authority.indexOf(']')
            if (end > 0 && end + 1 < authority.length && authority[end + 1] == ':') {
                return authority.substring(end + 2).toIntOrNull() ?: default
            }
            return default
        }
        if (!authority.contains(':')) return default
        return authority.substringAfterLast(':').toIntOrNull() ?: default
    }

    /** 显示用：把过长的预签名 query 截断，避免撑破界面。 */
    fun displayUrl(url: String?, maxLength: Int = 96): String {
        val value = url?.trim().orEmpty()
        if (value.isEmpty()) return "—"
        if (value.length <= maxLength) return value
        val cut = value.indexOf('?')
        return if (cut in 1 until maxLength) {
            value.substring(0, cut) + "?…（预签名参数已折叠）"
        } else {
            value.take(maxLength) + "…"
        }
    }
}
