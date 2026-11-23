package com.fantasylibrary.api;

import com.fantasylibrary.catalog.model.Catalog.Page;
import com.fantasylibrary.personal.PersonalModels.Inbox;
import com.fantasylibrary.personal.PersonalModels.WatchEvent;
import com.fantasylibrary.service.PersonalService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class PersonalController {
    private final PersonalService service;

    public PersonalController(PersonalService service) {
        this.service = service;
    }

    @GetMapping("/api/me/reading-history")
    public ResponseEntity<ApiResponse<Page<WatchEvent>>> history(@AuthenticationPrincipal Long user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "8") @Min(1) @Max(30) int size) {
        return privateResponse(service.history(user, page, size));
    }

    @GetMapping("/api/me/inbox")
    public ResponseEntity<ApiResponse<Inbox>> inbox(@AuthenticationPrincipal Long user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "8") @Min(1) @Max(30) int size) {
        return privateResponse(service.inbox(user, page, size));
    }

    @PutMapping("/api/me/inbox/{id}/read")
    public ApiResponse<Void> read(@AuthenticationPrincipal Long user, @PathVariable @Positive long id) {
        service.markRead(user, id);
        return ApiResponse.success(null);
    }

    @PutMapping("/api/me/inbox/read")
    public ApiResponse<Void> readAll(@AuthenticationPrincipal Long user) {
        service.markAllRead(user);
        return ApiResponse.success(null);
    }

    private static <T> ResponseEntity<ApiResponse<T>> privateResponse(T data) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(data));
    }
}
