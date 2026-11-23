package com.fantasylibrary.android.data.network

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 现有 Spring Boot 用户端接口（与后端 Controller 一一对应，路径不做任何改写）。
 * 注意：本项目不使用 /api/admin/ 段的管理端接口。
 */
interface FantasyApi {

    /** HealthController：返回裸对象，不套 ApiResponse。 */
    @GET("api/health")
    suspend fun health(): HealthDto

    /** CatalogController#anime → ApiResponse<Page<AnimeItem>> */
    @GET("api/anime")
    suspend fun anime(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
    ): ApiEnvelope<PageDto<AnimeItemDto>>

    /** CatalogController#games → ApiResponse<Page<GameItem>> */
    @GET("api/games")
    suspend fun games(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
    ): ApiEnvelope<PageDto<GameItemDto>>

    /** CatalogController#categories */
    @GET("api/categories")
    suspend fun categories(): ApiEnvelope<List<CategoryDto>>

    /** CatalogController#detail */
    @GET("api/works/{id}")
    suspend fun work(@Path("id") id: Long): ApiEnvelope<WorkDetailDto>

    /** PlaybackController#episodes，allNodes=true 可拿到全部节点下的分集 */
    @GET("api/works/{id}/episodes")
    suspend fun episodes(
        @Path("id") id: Long,
        @Query("allNodes") allNodes: Boolean = true,
    ): ApiEnvelope<List<EpisodeDto>>

    /** AccountController#csrf：返回 { headerName, token }，同时下发 XSRF-TOKEN Cookie */
    @GET("api/auth/csrf")
    suspend fun csrf(): ApiEnvelope<CsrfProofDto>

    /** AccountController#login：需要 CSRF 头（由 CsrfInterceptor 自动附加） */
    @POST("api/auth/login")
    suspend fun login(@Body credentials: CredentialsDto): ApiEnvelope<JsonElement>

    /** AccountController#logout：同样需要 CSRF 头 */
    @POST("api/auth/logout")
    suspend fun logout(): ApiEnvelope<JsonElement>

    /** AccountController#profile：受保护接口，依赖会话 Cookie */
    @GET("api/me/profile")
    suspend fun profile(): ApiEnvelope<ProfileDto>
}
