package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.playback.PlaybackModels.EpisodeRow;
import com.fantasylibrary.playback.PlaybackModels.SourceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PlaybackMapper {
    @Select("""
            SELECT e.id, e.edition, e.episode_no AS "NUMBER", e.title, e.duration_seconds,
                   n.id AS node_id, n.label AS node_label, n.is_listed AS node_listed
            FROM fl_episode e JOIN fl_playback_node n ON n.id = e.node_id
            WHERE e.work_id = #{workId}
            ORDER BY n.id, CASE e.edition WHEN 'EPISODE' THEN 0 ELSE 1 END, e.episode_no
            """)
    List<EpisodeRow> episodes(long workId);

    @Select("""
            SELECT s.episode_id, s.id, s.label, s.quality, s.media_url AS url, s.format
            FROM fl_episode_source s JOIN fl_episode e ON e.id = s.episode_id
            WHERE e.work_id = #{workId} AND s.is_listed = 1
            ORDER BY s.episode_id, s.sort_order, s.id
            """)
    List<SourceRow> sources(long workId);

    @Select("""
            SELECT COUNT(*) FROM fl_episode_source s JOIN fl_episode e ON e.id = s.episode_id
            JOIN fl_work w ON w.id = e.work_id JOIN fl_playback_node n ON n.id = e.node_id
            WHERE n.is_listed = 1 AND s.media_url = #{url} AND s.is_listed = 1 AND w.is_listed = 1
            """)
    int listedMedia(String url);
}
