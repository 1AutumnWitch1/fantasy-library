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
    private final ObjectProvider<TransactionTemplate> transactions;

    public CollectionService(ObjectProvider<CollectionMapper> collections, ObjectProvider<CatalogMapper> catalogues,
                             ObjectProvider<PersonalMapper> personal, ObjectProvider<TransactionTemplate> transactions) {
        this.collections = collections;
        this.catalogues = catalogues;
        this.personal = personal;
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
        var item = new SavedWork(work.id(), work.type(), work.title(), work.coverUrl(), work.coverAltText(), work.episodeCount(),
                form.favorite() ? 1 : 0, form.wishlist() ? 1 : 0, form.watchStatus(), form.watchedEpisodes());
        return transactions.getObject().execute(status -> {
            var history = personal.getObject();
            lockMember(history, userId);
            var previous = item.workType() == WorkType.ANIME ? collection.find(userId, workId) : null;
            collection.save(userId, item);
            if (recordsProgress(item, previous)) history.addWatchEvent(userId, item);
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

    private static boolean recordsProgress(SavedWork item, SavedWork previous) {
        boolean started = item.watchedEpisodes() > 0
                || item.watchStatus() == WatchStatus.WATCHING || item.watchStatus() == WatchStatus.COMPLETED;
        return item.workType() == WorkType.ANIME && started
                && (previous == null || previous.watchStatus() != item.watchStatus()
                    || previous.watchedEpisodes() != item.watchedEpisodes());
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
