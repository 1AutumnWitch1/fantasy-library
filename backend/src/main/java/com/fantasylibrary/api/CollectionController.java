package com.fantasylibrary.api;

import com.fantasylibrary.account.AccountModels.Entry;
import com.fantasylibrary.account.AccountModels.SaveWork;
import com.fantasylibrary.account.AccountModels.SavedWork;
import com.fantasylibrary.service.CollectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/collection")
public class CollectionController {
    private final CollectionService collection;

    public CollectionController(CollectionService collection) {
        this.collection = collection;
    }

    @GetMapping
    public ApiResponse<List<SavedWork>> list(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(collection.list(userId));
    }

    @GetMapping("/{workId}")
    public ApiResponse<Entry> item(@AuthenticationPrincipal Long userId, @PathVariable @Positive long workId) {
        return ApiResponse.success(new Entry(collection.find(userId, workId)));
    }

    @PutMapping("/{workId}")
    public ApiResponse<SavedWork> save(@AuthenticationPrincipal Long userId, @PathVariable @Positive long workId,
                                       @Valid @RequestBody SaveWork form) {
        return ApiResponse.success(collection.save(userId, workId, form));
    }

    @DeleteMapping("/{workId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal Long userId, @PathVariable @Positive long workId) {
        collection.remove(userId, workId);
        return ApiResponse.success(null);
    }
}
