package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.admin.LibraryAdminModels.NodeKind;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.persistence.mapper.PlaybackEditorialMapper;
import com.fantasylibrary.persistence.mapper.PlaybackMapper;
import com.fantasylibrary.playback.PlaybackEditorialModels.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PlaybackEditorialService {
    private static final String LOCAL_MEDIA = "/api/media/playback/";
    private final ObjectProvider<PlaybackEditorialMapper> editors;
    private final ObjectProvider<PlaybackMapper> playback;
    private final ObjectProvider<TransactionTemplate> transactions;
    private final ResourceRetirementService retirement;

    public PlaybackEditorialService(ObjectProvider<PlaybackEditorialMapper> editors,
                                    ObjectProvider<PlaybackMapper> playback,
                                    ObjectProvider<TransactionTemplate> transactions,
                                    ResourceRetirementService retirement) {
        this.editors = editors;
        this.playback = playback;
        this.transactions = transactions;
        this.retirement = retirement;
    }

    public List<Episode> episodes(long workId) {
        var db = editor();
        if (db.anime(workId) == null) throw missingWork();
        var sources = db.sources(workId).stream().collect(Collectors.groupingBy(
                SourceRow::episodeId, Collectors.mapping(row ->
                        new Source(row.id(), row.label(), row.quality(), row.url(), row.format(), row.listed()),
                        Collectors.toUnmodifiableList())));
        return playback.getObject().episodes(workId).stream().map(row -> new Episode(row.id(), row.edition(), row.number(),
                row.title(), row.durationSeconds(), sources.getOrDefault(row.id(), List.of()), row.nodeId())).toList();
    }

    public List<Node> nodes(long workId) {
        var db = editor();
        if (db.anime(workId) == null) throw missingWork();
        return db.nodes(workId);
    }

    public void saveNode(long workId, Long nodeId, NodeDraft draft) {
        var db = editor();
        var clean = new NodeDraft(draft.label().strip(), draft.listed());
        try {
            transactions.getObject().executeWithoutResult(status -> {
                if (db.lockAnime(workId) == null) throw missingWork();
                if (nodeId == null) db.addNode(workId, clean);
                else if (db.updateNode(workId, nodeId, clean) == 0) throw missingResource("找不到该播放线路");
            });
        } catch (DuplicateKeyException error) {
            throw conflict("此作品已有同名播放线路");
        }
    }

    public void saveEpisode(long workId, Long episodeId, EpisodeDraft draft) {
        var db = editor();
        try {
            transactions.getObject().executeWithoutResult(status -> {
                if (db.lockAnime(workId) == null) throw missingWork();
                var nodeId = draft.nodeId();
                if (episodeId != null) {
                    var previous = db.episodeNode(workId, episodeId);
                    if (previous == null) throw missingResource("找不到该分集");
                    if (nodeId == null) nodeId = previous;
                }
                var nodes = db.nodes(workId);
                if (nodeId == null) {
                    if (nodes.isEmpty()) {
                        db.addNode(workId, new NodeDraft("默认线路", true));
                        nodes = db.nodes(workId);
                    }
                    nodeId = nodes.getFirst().id();
                }
                final long targetNode = nodeId;
                if (nodes.stream().noneMatch(node -> node.id() == targetNode)) {
                    throw missingResource("找不到该作品的播放线路");
                }
                var clean = new EpisodeDraft(draft.edition(), draft.number(), draft.title().strip(),
                        draft.durationSeconds(), targetNode);
                if (episodeId == null) {
                    db.addEpisode(workId, clean);
                } else if (db.updateEpisode(workId, episodeId, clean) == 0) {
                    throw missingResource("找不到该分集");
                }
            });
        } catch (DuplicateKeyException error) {
            throw conflict("该线路和分集类型下已存在相同编号");
        }
    }

    public void saveSource(long workId, long episodeId, Long sourceId, SourceDraft draft) {
        var db = editor();
        var clean = normalize(draft);
        transactions.getObject().executeWithoutResult(status -> {
            retirement.requireAttachable(NodeKind.PLAYBACK, clean.url());
            if (db.lockEpisode(workId, episodeId) == null) throw missingResource("找不到该分集");
            if (sourceId == null) {
                db.addSource(episodeId, clean);
            } else if (db.updateSource(episodeId, sourceId, clean) == 0) {
                throw missingResource("找不到该播放节点");
            }
        });
    }

    static SourceDraft normalize(SourceDraft draft) {
        String address = draft.url().strip();
        if (!PlaybackService.permitted(address)) {
            throw new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, "视频地址须使用 HTTPS 或本站播放资源路径");
        }
        if (address.startsWith(LOCAL_MEDIA) && !address.endsWith("." + draft.format().name().toLowerCase(Locale.ROOT))) {
            throw new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, "视频格式与站内资源路径不一致");
        }
        return new SourceDraft(draft.label().strip(), draft.quality().strip(), address, draft.format());
    }

    private PlaybackEditorialMapper editor() {
        var db = editors.getIfAvailable();
        if (db == null) throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        return db;
    }

    private static CatalogProblem missingWork() {
        return new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
    }

    private static AccountProblem conflict(String message) {
        return new AccountProblem(HttpStatus.CONFLICT, Code.INVALID_ARGUMENT, message);
    }

    private static AccountProblem missingResource(String message) {
        return new AccountProblem(HttpStatus.NOT_FOUND, Code.ROUTE_NOT_FOUND, message);
    }
}
