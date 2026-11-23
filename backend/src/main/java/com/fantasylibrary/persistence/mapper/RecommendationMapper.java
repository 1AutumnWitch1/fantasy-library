package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.persistence.mapper.CatalogMapper.WorkTag;
import com.fantasylibrary.recommendation.RecommendationModels.Heat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecommendationMapper {
    List<Heat> popular(@Param("type") WorkType type, @Param("excludeUser") Long excludeUser,
                       @Param("limit") int limit);

    List<Long> newest(@Param("type") WorkType type, @Param("excludeUser") Long excludeUser,
                      @Param("limit") int limit);

    List<Long> editorial(@Param("type") WorkType type, @Param("excludeUser") Long excludeUser,
                         @Param("limit") int limit);

    List<WorkTag> savedTags(@Param("userId") long userId);

    List<CandidateTrait> candidateTraits(@Param("type") WorkType type, @Param("excludeUser") long excludeUser);

    List<Intake> intakeDates(@Param("ids") List<Long> ids);

    record CandidateTrait(long workId, WorkType type, Long tagId) {
    }

    record Intake(long workId, String addedAt) {
    }
}
