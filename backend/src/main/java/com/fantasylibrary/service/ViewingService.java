package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import com.fantasylibrary.persistence.mapper.PersonalMapper;
import com.fantasylibrary.persistence.mapper.ViewingMapper;
import com.fantasylibrary.playback.PlaybackModels.Edition;
import com.fantasylibrary.playback.ViewingModels.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ViewingService {
    private final ObjectProvider<ViewingMapper> viewing;
    private final ObjectProvider<CatalogMapper> catalog;
    private final ObjectProvider<PersonalMapper> personal;
    private final ObjectProvider<TransactionTemplate> transactions;

    public ViewingService(ObjectProvider<ViewingMapper> viewing, ObjectProvider<CatalogMapper> catalog,
                          ObjectProvider<PersonalMapper> personal, ObjectProvider<TransactionTemplate> transactions) {
        this.viewing = viewing;
        this.catalog = catalog;
        this.personal = personal;
        this.transactions = transactions;
    }

    public Stats stats(long workId) {
        anime(workId);
        return db().stats(workId);
    }

    public Progress progress(long userId, long workId) {
        anime(workId);
        var db = db();
        return new Progress(db.latest(userId, workId), db.completed(userId, workId));
    }

    public Started start(Long userId, String address, long workId, Start form) {
        var db = db();
        return transactions.getObject().execute(tx -> {
            lockMember(userId);
            requireSource(db, workId, form.sourceId());
            String id = UUID.randomUUID().toString();
            db.expire();
            db.start(id, viewer(userId, address), userId, form.sourceId(), form.positionMs());
            return new Started(id);
        });
    }

    public boolean sample(Long userId, String address, long workId, String id, Sample form) {
        var db = db();
        return Boolean.TRUE.equals(transactions.getObject().execute(tx -> {
            lockMember(userId);
            String viewer = viewer(userId, address);
            var session = db.lockSession(id, viewer);
            if (session == null) throw new CatalogProblem(CatalogProblem.Reason.PLAYBACK_NOT_FOUND);
            var source = requireSource(db, workId, session.sourceId());
            long position = Math.min(form.positionMs(), form.durationMs());
            long elapsed = Duration.between(session.lastSampleAt(), OffsetDateTime.now()).toMillis();
            long delta = validAdvance(session.lastPositionMs(), position, elapsed, form);
            long watched = session.watchedMs() + Math.min(Math.max(0, elapsed), delta);
            long referenceDuration = Math.max(form.durationMs(), source.durationSeconds() * 1000L);
            boolean qualifies = !session.settled() && watched >= Math.min(15_000, referenceDuration / 2);
            boolean counted = false;
            if (qualifies) {
                db.prepareTotal(workId, source.edition(), source.episodeNo(), viewer);
                counted = db.countPlay(workId, source.edition(), source.episodeNo(), viewer) > 0;
            }
            db.sample(id, position, watched, session.settled() || qualifies ? 1 : 0);
            if (userId != null && source.edition() == Edition.EPISODE && delta > 0) {
                saveProgress(db, userId, source, session.lastPositionMs(), position, form.durationMs());
            }
            return counted;
        }));
    }

    static long validAdvance(long previous, long position, long elapsed, Sample form) {
        long delta = position - previous;
        // Up to 2x speed, with transport jitter. A seek or long sampling gap starts a new interval.
        return form.playing() && !form.discontinuity() && elapsed >= 250 && elapsed <= 45_000
                && delta > 0 && delta <= elapsed * 2 + 750 ? delta : 0;
    }

    private void saveProgress(ViewingMapper db, long userId, Source source, long previous, long position, long duration) {
        long workId = source.workId();
        int episode = source.episodeNo();
        var old = db.recording(userId, workId, episode);
        var spans = combine(old == null ? "" : old.coverage(), previous, position, duration);
        boolean complete = completed(spans, position, duration)
                && duration + 2_000 >= source.durationSeconds() * 1000L;
        String coverage = spans.stream().map(span -> span.begin() + ":" + span.end()).collect(Collectors.joining(","));
        db.saveProgress(userId, workId, episode, source.episodeId(), position, duration, coverage, complete ? 1 : 0);
        if (old == null || complete && !old.completed()) {
            int count = db.completed(userId, workId);
            var total = anime(workId).episodeCount();
            String status = total != null && total > 0 && count >= total ? "COMPLETED" : "WATCHING";
            db.history(userId, workId, status, count);
            db.updateFollowProgress(userId, workId, status, count);
        }
    }

    static boolean completed(ArrayList<Span> spans, long position, long duration) {
        long covered = spans.stream().mapToLong(span -> span.end() - span.begin()).sum();
        return position >= duration * 95 / 100 && covered >= duration * 90 / 100;
    }

    record Span(long begin, long end) {}

    static ArrayList<Span> combine(String coverage, long begin, long end, long duration) {
        var spans = new ArrayList<Span>();
        if (!coverage.isBlank()) {
            for (String range : coverage.split(",")) {
                int split = range.indexOf(':');
                long from = Long.parseLong(range.substring(0, split));
                long to = Math.min(duration, Long.parseLong(range.substring(split + 1)));
                if (from < to) spans.add(new Span(from, to));
            }
        }
        spans.add(new Span(begin, end));
        spans.sort(Comparator.comparingLong(Span::begin));
        var merged = new ArrayList<Span>();
        for (var span : spans) {
            if (!merged.isEmpty() && merged.getLast().end() >= span.begin()) {
                var last = merged.removeLast();
                merged.add(new Span(last.begin(), Math.max(last.end(), span.end())));
            } else merged.add(span);
        }
        return merged;
    }

    private CatalogMapper.WorkRow anime(long workId) {
        var mapper = catalog.getIfAvailable();
        if (mapper == null) throw AccountService.unavailable();
        var work = mapper.findWork(workId);
        if (work == null || work.type() != WorkType.ANIME) {
            throw new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
        }
        return work;
    }

    private ViewingMapper db() {
        var mapper = viewing.getIfAvailable();
        if (mapper == null) throw AccountService.unavailable();
        return mapper;
    }

    private void lockMember(Long userId) {
        if (userId != null && personal.getObject().lockMember(userId) == null) {
            throw new AccountProblem(HttpStatus.UNAUTHORIZED, Code.LOGIN_REQUIRED, "请先登录");
        }
    }

    private static Source requireSource(ViewingMapper db, long workId, long sourceId) {
        var source = db.source(workId, sourceId);
        if (source == null || !PlaybackService.permitted(source.url())) {
            throw new CatalogProblem(CatalogProblem.Reason.PLAYBACK_NOT_FOUND);
        }
        return source;
    }

    private static String viewer(Long userId, String address) {
        if (userId != null) return "u:" + userId;
        try {
            byte[] key = MessageDigest.getInstance("SHA-256").digest(address.getBytes(StandardCharsets.UTF_8));
            return "g:" + HexFormat.of().formatHex(key);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
