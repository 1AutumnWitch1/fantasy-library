package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountModels.SaveWork;
import com.fantasylibrary.account.AccountModels.WatchStatus;
import com.fantasylibrary.account.AccountModels.SavedWork;
import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import com.fantasylibrary.persistence.mapper.CollectionMapper;
import com.fantasylibrary.persistence.mapper.ViewingMapper;
import org.springframework.beans.factory.ObjectProvider;
import com.fantasylibrary.persistence.mapper.PersonalMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class CollectionService {
    private final ObjectProvider<CollectionMapper> collections;
    private final ObjectProvider<CatalogMapper> catalogues;

    private final ObjectProvider<PersonalMapper> personal;
    private final ObjectProvider<ViewingMapper> viewing;
    private final ObjectProvider<TransactionTemplate> transactions;

    public CollectionService(ObjectProvider<CollectionMapper> collections, ObjectProvider<CatalogMapper> catalogues,
                             ObjectProvider<PersonalMapper> personal, ObjectProvider<ViewingMapper> viewing,
                             ObjectProvider<TransactionTemplate> transactions) {
        this.collections = collections;
        this.catalogues = catalogues;
        this.personal = personal;
        this.viewing = viewing;
        this.transactions = transactions;
    }

    public List<SavedWork> list(long userId) {
        return mapper().list(userId);
    }

    public SavedWork find(long userId, long workId) {
        return mapper().find(userId, workId);
    }

    public SavedWork save(long userId, long workId, SaveWork form) {
        var collection = mapper();
        var catalogue = catalogues.getIfAvailable();
        if (catalogue == null) throw AccountService.unavailable();
        var work = catalogue.findWork(workId);
        if (work == null) throw new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
        validate(work, form);
        return transactions.getObject().execute(status -> {
            lockMember(personal.getObject(), userId);
            var item = work.type() == WorkType.ANIME
                    ? followed(work, userId, collection.find(userId, workId))
                    : game(work, form.favorite(), form.wishlist());
            collection.save(userId, item);
            return item;
        });
    }

    public enum Selection { FOLLOW, FAVORITE, WISHLIST }

    public SavedWork select(long userId, long workId, Selection kind, boolean enabled) {
        return transactions.getObject().execute(status -> {
            lockMember(personal.getObject(), userId);
            var catalog = catalogues.getIfAvailable();
            if (catalog == null) throw AccountService.unavailable();
            var work = catalog.findWork(workId);
            if (work == null) throw new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
            boolean anime = work.type() == WorkType.ANIME;
            if (anime != (kind == Selection.FOLLOW)) throw invalid("操作不适用于这类作品");

            var collection = mapper();
            var previous = collection.find(userId, workId);
            if (anime && enabled && previous != null) return previous;
            boolean favorite = kind != Selection.WISHLIST ? enabled : previous != null && previous.favorite() == 1;
            boolean wishlist = kind == Selection.WISHLIST ? enabled : previous != null && previous.wishlist() == 1;
            if (!favorite && !wishlist) {
                collection.remove(userId, workId);
                return null;
            }
            var item = anime ? followed(work, userId, previous) : game(work, favorite, wishlist);
            collection.save(userId, item);
            return item;
        });
    }

    public void remove(long userId, long workId) {
        var collection = mapper();
        transactions.getObject().executeWithoutResult(status -> {
            lockMember(personal.getObject(), userId);
            collection.remove(userId, workId);
        });
    }

    private static void lockMember(PersonalMapper history, long userId) {
        if (history.lockMember(userId) == null) {
            throw new AccountProblem(HttpStatus.UNAUTHORIZED, Code.LOGIN_REQUIRED, "请先登录");
        }
    }

    private SavedWork followed(CatalogMapper.WorkRow work, long userId, SavedWork previous) {
        var history = viewing.getObject();
        int watched = 0;
        var state = WatchStatus.PLAN_TO_WATCH;
        if (history.latest(userId, work.id()) != null) {
            watched = history.completed(userId, work.id());
            state = work.episodeCount() != null && watched >= work.episodeCount()
                    ? WatchStatus.COMPLETED : WatchStatus.WATCHING;
        } else if (previous != null) {
            // Keep any saved pre-migration record when a legacy client repeats its request.
            watched = previous.watchedEpisodes();
            state = previous.watchStatus();
        }
        return new SavedWork(work.id(), work.type(), work.title(), work.coverUrl(), work.coverAltText(),
                work.episodeCount(), 1, 0, state, watched);
    }

    private static SavedWork game(CatalogMapper.WorkRow work, boolean favorite, boolean wishlist) {
        return new SavedWork(work.id(), work.type(), work.title(), work.coverUrl(), work.coverAltText(),
                work.episodeCount(), favorite ? 1 : 0, wishlist ? 1 : 0, null, 0);
    }

    private CollectionMapper mapper() {
        var mapper = collections.getIfAvailable();
        if (mapper == null) throw AccountService.unavailable();
        return mapper;
    }

    private static void validate(CatalogMapper.WorkRow work, SaveWork form) {
        if (work.type() == WorkType.ANIME) {
            if (!form.favorite() || form.wishlist() || form.watchStatus() == null) {
                throw invalid("收藏番剧需要选择追番状态");
            }
            if (work.episodeCount() != null && form.watchedEpisodes() > work.episodeCount()) {
                throw invalid("观看进度不能超过已收录的总集数");
            }
        } else if (form.watchStatus() != null || form.watchedEpisodes() != 0 || (!form.favorite() && !form.wishlist())) {
            throw invalid("请选择游戏收藏或愿望单");
        }
    }

    private static AccountProblem invalid(String message) {
        return new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, message);
    }
}
