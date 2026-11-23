package com.fantasylibrary.service;

import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.download.GameDownload;
import com.fantasylibrary.download.GameDownloadFiles;
import com.fantasylibrary.media.MediaStoreException;
import com.fantasylibrary.media.ObjectMediaStore;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import com.fantasylibrary.persistence.mapper.GameDownloadMapper;
import com.fantasylibrary.persistence.mapper.GameDownloadMapper.Node;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class GameDownloadService {

    private final ObjectProvider<CatalogMapper> catalog;
    private final ObjectProvider<GameDownloadMapper> downloads;
    private final Path directory;
    private final ObjectMediaStore storage;

    public GameDownloadService(ObjectProvider<CatalogMapper> catalog, ObjectProvider<GameDownloadMapper> downloads,
                               @Value("${library.downloads.directory}") String directory, ObjectMediaStore storage) {
        this.catalog = catalog;
        this.downloads = downloads;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.storage = storage;
    }

    public List<GameDownload> nodes(long workId) {
        var works = catalog.getIfAvailable();
        if (works == null) throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        var work = works.findWork(workId);
        if (work == null || work.type() != WorkType.GAME) {
            throw new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
        }
        return mapper().nodes(workId).stream().map(this::resolve).filter(Objects::nonNull).toList();
    }

    public DownloadFile file(long nodeId) {
        String name = mapper().fileName(nodeId);
        if (GameDownloadFiles.isObjectKey(name) && storage.enabled()) {
            try {
                return new DownloadFile(GameDownloadFiles.attachmentName(name), storage.resource(name));
            } catch (MediaStoreException failure) {
                if (!failure.isMissing()) throw failure;
                throw unavailable();
            }
        }
        Path path = localFile(name).orElseThrow(GameDownloadService::unavailable);
        return new DownloadFile(name, new FileSystemResource(path));
    }

    private GameDownload resolve(Node node) {
        if (node.externalUrl() != null) {
            return isHttps(node.externalUrl())
                    ? new GameDownload(node.id(), node.label(), node.platform(), node.externalUrl(), true)
                    : null;
        }
        boolean available = GameDownloadFiles.isObjectKey(node.fileName()) ? storage.enabled() : localFile(node.fileName()).isPresent();
        return available
                ? new GameDownload(node.id(), node.label(), node.platform(), "/api/game-downloads/" + node.id() + "/file", false)
                : null;
    }

    private Optional<Path> localFile(String name) {
        if (!GameDownloadFiles.isLegacyFile(name)) return Optional.empty();
        try {
            Path root = directory.toRealPath();
            Path file = root.resolve(name).toRealPath();
            return file.startsWith(root) && Files.isRegularFile(file) && Files.isReadable(file)
                    ? Optional.of(file) : Optional.empty();
        } catch (IOException unavailable) {
            return Optional.empty();
        }
    }

    static boolean isHttps(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private GameDownloadMapper mapper() {
        var mapper = downloads.getIfAvailable();
        if (mapper == null) throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        return mapper;
    }

    private static CatalogProblem unavailable() {
        return new CatalogProblem(CatalogProblem.Reason.DOWNLOAD_NOT_FOUND);
    }

    public record DownloadFile(String name, Resource resource) {}
}
