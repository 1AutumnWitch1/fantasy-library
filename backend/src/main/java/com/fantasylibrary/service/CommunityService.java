package com.fantasylibrary.service;

import com.fantasylibrary.catalog.model.Catalog.Page;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.community.CommunityImages;
import com.fantasylibrary.community.CommunityModels.*;
import com.fantasylibrary.community.CommunityProblem;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import com.fantasylibrary.persistence.mapper.CommunityMapper;
import com.fantasylibrary.persistence.mapper.PersonalMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.fantasylibrary.community.CommunityProblem.*;

@Service
public class CommunityService {
    private final ObjectProvider<CommunityMapper> database;
    private final ObjectProvider<CatalogMapper> catalog;
    private final ObjectProvider<TransactionTemplate> transactions;
    private final CommunityImages images;
    private final ObjectProvider<PersonalMapper> personal;

    public CommunityService(ObjectProvider<CommunityMapper> database, ObjectProvider<CatalogMapper> catalog,
                            ObjectProvider<TransactionTemplate> transactions, CommunityImages images,
                            ObjectProvider<PersonalMapper> personal) {
        this.database = database;
        this.catalog = catalog;
        this.transactions = transactions;
        this.images = images;
        this.personal = personal;
    }

    public Page<Post> posts(Topic topic, Long workId, int page, int size, PostOrder order) {
        var db = openForum();
        long offset = offset(page, size);
        long count = db.countPosts(topic, workId);
        return Page.of(offset < count ? db.posts(topic, workId, offset, size, order) : List.of(), page, size, count);
    }

    public PostDetail post(long id) {
        var db = openForum();
        var post = db.post(id);
        if (post == null) throw notFound();
        return new PostDetail(post, db.images(id));
    }

    public PostDetail view(long id) {
        var db = openForum();
        return transaction().execute(status -> {
            if (db.addView(id) == 0) throw notFound();
            return post(id);
        });
    }

    public Resource image(String key) {
        if (!CommunityImages.valid(key) || openForum().visibleImage(key) == 0) throw notFound();
        return images.load(key);
    }

    public Created publish(long author, PostForm form, List<MultipartFile> files) {
        var db = openForum();
        var normalized = normalize(form);
        var keep = normalized.keepImages();
        if (!keep.isEmpty()) throw invalid("新帖子需要重新上传图片");
        limit(keep, files);
        var uploaded = images.save(files);
        try {
            return transaction().execute(status -> {
                long id = db.nextPost();
                db.addPost(id, author, normalized);
                attach(db, id, uploaded);
                return new Created(id);
            });
        } catch (RuntimeException error) {
            images.remove(uploaded);
            throw error;
        }
    }

    public void edit(long id, long author, PostForm form, List<MultipartFile> files) {
        var db = openForum();
        if (post(id).post().authorId() != author) throw forbidden();
        var normalized = normalize(form);
        var keep = normalized.keepImages();
        limit(keep, files);
        var uploaded = images.save(files);
        List<String> removed;
        try {
            removed = transaction().execute(status -> {
                owned(db.lockForumPost(id), author);
                var previous = db.images(id);
                if (!previous.containsAll(keep)) throw invalid("要保留的图片不属于此帖");
                db.editPost(id, author, normalized);
                db.clearImages(id);
                var current = new ArrayList<>(keep);
                current.addAll(uploaded);
                attach(db, id, current);
                return previous.stream().filter(key -> !keep.contains(key)).toList();
            });
        } catch (RuntimeException error) {
            images.remove(uploaded);
            throw error;
        }
        images.remove(removed);
    }

    public void delete(long id, long author) {
        var db = openForum();
        var removed = transaction().execute(status -> {
            owned(db.lockForumPost(id), author);
            return removePost(db, id, null);
        });
        images.remove(removed);
    }

    public Page<Comment> comments(long post, int page, int size) {
        var db = openForum();
        visible(db, post);
        return commentPage(db, post, page, size);
    }

    public Page<Comment> screeningComments(long workId, int page, int size) {
        var db = mapper();
        var post = screeningPost(db, workId);
        return post == null ? Page.of(List.of(), page, size, 0) : commentPage(db, post, page, size);
    }

    public Page<ScreeningThread> screeningThreads(long workId, Long viewer, int page, int size) {
        var db = mapper();
        var post = screeningPost(db, workId);
        if (post == null) return Page.of(List.of(), page, size, 0);
        long offset = offset(page, size);
        long total = db.countScreeningThreads(post);
        if (offset >= total) return Page.of(List.of(), page, size, total);
        var roots = db.screeningRoots(post, viewer, offset, size);
        if (roots.isEmpty()) return Page.of(List.of(), page, size, total);
        var ids = roots.stream().map(ScreeningComment::id).toList();
        var replies = db.screeningPreviews(post, viewer, ids).stream()
                .collect(Collectors.groupingBy(ScreeningComment::rootId));
        var groups = roots.stream().map(root -> new ScreeningThread(root, replies.getOrDefault(root.id(), List.of())))
                .toList();
        return Page.of(groups, page, size, total);
    }

