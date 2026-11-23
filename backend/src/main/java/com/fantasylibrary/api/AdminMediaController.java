package com.fantasylibrary.api;

import com.fantasylibrary.media.AdminMediaService;
import com.fantasylibrary.media.AdminMediaService.Uploaded;
import com.fantasylibrary.media.AdminMediaService.PackageUpload;
import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.api.ApiResponse.Code;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/library/media")
public class AdminMediaController {
    private final AdminMediaService media;

    public AdminMediaController(AdminMediaService media) {
        this.media = media;
    }

    @PostMapping(value = "/catalog", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Uploaded> image(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(media.uploadImage(file));
    }

    @PostMapping(value = "/playback", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Uploaded> video(HttpServletRequest request) {
        return ApiResponse.success(media.uploadVideo(request.getContentLengthLong(), request::getInputStream));
    }

    @PostMapping(value = "/download", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PackageUpload> gamePackage(@RequestHeader("X-File-Name") String encodedName,
                                                   HttpServletRequest request) {
        String name;
        try {
            name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException failure) {
            throw new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, "安装包文件名无效");
        }
        return ApiResponse.success(media.uploadGamePackage(name, request.getContentLengthLong(), request::getInputStream));
    }

    @PostMapping(value = "/playback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Uploaded> smallVideo(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(media.uploadVideo(file));
    }
}
