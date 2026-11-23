package com.fantasylibrary.catalog.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.stream.Stream;

public record CatalogQuery(
        @Min(value = 1, message = "页码必须从 1 开始") Integer page,
        @Min(value = 1, message = "每页数量至少为 1")
        @Max(value = 100, message = "每页最多 100 条") Integer size,
        @Pattern(regexp = SLUG_PATTERN, message = "分类标识格式不正确") String category,
        @Pattern(regexp = SLUG_PATTERN, message = "标签标识格式不正确") String tag,
        @Size(max = 100, message = "关键词最多 100 个字符") String q,
        @Size(max = 100, message = "最多可查询 100 个标签")
        List<@NotNull @Pattern(regexp = SLUG_PATTERN, message = "标签标识格式不正确") String> tags
) {
    private static final String SLUG_PATTERN = "(?=.{1,60}$)[a-z0-9]+(?:-[a-z0-9]+)*";

    public CatalogQuery {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
        tags = tags == null ? List.of() : tags.stream().toList();
    }

    public List<String> filterTags() {
        return Stream.concat(Stream.ofNullable(tag), tags.stream()).distinct().toList();
    }

    public long offset() {
        return ((long) page - 1) * size;
    }

    public String keyword() {
        return q == null || q.isBlank() ? null : q.strip();
    }
}
