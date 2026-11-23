package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.admin.LibraryAdminModels.*;
import com.fantasylibrary.catalog.model.Catalog.WorkType;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LibraryAdminMapper {
    long countWorks(@Param("type") WorkType type, @Param("q") String q, @Param("listed") Boolean listed);
    List<Work> works(@Param("type") WorkType type, @Param("q") String q,
                     @Param("listed") Boolean listed, @Param("offset") long offset, @Param("size") int size);
    List<Node> nodes(long workId);
    long countPosts(@Param("q") String q);
    List<Post> posts(@Param("q") String q, @Param("offset") long offset, @Param("size") int size);
    long countComments(@Param("q") String q, @Param("scope") CommentScope scope);
    List<Comment> comments(@Param("q") String q, @Param("scope") CommentScope scope,
                           @Param("offset") long offset, @Param("size") int size);
    long countMembers(@Param("q") String q);
    List<Member> members(@Param("q") String q, @Param("offset") long offset, @Param("size") int size);

    @Update("UPDATE fl_work SET is_listed = #{listed} WHERE id = #{id}")
    int setWork(@Param("id") long id, @Param("listed") int listed);

    @Update("UPDATE fl_episode_source SET is_listed = #{listed} WHERE id = #{id}")
    int setPlaybackNode(@Param("id") long id, @Param("listed") int listed);

    @Update("UPDATE fl_game_download SET is_listed = #{listed} WHERE id = #{id}")
    int setDownloadNode(@Param("id") long id, @Param("listed") int listed);

    @Update("UPDATE fl_community_setting SET is_open = #{open} WHERE id = 1")
    void setForumOpen(@Param("open") int open);

    @Select("SELECT account_role AS role, avatar_key FROM fl_account WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    MemberLock lockMember(long id);

    @Update("""
            UPDATE fl_account SET is_deleted = 1, username = #{anonymous}, password_hash = '!',
              display_name = '已注销馆员', bio = NULL, avatar_key = NULL
            WHERE id = #{id} AND account_role = 'MEMBER' AND is_deleted = 0
            """)
    void anonymizeMember(@Param("id") long id, @Param("anonymous") String anonymous);

    @Delete("DELETE FROM fl_login_token WHERE user_id = #{id}")
    void revokeSessions(long id);

    @Delete("DELETE FROM fl_saved_work WHERE user_id = #{id}")
    void clearCollection(long id);

    @Delete("DELETE FROM fl_watch_history WHERE user_id = #{id}")
    void clearHistory(long id);

    @Delete("DELETE FROM fl_inbox_notice WHERE recipient_id = #{id}")
    void clearInbox(long id);

    @Delete("DELETE FROM fl_forum_like WHERE user_id = #{id}")
    void clearLikes(long id);
}
