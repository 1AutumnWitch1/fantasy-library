package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.personal.PersonalModels.Notice;
import com.fantasylibrary.personal.PersonalModels.WatchEvent;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PersonalMapper {
    @Select("SELECT id FROM fl_account WHERE id = #{userId} AND is_deleted = 0 FOR UPDATE")
    Long lockMember(long userId);

    @Select("SELECT COUNT(*) FROM fl_watch_history WHERE user_id = #{userId}")
    long countHistory(long userId);

    List<WatchEvent> history(@Param("userId") long userId, @Param("offset") long offset, @Param("size") int size);

    long countNotices(@Param("userId") long userId, @Param("unreadOnly") boolean unreadOnly);

    List<Notice> notices(@Param("userId") long userId, @Param("offset") long offset, @Param("size") int size);

    @Update("UPDATE fl_inbox_notice SET read_at = SYSTIMESTAMP WHERE recipient_id = #{userId} AND id = #{id} AND read_at IS NULL")
    void readNotice(@Param("userId") long userId, @Param("id") long id);

    @Update("UPDATE fl_inbox_notice SET read_at = SYSTIMESTAMP WHERE recipient_id = #{userId} AND read_at IS NULL")
    void readAll(long userId);

    void notifyComment(long commentId);

    void notifyLike(@Param("postId") long postId, @Param("actorId") long actorId);
}
