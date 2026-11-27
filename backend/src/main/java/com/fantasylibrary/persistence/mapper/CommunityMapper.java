package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.community.CommunityModels.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommunityMapper {
    @Select("SELECT is_open FROM fl_community_setting WHERE id = 1")
    boolean forumOpen();

    List<Post> activePosts(@Param("type") WorkType type, @Param("limit") int limit);

    long countPosts(@Param("topic") Topic topic, @Param("workId") Long workId);
    List<Post> posts(@Param("topic") Topic topic, @Param("workId") Long workId,
                     @Param("offset") long offset, @Param("size") int size, @Param("order") PostOrder order);
    Post post(long id);

    @Update("UPDATE fl_forum_post SET view_count = view_count + 1 WHERE id = #{id} AND state = 'VISIBLE' AND screening_thread = 0")
    int addView(long id);

    @Select("SELECT fl_forum_post_seq.NEXTVAL FROM dual")
    long nextPost();
    @Select("SELECT id, author_id, state, screening_thread FROM fl_forum_post WHERE id = #{id} FOR UPDATE")
    Row lockPost(long id);
    @Select("SELECT id, author_id, state, screening_thread FROM fl_forum_post WHERE id = #{id} AND screening_thread = 0 AND migrated_comment_id IS NULL FOR UPDATE")
    Row lockForumPost(long id);

    @Select("""
            SELECT p.id, p.author_id, p.state, p.screening_thread FROM fl_forum_post p WHERE p.id = #{id}
              AND (p.screening_thread = 0 OR EXISTS (
                SELECT 1 FROM fl_work w WHERE w.id = p.work_id AND w.is_listed = 1
              ))
            FOR UPDATE
            """)
    Row lockCommentThread(long id);

    @Select("SELECT id FROM fl_work WHERE id = #{workId} AND work_type = #{type} AND is_listed = 1 FOR UPDATE")
    Long lockScreeningWork(@Param("workId") long workId, @Param("type") WorkType type);

    @Select("SELECT id FROM fl_forum_post WHERE work_id = #{workId} AND screening_thread = 1")
    Long screeningThread(long workId);

    @Insert("""
            INSERT INTO fl_forum_post (id, author_id, work_id, topic, title, body, screening_thread)
            SELECT #{id}, #{author}, id, work_type,
                   SUBSTR(title, 1, 90) || CASE work_type WHEN 'GAME' THEN ' · 游戏评论' ELSE ' · 放映评论' END,
                   CASE work_type WHEN 'GAME' THEN '游戏评论' ELSE '放映室评论' END, 1
            FROM fl_work WHERE id = #{workId}
            """)
    void addScreeningThread(@Param("id") long id, @Param("author") long author, @Param("workId") long workId);

    @Select("SELECT COUNT(*) FROM fl_forum_post WHERE id = #{id} AND state = 'VISIBLE' AND screening_thread = 0")
    int visiblePost(long id);
    @Insert("""
            INSERT INTO fl_forum_post (id, author_id, work_id, topic, title, body)
            VALUES (#{id}, #{author}, #{form.workId,jdbcType=NUMERIC}, #{form.topic}, #{form.title}, #{form.body,jdbcType=CLOB})
            """)
    void addPost(@Param("id") long id, @Param("author") long author, @Param("form") PostForm form);
    @Update("""
            UPDATE fl_forum_post SET work_id = #{form.workId,jdbcType=NUMERIC}, topic = #{form.topic},
              title = #{form.title}, body = #{form.body,jdbcType=CLOB}, updated_at = SYSTIMESTAMP
            WHERE id = #{id} AND author_id = #{author} AND state = 'VISIBLE'
            """)
    void editPost(@Param("id") long id, @Param("author") long author, @Param("form") PostForm form);
    @Update("UPDATE fl_forum_post SET state = #{state}, updated_at = SYSTIMESTAMP WHERE id = #{id}")
    void setPostState(@Param("id") long id, @Param("state") Visibility state);

    @Select("SELECT image_key FROM fl_forum_image WHERE post_id = #{postId} ORDER BY position")
    List<String> images(long postId);
    @Select("""
            SELECT COUNT(*) FROM fl_forum_image i JOIN fl_forum_post p ON p.id = i.post_id
            WHERE i.image_key = #{key} AND p.state = 'VISIBLE'
            """)
    int visibleImage(String key);
    @Insert("INSERT INTO fl_forum_image (post_id, image_key, position) VALUES (#{postId}, #{key}, #{position})")
    void addImage(@Param("postId") long postId, @Param("key") String key, @Param("position") int position);
    @Delete("DELETE FROM fl_forum_image WHERE post_id = #{postId}")
    void clearImages(long postId);

    @Select("SELECT COUNT(*) FROM fl_forum_comment WHERE post_id = #{postId} AND state = 'VISIBLE'")
    long countComments(long postId);
    List<Comment> comments(@Param("postId") long postId, @Param("offset") long offset, @Param("size") int size);
    @Select("SELECT fl_forum_comment_seq.NEXTVAL FROM dual")
    long nextComment();
    @Select("SELECT post_id, state, parent_id, thread_root_id AS root_id FROM fl_forum_comment WHERE id = #{id}")
    ReplyRow comment(long id);
    @Insert("""
            INSERT INTO fl_forum_comment (id, post_id, author_id, parent_id, thread_root_id, body)
            VALUES (#{id}, #{postId}, #{author}, #{parent,jdbcType=NUMERIC}, #{root,jdbcType=NUMERIC}, #{body})
            """)
    void addComment(@Param("id") long id, @Param("postId") long postId, @Param("author") long author,
                    @Param("parent") Long parent, @Param("root") Long root, @Param("body") String body);
    long countScreeningThreads(long postId);
    List<ScreeningComment> screeningRoots(@Param("postId") long postId, @Param("viewer") Long viewer,
                                          @Param("offset") long offset, @Param("size") int size);
    List<ScreeningComment> screeningPreviews(@Param("postId") long postId, @Param("viewer") Long viewer,
                                             @Param("rootIds") List<Long> rootIds);
    long countScreeningReplies(@Param("postId") long postId, @Param("root") long root);
    List<ScreeningComment> screeningReplies(@Param("postId") long postId, @Param("root") long root,
                                            @Param("viewer") Long viewer, @Param("offset") long offset,
                                            @Param("size") int size);

    @Insert("""
            MERGE INTO fl_screening_comment_like existing
            USING (SELECT #{id} AS comment_id, #{user} AS user_id FROM dual) wanted
            ON (existing.comment_id = wanted.comment_id AND existing.user_id = wanted.user_id)
            WHEN NOT MATCHED THEN INSERT (comment_id, user_id) VALUES (wanted.comment_id, wanted.user_id)
            """)
    void likeScreeningComment(@Param("id") long id, @Param("user") long user);

    @Delete("DELETE FROM fl_screening_comment_like WHERE comment_id = #{id} AND user_id = #{user}")
    void unlikeScreeningComment(@Param("id") long id, @Param("user") long user);

    @Select("SELECT COUNT(*) FROM fl_screening_comment_like WHERE comment_id = #{id}")
    long screeningLikeCount(long id);

    @Update("UPDATE fl_forum_comment SET state = #{state} WHERE id = #{id}")
    void setCommentState(@Param("id") long id, @Param("state") Visibility state);
    @Select("SELECT COUNT(*) FROM fl_forum_like WHERE post_id = #{id}")
    long countLikes(long id);
    @Select("SELECT COUNT(*) FROM fl_forum_like WHERE post_id = #{id} AND user_id = #{userId}")
    int liked(@Param("id") long id, @Param("userId") long userId);
    @Insert("INSERT INTO fl_forum_like (post_id, user_id) VALUES (#{id}, #{userId})")
    void addLike(@Param("id") long id, @Param("userId") long userId);
    @Delete("DELETE FROM fl_forum_like WHERE post_id = #{id} AND user_id = #{userId}")
    void removeLike(@Param("id") long id, @Param("userId") long userId);

    @Insert("""
            INSERT INTO fl_forum_report (reporter_id, target_type, target_id, reason)
            VALUES (#{userId}, #{type}, #{targetId}, #{reason})
            """)
    void addReport(@Param("userId") long userId, @Param("type") Target type,
                   @Param("targetId") long targetId, @Param("reason") String reason);
    @Select("SELECT id, target_type, target_id, state FROM fl_forum_report WHERE id = #{id}")
    ReportRow report(long id);
    @Select("SELECT COUNT(*) FROM fl_forum_report WHERE state = 'OPEN'")
    long countReports();
    List<Report> reports(@Param("offset") long offset, @Param("size") int size);
    @Update("""
            UPDATE fl_forum_report SET state = 'DISMISSED', handled_by = #{admin}, handled_at = SYSTIMESTAMP
            WHERE id = #{id} AND state = 'OPEN'
            """)
    void dismissReport(@Param("id") long id, @Param("admin") long admin);
    @Update("""
            UPDATE fl_forum_report SET state = #{state}, handled_by = #{admin,jdbcType=NUMERIC}, handled_at = SYSTIMESTAMP
            WHERE target_type = #{type} AND target_id = #{id} AND state = 'OPEN'
            """)
    void closeReports(@Param("type") Target type, @Param("id") long id, @Param("state") ReportState state,
                      @Param("admin") Long admin);
}
