package com.fantasylibrary.android.data.network

import kotlinx.serialization.Serializable

enum class Topic(val label: String) {
    GENERAL("馆内闲谈"), ANIME("追番讨论"), GAME("游戏讨论"), GUIDE("攻略与资料")
}

@Serializable data class PostDto(
    val id: Long, val authorId: Long, val authorName: String, val topic: String,
    val title: String, val body: String, val createdAt: String,
    val likeCount: Long, val commentCount: Long, val viewCount: Long,
    val previewImage: String? = null, val avatarKey: String? = null,
    val workId: Long? = null, val workTitle: String? = null,
)

@Serializable data class PostDetailDto(val post: PostDto, val images: List<String>)
@Serializable data class CommentDto(
    val id: Long, val authorName: String, val body: String, val createdAt: String,
    val avatarKey: String? = null, val replyTo: String? = null,
)
@Serializable data class PostDraft(val title: String, val body: String, val topic: String)
@Serializable data class CommentDraft(val body: String, val parentId: Long? = null)
@Serializable data class CreatedDto(val id: Long)
@Serializable data class ReactionDto(val liked: Boolean, val count: Long)
@Serializable data class WorkEntryDto(val work: WorkDto, val tags: List<TagDto>, val addedAt: String? = null)
@Serializable data class SuggestedDto(val entry: WorkEntryDto, val reason: String)
@Serializable data class HeatDto(val favoriteCount: Long, val viewCount: Long, val commentCount: Long)
@Serializable data class RankedDto(val entry: WorkEntryDto, val heat: HeatDto)
@Serializable data class FeedDto(
    val mode: String, val forYou: List<SuggestedDto>, val popular: List<RankedDto>,
    val newest: List<WorkEntryDto>, val discussions: List<PostDto>,
)

@Serializable data class SavedWorkDto(
    val workId: Long, val workType: String, val title: String, val coverUrl: String? = null, val coverAlt: String? = null,
    val episodeCount: Int? = null, val favorite: Int, val wishlist: Int, val watchStatus: String? = null, val watchedEpisodes: Int,
)
@Serializable data class SavedEntryDto(val item: SavedWorkDto? = null)
@Serializable data class SaveWorkDto(
    val favorite: Boolean, val wishlist: Boolean, val watchStatus: String? = null, val watchedEpisodes: Int = 0,
)
enum class CollectionSelection { FOLLOW, FAVORITE, WISHLIST }
@Serializable data class SelectionDto(val selected: Boolean)
@Serializable data class PlaybackStatsDto(val followers: Long, val plays: Long)
@Serializable data class SavedEpisodeDto(
    val episodeNo: Int, val positionMs: Long, val durationMs: Long, val completed: Boolean, val updatedAt: String,
)
@Serializable data class ViewingProgressDto(val latest: SavedEpisodeDto? = null, val completedEpisodes: Int)
@Serializable data class PlaybackStartDto(val sourceId: Long, val positionMs: Long)
@Serializable data class PlaybackSessionDto(val id: String)
@Serializable data class PlaybackSampleDto(
    val positionMs: Long, val durationMs: Long, val playing: Boolean, val discontinuity: Boolean,
)
@Serializable data class PlaybackSampleResultDto(val counted: Boolean)
@Serializable data class WatchEventDto(
    val id: Long, val workId: Long, val title: String, val coverUrl: String? = null,
    val watchStatus: String, val watchedEpisodes: Int, val episodeCount: Int? = null, val recordedAt: String,
)
@Serializable data class NoticeDto(
    val id: Long, val actorName: String, val postId: Long, val postTitle: String,
    val excerpt: String? = null, val createdAt: String, val read: Boolean, val screeningWorkId: Long? = null,
)
@Serializable data class InboxDto(val page: PageDto<NoticeDto>, val unread: Long)
@Serializable data class DownloadNodeDto(val id: Long, val label: String, val platform: String? = null, val url: String)

@Serializable data class ScreeningCommentDto(
    val id: Long, val rootId: Long? = null, val parentId: Long? = null, val replyTo: String? = null,
    val authorName: String? = null, val avatarKey: String? = null, val body: String? = null,
    val createdAt: String, val removed: Boolean, val replyCount: Long, val likeCount: Long, val liked: Boolean,
)
@Serializable data class ScreeningThreadDto(val comment: ScreeningCommentDto, val replies: List<ScreeningCommentDto>)
@Serializable data class CommentLikeDto(val liked: Boolean)

@Serializable data class ProfileEditDto(val displayName: String, val bio: String)
