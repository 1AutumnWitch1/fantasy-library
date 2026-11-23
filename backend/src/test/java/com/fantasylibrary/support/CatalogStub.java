package com.fantasylibrary.support;

import com.fantasylibrary.catalog.model.Catalog.AiringStatus;
import com.fantasylibrary.catalog.model.Catalog.Category;
import com.fantasylibrary.catalog.model.Catalog.Image;
import com.fantasylibrary.catalog.model.Catalog.ImageKind;
import com.fantasylibrary.catalog.model.Catalog.Tag;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogQuery;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

public class CatalogStub implements CatalogMapper {

    private static final Category FANTASY = new Category(1, "fantasy", "奇幻", 10);
    private static final String COVER = "/api/media/catalog/mist-harbor.webp";

    private final List<WorkRow> anime = List.of(
            row(11, WorkType.ANIME, "mist-harbor", "雾港来信", COVER),
            row(12, WorkType.ANIME, "star-court", "星庭遗录", null));
    private final List<WorkRow> games = List.of(
            row(21, WorkType.GAME, "ash-vigil", "余火守夜人", null),
            row(22, WorkType.GAME, "tide-ruins", "空潮遗迹", null));
    private final List<WorkPlatform> platforms = List.of(
            new WorkPlatform(21, 1, "pc", "PC"),
            new WorkPlatform(21, 2, "ps5", "PS5"),
            new WorkPlatform(22, 3, "switch", "Switch"));

    public List<Long> lastPlatformIds = List.of();
    public int pageQueries;

    private static WorkRow row(long id, WorkType type, String slug, String title, String cover) {
        boolean isAnime = type == WorkType.ANIME;
        return new WorkRow(id, type, slug, title, "示例简介", Date.valueOf("2026-06-21"),
                FANTASY.id(), FANTASY.slug(), FANTASY.name(), FANTASY.sortOrder(),
                cover == null ? null : id + 100, cover, cover == null ? null : "封面",
                isAnime ? 12 : null, isAnime ? "作画室" : null, isAnime ? AiringStatus.COMPLETED : null,
                isAnime ? null : "游戏工坊", isAnime ? null : "16+");
    }

    @Override
    public List<Category> findCategories() {
        return List.of(FANTASY);
    }

    @Override
    public long countWorks(WorkType type, CatalogQuery query) {
        return matching(type, query).count();
    }

    @Override
    public List<WorkRow> findWorks(WorkType type, CatalogQuery query) {
        pageQueries++;
        return matching(type, query).skip(query.offset()).limit(query.size()).toList();
    }

    private Stream<WorkRow> matching(WorkType type, CatalogQuery query) {
        if (query.category() != null && !FANTASY.slug().equals(query.category())) {
            return Stream.empty();
        }
        var selectedTags = query.filterTags();
        return (type == WorkType.ANIME ? anime : games).stream()
                .filter(row -> findTags(row.id()).stream().map(Tag::slug).toList().containsAll(selectedTags))
                .filter(row -> query.keyword() == null || row.title().contains(query.keyword()));
    }

    @Override
    public WorkRow findWork(long id) {
        if (id == 503) {
            throw new CannotGetJdbcConnectionException("private connection info");
        }
        if (id == 942) {
            var codedFailure = new SQLException("private table", "42000", 942, new SQLException("driver diagnostics"));
            throw new BadSqlGrammarException("catalog", "private SQL", codedFailure);
        }
        if (id == 500) {
            throw new IllegalStateException("private failure detail");
        }
        return Stream.concat(anime.stream(), games.stream()).filter(row -> row.id() == id).findFirst().orElse(null);
    }

    @Override
    public List<WorkRow> findWorksByIds(List<Long> ids) {
        return Stream.concat(anime.stream(), games.stream()).filter(row -> ids.contains(row.id())).toList();
    }

    @Override
    public List<Tag> availableTags(WorkType type) {
        return type == WorkType.ANIME ? findTags(11) : List.of();
    }

    @Override
    public List<WorkTag> findWorkTags(List<Long> ids) {
        return ids.stream().flatMap(id -> findTags(id).stream()
                .map(tag -> new WorkTag(id, tag.id(), tag.slug(), tag.name()))).toList();
    }

    @Override
    public List<Tag> findTags(long workId) {
        var mystery = new Tag(1, "mystery", "悬疑");
        if (workId == 11) return List.of(mystery, new Tag(2, "detective", "推理"));
        return workId == 12 ? List.of(mystery) : List.of();
    }

    @Override
    public List<Image> findImages(long workId) {
        return workId == 11 ? List.of(new Image(111, ImageKind.COVER, COVER, "封面", 0)) : List.of();
    }

    @Override
    public List<WorkPlatform> findPlatforms(List<Long> workIds) {
        lastPlatformIds = List.copyOf(workIds);
        return platforms.stream().filter(row -> workIds.contains(row.workId())).toList();
    }
}
