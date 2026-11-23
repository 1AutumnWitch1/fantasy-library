package com.fantasylibrary.service;

import com.fantasylibrary.catalog.model.Catalog.Platform;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.catalog.model.CatalogQuery;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import com.fantasylibrary.support.CatalogStub;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogServiceTest {

    private final CatalogStub mapper = new CatalogStub();
    private final CatalogService service = service(Map.of("catalogMapper", mapper));

    @Test
    void computesPageMetadataAndSkipsFetchingOutOfRangePages() {
        var page = service.anime(new CatalogQuery(2, 1, "fantasy", null, "  ", null));
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().work().title()).isEqualTo("星庭遗录");
        assertThat(page.items().getFirst().work().releaseDate()).isEqualTo(LocalDate.of(2026, 6, 21));
        assertThat(page.items().getFirst().anime().episodeCount()).isEqualTo(12);

        var outOfRange = service.anime(new CatalogQuery(Integer.MAX_VALUE, 100, null, null, null, null));
        assertThat(outOfRange.items()).isEmpty();
        assertThat(outOfRange.total()).isEqualTo(2);
        assertThat(outOfRange.totalPages()).isEqualTo(1);
        assertThat(mapper.pageQueries).isEqualTo(1);
    }

    @Test
    void loadsPlatformsInOneBatchAndKeepsThemWithTheirGames() {
        var page = service.games(new CatalogQuery(null, null, null, null, null, null));
        assertThat(mapper.lastPlatformIds).containsExactly(21L, 22L);
        assertThat(page.items().get(0).game().platforms()).extracting(Platform::slug).containsExactly("pc", "ps5");
        assertThat(page.items().get(1).game().platforms()).extracting(Platform::slug).containsExactly("switch");
    }

    @Test
    void assemblesOnlyTheMatchingDetailVariantAndItsRelatedData() {
        var anime = service.detail(11);
        assertThat(anime.anime().studio()).isEqualTo("作画室");
        assertThat(anime.game()).isNull();
        assertThat(anime.tags()).extracting(tag -> tag.slug()).containsExactly("mystery", "detective");
        assertThat(anime.images()).containsExactly(anime.work().cover());
        assertThat(mapper.lastPlatformIds).isEmpty();

        var game = service.detail(21);
        assertThat(game.anime()).isNull();
        assertThat(game.game().developer()).isEqualTo("游戏工坊");
        assertThat(game.game().platforms()).hasSize(2);
    }

    @Test
    void reportsMissingWorkAndUnconfiguredDatabase() {
        assertThatThrownBy(() -> service.detail(999))
                .isInstanceOfSatisfying(CatalogProblem.class,
                        problem -> assertThat(problem.reason()).isEqualTo(CatalogProblem.Reason.WORK_NOT_FOUND));
        assertThatThrownBy(service(Map.of())::categories)
                .isInstanceOfSatisfying(CatalogProblem.class,
                        problem -> assertThat(problem.reason()).isEqualTo(CatalogProblem.Reason.DATABASE_UNAVAILABLE));
    }

    private static CatalogService service(Map<String, Object> beans) {
        return new CatalogService(new StaticListableBeanFactory(beans).getBeanProvider(CatalogMapper.class));
    }
}
