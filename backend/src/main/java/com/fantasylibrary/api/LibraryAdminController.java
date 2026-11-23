package com.fantasylibrary.api;

import com.fantasylibrary.account.AccountModels.Registration;
import com.fantasylibrary.admin.LibraryAdminModels.*;
import com.fantasylibrary.catalog.model.Catalog.Page;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.service.AccountService;
import com.fantasylibrary.service.CommunityService;
import com.fantasylibrary.service.LibraryAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/library")
public class LibraryAdminController {
    private final LibraryAdminService library;
    private final CommunityService community;
    private final AccountService accounts;

    public LibraryAdminController(LibraryAdminService library, CommunityService community, AccountService accounts) {
        this.library = library;
        this.community = community;
        this.accounts = accounts;
    }

    @GetMapping("/works")
    public ResponseEntity<ApiResponse<Page<Work>>> works(@RequestParam WorkType type,
            @RequestParam(required = false) @Size(max = 80) String q,
            @RequestParam(required = false) Boolean listed,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(30) int size) {
        return privateRead(library.works(type, q, listed, page, size));
    }

    @GetMapping("/works/{id}/nodes")
    public ResponseEntity<ApiResponse<List<Node>>> nodes(@PathVariable @Positive long id) {
        return privateRead(library.nodes(id));
    }

    @PutMapping("/works/{id}/publication")
    public ApiResponse<Void> work(@PathVariable @Positive long id, @Valid @RequestBody Publication form) {
        library.publishWork(id, form.listed());
        return ApiResponse.success(null);
    }

    @PutMapping("/nodes/{kind}/{id}/publication")
    public ApiResponse<ResourceChange> node(@PathVariable NodeKind kind, @PathVariable @Positive long id,
                                            @Valid @RequestBody NodePublication form) {
        return ApiResponse.success(library.publishNode(kind, id, form));
    }

    @GetMapping("/forum")
    public ResponseEntity<ApiResponse<ForumStatus>> forum() {
        return privateRead(new ForumStatus(community.isOpen()));
    }

    @PutMapping("/forum")
    public ApiResponse<Void> setForum(@Valid @RequestBody ForumStatus form) {
        library.forumOpen(form.open());
        return ApiResponse.success(null);
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<Post>>> posts(@RequestParam(required = false) @Size(max = 80) String q,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(30) int size) {
        return privateRead(library.posts(q, page, size));
    }

    @PutMapping("/posts/{id}/publication")
    public ApiResponse<Void> post(@PathVariable @Positive long id, @Valid @RequestBody Publication form) {
        community.publishExistingPost(id, form.listed());
        return ApiResponse.success(null);
    }

    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<Page<Comment>>> comments(@RequestParam(required = false) @Size(max = 80) String q,
            @RequestParam(required = false) CommentScope scope,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(30) int size) {
        return privateRead(library.comments(q, scope, page, size));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> removeComment(@AuthenticationPrincipal Long actor, @PathVariable @Positive long id) {
        community.removeComment(actor, id);
        return ApiResponse.success(null);
    }

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<Page<Member>>> members(@RequestParam(required = false) @Size(max = 80) String q,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(30) int size) {
        return privateRead(library.members(q, page, size));
    }

    @PostMapping("/members")
    public ApiResponse<Void> addMember(@Valid @RequestBody Registration form) {
        accounts.register(form);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/members/{id}")
    public ApiResponse<Void> removeMember(@PathVariable @Positive long id) {
        library.removeMember(id);
        return ApiResponse.success(null);
    }

    private static <T> ResponseEntity<ApiResponse<T>> privateRead(T data) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(data));
    }
}
