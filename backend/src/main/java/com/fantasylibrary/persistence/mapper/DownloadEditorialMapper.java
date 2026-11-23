package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.download.DownloadEditorialModels.Draft;
import com.fantasylibrary.download.DownloadEditorialModels.Entry;
import com.fantasylibrary.download.DownloadEditorialModels.Platform;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DownloadEditorialMapper {
    @Select("SELECT work_id FROM fl_game WHERE work_id = #{workId}")
    Long game(long workId);

    @Select("SELECT work_id FROM fl_game WHERE work_id = #{workId} FOR UPDATE")
    Long lockGame(long workId);

    @Select("SELECT platform_id FROM fl_game_platform WHERE work_id = #{workId} AND platform_id = #{platformId} FOR UPDATE")
    Long lockPlatform(@Param("workId") long workId, @Param("platformId") long platformId);

    @Select("SELECT is_listed FROM fl_game_download WHERE id = #{nodeId} AND work_id = #{workId} FOR UPDATE")
    Integer publication(@Param("workId") long workId, @Param("nodeId") long nodeId);

    @Select("""
            SELECT p.id, p.name FROM fl_game_platform g JOIN fl_platform p ON p.id = g.platform_id
            WHERE g.work_id = #{workId} ORDER BY p.sort_order, p.id
            """)
    List<Platform> platforms(long workId);

    @Select("""
            SELECT d.id, d.platform_id, d.label, d.external_url, d.file_name, d.is_listed AS listed
            FROM fl_game_download d JOIN fl_platform p ON p.id = d.platform_id
            WHERE d.work_id = #{workId} ORDER BY p.sort_order, p.id, d.sort_order, d.id
            """)
    List<Entry> nodes(long workId);

    @Insert("""
            INSERT INTO fl_game_download (work_id, platform_id, label, external_url, file_name, sort_order, is_listed)
            VALUES (#{workId}, #{draft.platformId}, #{draft.label}, #{draft.externalUrl,jdbcType=VARCHAR},
                    #{draft.fileName,jdbcType=VARCHAR}, (SELECT NVL(MAX(d.sort_order) + 1, 0)
                        FROM fl_game_download d WHERE d.work_id = #{workId}), #{draft.listed})
            """)
    void add(@Param("workId") long workId, @Param("draft") Draft draft);

    @Update("""
            UPDATE fl_game_download SET platform_id = #{draft.platformId}, label = #{draft.label},
                external_url = #{draft.externalUrl,jdbcType=VARCHAR}, file_name = #{draft.fileName,jdbcType=VARCHAR},
                is_listed = #{draft.listed}
            WHERE id = #{nodeId} AND work_id = #{workId}
            """)
    int update(@Param("workId") long workId, @Param("nodeId") long nodeId, @Param("draft") Draft draft);
}
