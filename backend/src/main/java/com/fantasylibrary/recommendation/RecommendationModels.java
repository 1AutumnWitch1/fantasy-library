package com.fantasylibrary.recommendation;

import com.fantasylibrary.catalog.model.Catalog.Tag;
import com.fantasylibrary.catalog.model.Catalog.Work;
import com.fantasylibrary.community.CommunityModels.Post;

import java.util.List;

public final class RecommendationModels {
    private RecommendationModels() {
    }

    public enum Mode { PERSONALIZED, POPULAR, EDITORIAL, EMPTY }

    public record Heat(long workId, long score, long favoriteCount, long wishlistCount,
                       long commentCount, long likeCount, long viewCount) {
    }

    public record WorkEntry(Work work, List<Tag> tags, String addedAt) {
    }

    public record Recommendation(WorkEntry entry, String reason) {
    }

    public record RankedWork(WorkEntry entry, Heat heat) {
    }

    public record Feed(Mode mode, List<Recommendation> forYou, List<RankedWork> popular,
                       List<WorkEntry> newest, List<Post> discussions) {
    }
}
