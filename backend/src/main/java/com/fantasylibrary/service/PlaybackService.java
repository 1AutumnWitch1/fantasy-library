package com.fantasylibrary.service;

import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.media.MediaStoreException;
import com.fantasylibrary.media.ObjectMediaStore;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import com.fantasylibrary.persistence.mapper.PlaybackMapper;
import com.fantasylibrary.playback.PlaybackModels.Episode;
import com.fantasylibrary.playback.PlaybackModels.Source;
import com.fantasylibrary.playback.PlaybackModels.SourceRow;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlaybackService {
    private final ObjectProvider<CatalogMapper> catalog;
    private final ObjectProvider<PlaybackMapper> playback;
    private final ObjectMediaStore storage;

    public PlaybackService(ObjectProvider<CatalogMapper> catalog, ObjectProvider<PlaybackMapper> playback,
                           ObjectMediaStore storage) {
        this.catalog = catalog;
        this.playback = playback;
        this.storage = storage;
    }

    public List<Episode> episodes(long workId) {
        return episodes(workId, false);
    }

    public List<Episode> episodes(long workId, boolean allNodes) {
        var works = catalog.getIfAvailable();
        var media = playback.getIfAvailable();
        if (works == null || media == null) throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        var work = works.findWork(workId);
        if (work == null || work.type() != WorkType.ANIME) {
            throw new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
        }

        var sources = media.sources(workId).stream().filter(row -> permitted(row.url())).collect(Collectors.groupingBy(
                SourceRow::episodeId, Collectors.mapping(row ->
                        new Source(row.id(), row.label(), row.quality(), row.url(), row.format()), Collectors.toUnmodifiableList())));

        var episodes = media.episodes(workId).stream().filter(row -> row.nodeListed())
                .map(row -> new Episode(row.id(), row.edition(), row.number(), row.title(), row.durationSeconds(),
                        sources.getOrDefault(row.id(), List.of()), row.nodeId(), row.nodeLabel())).toList();
        if (allNodes || episodes.isEmpty()) return episodes;
        long nodeId = episodes.stream().filter(episode -> !episode.sources().isEmpty())
                .findFirst().orElse(episodes.getFirst()).nodeId();
        return episodes.stream().filter(episode -> episode.nodeId() == nodeId).toList();
    }

    public Resource media(String fileName) {
        String url = "/api/media/playback/" + fileName;
        if (!permitted(url)) throw new CatalogProblem(CatalogProblem.Reason.PLAYBACK_NOT_FOUND);
        var mapper = playback.getIfAvailable();
        if (mapper == null) throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        if (mapper.listedMedia(url) == 0) throw unavailable();
        if (storage.enabled()) {
            try {
                return storage.resource("playback/" + fileName);
            } catch (MediaStoreException error) {
                if (error.isMissing()) throw unavailable();
                throw error;
            }
        }
        var resource = new ClassPathResource("static" + url);
        if (!resource.exists()) throw unavailable();
        return resource;
    }

    private static CatalogProblem unavailable() {
        return new CatalogProblem(CatalogProblem.Reason.PLAYBACK_NOT_FOUND);
    }

    static boolean permitted(String url) {
        try {
            var uri = URI.create(url);
            if (!uri.isAbsolute()) {
                return uri.getRawAuthority() == null && uri.getRawQuery() == null && uri.getRawFragment() == null
                        && uri.getRawPath().matches("/api/media/playback/[A-Za-z0-9_-]+\\.(mp4|webm)");
            }
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
