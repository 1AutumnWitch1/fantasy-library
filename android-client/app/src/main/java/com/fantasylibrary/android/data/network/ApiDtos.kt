package com.fantasylibrary.android.data.network

/**
 * 现有 Spring Boot 接口的 DTO 定义。
 *
 * 字段严格对照后端源码（backend/src/main/java/com/fantasylibrary/...）与真实响应，
 * 不做任何猜测：
 *   - CatalogController      GET /api/anime, /api/games, /api/works/{id}
 *   - PlaybackController     GET /api/works/{id}/episodes
 *   - HealthController       GET /api/health            （非 ApiResponse 包装）
 *   - CatalogMediaController GET /api/media/catalog/... （MinIO 图片，返回二进制）
 *   - PlaybackController     GET /api/media/playback/.. （MinIO 视频，支持 Range）
 *   - AccountController      GET /api/auth/csrf, POST /api/auth/login, /api/auth/logout, /api/me/profile
 *
 * 所有响应字段都给了默认值：测试客户端遇到后端新增字段或可空字段时不会整体解析失败，
 * 但仍会通过 `ApiEnvelope.code` 判断业务是否成功。
 */
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** 统一响应包装：{ code, message, data, errors } */
@Serializable
data class ApiEnvelope<T>(
    val code: String = "",
    val message: String = "",
    val data: T? = null,
    val errors: List<FieldIssueDto> = emptyList(),
) {
    val isOk: Boolean get() = code.equals("OK", ignoreCase = true)
}

@Serializable
data class FieldIssueDto(
    val field: String? = null,
    val message: String? = null,
)

/** GET /api/health → {"application":"UP","database":"UP"}，不套 ApiResponse。 */
@Serializable
data class HealthDto(
    val application: String = "",
    val database: String = "",
)

@Serializable
data class CategoryDto(
    val id: Long = 0,
    val slug: String = "",
    val name: String = "",
    val sortOrder: Int = 0,
)

@Serializable
data class TagDto(
    val id: Long = 0,
    val slug: String = "",
    val name: String = "",
)

@Serializable
data class ImageDto(
    val id: Long = 0,
    val kind: String = "",
    val url: String? = null,
    val altText: String? = null,
    val sortOrder: Int = 0,
)

@Serializable
data class PlatformDto(
    val id: Long = 0,
    val slug: String = "",
    val name: String = "",
)

@Serializable
data class WorkDto(
    val id: Long = 0,
    val type: String = "",
    val slug: String = "",
    val title: String = "",
    val summary: String? = null,
    val releaseDate: String? = null,
    val category: CategoryDto? = null,
    val cover: ImageDto? = null,
)

@Serializable
data class AnimeInfoDto(
    val episodeCount: Int? = null,
    val studio: String? = null,
    val airingStatus: String? = null,
)

@Serializable
data class GameInfoDto(
    val developer: String? = null,
    val ageRating: String? = null,
    val platforms: List<PlatformDto> = emptyList(),
)

@Serializable
data class AnimeItemDto(
    val work: WorkDto = WorkDto(),
    val anime: AnimeInfoDto? = null,
    val tags: List<TagDto> = emptyList(),
)

@Serializable
data class GameItemDto(
    val work: WorkDto = WorkDto(),
    val game: GameInfoDto? = null,
    val tags: List<TagDto> = emptyList(),
)

/** Catalog.Page：{ items, page, size, total, totalPages } */
@Serializable
data class PageDto<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val size: Int = 20,
    val total: Long = 0,
    val totalPages: Long = 0,
)

@Serializable
data class WorkDetailDto(
    val work: WorkDto = WorkDto(),
    val tags: List<TagDto> = emptyList(),
    val images: List<ImageDto> = emptyList(),
    val anime: AnimeInfoDto? = null,
    val game: GameInfoDto? = null,
)

@Serializable
data class SourceDto(
    val id: Long = 0,
    val label: String? = null,
    val quality: String? = null,
    val url: String = "",
    val format: String? = null,
)

@Serializable
data class EpisodeDto(
    val id: Long = 0,
    val edition: String? = null,
    val number: Int = 0,
    val title: String? = null,
    val durationSeconds: Int? = null,
    val sources: List<SourceDto> = emptyList(),
    val nodeId: Long? = null,
    val nodeLabel: String? = null,
)

/** AccountModels.CsrfProof */
@Serializable
data class CsrfProofDto(
    val headerName: String = "X-XSRF-TOKEN",
    val token: String = "",
)

/** AccountModels.Credentials */
@Serializable
data class CredentialsDto(
    val username: String,
    val password: String,
)

/** AccountModels.Profile */
@Serializable
data class ProfileDto(
    val id: Long = 0,
    val username: String = "",
    val displayName: String = "",
    val bio: String? = null,
    val accountRole: String = "",
    val avatarKey: String? = null,
)

/** login / logout 的 data 为 null，用 JsonElement 接收避免强类型麻烦。 */
internal typealias EmptyData = JsonElement
