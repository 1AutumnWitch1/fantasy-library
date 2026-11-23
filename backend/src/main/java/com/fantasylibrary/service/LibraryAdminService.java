package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.admin.LibraryAdminModels.*;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.catalog.model.Catalog.Page;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.community.CommunityImages;
import com.fantasylibrary.persistence.mapper.LibraryAdminMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.LongFunction;

@Service
public class LibraryAdminService {
    private final ObjectProvider<LibraryAdminMapper> database;
    private final ObjectProvider<TransactionTemplate> transactions;
    private final CommunityImages images;
    private final ResourceRetirementService retirement;

    public LibraryAdminService(ObjectProvider<LibraryAdminMapper> database, ObjectProvider<TransactionTemplate> transactions,
                               CommunityImages images, ResourceRetirementService retirement) {
        this.database = database;
        this.transactions = transactions;
        this.images = images;
        this.retirement = retirement;
    }

    public Page<Work> works(WorkType type, String search, Boolean listed, int page, int size) {
        var db = mapper();
        var q = keyword(search);
        return page(db.countWorks(type, q, listed), page, size, offset -> db.works(type, q, listed, offset, size));
    }

    public Page<Post> posts(String search, int page, int size) {
        var db = mapper();
        var q = keyword(search);
        return page(db.countPosts(q), page, size, offset -> db.posts(q, offset, size));
    }

    public Page<Comment> comments(String search, CommentScope scope, int page, int size) {
        var db = mapper();
        var q = keyword(search);
        return page(db.countComments(q, scope), page, size, offset -> db.comments(q, scope, offset, size));
    }

    public Page<Member> members(String search, int page, int size) {
        var db = mapper();
        var q = keyword(search);
        return page(db.countMembers(q), page, size, offset -> db.members(q, offset, size));
    }

    public List<Node> nodes(long id) {
        return mapper().nodes(id);
    }

    public void publishWork(long id, boolean listed) {
        if (mapper().setWork(id, listed ? 1 : 0) == 0) {
            throw new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
        }
    }

    public ResourceChange publishNode(NodeKind kind, long id, NodePublication form) {
        if (!form.listed()) {
            if (!form.confirmRemoval()) {
                throw new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, "请确认永久删除资源记录及相关文件");
            }
            return retirement.remove(kind, id);
        }

        var db = mapper();
        int changed = switch (kind) {
            case PLAYBACK -> db.setPlaybackNode(id, 1);
            case DOWNLOAD -> db.setDownloadNode(id, 1);
        };
        if (changed == 0) throw new AccountProblem(HttpStatus.NOT_FOUND, Code.ROUTE_NOT_FOUND, "该节点已不存在");
        return ResourceChange.PUBLISHED;
    }

    public void forumOpen(boolean open) {
        mapper().setForumOpen(open ? 1 : 0);
    }

    public void removeMember(long id) {
        var db = mapper();
        String avatar = transactions.getObject().execute(status -> {
            var account = db.lockMember(id);
            if (account == null) {
                throw new AccountProblem(HttpStatus.NOT_FOUND, Code.ROUTE_NOT_FOUND, "该用户已不存在");
            }
            if (!"MEMBER".equals(account.role())) {
                throw new AccountProblem(HttpStatus.FORBIDDEN, Code.FORBIDDEN, "管理账号受保护");
            }
            db.anonymizeMember(id, "deleted_" + UUID.randomUUID().toString().replace("-", ""));
            db.revokeSessions(id);
            db.clearCollection(id);
            db.clearHistory(id);
            db.clearInbox(id);
            db.clearLikes(id);
            return account.avatarKey();
        });
        images.removeAvatar(avatar);
    }

    private static <T> Page<T> page(long total, int page, int size, LongFunction<List<T>> fetch) {
        long offset = ((long) page - 1) * size;
        return Page.of(offset < total ? fetch.apply(offset) : List.of(), page, size, total);
    }

    private static String keyword(String search) {
        return search == null || search.isBlank() ? null : search.strip();
    }

    private LibraryAdminMapper mapper() {
        var db = database.getIfAvailable();
        if (db == null) throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        return db;
    }
}
