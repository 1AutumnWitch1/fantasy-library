package com.fantasylibrary.recommendation;

import com.fantasylibrary.account.AccountModels.SavedWork;
import com.fantasylibrary.account.AccountModels.WatchStatus;
import com.fantasylibrary.persistence.mapper.CatalogMapper.WorkTag;
import com.fantasylibrary.persistence.mapper.RecommendationMapper.CandidateTrait;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.fantasylibrary.catalog.model.Catalog.WorkType.ANIME;
import static com.fantasylibrary.catalog.model.Catalog.WorkType.GAME;
import static org.assertj.core.api.Assertions.assertThat;

class PreferenceScoringTest {
    @Test
    void ranksSharedTagsAheadOfTypeOnlyAndExplainsWithTheStrongestWatchSignal() {
        var history = List.of(anime(1, "已看作品", WatchStatus.COMPLETED, 12),
                anime(2, "想看作品", WatchStatus.PLAN_TO_WATCH, 0),
                anime(3, "搁置作品", WatchStatus.ON_HOLD, 8));
        var tags = List.of(new WorkTag(1, 10, "fantasy", "奇幻"), new WorkTag(2, 10, "fantasy", "奇幻"),
                new WorkTag(3, 20, "sports", "运动"));
        var candidates = List.of(new CandidateTrait(1, ANIME, 10L), new CandidateTrait(4, ANIME, null),
                new CandidateTrait(5, GAME, 10L), new CandidateTrait(6, GAME, 20L));
        var ranked = PreferenceScoring.top(history, tags, candidates, 5);

        assertThat(ranked).extracting(PreferenceScoring.Match::workId).containsExactly(5L, 4L);
        assertThat(ranked.getFirst().score()).isEqualTo(28L);
        assertThat(ranked.getFirst().reason()).contains("已经看完", "已看作品", "奇幻");
        assertThat(ranked.getLast().reason()).contains("同为番剧");
    }

    @Test
    void usesWishlistAndWatchingProgressAndIgnoresPausedOnlyHistory() {
        var game = new SavedWork(1, GAME, "愿望作品", null, null, null, 0, 1, null, 0);
        var watching = anime(2, "追番作品", WatchStatus.WATCHING, 1);
        var paused = anime(3, "搁置作品", WatchStatus.ON_HOLD, 5);
        assertThat(PreferenceScoring.hasHistory(List.of(paused))).isFalse();
        assertThat(PreferenceScoring.hasHistory(List.of(game))).isTrue();
        var ranked = PreferenceScoring.top(List.of(game, watching, paused), List.of(),
                List.of(new CandidateTrait(4, GAME, null), new CandidateTrait(5, ANIME, null)), 2);
        assertThat(ranked).extracting(PreferenceScoring.Match::score).containsExactly(5L, 2L);
        assertThat(ranked.getLast().reason()).contains("加入愿望单");
    }

    private static SavedWork anime(long id, String name, WatchStatus status, int watched) {
        return new SavedWork(id, ANIME, name, null, null, 12, 1, 0, status, watched);
    }
}
