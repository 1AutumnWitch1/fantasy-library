package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountModels.Profile;
import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.community.CommunityImages;
import com.fantasylibrary.community.CommunityProblem;
import com.fantasylibrary.persistence.mapper.AccountMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarService {
    private final ObjectProvider<AccountMapper> accounts;
    private final ObjectProvider<TransactionTemplate> transactions;
    private final CommunityImages images;

    public AvatarService(ObjectProvider<AccountMapper> accounts, ObjectProvider<TransactionTemplate> transactions,
                         CommunityImages images) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.images = images;
    }

    public Resource load(String key) {
        if (!CommunityImages.valid(key) || mapper().referencesAvatar(key) == 0) throw CommunityProblem.notFound();
        return images.loadAvatar(key);
    }

    public Profile upload(long user, MultipartFile file) {
        return change(user, images.saveAvatar(file));
    }

    public Profile remove(long user) {
        return change(user, null);
    }

    private Profile change(long user, String newKey) {
        Change result;
        try {
            var db = mapper();
            result = transactions.getObject().execute(status -> {
                String oldKey = db.lockAvatar(user);
                if (db.updateAvatar(user, newKey) == 0) {
                    throw new AccountProblem(HttpStatus.UNAUTHORIZED, Code.LOGIN_REQUIRED, "请重新登录");
                }
                return new Change(oldKey, db.profile(user));
            });
        } catch (RuntimeException failure) {
            removeFile(newKey);
            throw failure;
        }
        removeFile(result.oldKey());
        return result.profile();
    }

    private void removeFile(String key) {
        images.removeAvatar(key);
    }

    private AccountMapper mapper() {
        var db = accounts.getIfAvailable();
        if (db == null) throw AccountService.unavailable();
        return db;
    }

    private record Change(String oldKey, Profile profile) {}
}
