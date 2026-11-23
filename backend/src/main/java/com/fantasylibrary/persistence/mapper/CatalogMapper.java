package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.catalog.model.Catalog.AiringStatus;
import com.fantasylibrary.catalog.model.Catalog.Category;
import com.fantasylibrary.catalog.model.Catalog.Image;
import com.fantasylibrary.catalog.model.Catalog.Tag;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Date;
import java.util.List;

@Mapper
public interface CatalogMapper {

    List<Category> findCategories();

    long countWorks(@Param("type") WorkType type, @Param("query") CatalogQuery query);

    List<WorkRow> findWorks(@Param("type") WorkType type, @Param("query") CatalogQuery query);

    WorkRow findWork(@Param("id") long id);

    List<WorkRow> findWorksByIds(@Param("ids") List<Long> ids);

    List<Tag> availableTags(@Param("type") WorkType type);

    List<WorkTag> findWorkTags(@Param("workIds") List<Long> workIds);

    record WorkTag(long workId, long id, String slug, String name) {
    }

    List<Tag> findTags(@Param("workId") long workId);

    List<Image> findImages(@Param("workId") long workId);

    List<WorkPlatform> findPlatforms(@Param("workIds") List<Long> workIds);

    record WorkRow(long id, WorkType type, String slug, String title, String summary,
                   Date releaseDate, long categoryId, String categorySlug, String categoryName,
                   int categorySortOrder, Long coverId, String coverUrl, String coverAltText,
                   Integer episodeCount, String studio, AiringStatus airingStatus,
                   String developer, String ageRating) {
    }

    record WorkPlatform(long workId, long id, String slug, String name) {
    }
}
