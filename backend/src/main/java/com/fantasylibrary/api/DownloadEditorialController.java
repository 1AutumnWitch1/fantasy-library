package com.fantasylibrary.api;

import com.fantasylibrary.download.DownloadEditorialModels.Archive;
import com.fantasylibrary.download.DownloadEditorialModels.Draft;
import com.fantasylibrary.service.DownloadEditorialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/library/works/{workId}/downloads")
public class DownloadEditorialController {
    private final DownloadEditorialService editorial;

    public DownloadEditorialController(DownloadEditorialService editorial) {
        this.editorial = editorial;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Archive>> archive(@PathVariable @Positive long workId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(editorial.archive(workId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> create(@PathVariable @Positive long workId, @Valid @RequestBody Draft draft) {
        editorial.save(workId, null, draft);
        return ApiResponse.success(null);
    }

    @PutMapping("/{nodeId}")
    public ApiResponse<Void> edit(@PathVariable @Positive long workId, @PathVariable @Positive long nodeId,
                                  @Valid @RequestBody Draft draft) {
        editorial.save(workId, nodeId, draft);
        return ApiResponse.success(null);
    }
}
