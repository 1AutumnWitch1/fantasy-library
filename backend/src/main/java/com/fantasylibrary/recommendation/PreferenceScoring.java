package com.fantasylibrary.recommendation;

import com.fantasylibrary.account.AccountModels.SavedWork;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.persistence.mapper.CatalogMapper.WorkTag;
import com.fantasylibrary.persistence.mapper.RecommendationMapper.CandidateTrait;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PreferenceScoring {
    private static final int TAG_MULTIPLIER = 4;

    private PreferenceScoring() {
    }

    public static boolean hasHistory(List<SavedWork> saved) {
        return saved.stream().anyMatch(item -> weight(item) > 0);
    }

    public static List<Match> top(List<SavedWork> saved, List<WorkTag> savedTags,
                                  List<CandidateTrait> traits, int limit) {
        Map<Long, SavedWork> byId = new HashMap<>();
        Map<WorkType, Signal> types = new EnumMap<>(WorkType.class);
        for (var item : saved) {
            byId.put(item.workId(), item);
            int weight = weight(item);
            if (weight > 0) types.merge(item.workType(), new Signal(weight, weight, item, ""), Signal::combine);
        }

        Map<Long, Signal> tags = new HashMap<>();
        for (var tag : savedTags) {
            var source = byId.get(tag.workId());
            if (source == null) continue;
            int weight = weight(source);
            if (weight > 0) tags.merge(tag.id(), new Signal(weight, weight, source, tag.name()), Signal::combine);
        }

        Map<Long, List<CandidateTrait>> candidates = new HashMap<>();
        for (var trait : traits) {
            if (!byId.containsKey(trait.workId())) {
                candidates.computeIfAbsent(trait.workId(), ignored -> new ArrayList<>()).add(trait);
            }
        }
        return candidates.values().stream()
                .map(rows -> match(rows, types, tags))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingLong(Match::score).thenComparingLong(Match::workId).reversed())
                .limit(limit)
                .toList();
    }

    private static Match match(List<CandidateTrait> traits, Map<WorkType, Signal> types, Map<Long, Signal> tags) {
        var first = traits.getFirst();
        var typeSignal = types.get(first.type());
        long score = typeSignal == null ? 0 : typeSignal.total();
        Signal strongest = null;
        for (var trait : traits) {
            var tag = trait.tagId() == null ? null : tags.get(trait.tagId());
            if (tag == null) continue;
            score += TAG_MULTIPLIER * tag.total();
            if (strongest == null || tag.total() > strongest.total()) strongest = tag;
        }
        String reason = strongest != null
                ? "和你" + interaction(strongest.source()) + "的《" + strongest.source().title() + "》同属「" + strongest.label() + "」"
                : typeSignal != null
                    ? "与你" + interaction(typeSignal.source()) + "的《" + typeSignal.source().title() + "》同为"
                        + (first.type() == WorkType.ANIME ? "番剧" : "游戏")
                    : "";
        return new Match(first.workId(), score, reason);
    }

    private static int weight(SavedWork item) {
        if (item.watchStatus() == null) return 5 * item.favorite() + 2 * item.wishlist();
        return switch (item.watchStatus()) {
            case PLAN_TO_WATCH -> 2;
            case WATCHING -> item.watchedEpisodes() > 0 ? 5 : 4;
            case COMPLETED -> 5;
            case ON_HOLD -> 0;
        };
    }

    private static String interaction(SavedWork item) {
        if (item.watchStatus() == null) return item.favorite() == 1 ? "收藏" : "加入愿望单";
        return switch (item.watchStatus()) {
            case PLAN_TO_WATCH -> "想看";
            case WATCHING -> "正在看";
            case COMPLETED -> "已经看完";
            case ON_HOLD -> "暂时搁置";
        };
    }

    private record Signal(long total, int sourceWeight, SavedWork source, String label) {
        Signal combine(Signal other) {
            var best = sourceWeight > other.sourceWeight ||
                    (sourceWeight == other.sourceWeight && source.workId() < other.source.workId()) ? this : other;
            return new Signal(total + other.total, best.sourceWeight(), best.source(), best.label());
        }
    }

    public record Match(long workId, long score, String reason) {
    }
}
