package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.admin.LibraryAdminModels.NodeKind;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.download.DownloadEditorialModels.Archive;
import com.fantasylibrary.download.DownloadEditorialModels.Draft;
import com.fantasylibrary.download.GameDownloadFiles;
import com.fantasylibrary.persistence.mapper.DownloadEditorialMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DownloadEditorialService {
    private final ObjectProvider<DownloadEditorialMapper> editors;
    private final ObjectProvider<TransactionTemplate> transactions;
    private final ResourceRetirementService retirement;

    public DownloadEditorialService(ObjectProvider<DownloadEditorialMapper> editors,
                                    ObjectProvider<TransactionTemplate> transactions, ResourceRetirementService retirement) {
        this.editors = editors;
        this.transactions = transactions;
        this.retirement = retirement;
    }

    public Archive archive(long workId) {
        var db = editor();
        if (db.game(workId) == null) throw new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
        return new Archive(db.platforms(workId), db.nodes(workId));
    }

    public void save(long workId, Long nodeId, Draft draft) {
        var db = editor();
        var external = clean(draft.externalUrl());
        var file = clean(draft.fileName());
        if ((external == null) == (file == null)) throw invalid("请选择外部地址或已上传的安装包");
        if (external != null && !GameDownloadService.isHttps(external)) throw invalid("下载地址须使用 HTTPS");
        if (file != null && !GameDownloadFiles.isObjectKey(file) && !GameDownloadFiles.isLegacyFile(file)) {
            throw invalid("安装包路径无效");
        }
        var input = new Draft(draft.platformId(), draft.label().strip(), external, file, draft.listed());
        try {
            transactions.getObject().executeWithoutResult(status -> {
                retirement.requireAttachable(NodeKind.DOWNLOAD, input.fileName());
                if (db.lockGame(workId) == null) throw new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
                if (db.lockPlatform(workId, input.platformId()) == null) throw invalid("请先保存该游戏的平台");
                if (nodeId == null) {
                    db.add(workId, input);
                } else {
                    var state = db.publication(workId, nodeId);
                    if (state == null) {
                        throw new AccountProblem(HttpStatus.NOT_FOUND, Code.ROUTE_NOT_FOUND, "找不到该下载节点");
                    }
                    if (state == 1 && !input.listed()) {
                        throw new AccountProblem(HttpStatus.CONFLICT, Code.INVALID_ARGUMENT, "请在下载节点列表确认下架并删除");
                    }
                    db.update(workId, nodeId, input);
                }
            });
        } catch (DuplicateKeyException failure) {
            throw new AccountProblem(HttpStatus.CONFLICT, Code.INVALID_ARGUMENT, "此平台已有同名下载节点");
        }
    }

    private DownloadEditorialMapper editor() {
        var db = editors.getIfAvailable();
        if (db == null) throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        return db;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static AccountProblem invalid(String message) {
        return new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, message);
    }
}
