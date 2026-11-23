-- SQL*Plus entry point. Connect to the target application schema before running.
WHENEVER OSERROR EXIT FAILURE ROLLBACK
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF

DECLARE
    existing_objects NUMBER;
BEGIN
    SELECT COUNT(*) INTO existing_objects
    FROM (
        SELECT object_name AS name FROM user_objects
        UNION ALL
        SELECT constraint_name AS name FROM user_constraints
    )
    WHERE name IN (
        'FL_CATEGORY', 'FL_CATEGORY_PK', 'FL_CATEGORY_SLUG_UQ', 'FL_CATEGORY_ORDER_CK',
        'FL_TAG', 'FL_TAG_PK', 'FL_TAG_SLUG_UQ', 'FL_PLATFORM',
        'FL_PLATFORM_PK', 'FL_PLATFORM_SLUG_UQ', 'FL_PLATFORM_ORDER_CK', 'FL_WORK',
        'FL_WORK_PK', 'FL_WORK_SLUG_UQ', 'FL_WORK_ID_TYPE_UQ', 'FL_WORK_TYPE_CK',
        'FL_WORK_CATEGORY_FK', 'FL_ANIME', 'FL_ANIME_PK', 'FL_ANIME_TYPE_CK',
        'FL_ANIME_EPISODES_CK', 'FL_ANIME_STATUS_CK', 'FL_ANIME_WORK_FK', 'FL_GAME',
        'FL_GAME_PK', 'FL_GAME_TYPE_CK', 'FL_GAME_WORK_FK', 'FL_WORK_TAG',
        'FL_WORK_TAG_PK', 'FL_WORK_TAG_WORK_FK', 'FL_WORK_TAG_TAG_FK', 'FL_GAME_PLATFORM',
        'FL_GAME_PLATFORM_PK', 'FL_GP_GAME_FK', 'FL_GP_PLATFORM_FK', 'FL_WORK_IMAGE',
        'FL_WORK_IMAGE_PK', 'FL_IMAGE_WORK_FK', 'FL_IMAGE_ORDER_UQ', 'FL_IMAGE_KIND_CK',
        'FL_IMAGE_ORDER_CK', 'FL_WORK_TYPE_RELEASE_IX', 'FL_WORK_CAT_RELEASE_IX', 'FL_WORK_TAG_TAG_IX',
        'FL_GP_PLATFORM_IX', 'FL_ACCOUNT', 'FL_LOGIN_TOKEN', 'FL_SAVED_WORK', 'FL_FORUM_POST', 'FL_FORUM_COMMENT',
        'FL_FORUM_IMAGE', 'FL_FORUM_LIKE', 'FL_FORUM_REPORT', 'FL_FORUM_POST_SEQ', 'FL_FORUM_COMMENT_SEQ',
        'FL_WATCH_HISTORY', 'FL_INBOX_NOTICE', 'FL_EPISODE', 'FL_EPISODE_SOURCE', 'FL_GAME_DOWNLOAD', 'FL_COMMUNITY_SETTING',
        'FL_PLAYBACK_NODE', 'FL_RESOURCE_REFERENCE_GUARD', 'FL_RESOURCE_RETIREMENT'
    );
    IF existing_objects > 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Catalog objects already exist. Inspect the schema before applying DDL.');
    END IF;
END;
/

@@01_schema.sql
@@02_seed.sql
@@03_reader_editions.sql
@@04_accounts.sql
@@05_community.sql
@@06_community_views.sql
@@07_account_avatar.sql
@@08_recommendations.sql
@@09_personal_library.sql
@@10_playback.sql
@@11_game_downloads.sql
@@12_library_admin.sql
@@13_playback_nodes.sql
@@14_resource_retirement.sql
@@15_seed_playback_lines.sql
@@16_work_retirement.sql
@@17_screening_comments.sql
@@18_screening_discussions.sql
EXIT SUCCESS
