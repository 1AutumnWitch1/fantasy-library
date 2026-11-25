package com.fantasylibrary.android.data.network

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Multipart
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
        @Query("tags") tags: List<String> = emptyList(),
    ): ApiEnvelope<PageDto<AnimeItemDto>>

    /** CatalogController#games → ApiResponse<Page<GameItem>> */
    @GET("api/games")
    suspend fun games(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
        @Query("tags") tags: List<String> = emptyList(),
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

    @PUT("api/me/profile")
    suspend fun updateProfile(@Body form: ProfileEditDto): ApiEnvelope<ProfileDto>

    @GET("api/tags")
    suspend fun tags(@Query("type") type: String): ApiEnvelope<List<TagDto>>

    @GET("api/recommendations")
    suspend fun publicFeed(@Query("type") type: String?): ApiEnvelope<FeedDto>

    @GET("api/me/recommendations")
    suspend fun memberFeed(@Query("type") type: String?): ApiEnvelope<FeedDto>

    @GET("api/community/posts")
    suspend fun posts(@Query("page") page: Int, @Query("topic") topic: String?, @Query("sort") sort: String,
        @Query("workId") workId: Long?, @Query("size") size: Int = 18): ApiEnvelope<PageDto<PostDto>>

    @GET("api/community/posts/{id}")
    suspend fun post(@Path("id") id: Long, @Query("view") countView: Boolean): ApiEnvelope<PostDetailDto>

    @GET("api/community/posts/{id}/comments")
    suspend fun comments(@Path("id") id: Long, @Query("page") page: Int): ApiEnvelope<PageDto<CommentDto>>

    @GET("api/me/community/posts/{id}/like")
    suspend fun reaction(@Path("id") id: Long): ApiEnvelope<ReactionDto>

    @PUT("api/me/community/posts/{id}/like")
    suspend fun like(@Path("id") id: Long): ApiEnvelope<ReactionDto>

    @DELETE("api/me/community/posts/{id}/like")
    suspend fun unlike(@Path("id") id: Long): ApiEnvelope<ReactionDto>

    @POST("api/me/community/posts/{id}/comments")
    suspend fun comment(@Path("id") id: Long, @Body form: CommentDraft): ApiEnvelope<CreatedDto>

    @Multipart
    @POST("api/me/community/posts")
    suspend fun publish(@Part("post") post: okhttp3.RequestBody, @Part images: List<okhttp3.MultipartBody.Part>): ApiEnvelope<CreatedDto>

    @GET("api/works/{id}/comment-threads")
    suspend fun screeningThreads(@Path("id") workId: Long, @Query("page") page: Int): ApiEnvelope<PageDto<ScreeningThreadDto>>

    @GET("api/me/works/{id}/comment-threads")
    suspend fun memberScreeningThreads(@Path("id") workId: Long, @Query("page") page: Int): ApiEnvelope<PageDto<ScreeningThreadDto>>

    @GET("api/works/{id}/comments/{root}/replies")
    suspend fun screeningReplies(@Path("id") workId: Long, @Path("root") root: Long, @Query("page") page: Int): ApiEnvelope<PageDto<ScreeningCommentDto>>

    @GET("api/me/works/{id}/comments/{root}/replies")
    suspend fun memberScreeningReplies(@Path("id") workId: Long, @Path("root") root: Long, @Query("page") page: Int): ApiEnvelope<PageDto<ScreeningCommentDto>>

    @POST("api/me/works/{id}/comments")
    suspend fun addScreeningComment(@Path("id") workId: Long, @Body form: CommentDraft): ApiEnvelope<CreatedDto>

    @PUT("api/me/works/{id}/comments/{comment}/like")
    suspend fun screeningLike(@Path("id") workId: Long, @Path("comment") comment: Long, @Body body: CommentLikeDto): ApiEnvelope<ReactionDto>

    @GET("api/me/collection")
    suspend fun collection(): ApiEnvelope<List<SavedWorkDto>>

    @GET("api/me/collection/{id}")
    suspend fun saved(@Path("id") id: Long): ApiEnvelope<SavedEntryDto>

    @PUT("api/me/collection/{id}")
    suspend fun save(@Path("id") id: Long, @Body form: SaveWorkDto): ApiEnvelope<SavedWorkDto>

    @DELETE("api/me/collection/{id}")
    suspend fun remove(@Path("id") id: Long): ApiEnvelope<JsonElement>

    @GET("api/me/reading-history")
    suspend fun history(@Query("page") page: Int): ApiEnvelope<PageDto<WatchEventDto>>

    @GET("api/me/inbox")
    suspend fun inbox(@Query("page") page: Int): ApiEnvelope<InboxDto>

    @PUT("api/me/inbox/{id}/read")
    suspend fun markRead(@Path("id") id: Long): ApiEnvelope<JsonElement>

    @GET("api/works/{id}/downloads")
    suspend fun downloads(@Path("id") id: Long): ApiEnvelope<List<DownloadNodeDto>>
}
