package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountModels.SavedWork;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.persistence.mapper.CommunityMapper;
import com.fantasylibrary.persistence.mapper.RecommendationMapper;
import com.fantasylibrary.recommendation.PreferenceScoring;
import com.fantasylibrary.recommendation.PreferenceScoring.Match;
import com.fantasylibrary.recommendation.RecommendationModels.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private static final int PICKS = 4;
    private static final int RANKS = 5;

    private final ObjectProvider<RecommendationMapper> recommendations;
    private final ObjectProvider<CommunityMapper> forums;
    private final CatalogService catalog;
    private final CollectionService collections;

    public RecommendationService(ObjectProvider<RecommendationMapper> recommendations, ObjectProvider<CommunityMapper> forums,
                                 CatalogService catalog, CollectionService collections) {
        this.recommendations = recommendations;
        this.forums = forums;
        this.catalog = catalog;
        this.collections = collections;
    }

    public Feed feed(WorkType type, Long userId) {
        var mapper = recommendations.getIfAvailable();
        var community = forums.getIfAvailable();
        if (mapper == null || community == null) {
            throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        }

        var popular = mapper.popular(type, null, RANKS);
        var newest = mapper.newest(type, null, PICKS);
        var saved = userId == null ? List.<SavedWork>of() : collections.list(userId);
        List<Match> matches = List.of();
        if (PreferenceScoring.hasHistory(saved)) {
            matches = PreferenceScoring.top(saved, mapper.savedTags(userId), mapper.candidateTraits(type, userId), PICKS);
        }

        Mode mode;
        if (!matches.isEmpty()) {
            mode = Mode.PERSONALIZED;
        } else {
            var fallbackPopular = saved.isEmpty() ? popular : mapper.popular(type, userId, PICKS);
            if (!fallbackPopular.isEmpty()) {
                mode = Mode.POPULAR;
                matches = fallbackPopular.stream().limit(PICKS)
                        .map(heat -> new Match(heat.workId(), heat.score(), "上榜作品，按收藏、愿望单及讨论互动排序"))
                        .toList();
            } else {
                var editorial = mapper.editorial(type, userId, PICKS);
                mode = editorial.isEmpty() ? Mode.EMPTY : Mode.EDITORIAL;
                matches = editorial.stream().map(id -> new Match(id, 0, "由馆员选入荐读目录")).toList();
            }
        }

        var ids = new LinkedHashSet<Long>();
        matches.forEach(match -> ids.add(match.workId()));
        popular.forEach(heat -> ids.add(heat.workId()));
        ids.addAll(newest);
        var works = catalog.summaries(List.copyOf(ids));
        var dates = ids.isEmpty() ? List.<RecommendationMapper.Intake>of() : mapper.intakeDates(List.copyOf(ids));
        var entries = dates.stream().filter(date -> works.containsKey(date.workId()))
                .collect(Collectors.toMap(RecommendationMapper.Intake::workId, date -> {
                    var work = works.get(date.workId());
                    return new WorkEntry(work.work(), work.tags(), date.addedAt());
                }));

        var picks = matches.stream().filter(match -> entries.containsKey(match.workId()))
                .map(match -> new Recommendation(entries.get(match.workId()), match.reason())).toList();
        var ranking = popular.stream().filter(heat -> entries.containsKey(heat.workId()))
                .map(heat -> new RankedWork(entries.get(heat.workId()), heat)).toList();
        var recent = newest.stream().map(entries::get).filter(Objects::nonNull).toList();
        return new Feed(picks.isEmpty() ? Mode.EMPTY : mode, picks, ranking, recent,
                community.activePosts(type, PICKS));
    }
}
