package com.fantasylibrary.android.data.repository

import com.fantasylibrary.android.data.network.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.serialization.json.Json

class ReaderRepository(private val client: ApiClient, private val auth: AuthRepository) {
    suspend fun recommendations(base: String, signedIn: Boolean = false, type: String? = null) = callApi("读取推荐") {
        val api = client.api(base)
        if (signedIn) api.memberFeed(type) else api.publicFeed(type)
    }

    suspend fun posts(base: String, page: Int, topic: Topic?, hot: Boolean, workId: Long?) = callApi("读取帖子") {
        client.api(base).posts(page, topic?.name, if (hot) "HOT" else "RECENT", workId)
    }

    suspend fun post(base: String, id: Long, countView: Boolean) = callApi("读取帖子") {
        client.api(base).post(id, countView)
    }

    suspend fun comments(base: String, id: Long, page: Int) = callApi("读取评论") { client.api(base).comments(id, page) }
    suspend fun postReaction(base: String, id: Long) = callApi("读取点赞") { client.api(base).reaction(id) }
    suspend fun react(base: String, id: Long, liked: Boolean) = write(base) { api ->
        callApi("更新点赞") { if (liked) api.like(id) else api.unlike(id) }
    }
    suspend fun comment(base: String, id: Long, draft: CommentDraft) = write(base) { api ->
        callApi("发表评论") { api.comment(id, draft) }
    }
    suspend fun publish(base: String, draft: PostDraft, images: List<MultipartBody.Part>) = write(base) { api ->
        val json = Json.encodeToString(PostDraft.serializer(), draft).toRequestBody("application/json".toMediaType())
        callApi("发表手稿") { api.publish(json, images) }
    }

    suspend fun screeningThreads(base: String, workId: Long, page: Int, member: Boolean) = callApi("读取番剧评论") {
        val api = client.api(base)
        if (member) api.memberScreeningThreads(workId, page) else api.screeningThreads(workId, page)
    }

    suspend fun screeningReplies(base: String, workId: Long, root: Long, page: Int, member: Boolean) = callApi("读取回复") {
        val api = client.api(base)
        if (member) api.memberScreeningReplies(workId, root, page) else api.screeningReplies(workId, root, page)
    }

    suspend fun screeningComment(base: String, workId: Long, draft: CommentDraft) = write(base) { api ->
        callApi("发送番剧评论") { api.addScreeningComment(workId, draft) }
    }

    suspend fun screeningLike(base: String, workId: Long, id: Long, liked: Boolean) = write(base) { api ->
        callApi("更新评论点赞") { api.screeningLike(workId, id, CommentLikeDto(liked)) }
    }

    suspend fun updateProfile(base: String, form: ProfileEditDto) = write(base) { api ->
        callApi("保存个人资料") { api.updateProfile(form) }
    }

    suspend fun collection(base: String) = callApi("读取私人藏书") { client.api(base).collection() }
    suspend fun saved(base: String, id: Long) = callApi("读取收藏状态") { client.api(base).saved(id) }
    suspend fun save(base: String, id: Long, draft: SaveWorkDto) = write(base) { api ->
        callApi("保存收藏") { api.save(id, draft) }
    }
    suspend fun remove(base: String, id: Long) = write(base) { api ->
        callApiUnit("移除收藏") { api.remove(id) }
    }

    suspend fun selection(base: String, id: Long, kind: CollectionSelection, selected: Boolean) = write(base) { api ->
        callApi("更新馆藏记录") { api.selection(id, kind.name, SelectionDto(selected)) }
    }
    suspend fun playbackStats(base: String, id: Long) = callApi("读取播放统计") { client.api(base).playbackStats(id) }
    suspend fun viewingProgress(base: String, id: Long) = callApi("读取观看进度") { client.api(base).viewingProgress(id) }
    suspend fun startPlayback(base: String, id: Long, source: Long, position: Long) = write(base) { api ->
        callApi("开始记录播放") { api.startPlayback(id, PlaybackStartDto(source, position)) }
    }
    suspend fun samplePlayback(base: String, id: Long, session: String, sample: PlaybackSampleDto) = write(base) { api ->
        callApi("保存观看进度") { api.samplePlayback(id, session, sample) }
    }

    suspend fun history(base: String, page: Int) = callApi("读取观看记录") { client.api(base).history(page) }
    suspend fun inbox(base: String, page: Int) = callApi("读取消息") { client.api(base).inbox(page) }
    suspend fun markRead(base: String, id: Long) = write(base) { api ->
        callApiUnit("标记已读") { api.markRead(id) }
    }
    suspend fun downloads(base: String, id: Long) = callApi("读取下载来源") { client.api(base).downloads(id) }

    private suspend fun <T> write(base: String, action: suspend (FantasyApi) -> Outcome<T>): Outcome<T> =
        when (val proof = auth.refreshCsrf(base)) {
            is Outcome.Ok -> action(client.api(base))
            is Outcome.Err -> proof
        }
}
