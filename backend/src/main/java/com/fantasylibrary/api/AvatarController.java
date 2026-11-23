package com.fantasylibrary.api;

import com.fantasylibrary.account.AccountModels.Profile;
import com.fantasylibrary.service.AvatarService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AvatarController {
    private final AvatarService avatars;

    public AvatarController(AvatarService avatars) {
        this.avatars = avatars;
    }

    @GetMapping("/api/avatars/{key}")
    public ResponseEntity<Resource> image(@PathVariable String key) {
        return ResponseEntity.ok()
                .contentType(key.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore()).body(avatars.load(key));
    }

    @PutMapping(value = "/api/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Profile> upload(@AuthenticationPrincipal Long user, @RequestPart("avatar") MultipartFile file) {
        return ApiResponse.success(avatars.upload(user, file));
    }

    @DeleteMapping("/api/me/avatar")
    public ApiResponse<Profile> remove(@AuthenticationPrincipal Long user) {
        return ApiResponse.success(avatars.remove(user));
    }
}
