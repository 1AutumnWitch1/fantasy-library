package com.fantasylibrary.api;

import com.fantasylibrary.download.GameDownload;
import com.fantasylibrary.service.GameDownloadService;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class GameDownloadController {
    private final GameDownloadService downloads;

    public GameDownloadController(GameDownloadService downloads) {
        this.downloads = downloads;
    }

    @GetMapping("/api/works/{id}/downloads")
    public ResponseEntity<ApiResponse<List<GameDownload>>> nodes(@PathVariable @Positive long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(downloads.nodes(id)));
    }

    @GetMapping("/api/game-downloads/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable @Positive long id) {
        var download = downloads.file(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.name(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noCache())
                .body(download.resource());
    }
}
