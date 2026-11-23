package com.fantasylibrary.api;

import com.fantasylibrary.catalog.model.Catalog.AnimeItem;
import com.fantasylibrary.catalog.model.Catalog.Category;
import com.fantasylibrary.catalog.model.Catalog.GameItem;
import com.fantasylibrary.catalog.model.Catalog.Page;
import com.fantasylibrary.catalog.model.Catalog.WorkDetail;
import com.fantasylibrary.catalog.model.CatalogQuery;
import com.fantasylibrary.service.CatalogService;
import com.fantasylibrary.catalog.model.Catalog.Tag;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.success(catalog.categories());
    }

    @GetMapping("/tags")
    public ApiResponse<List<Tag>> tags(@RequestParam WorkType type) {
        return ApiResponse.success(catalog.availableTags(type));
    }

    @GetMapping("/anime")
    public ApiResponse<Page<AnimeItem>> anime(@Valid @ModelAttribute CatalogQuery query) {
        return ApiResponse.success(catalog.anime(query));
    }

    @GetMapping("/games")
    public ApiResponse<Page<GameItem>> games(@Valid @ModelAttribute CatalogQuery query) {
        return ApiResponse.success(catalog.games(query));
    }

    @GetMapping("/works/{id}")
    public ApiResponse<WorkDetail> detail(@PathVariable @Positive(message = "作品 ID 必须为正整数") long id) {
        return ApiResponse.success(catalog.detail(id));
    }
}
