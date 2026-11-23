package com.fantasylibrary.api;

import com.fantasylibrary.playback.PlaybackEditorialModels.Episode;
import com.fantasylibrary.playback.PlaybackEditorialModels.EpisodeDraft;
import com.fantasylibrary.playback.PlaybackEditorialModels.Node;
import com.fantasylibrary.playback.PlaybackEditorialModels.NodeDraft;
import com.fantasylibrary.playback.PlaybackEditorialModels.SourceDraft;
import com.fantasylibrary.service.PlaybackEditorialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/library/works/{workId}/playback")
public class PlaybackEditorialController {
    private final PlaybackEditorialService editorial;

    public PlaybackEditorialController(PlaybackEditorialService editorial) {
        this.editorial = editorial;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Episode>>> episodes(@PathVariable @Positive long workId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(editorial.episodes(workId)));
    }

    @GetMapping("/nodes")
    public ResponseEntity<ApiResponse<List<Node>>> nodes(@PathVariable @Positive long workId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(editorial.nodes(workId)));
    }

    @PostMapping("/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createNode(@PathVariable @Positive long workId, @Valid @RequestBody NodeDraft draft) {
        editorial.saveNode(workId, null, draft);
        return ApiResponse.success(null);
    }

    @PutMapping("/nodes/{nodeId}")
    public ApiResponse<Void> editNode(@PathVariable @Positive long workId, @PathVariable @Positive long nodeId,
                                      @Valid @RequestBody NodeDraft draft) {
        editorial.saveNode(workId, nodeId, draft);
        return ApiResponse.success(null);
    }

    @PostMapping("/episodes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createEpisode(@PathVariable @Positive long workId, @Valid @RequestBody EpisodeDraft draft) {
        editorial.saveEpisode(workId, null, draft);
        return ApiResponse.success(null);
    }

    @PutMapping("/episodes/{episodeId}")
    public ApiResponse<Void> editEpisode(@PathVariable @Positive long workId, @PathVariable @Positive long episodeId,
                                         @Valid @RequestBody EpisodeDraft draft) {
        editorial.saveEpisode(workId, episodeId, draft);
        return ApiResponse.success(null);
    }

    @PostMapping("/episodes/{episodeId}/sources")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createSource(@PathVariable @Positive long workId, @PathVariable @Positive long episodeId,
                                         @Valid @RequestBody SourceDraft draft) {
        editorial.saveSource(workId, episodeId, null, draft);
        return ApiResponse.success(null);
    }

    @PutMapping("/episodes/{episodeId}/sources/{sourceId}")
    public ApiResponse<Void> editSource(@PathVariable @Positive long workId, @PathVariable @Positive long episodeId,
                                        @PathVariable @Positive long sourceId, @Valid @RequestBody SourceDraft draft) {
        editorial.saveSource(workId, episodeId, sourceId, draft);
        return ApiResponse.success(null);
    }
}
