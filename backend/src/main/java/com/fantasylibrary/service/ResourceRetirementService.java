package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.admin.LibraryAdminModels.NodeKind;
import com.fantasylibrary.admin.LibraryAdminModels.ResourceChange;
import com.fantasylibrary.admin.WorkEditorialModels.WorkRemoval;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.catalog.model.CatalogImagePaths;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.download.GameDownloadFiles;
import com.fantasylibrary.media.MediaStoreException;
import com.fantasylibrary.media.ObjectMediaStore;
import com.fantasylibrary.persistence.mapper.ResourceRetirementMapper;
import com.fantasylibrary.persistence.mapper.ResourceRetirementMapper.ReferenceKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;

import static com.fantasylibrary.admin.LibraryAdminModels.ResourceChange.*;

@Service
public class ResourceRetirementService {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceRetirementService.class);
    private static final String MEDIA_PREFIX = "/api/media/";
    private final ObjectProvider<ResourceRetirementMapper> mappers;
    private final ObjectProvider<TransactionTemplate> transactions;
    private final ObjectMediaStore storage;

    public ResourceRetirementService(ObjectProvider<ResourceRetirementMapper> mappers,
                                     ObjectProvider<TransactionTemplate> transactions, ObjectMediaStore storage) {
        this.mappers = mappers;
        this.transactions = transactions;
        this.storage = storage;
    }

    // Call inside the transaction that adds or changes a managed object reference.
    public void requireAttachable(NodeKind kind, String location) {
        var referenceKind = referenceKind(kind);
        String key = objectKey(referenceKind, location);
        if (key == null) return;
        var db = mapper();
        db.lockReferences();
        checkAttachable(db, referenceKind, key);
    }

    // Acquire this before locking a work row when editing catalog images.
    public void lockCatalogReferences() {
        mapper().lockReferences();
    }

    public void requireCatalogAttachable(String location) {
        String key = objectKey(ReferenceKind.CATALOG, location);
        if (key != null) checkAttachable(mapper(), ReferenceKind.CATALOG, key);
    }

    private void checkAttachable(ResourceRetirementMapper db, ReferenceKind kind, String key) {
        if (db.pending(key) != 0) {
            throw new AccountProblem(HttpStatus.CONFLICT, Code.INVALID_ARGUMENT, "此文件正在清理，请重新上传");
        }
        if (!storage.enabled()) {
            if (kind == ReferenceKind.DOWNLOAD) throw new MediaStoreException(false, null);
            return;
        }
        try {
            storage.resource(key);
        } catch (MediaStoreException failure) {
            if (failure.isMissing()) {
                throw new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, "找不到对应文件，请重新上传");
            }
            throw failure;
        }
    }

    public ResourceChange remove(NodeKind kind, long id) {
        var db = mapper();
        var referenceKind = referenceKind(kind);
        var removed = transactions.getObject().execute(status -> {
            db.lockReferences();
            var row = kind == NodeKind.PLAYBACK ? db.playback(id) : db.download(id);
            if (row == null) return new Removal(null, REMOVED);
            String key = objectKey(referenceKind, row.location());
            if (kind == NodeKind.PLAYBACK) db.deletePlayback(id);
            else db.deleteDownload(id);
            if (key == null) return new Removal(null, REMOVED);
            if (!queueIfUnreferenced(db, referenceKind, key)) return new Removal(null, SHARED_FILE_RETAINED);
            return new Removal(key, CLEANUP_PENDING);
        });
        if (removed.key() == null) return removed.result();
        return safeClean(removed.key());
    }

    public WorkRemoval removeWork(long id) {
        var db = mapper();
        return transactions.getObject().execute(status -> {
            db.lockReferences();
            if (db.lockWork(id) == null) return new WorkRemoval(0, 0);
            var files = new LinkedHashMap<String, ReferenceKind>();
            for (var asset : db.workAssets(id)) {
                String key = objectKey(asset.kind(), asset.location());
                if (key != null) files.putIfAbsent(key, asset.kind());
            }

            // Oracle cascades through work-owned records and clears the optional link on forum posts.
            db.deleteWork(id);

            int queued = 0;
            for (var file : files.entrySet()) {
                if (queueIfUnreferenced(db, file.getValue(), file.getKey())) queued++;
            }
            return new WorkRemoval(queued, files.size() - queued);
        });
    }

    private static boolean queueIfUnreferenced(ResourceRetirementMapper db, ReferenceKind kind, String key) {
        if (references(db, kind, key) > 0) return false;
        if (db.pending(key) == 0) db.enqueue(key, kind);
        return true;
    }

    @Scheduled(fixedDelay = 5_000, initialDelay = 5_000)
    public void retryCleanup() {
        var db = mappers.getIfAvailable();
        if (db == null || !storage.enabled()) return;
        try {
            db.due().forEach(this::safeClean);
        } catch (DataAccessException failure) {
            LOG.debug("Resource cleanup queue is temporarily unavailable", failure);
        }
    }

    private ResourceChange safeClean(String key) {
        if (!storage.enabled()) return CLEANUP_PENDING;
        try {
            return transactions.getObject().execute(status -> clean(mapper(), key));
        } catch (DataAccessException failure) {
            LOG.warn("Resource cleanup will retry after a database failure");
            return CLEANUP_PENDING;
        }
    }

    private ResourceChange clean(ResourceRetirementMapper db, String key) {
        var kind = db.lockPending(key);
        if (kind == null) return db.pending(key) > 0 ? CLEANUP_PENDING : REMOVED;
        if (references(db, kind, key) > 0) {
            db.complete(key);
            return SHARED_FILE_RETAINED;
        }
        try {
            storage.delete(key);
        } catch (MediaStoreException failure) {
            if (!failure.isMissing()) {
                db.reschedule(key);
                LOG.warn("Resource cleanup will retry after an object storage failure");
                return CLEANUP_PENDING;
            }
        }
        db.complete(key);
        return REMOVED;
    }

    private static ReferenceKind referenceKind(NodeKind kind) {
        return kind == NodeKind.PLAYBACK ? ReferenceKind.PLAYBACK : ReferenceKind.DOWNLOAD;
    }

    private static String objectKey(ReferenceKind kind, String location) {
        if (location == null) return null;
        return switch (kind) {
            case DOWNLOAD -> GameDownloadFiles.isObjectKey(location) ? location : null;
            case CATALOG -> CatalogImagePaths.objectKey(location);
            case PLAYBACK -> location.startsWith(MEDIA_PREFIX + "playback/") && PlaybackService.permitted(location)
                    ? location.substring(MEDIA_PREFIX.length()) : null;
        };
    }

    private static int references(ResourceRetirementMapper db, ReferenceKind kind, String key) {
        return switch (kind) {
            case PLAYBACK -> db.playbackReferences(MEDIA_PREFIX + key);
            case DOWNLOAD -> db.downloadReferences(key);
            case CATALOG -> db.catalogReferences(MEDIA_PREFIX + key);
        };
    }

    private ResourceRetirementMapper mapper() {
        var db = mappers.getIfAvailable();
        if (db == null) throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        return db;
    }

    private record Removal(String key, ResourceChange result) {}
}
