package com.fantasylibrary.service;

import com.fantasylibrary.catalog.model.Catalog.Page;
import com.fantasylibrary.personal.PersonalModels.Inbox;
import com.fantasylibrary.personal.PersonalModels.WatchEvent;
import com.fantasylibrary.persistence.mapper.PersonalMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonalService {
    private final ObjectProvider<PersonalMapper> database;

    public PersonalService(ObjectProvider<PersonalMapper> database) {
        this.database = database;
    }

    public Page<WatchEvent> history(long userId, int page, int size) {
        var db = mapper();
        long total = db.countHistory(userId);
        long offset = offset(page, size);
        return Page.of(offset < total ? db.history(userId, offset, size) : List.of(), page, size, total);
    }

    public Inbox inbox(long userId, int page, int size) {
        var db = mapper();
        long total = db.countNotices(userId, false);
        long offset = offset(page, size);
        return new Inbox(Page.of(offset < total ? db.notices(userId, offset, size) : List.of(), page, size, total),
                db.countNotices(userId, true));
    }

    public void markRead(long userId, long noticeId) {
        mapper().readNotice(userId, noticeId);
    }

    public void markAllRead(long userId) {
        mapper().readAll(userId);
    }

    private PersonalMapper mapper() {
        var mapper = database.getIfAvailable();
        if (mapper == null) throw AccountService.unavailable();
        return mapper;
    }

    private static long offset(int page, int size) {
        return ((long) page - 1) * size;
    }
}
