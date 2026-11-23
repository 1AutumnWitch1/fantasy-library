package com.fantasylibrary.api;

import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.recommendation.RecommendationModels.Feed;
import com.fantasylibrary.service.RecommendationService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationController {
    private final RecommendationService recommendations;

    public RecommendationController(RecommendationService recommendations) {
        this.recommendations = recommendations;
    }

    @GetMapping("/api/recommendations")
    public ApiResponse<Feed> publicFeed(@RequestParam(required = false) WorkType type) {
        return ApiResponse.success(recommendations.feed(type, null));
    }

    @GetMapping("/api/me/recommendations")
    public ResponseEntity<ApiResponse<Feed>> memberFeed(@RequestParam(required = false) WorkType type,
                                                       @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(recommendations.feed(type, userId)));
    }
}
