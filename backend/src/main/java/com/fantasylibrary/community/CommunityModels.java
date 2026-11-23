package com.fantasylibrary.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class CommunityModels {
    private CommunityModels() {}
    public enum Topic { GENERAL, ANIME, GAME, GUIDE }
    public enum PostOrder { RECENT, HOT }
    public enum Target { POST, COMMENT }
    public enum Visibility { VISIBLE, HIDDEN, DELETED }
    public enum Action { HIDE, DELETE, DISMISS }
    public enum ReportState { OPEN, DISMISSED, HIDDEN, DELETED }

    public record PostForm(
            @NotBlank @Size(max = 100, message = "标题最多 100 个字符") String title,
            @NotBlank @Size(max = 6000, message = "正文最多 6000 个字符") String body,
            @NotNull Topic topic,
            @Positive Long workId,
            @Size(max = 4) List<@NotNull @Pattern(regexp = "[0-9a-f-]{36}\\.(jpg|png)") String> keepImages) {
    }
    public record CommentForm(@NotBlank @Size(max = 900, message = "评论最多 900 个字符") String body,
                              @Positive Long parentId) {}
    public record ReportForm(@NotNull Target targetType, @NotNull @Positive Long targetId,
                             @NotBlank @Size(max = 240, message = "举报理由最多 240 个字符") String reason) {}
    public record ReviewForm(@NotNull Action action) {}

    public record Post(long id, long authorId, String authorName, Long workId, String workTitle, Topic topic,
                       String title, String body, String createdAt, String updatedAt, long likeCount, long commentCount,
                       long viewCount, String previewImage, String avatarKey) {}
    public record PostDetail(Post post, List<String> images) {}
    public record Comment(long id, long postId, Long parentId, String replyTo, long authorId, String authorName,
                          String body, String createdAt, String avatarKey) {}
    public record ScreeningComment(long id, Long rootId, Long parentId, String replyTo, String authorName,
                                   String avatarKey, String body, String createdAt, boolean removed,
                                   long replyCount, long likeCount, boolean liked) {}
    public record ScreeningThread(ScreeningComment comment, List<ScreeningComment> replies) {}
    public record CommentLike(@NotNull Boolean liked) {}
    public record Row(long id, long authorId, Visibility state, boolean screeningThread) {}
    public record ReplyRow(long postId, Visibility state, Long parentId, Long rootId) {}
    public record ReportRow(long id, Target targetType, long targetId, ReportState state) {}
    public record Report(long id, Target targetType, long targetId, long postId, String content,
                         String reporter, String reason, String createdAt) {}
    public record Created(long id) {}
    public record Reaction(boolean liked, long count) {}
}
