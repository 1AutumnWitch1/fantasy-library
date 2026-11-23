package com.fantasylibrary.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GameDownloadMapper {
    @Select("""
            SELECT d.id, d.label, p.name AS platform, d.external_url, d.file_name
            FROM fl_game_download d JOIN fl_platform p ON p.id = d.platform_id
            JOIN fl_work w ON w.id = d.work_id
            WHERE d.work_id = #{workId} AND d.is_listed = 1 AND w.is_listed = 1
            ORDER BY p.sort_order, p.id, d.sort_order, d.id
            """)
    List<Node> nodes(long workId);

    @Select("""
            SELECT d.file_name FROM fl_game_download d JOIN fl_work w ON w.id = d.work_id
            WHERE d.id = #{id} AND d.is_listed = 1 AND w.is_listed = 1
            """)
    String fileName(long id);

    record Node(long id, String label, String platform, String externalUrl, String fileName) {}
}