    public Page<ScreeningComment> screeningReplies(long workId, long root, Long viewer, int page, int size) {
        var db = mapper();
        var post = screeningPost(db, workId);
        var comment = db.comment(root);
        if (post == null || comment == null || comment.postId() != post || comment.parentId() != null) {
            throw notFound();
        }
        long offset = offset(page, size);
        long total = db.countScreeningReplies(post, root);
        return Page.of(offset < total ? db.screeningReplies(post, root, viewer, offset, size) : List.of(), page, size, total);
    }

    public Reaction screenReaction(long workId, long id, long user, boolean liked) {
        var db = mapper();
        return transaction().execute(status -> {
            if (db.lockScreeningWork(workId) == null) throw notFound();
            var post = db.screeningThread(workId);
            if (post == null) throw notFound();
            visible(db.lockPost(post));
            var comment = db.comment(id);
            if (comment == null || comment.postId() != post || comment.state() != Visibility.VISIBLE) {
                throw notFound();
            }
            if (liked) db.likeScreeningComment(id, user);
            else db.unlikeScreeningComment(id, user);
            return new Reaction(liked, db.screeningLikeCount(id));
        });
    }

    private Long screeningPost(CommunityMapper db, long workId) {
        var work = catalog.getObject().findWork(workId);
        if (work == null || work.type() != WorkType.ANIME) throw notFound();
        return db.screeningThread(workId);
    }

    private static Page<Comment> commentPage(CommunityMapper db, long post, int page, int size) {
        long offset = offset(page, size);
        long count = db.countComments(post);
        return Page.of(offset < count ? db.comments(post, offset, size) : List.of(), page, size, count);
    }

    public Created comment(long post, long author, CommentForm form) {
        var db = openForum();
        String body = text(form.body());
        return transaction().execute(status -> {
            visible(db.lockForumPost(post));
            return saveComment(db, post, author, form.parentId(), body);
        });
    }

    public Created screeningComment(long workId, long author, CommentForm form) {
        var db = mapper();
        String body = text(form.body());
        return transaction().execute(status -> {
            if (db.lockScreeningWork(workId) == null) throw notFound();
            var post = db.screeningThread(workId);
            if (post == null) {
                if (form.parentId() != null) throw invalid("只能回复当前讨论中可见的评论");
                post = db.nextPost();
                db.addScreeningThread(post, author, workId);
            }
            visible(db.lockPost(post));
            return saveComment(db, post, author, form.parentId(), body);
        });
    }

    private Created saveComment(CommunityMapper db, long post, long author, Long parentId, String body) {
        Long rootId = null;
        if (parentId != null) {
            var parent = db.comment(parentId);
            if (parent == null || parent.postId() != post || parent.state() != Visibility.VISIBLE) {
                throw invalid("只能回复当前讨论中可见的评论");
            }
            rootId = parent.rootId() == null ? parentId : parent.rootId();
        }
        long id = db.nextComment();
        db.addComment(id, post, author, parentId, rootId, body);
        personal.getObject().notifyComment(id);
        return new Created(id);
    }

    public Reaction reaction(long post, long user) {
        var db = openForum();
        visible(db, post);
        return new Reaction(db.liked(post, user) != 0, db.countLikes(post));
    }

    public Reaction react(long post, long user, boolean liked) {
        var db = openForum();
        return transaction().execute(status -> {
            visible(db.lockForumPost(post));
            if (liked && db.liked(post, user) == 0) {
                db.addLike(post, user);
                personal.getObject().notifyLike(post, user);
            }
            if (!liked) db.removeLike(post, user);
            return new Reaction(liked, db.countLikes(post));
        });
    }

    public void report(long user, ReportForm form) {
        var db = mapper();
        String reason = text(form.reason());
        long post = parentPost(db, form.targetType(), form.targetId());
        transaction().executeWithoutResult(status -> {
            if (form.targetType() == Target.COMMENT) {
                var row = db.lockCommentThread(post);
                visible(row);
                if (!row.screeningThread() && !db.forumOpen()) throw closed();
                checkComment(db.comment(form.targetId()));
            } else {
                if (!db.forumOpen()) throw closed();
                visible(db.lockForumPost(post));
            }
            try {
                db.addReport(user, form.targetType(), form.targetId(), reason);
            } catch (DuplicateKeyException alreadyReported) {
                // Repeated submissions by the same person refer to the existing report.
            }
        });
    }

