package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.admin.WorkEditorialModels.*;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogImagePaths;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.persistence.mapper.WorkEditorialMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class WorkEditorialService {
    private final ObjectProvider<WorkEditorialMapper> mappers;
    private final ObjectProvider<TransactionTemplate> transactions;
    private final ResourceRetirementService retirement;

    public WorkEditorialService(ObjectProvider<WorkEditorialMapper> mappers, ObjectProvider<TransactionTemplate> transactions,
                                ResourceRetirementService retirement) {
        this.mappers = mappers;
        this.transactions = transactions;
        this.retirement = retirement;
    }

    public Options options() {
        return options(mapper());
    }

    public Archive archive(long id) {
        var db = mapper();
        var entry = db.find(id);
        if (entry == null) throw missing();
        var anime = entry.type() == WorkType.ANIME ? db.anime(id) : null;
        var gameRow = entry.type() == WorkType.GAME ? db.game(id) : null;
        var game = gameRow == null ? null : new Game(gameRow.developer(), gameRow.ageRating(), db.platformIds(id));
        return new Archive(entry, db.tagIds(id), anime, game, db.images(id, "COVER").stream().findFirst().orElse(null),
                db.images(id, "GALLERY"));
    }

    public Created save(Long id, Draft draft) {
        var db = mapper();
        validate(draft, options(db));
        try {
            return transactions.getObject().execute(status -> {
                retirement.lockCatalogReferences();
                long workId;
                if (id == null) {
                    db.create(draft);
                    workId = db.idBySlug(draft.slug());
                } else {
                    var type = db.lockType(id);
                    if (type == null) throw missing();
                    if (type != draft.type()) throw invalid("已登记作品不能更改作品类型");
                    db.update(id, draft);
                    workId = id;
                }
                if (draft.type() == WorkType.ANIME) {
                    db.saveAnime(workId, draft.anime());
                } else {
                    db.saveGame(workId, draft.game());
                    replacePlatforms(db, workId, new LinkedHashSet<>(draft.game().platformIds()));
                }

                var tagIds = new LinkedHashSet<>(draft.tagIds());
                if (!tagIds.equals(Set.copyOf(db.tagIds(workId)))) {
                    db.clearTags(workId);
                    for (long tag : tagIds) db.addTag(workId, tag);
                }
                var oldCover = db.images(workId, "COVER");
                var oldGallery = db.images(workId, "GALLERY");
                var newCover = draft.cover() == null ? List.<Image>of() : List.of(draft.cover());
                if (!oldCover.equals(newCover) || !oldGallery.equals(draft.gallery())) {
                    var oldPaths = Stream.concat(oldCover.stream(), oldGallery.stream())
                            .map(Image::url).collect(java.util.stream.Collectors.toSet());
                    Stream.concat(newCover.stream(), draft.gallery().stream()).map(Image::url).distinct()
                            .filter(path -> !oldPaths.contains(path)).forEach(retirement::requireCatalogAttachable);
                    db.clearImages(workId);
                    if (draft.cover() != null) db.addImage(workId, "COVER", draft.cover(), 0);
                    for (int i = 0; i < draft.gallery().size(); i++) db.addImage(workId, "GALLERY", draft.gallery().get(i), i);
                }
                return new Created(workId);
            });
        } catch (DuplicateKeyException error) {
            throw new AccountProblem(HttpStatus.CONFLICT, Code.INVALID_ARGUMENT, "作品标识已被使用");
        }
    }

    private static void replacePlatforms(WorkEditorialMapper db, long id, Set<Long> desired) {
        var existing = db.platformIds(id);
        for (long platform : existing) {
            if (!desired.contains(platform) && db.removePlatform(id, platform) == 0) {
                throw new AccountProblem(HttpStatus.CONFLICT, Code.INVALID_ARGUMENT, "该平台仍有下载节点，请保留该平台");
            }
        }
        for (long platform : desired) {
            if (!existing.contains(platform)) db.addPlatform(id, platform);
        }
    }

    private static void validate(Draft draft, Options choices) {
        if (choices.categories().stream().noneMatch(category -> category.id() == draft.categoryId())) {
            throw invalid("请选择有效的分类");
        }
        if (!ids(choices.tags()).containsAll(draft.tagIds())) throw invalid("请选择有效的标签");
        if (draft.type() == WorkType.ANIME ? draft.anime() == null || draft.game() != null
                : draft.game() == null || draft.anime() != null) {
            throw invalid("请填写与作品类型对应的资料");
        }
        if (draft.game() != null && !ids(choices.platforms()).containsAll(draft.game().platformIds())) {
            throw invalid("请选择有效的游戏平台");
        }
        if (draft.cover() != null) checkImage(draft.cover());
        draft.gallery().forEach(WorkEditorialService::checkImage);
    }

    static void checkImage(Image image) {
        try {
            var url = URI.create(image.url());
            if ("https".equalsIgnoreCase(url.getScheme()) && url.getHost() != null && url.getRawUserInfo() == null) return;
            if (!url.isAbsolute() && url.getRawAuthority() == null && url.getRawQuery() == null && url.getRawFragment() == null
                    && CatalogImagePaths.objectKey(url.getRawPath()) != null) return;
        } catch (IllegalArgumentException ignored) {
            // Invalid input is reported below using the API error format.
        }
        throw invalid("图片地址须使用 HTTPS 或本站馆藏图片路径");
    }

    private static Set<Long> ids(List<Option> options) {
        return options.stream().map(Option::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Options options(WorkEditorialMapper db) {
        return new Options(db.categories(), db.tags(), db.platforms());
    }

    private WorkEditorialMapper mapper() {
        var db = mappers.getIfAvailable();
        if (db == null) throw new CatalogProblem(CatalogProblem.Reason.DATABASE_UNAVAILABLE);
        return db;
    }

    private static CatalogProblem missing() {
        return new CatalogProblem(CatalogProblem.Reason.WORK_NOT_FOUND);
    }

    private static AccountProblem invalid(String message) {
        return new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, message);
    }
}
