package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.admin.WorkEditorialModels.*;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkEditorialMapper {
    @Select("""
            SELECT id, work_type AS type, slug, title, summary, release_date, category_id, is_listed AS listed
            FROM fl_work WHERE id = #{id}
            """)
    Entry find(long id);

    @Select("SELECT work_type FROM fl_work WHERE id = #{id} FOR UPDATE")
    WorkType lockType(long id);

    @Select("SELECT id FROM fl_work WHERE slug = #{slug}")
    long idBySlug(String slug);

    @Insert("""
            INSERT INTO fl_work (work_type, slug, title, summary, release_date, category_id, is_listed)
            VALUES (#{type}, #{slug}, #{title}, #{summary}, #{releaseDate,jdbcType=DATE}, #{categoryId}, 0)
            """)
    void create(Draft draft);

    @Update("""
            UPDATE fl_work SET slug = #{draft.slug}, title = #{draft.title}, summary = #{draft.summary},
              release_date = #{draft.releaseDate,jdbcType=DATE}, category_id = #{draft.categoryId}
            WHERE id = #{id}
            """)
    void update(@Param("id") long id, @Param("draft") Draft draft);

    @Select("SELECT studio, airing_status, episode_count FROM fl_anime WHERE work_id = #{id}")
    Anime anime(long id);

    @Select("SELECT developer, age_rating FROM fl_game WHERE work_id = #{id}")
    GameRow game(long id);

    @Insert("""
            MERGE INTO fl_anime a USING (SELECT #{id} AS id FROM dual) source ON (a.work_id = source.id)
            WHEN MATCHED THEN UPDATE SET studio = #{form.studio}, airing_status = #{form.airingStatus},
              episode_count = #{form.episodeCount,jdbcType=INTEGER}
            WHEN NOT MATCHED THEN INSERT (work_id, studio, airing_status, episode_count)
              VALUES (source.id, #{form.studio}, #{form.airingStatus}, #{form.episodeCount,jdbcType=INTEGER})
            """)
    void saveAnime(@Param("id") long id, @Param("form") Anime form);

    @Insert("""
            MERGE INTO fl_game g USING (SELECT #{id} AS id FROM dual) source ON (g.work_id = source.id)
            WHEN MATCHED THEN UPDATE SET developer = #{form.developer}, age_rating = #{form.ageRating,jdbcType=VARCHAR}
            WHEN NOT MATCHED THEN INSERT (work_id, developer, age_rating)
              VALUES (source.id, #{form.developer}, #{form.ageRating,jdbcType=VARCHAR})
            """)
    void saveGame(@Param("id") long id, @Param("form") Game form);

    @Select("SELECT platform_id FROM fl_game_platform WHERE work_id = #{id} ORDER BY platform_id")
    List<Long> platformIds(long id);

    @Delete("""
            DELETE FROM fl_game_platform p WHERE p.work_id = #{id} AND p.platform_id = #{platform}
              AND NOT EXISTS (SELECT 1 FROM fl_game_download d WHERE d.work_id = p.work_id AND d.platform_id = p.platform_id)
            """)
    int removePlatform(@Param("id") long id, @Param("platform") long platform);

    @Insert("INSERT INTO fl_game_platform (work_id, platform_id) VALUES (#{id}, #{platform})")
    void addPlatform(@Param("id") long id, @Param("platform") long platform);

    @Select("SELECT tag_id FROM fl_work_tag WHERE work_id = #{id} ORDER BY tag_id")
    List<Long> tagIds(long id);

    @Delete("DELETE FROM fl_work_tag WHERE work_id = #{id}")
    void clearTags(long id);

    @Insert("INSERT INTO fl_work_tag (work_id, tag_id) VALUES (#{id}, #{tag})")
    void addTag(@Param("id") long id, @Param("tag") long tag);

    @Select("""
            SELECT resource_url AS url, alt_text AS alt FROM fl_work_image
            WHERE work_id = #{id} AND image_kind = #{kind} ORDER BY sort_order
            """)
    List<Image> images(@Param("id") long id, @Param("kind") String kind);

    @Delete("DELETE FROM fl_work_image WHERE work_id = #{id}")
    void clearImages(long id);

    @Insert("""
            INSERT INTO fl_work_image (work_id, image_kind, resource_url, alt_text, sort_order)
            VALUES (#{id}, #{kind}, #{image.url}, #{image.alt}, #{position})
            """)
    void addImage(@Param("id") long id, @Param("kind") String kind,
                  @Param("image") Image image, @Param("position") int position);

    @Select("SELECT id, name FROM fl_category ORDER BY sort_order, id")
    List<Option> categories();

    @Select("SELECT id, name FROM fl_tag ORDER BY name, id")
    List<Option> tags();

    @Select("SELECT id, name FROM fl_platform ORDER BY sort_order, id")
    List<Option> platforms();
}
