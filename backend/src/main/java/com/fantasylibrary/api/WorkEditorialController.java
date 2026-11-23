package com.fantasylibrary.api;

import com.fantasylibrary.admin.WorkEditorialModels.*;
import com.fantasylibrary.service.ResourceRetirementService;
import com.fantasylibrary.service.WorkEditorialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/library")
public class WorkEditorialController {
    private final WorkEditorialService editorial;
    private final ResourceRetirementService retirement;

    public WorkEditorialController(WorkEditorialService editorial, ResourceRetirementService retirement) {
        this.editorial = editorial;
        this.retirement = retirement;
    }

    @GetMapping("/work-options")
    public ResponseEntity<ApiResponse<Options>> options() {
        return privateRead(editorial.options());
    }

    @GetMapping("/works/{id}/details")
    public ResponseEntity<ApiResponse<Archive>> archive(@PathVariable @Positive long id) {
        return privateRead(editorial.archive(id));
    }

    @PostMapping("/works")
    public ResponseEntity<ApiResponse<Created>> create(@Valid @RequestBody Draft draft) {
        return ResponseEntity.status(201).body(ApiResponse.success(editorial.save(null, draft)));
    }

    @PutMapping("/works/{id}/details")
    public ApiResponse<Created> update(@PathVariable @Positive long id, @Valid @RequestBody Draft draft) {
        return ApiResponse.success(editorial.save(id, draft));
    }

    @DeleteMapping("/works/{id}")
    public ApiResponse<WorkRemoval> remove(@PathVariable @Positive long id) {
        return ApiResponse.success(retirement.removeWork(id));
    }

    private static <T> ResponseEntity<ApiResponse<T>> privateRead(T data) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(data));
    }
}