    public boolean isOpen() {
        return mapper().forumOpen();
    }

    public void publishExistingPost(long id, boolean listed) {
        var db = mapper();
        transaction().executeWithoutResult(status -> {
            var row = db.lockForumPost(id);
            if (row == null || row.state() == Visibility.DELETED) throw notFound();
            db.setPostState(id, listed ? Visibility.VISIBLE : Visibility.HIDDEN);
        });
    }

    public void removeComment(long admin, long id) {
        var db = mapper();
        var comment = db.comment(id);
        if (comment == null) throw notFound();
        transaction().executeWithoutResult(status -> {
            var post = db.lockPost(comment.postId());
            if (post == null || post.state() == Visibility.DELETED) throw notFound();
            db.setCommentState(id, Visibility.DELETED);
            db.closeReports(Target.COMMENT, id, ReportState.DELETED, admin);
        });
    }

    public Page<Report> reports(int page, int size) {
        var db = mapper();
        long offset = offset(page, size);
        long count = db.countReports();
        return Page.of(offset < count ? db.reports(offset, size) : List.of(), page, size, count);
    }

    public void review(long admin, long id, Action action) {
        var db = mapper();
        var report = db.report(id);
        if (report == null) throw notFound();
        long post = parentPost(db, report.targetType(), report.targetId());
        var removed = transaction().execute(status -> {
            var row = db.lockPost(post);
            if (row == null) throw notFound();
            var current = db.report(id);
            if (current.state() != ReportState.OPEN) return List.<String>of();
            if (action == Action.DISMISS) {
                db.dismissReport(id, admin);
                return List.<String>of();
            }
            if (action == Action.DELETE && report.targetType() == Target.POST) return removePost(db, post, admin);
            var visibility = action == Action.DELETE ? Visibility.DELETED : Visibility.HIDDEN;
            if (report.targetType() == Target.POST) db.setPostState(post, visibility);
            else db.setCommentState(report.targetId(), visibility);
            var result = action == Action.DELETE ? ReportState.DELETED : ReportState.HIDDEN;
            db.closeReports(report.targetType(), report.targetId(), result, admin);
            return List.<String>of();
        });
        images.remove(removed);
    }

    private static List<String> removePost(CommunityMapper db, long id, Long admin) {
        var removed = db.images(id);
        db.setPostState(id, Visibility.DELETED);
        db.clearImages(id);
        db.closeReports(Target.POST, id, ReportState.DELETED, admin);
        return removed;
    }

    private static long parentPost(CommunityMapper db, Target type, long id) {
        if (type == Target.POST) return id;
        var comment = db.comment(id);
        if (comment == null) throw notFound();
        return comment.postId();
    }

    private PostForm normalize(PostForm form) {
        if (form.workId() != null && catalog.getObject().findWork(form.workId()) == null) {
            throw invalid("关联的作品不存在");
        }
        var keys = form.keepImages() == null ? List.<String>of() : List.copyOf(form.keepImages());
        if (new HashSet<>(keys).size() != keys.size()) throw invalid("图片不能重复");
        return new PostForm(text(form.title()), text(form.body()), form.topic(), form.workId(), keys);
    }

    private static String text(String raw) {
        String clean = raw.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (clean.isBlank() || clean.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t')) {
            throw invalid("请输入有效文字，内容不能含控制字符");
        }
        return clean;
    }

    private static void limit(List<String> keep, List<MultipartFile> files) {
        if (keep.size() + files.size() > 4) throw invalid("一个帖子最多可包含 4 张图片");
    }

    private static void attach(CommunityMapper db, long id, List<String> keys) {
        for (int i = 0; i < keys.size(); i++) db.addImage(id, keys.get(i), i);
    }

    private static void owned(Row post, long author) {
        visible(post);
        if (post.authorId() != author) throw forbidden();
    }

    private static void visible(Row post) {
        if (post == null || post.state() != Visibility.VISIBLE) throw notFound();
    }

    private static void visible(CommunityMapper db, long id) {
        if (db.visiblePost(id) == 0) throw notFound();
    }

    private static void checkComment(ReplyRow comment) {
        if (comment == null || comment.state() != Visibility.VISIBLE) throw notFound();
    }

    private CommunityMapper openForum() {
        var mapper = mapper();
        if (!mapper.forumOpen()) throw closed();
        return mapper;
    }

    private CommunityMapper mapper() {
        var mapper = database.getIfAvailable();
        if (mapper == null) throw unavailable();
        return mapper;
    }

    private TransactionTemplate transaction() {
        return transactions.getObject();
    }

    private static long offset(int page, int size) {
        return ((long) page - 1) * size;
    }
}
