package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.playback.ViewingModels.*;
import com.fantasylibrary.playback.PlaybackModels.Edition;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ViewingMapper {
    @Select("""
            SELECT e.work_id, e.id AS episode_id, e.edition, e.episode_no, e.duration_seconds, s.media_url AS url
            FROM fl_episode_source s JOIN fl_episode e ON e.id = s.episode_id
            JOIN fl_playback_node n ON n.id = e.node_id JOIN fl_work w ON w.id = e.work_id
            WHERE s.id = #{sourceId} AND e.work_id = #{workId}
              AND s.is_listed = 1 AND n.is_listed = 1 AND w.is_listed = 1
            """)
    Source source(long workId, long sourceId);

    @Select("""
            SELECT (SELECT COUNT(*) FROM fl_saved_work s JOIN fl_account a ON a.id = s.user_id
                    WHERE s.work_id = #{workId} AND s.work_type = 'ANIME' AND s.favorite = 1 AND a.is_deleted = 0) AS followers,
                   (SELECT NVL(SUM(play_count), 0) FROM fl_play_total WHERE work_id = #{workId}) AS plays
            FROM dual
            """)
    Stats stats(long workId);

    @Select("""
            SELECT episode_no, position_ms, duration_ms, completed, updated_at FROM fl_episode_progress
            WHERE user_id = #{userId} AND work_id = #{workId}
            ORDER BY updated_at DESC, episode_no DESC FETCH FIRST 1 ROW ONLY
            """)
    SavedEpisode latest(long userId, long workId);

    @Select("SELECT COUNT(*) FROM fl_episode_progress WHERE user_id = #{userId} AND work_id = #{workId} AND completed = 1")
    int completed(long userId, long workId);

    @Select("""
            SELECT coverage, completed FROM fl_episode_progress
            WHERE user_id = #{userId} AND work_id = #{workId} AND episode_no = #{episode}
            """)
    @ConstructorArgs({@Arg(column = "coverage", javaType = String.class, jdbcType = org.apache.ibatis.type.JdbcType.CLOB),
            @Arg(column = "completed", javaType = boolean.class)})
    Recording recording(long userId, long workId, int episode);

    @Delete("DELETE FROM fl_play_session WHERE last_sample_at < SYSTIMESTAMP - INTERVAL '2' HOUR")
    void expire();

    @Insert("""
            INSERT INTO fl_play_session (id, viewer_key, user_id, source_id, last_position_ms)
            VALUES (#{id}, #{viewer}, #{userId,jdbcType=NUMERIC}, #{sourceId}, #{positionMs})
            """)
    void start(String id, String viewer, Long userId, long sourceId, long positionMs);

    @Select("""
            SELECT id, source_id, last_position_ms, last_sample_at, watched_ms, settled FROM fl_play_session
            WHERE id = #{id} AND viewer_key = #{viewer} AND last_sample_at >= SYSTIMESTAMP - INTERVAL '2' HOUR
            FOR UPDATE
            """)
    Session lockSession(String id, String viewer);

    @Update("""
            UPDATE fl_play_session SET last_position_ms = #{positionMs}, last_sample_at = SYSTIMESTAMP,
                   watched_ms = #{watched}, settled = #{settled}
            WHERE id = #{id}
            """)
    void sample(String id, long positionMs, long watched, int settled);

    @Insert("""
            INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX(fl_play_total (work_id, edition, episode_no, viewer_key)) */
            INTO fl_play_total (work_id, edition, episode_no, viewer_key)
            VALUES (#{workId}, #{edition}, #{episode}, #{viewer})
            """)
    void prepareTotal(long workId, Edition edition, int episode, String viewer);

    @Update("""
            UPDATE fl_play_total SET play_count = play_count + 1, last_counted_at = SYSTIMESTAMP
            WHERE work_id = #{workId} AND edition = #{edition} AND episode_no = #{episode}
              AND viewer_key = #{viewer}
              AND (last_counted_at IS NULL OR last_counted_at <= SYSTIMESTAMP - INTERVAL '30' MINUTE)
            """)
    int countPlay(long workId, Edition edition, int episode, String viewer);

    @Update("""
            MERGE INTO fl_episode_progress p
            USING (SELECT #{userId} AS user_id, #{workId} AS work_id, #{episode} AS episode_no FROM dual) x
            ON (p.user_id = x.user_id AND p.work_id = x.work_id AND p.episode_no = x.episode_no)
            WHEN MATCHED THEN UPDATE SET episode_id = #{episodeId}, position_ms = #{position}, duration_ms = #{duration},
                coverage = #{coverage,jdbcType=CLOB}, completed = GREATEST(p.completed, #{complete}),
                updated_at = SYSTIMESTAMP
            WHEN NOT MATCHED THEN INSERT (user_id, work_id, episode_no, episode_id, position_ms, duration_ms, coverage, completed)
                VALUES (x.user_id, x.work_id, x.episode_no, #{episodeId}, #{position}, #{duration}, #{coverage,jdbcType=CLOB}, #{complete})
            """)
    void saveProgress(long userId, long workId, int episode, long episodeId, long position, long duration, String coverage, int complete);

    @Insert("""
            INSERT INTO fl_watch_history (user_id, work_id, watch_status, watched_episodes)
            VALUES (#{userId}, #{workId}, #{status}, #{count})
            """)
    void history(long userId, long workId, String status, int count);

    @Update("""
            UPDATE fl_saved_work SET watched_episodes = #{count}, watch_status = #{status}
            WHERE user_id = #{userId} AND work_id = #{workId} AND work_type = 'ANIME'
            """)
    void updateFollowProgress(long userId, long workId, String status, int count);
}
