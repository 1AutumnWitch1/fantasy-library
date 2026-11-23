package com.fantasylibrary.service;

import com.fantasylibrary.catalog.model.Catalog.AnimeInfo;
import com.fantasylibrary.catalog.model.Catalog.AnimeItem;
import com.fantasylibrary.catalog.model.Catalog.Category;
import com.fantasylibrary.catalog.model.Catalog.GameInfo;
import com.fantasylibrary.catalog.model.Catalog.GameItem;
import com.fantasylibrary.catalog.model.Catalog.Image;
import com.fantasylibrary.catalog.model.Catalog.ImageKind;
import com.fantasylibrary.catalog.model.Catalog.Page;
import com.fantasylibrary.catalog.model.Catalog.Platform;
import com.fantasylibrary.catalog.model.Catalog.Work;
import com.fantasylibrary.catalog.model.Catalog.WorkDetail;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.catalog.model.CatalogQuery;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import com.fantasylibrary.persistence.mapper.CatalogMapper.WorkPlatform;
import com.fantasylibrary.persistence.mapper.CatalogMapper.WorkRow;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.fantasylibrary.catalog.model.Catalog.Tag;
import com.fantasylibrary.persistence.mapper.CatalogMapper.WorkTag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final ObjectProvider<CatalogMapper> oracleMapper;

    public CatalogService(ObjectProvider<CatalogMapper> oracleMapper) {
        this.oracleMapper = oracleMapper;
    }

    public List<Category> categories() {
        return mapper().findCategories();
    }

    public List<Tag> availableTags(WorkType type) {
        return mapper().availableTags(type);
    }

    public Page<AnimeItem> anime(CatalogQuery query) {
        CatalogMapper mapper = mapper();
        long total = mapper.countWorks(WorkType.ANIME, query);
        if (query.offset() >= total) return Page.of(List.of(), query.page(), query.size(), total);
        List<WorkRow> rows = mapper.findWorks(WorkType.ANIME, query);
        var tags = tagsFor(mapper, rows.stream().map(WorkRow::id).toList());
        List<AnimeItem> items = rows.stream()
                .map(row -> new AnimeItem(work(row), animeInfo(row), tags.getOrDefault(row.id(), List.of())))
                .toList();
        return Page.of(items, query.page(), query.size(), total);
    }

    public Page<GameItem> games(CatalogQuery query) {
        CatalogMapper mapper = mapper();
        long total = mapper.countWorks(WorkType.GAME, query);
        if (query.offset() >= total) {
            return Page.of(List.of(), query.page(), query.size(), total);
        }
        List<WorkRow> rows = mapper.findWorks(WorkType.GAME, query);
        List<Long> ids = rows.stream().map(WorkRow::id).toList();
        var platforms = platformsFor(mapper, ids);
        var tags = tagsFor(mapper, ids);
        List<GameItem> items = rows.stream()
                .map(row -> new GameItem(work(row), gameInfo(row, platforms), tags.getOrDefault(row.id(), List.of())))
                .toList();
        return Page.of(items, query.page(), query.size(), total);
    }

    public WorkDetail detail(long id) {
        CatalogMapper mapper = mapper();
        WorkRow row = mapper.findWork(id);
        if (row == null) {
            throw new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
        }
        AnimeInfo anime = row.type() == WorkType.ANIME ? animeInfo(row) : null;
        GameInfo game = row.type() == WorkType.GAME ? gameInfo(row, platformsFor(mapper, List.of(id))) : null;
        return new WorkDetail(work(row), mapper.findTags(id), mapper.findImages(id), anime, game);
    }

    public Map<Long, WorkSummary> summaries(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        var mapper = mapper();
        var rows = mapper.findWorksByIds(ids);
        var tags = tagsFor(mapper, rows.stream().map(WorkRow::id).toList());
        return rows.stream().collect(Collectors.toMap(WorkRow::id,
                row -> new WorkSummary(work(row), tags.getOrDefault(row.id(), List.of()))));
    }

    public record WorkSummary(Work work, List<Tag> tags) {
    }

    private CatalogMapper mapper() {
        CatalogMapper mapper = oracleMapper.getIfAvailable();
        if (mapper == null) {
            throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        }
        return mapper;
    }

    private static Work work(WorkRow row) {
        Category category = new Category(row.categoryId(), row.categorySlug(), row.categoryName(), row.categorySortOrder());
        Image cover = row.coverId() == null ? null
                : new Image(row.coverId(), ImageKind.COVER, row.coverUrl(), row.coverAltText(), 0);
        return new Work(row.id(), row.type(), row.slug(), row.title(), row.summary(),
                row.releaseDate() == null ? null : row.releaseDate().toLocalDate(), category, cover);
    }

    private static AnimeInfo animeInfo(WorkRow row) {
        return new AnimeInfo(row.episodeCount(), row.studio(), row.airingStatus());
    }

    private static GameInfo gameInfo(WorkRow row, Map<Long, List<Platform>> platforms) {
        return new GameInfo(row.developer(), row.ageRating(), platforms.getOrDefault(row.id(), List.of()));
    }

    private static Map<Long, List<Tag>> tagsFor(CatalogMapper mapper, List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return mapper.findWorkTags(ids).stream().collect(Collectors.groupingBy(WorkTag::workId,
                Collectors.mapping(row -> new Tag(row.id(), row.slug(), row.name()), Collectors.toUnmodifiableList())));
    }

    private static Map<Long, List<Platform>> platformsFor(CatalogMapper mapper, List<Long> workIds) {
        if (workIds.isEmpty()) {
            return Map.of();
        }
        return mapper.findPlatforms(workIds).stream().collect(Collectors.groupingBy(
                WorkPlatform::workId,
                Collectors.mapping(row -> new Platform(row.id(), row.slug(), row.name()), Collectors.toUnmodifiableList())));
    }
}
