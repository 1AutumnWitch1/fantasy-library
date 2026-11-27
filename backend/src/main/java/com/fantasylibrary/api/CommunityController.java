package com.fantasylibrary.api;

import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.Catalog.Page;
import com.fantasylibrary.community.CommunityModels.*;
import com.fantasylibrary.service.CommunityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class CommunityController {
    private final CommunityService community;

    public CommunityController(CommunityService community) {
        this.community = community;
    }

    @GetMapping("/api/community/posts")
    public ApiResponse<Page<Post>> posts(@RequestParam(required = false) Topic topic,
            @RequestParam(required = false) @Positive Long workId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "8") @Min(1) @Max(30) int size,
            @RequestParam(defaultValue = "RECENT") PostOrder sort) {
        return ApiResponse.success(community.posts(topic, workId, page, size, sort));
    }

    @GetMapping("/api/community/posts/{id}")
    public ResponseEntity<ApiResponse<PostDetail>> post(@PathVariable @Positive long id,
                                                       @RequestParam(defaultValue = "false") boolean view,
                                                       HttpServletRequest request) {
        var detail = view && "GET".equals(request.getMethod()) ? community.view(id) : community.post(id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(detail));
    }

    @GetMapping("/api/community/posts/{id}/comments")
    public ApiResponse<Page<Comment>> comments(@PathVariable @Positive long id,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(30) int size) {
        return ApiResponse.success(community.comments(id, page, size));
    }

    @GetMapping("/api/works/{id}/comments")
    public ApiResponse<Page<Comment>> screeningComments(@PathVariable @Positive long id,
            @RequestParam(defaultValue = "ANIME") WorkType type,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(30) int size) {
        return ApiResponse.success(community.screeningComments(id, type, page, size));
    }

    @GetMapping({"/api/works/{id}/comment-threads", "/api/me/works/{id}/comment-threads"})
    public ResponseEntity<ApiResponse<Page<ScreeningThread>>> screeningThreads(
            @AuthenticationPrincipal Long viewer, @PathVariable @Positive long id,
            @RequestParam(defaultValue = "ANIME") WorkType type,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(community.screeningThreads(id, type, viewer, page, size)));
    }

    @GetMapping({"/api/works/{id}/comments/{root}/replies", "/api/me/works/{id}/comments/{root}/replies"})
    public ResponseEntity<ApiResponse<Page<ScreeningComment>>> screeningReplies(
            @AuthenticationPrincipal Long viewer, @PathVariable @Positive long id, @PathVariable @Positive long root,
            @RequestParam(defaultValue = "ANIME") WorkType type,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "6") @Min(1) @Max(30) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(community.screeningReplies(id, type, root, viewer, page, size)));
    }

    @PutMapping("/api/me/works/{id}/comments/{comment}/like")
    public ApiResponse<Reaction> likeScreeningComment(@AuthenticationPrincipal Long user,
            @PathVariable @Positive long id, @PathVariable @Positive long comment,
            @RequestParam(defaultValue = "ANIME") WorkType type, @Valid @RequestBody CommentLike form) {
        return ApiResponse.success(community.screenReaction(id, type, comment, user, form.liked()));
    }

    @PostMapping("/api/me/works/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Created> screeningComment(@AuthenticationPrincipal Long user, @PathVariable @Positive long id,
                                                 @RequestParam(defaultValue = "ANIME") WorkType type,
                                                 @Valid @RequestBody CommentForm form) {
        return ApiResponse.success(community.screeningComment(id, type, user, form));
    }

    @GetMapping("/api/community/media/{key}")
    public ResponseEntity<Resource> media(@PathVariable String key) {
        var file = community.image(key);
        return ResponseEntity.ok().contentType(key.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore()).body(file);
    }

    @PostMapping(value = "/api/me/community/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Created> publish(@AuthenticationPrincipal Long user, @Valid @RequestPart("post") PostForm form,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ApiResponse.success(community.publish(user, form, images == null ? List.of() : images));
    }

    @PutMapping(value = "/api/me/community/posts/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> edit(@AuthenticationPrincipal Long user, @PathVariable @Positive long id,
            @Valid @RequestPart("post") PostForm form,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        community.edit(id, user, form, images == null ? List.of() : images);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/api/me/community/posts/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long user, @PathVariable @Positive long id) {
        community.delete(id, user);
        return ApiResponse.success(null);
    }

    @PostMapping("/api/me/community/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Created> comment(@AuthenticationPrincipal Long user, @PathVariable @Positive long id,
                                       @Valid @RequestBody CommentForm form) {
        return ApiResponse.success(community.comment(id, user, form));
    }

    @GetMapping("/api/me/community/posts/{id}/like")
    public ApiResponse<Reaction> reaction(@AuthenticationPrincipal Long user, @PathVariable @Positive long id) {
        return ApiResponse.success(community.reaction(id, user));
    }

    @PutMapping("/api/me/community/posts/{id}/like")
    public ApiResponse<Reaction> like(@AuthenticationPrincipal Long user, @PathVariable @Positive long id) {
        return ApiResponse.success(community.react(id, user, true));
    }

    @DeleteMapping("/api/me/community/posts/{id}/like")
    public ApiResponse<Reaction> unlike(@AuthenticationPrincipal Long user, @PathVariable @Positive long id) {
        return ApiResponse.success(community.react(id, user, false));
    }

    @PostMapping("/api/me/community/reports")
    public ApiResponse<Void> report(@AuthenticationPrincipal Long user, @Valid @RequestBody ReportForm form) {
        community.report(user, form);
        return ApiResponse.success(null);
    }

    @GetMapping("/api/admin/community/reports")
    public ApiResponse<Page<Report>> reports(@RequestParam(defaultValue = "1") @Min(1) int page,
                                             @RequestParam(defaultValue = "10") @Min(1) @Max(30) int size) {
        return ApiResponse.success(community.reports(page, size));
    }

    @PutMapping("/api/admin/community/reports/{id}")
    public ApiResponse<Void> review(@AuthenticationPrincipal Long admin, @PathVariable @Positive long id,
                                   @Valid @RequestBody ReviewForm form) {
        community.review(admin, id, form.action());
        return ApiResponse.success(null);
    }
}
