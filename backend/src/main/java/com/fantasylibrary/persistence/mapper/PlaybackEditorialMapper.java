package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.playback.PlaybackEditorialModels.EpisodeDraft;
import com.fantasylibrary.playback.PlaybackEditorialModels.Node;
import com.fantasylibrary.playback.PlaybackEditorialModels.NodeDraft;
import com.fantasylibrary.playback.PlaybackEditorialModels.SourceDraft;
import com.fantasylibrary.playback.PlaybackEditorialModels.SourceRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PlaybackEditorialMapper {
    @Select("SELECT work_id FROM fl_anime WHERE work_id = #{workId}")
    Long anime(long workId);

    @Select("SELECT work_id FROM fl_anime WHERE work_id = #{workId} FOR UPDATE")
    Long lockAnime(long workId);

    @Select("SELECT id, label, is_listed AS listed FROM fl_playback_node WHERE work_id = #{workId} ORDER BY id")
    List<Node> nodes(long workId);

    @Insert("INSERT INTO fl_playback_node (work_id, label, is_listed) VALUES (#{workId}, #{draft.label}, #{draft.listed})")
    void addNode(@Param("workId") long workId, @Param("draft") NodeDraft draft);

    @Update("UPDATE fl_playback_node SET label = #{draft.label}, is_listed = #{draft.listed} WHERE work_id = #{workId} AND id = #{nodeId}")
    int updateNode(@Param("workId") long workId, @Param("nodeId") long nodeId, @Param("draft") NodeDraft draft);

    @Select("SELECT node_id FROM fl_episode WHERE id = #{episodeId} AND work_id = #{workId} FOR UPDATE")
    Long episodeNode(@Param("workId") long workId, @Param("episodeId") long episodeId);

    @Select("SELECT id FROM fl_episode WHERE id = #{episodeId} AND work_id = #{workId} FOR UPDATE")
    Long lockEpisode(@Param("workId") long workId, @Param("episodeId") long episodeId);

    @Select("""
            SELECT s.episode_id, s.id, s.label, s.quality, s.media_url AS url, s.format, s.is_listed AS listed
            FROM fl_episode_source s JOIN fl_episode e ON e.id = s.episode_id
            WHERE e.work_id = #{workId}
            ORDER BY s.episode_id, s.sort_order, s.id
            """)
    List<SourceRow> sources(long workId);

    @Insert("""
            INSERT INTO fl_episode (work_id, edition, episode_no, title, duration_seconds, node_id)
            VALUES (#{workId}, #{draft.edition}, #{draft.number}, #{draft.title}, #{draft.durationSeconds}, #{draft.nodeId})
            """)
    void addEpisode(@Param("workId") long workId, @Param("draft") EpisodeDraft draft);

    @Update("""
            UPDATE fl_episode SET edition = #{draft.edition}, episode_no = #{draft.number},
              title = #{draft.title}, duration_seconds = #{draft.durationSeconds}, node_id = #{draft.nodeId}
            WHERE id = #{episodeId} AND work_id = #{workId}
            """)
    int updateEpisode(@Param("workId") long workId, @Param("episodeId") long episodeId,
                      @Param("draft") EpisodeDraft draft);

    @Insert("""
            INSERT INTO fl_episode_source (episode_id, label, quality, media_url, format, sort_order, is_listed)
            VALUES (#{episodeId}, #{draft.label}, #{draft.quality}, #{draft.url}, #{draft.format},
                    (SELECT NVL(MAX(s.sort_order) + 1, 0) FROM fl_episode_source s WHERE s.episode_id = #{episodeId}), 0)
            """)
    void addSource(@Param("episodeId") long episodeId, @Param("draft") SourceDraft draft);

    @Update("""
            UPDATE fl_episode_source SET label = #{draft.label}, quality = #{draft.quality},
              media_url = #{draft.url}, format = #{draft.format}
            WHERE id = #{sourceId} AND episode_id = #{episodeId}
            """)
    int updateSource(@Param("episodeId") long episodeId, @Param("sourceId") long sourceId,
                     @Param("draft") SourceDraft draft);
}
