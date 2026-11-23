package com.fantasylibrary.admin;

import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.community.CommunityModels.Topic;
import jakarta.validation.constraints.NotNull;

public final class LibraryAdminModels {
    private LibraryAdminModels() {}

    public enum NodeKind { PLAYBACK, DOWNLOAD }

    public record Publication(@NotNull Boolean listed) {}
    public record NodePublication(@NotNull Boolean listed, boolean confirmRemoval) {}
    public enum ResourceChange { PUBLISHED, REMOVED, SHARED_FILE_RETAINED, CLEANUP_PENDING }
    public record ForumStatus(@NotNull Boolean open) {}

    public record Work(long id, String title, WorkType type, boolean listed, String slug, String coverUrl) {}
    public record Node(long id, NodeKind kind, String label, String context, boolean listed) {}
    public record Post(long id, String title, String author, Topic topic, boolean listed, String createdAt) {}
    public enum CommentScope { SCREENING, COMMUNITY }
    public record Comment(long id, long postId, String postTitle, CommentScope scope, Long workId, String workTitle,
                          String author, String body, String createdAt) {}
    public record Member(long id, String username, String displayName, String role) {}
    public record MemberLock(String role, String avatarKey) {}
}
