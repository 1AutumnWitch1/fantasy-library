package com.fantasylibrary.api;

import com.fantasylibrary.playback.ViewingModels.*;
import com.fantasylibrary.service.ViewingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class ViewingController {
    private final ViewingService viewing;

    public ViewingController(ViewingService viewing) {
        this.viewing = viewing;
    }

    @GetMapping("/api/works/{workId}/playback-stats")
    public ApiResponse<Stats> statistics(@PathVariable @Positive long workId) {
        return ApiResponse.success(viewing.stats(workId));
    }

    @GetMapping("/api/me/works/{workId}/viewing-progress")
    public ApiResponse<Progress> progress(@AuthenticationPrincipal Long userId, @PathVariable @Positive long workId) {
        return ApiResponse.success(viewing.progress(userId, workId));
    }

    @PostMapping("/api/works/{workId}/playback-sessions")
    public ApiResponse<Started> start(@AuthenticationPrincipal Long userId, HttpServletRequest request,
                                      @PathVariable @Positive long workId, @Valid @RequestBody Start form) {
        return ApiResponse.success(viewing.start(userId, request.getRemoteAddr(), workId, form));
    }

    record Sampled(boolean counted) {}

    @PutMapping("/api/works/{workId}/playback-sessions/{id}")
    public ApiResponse<Sampled> sample(@AuthenticationPrincipal Long userId, HttpServletRequest request,
                                       @PathVariable @Positive long workId, @PathVariable UUID id,
                                       @Valid @RequestBody Sample form) {
        return ApiResponse.success(new Sampled(viewing.sample(userId, request.getRemoteAddr(), workId, id.toString(), form)));
    }
}
