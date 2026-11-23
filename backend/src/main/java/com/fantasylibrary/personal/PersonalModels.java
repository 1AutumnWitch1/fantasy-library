package com.fantasylibrary.personal;

import com.fantasylibrary.account.AccountModels.WatchStatus;
import com.fantasylibrary.catalog.model.Catalog.Page;

public final class PersonalModels {
    private PersonalModels() {
    }

    public enum NoticeKind { POST_COMMENT, COMMENT_REPLY, POST_LIKE }

    public record WatchEvent(long id, long workId, String title, String coverUrl, WatchStatus watchStatus,
                             int watchedEpisodes, Integer episodeCount, String recordedAt) {
    }

    public record Notice(long id, NoticeKind kind, String actorName, String avatarKey, long postId,
                         String postTitle, Long screeningWorkId, String excerpt, String createdAt, boolean read) {
    }

    public record Inbox(Page<Notice> page, long unread) {
    }
}
