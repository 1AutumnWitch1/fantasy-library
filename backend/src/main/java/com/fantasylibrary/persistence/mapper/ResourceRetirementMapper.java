package com.fantasylibrary.persistence.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ResourceRetirementMapper {
    @Select("SELECT id FROM fl_resource_reference_guard WHERE id = 1 FOR UPDATE")
    int lockReferences();

    @Select("SELECT id FROM fl_work WHERE id = #{id} FOR UPDATE")
    Long lockWork(long id);

    @Select("""
            SELECT 'PLAYBACK' AS kind, s.media_url AS location
            FROM fl_episode_source s JOIN fl_episode e ON e.id = s.episode_id WHERE e.work_id = #{id}
            UNION ALL
            SELECT 'DOWNLOAD', d.file_name FROM fl_game_download d
            WHERE d.work_id = #{id} AND d.file_name IS NOT NULL
            UNION ALL
            SELECT 'CATALOG', i.resource_url FROM fl_work_image i WHERE i.work_id = #{id}
            """)
    List<WorkAsset> workAssets(long id);

    @Delete("DELETE FROM fl_work WHERE id = #{id}")
    void deleteWork(long id);

    @Select("SELECT id, media_url AS location FROM fl_episode_source WHERE id = #{id} FOR UPDATE")
    NodeFile playback(long id);

    @Select("SELECT id, file_name AS location FROM fl_game_download WHERE id = #{id} FOR UPDATE")
    NodeFile download(long id);

    @Delete("DELETE FROM fl_episode_source WHERE id = #{id}")
    void deletePlayback(long id);

    @Delete("DELETE FROM fl_game_download WHERE id = #{id}")
    void deleteDownload(long id);

    @Select("SELECT COUNT(*) FROM fl_episode_source WHERE media_url = #{url}")
    int playbackReferences(String url);

    @Select("SELECT COUNT(*) FROM fl_game_download WHERE file_name = #{key}")
    int downloadReferences(String key);

    @Select("SELECT COUNT(*) FROM fl_work_image WHERE resource_url = #{url}")
    int catalogReferences(String url);

    @Insert("INSERT INTO fl_resource_retirement (object_key, reference_kind) VALUES (#{key}, #{kind})")
    void enqueue(@Param("key") String key, @Param("kind") ReferenceKind kind);

    @Select("SELECT COUNT(*) FROM fl_resource_retirement WHERE object_key = #{key}")
    int pending(String key);

    @Select("SELECT reference_kind FROM fl_resource_retirement WHERE object_key = #{key} FOR UPDATE SKIP LOCKED")
    ReferenceKind lockPending(String key);

    @Select("""
            SELECT object_key FROM fl_resource_retirement WHERE retry_after <= SYSTIMESTAMP
            ORDER BY retry_after, object_key FETCH FIRST 32 ROWS ONLY
            """)
    List<String> due();

    @Update("UPDATE fl_resource_retirement SET retry_after = SYSTIMESTAMP + INTERVAL '30' SECOND WHERE object_key = #{key}")
    void reschedule(String key);

    @Delete("DELETE FROM fl_resource_retirement WHERE object_key = #{key}")
    void complete(String key);

    enum ReferenceKind { PLAYBACK, DOWNLOAD, CATALOG }
    record WorkAsset(ReferenceKind kind, String location) {}
    record NodeFile(long id, String location) {}
}
