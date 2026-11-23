package com.fantasylibrary.api;

import com.fantasylibrary.playback.PlaybackModels.Episode;
import com.fantasylibrary.service.PlaybackService;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PlaybackController {
    private final PlaybackService playback;

    public PlaybackController(PlaybackService playback) {
        this.playback = playback;
    }

    @GetMapping("/api/media/playback/{fileName}")
    public ResponseEntity<Resource> media(@PathVariable String fileName) {
        var resource = playback.media(fileName);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(fileName.endsWith(".webm") ? "video/webm" : "video/mp4"))
                .header("X-Content-Type-Options", "nosniff").body(resource);
    }

    @GetMapping("/api/works/{id}/episodes")
    public ApiResponse<List<Episode>> episodes(@PathVariable @Positive long id,
                                               @RequestParam(defaultValue = "false") boolean allNodes) {
        return ApiResponse.success(playback.episodes(id, allNodes));
    }
}
