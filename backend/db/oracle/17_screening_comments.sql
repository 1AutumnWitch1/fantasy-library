-- Reuse the existing comment, report and moderation tables for one thread per anime.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF

DECLARE
    present NUMBER;
    next_key NUMBER;
BEGIN
    -- Imported databases can contain comments and posts without their original sequences.
    SELECT COUNT(*) INTO present FROM user_sequences WHERE sequence_name = 'FL_FORUM_POST_SEQ';
    IF present = 0 THEN
        SELECT NVL(MAX(id), 0) + 1 INTO next_key FROM fl_forum_post;
        EXECUTE IMMEDIATE 'CREATE SEQUENCE fl_forum_post_seq START WITH ' || next_key || ' CACHE 20';
    END IF;

    SELECT COUNT(*) INTO present FROM user_sequences WHERE sequence_name = 'FL_FORUM_COMMENT_SEQ';
    IF present = 0 THEN
        SELECT NVL(MAX(id), 0) + 1 INTO next_key FROM fl_forum_comment;
        EXECUTE IMMEDIATE 'CREATE SEQUENCE fl_forum_comment_seq START WITH ' || next_key || ' CACHE 20';
    END IF;

    SELECT COUNT(*) INTO present FROM user_tab_columns
    WHERE table_name = 'FL_FORUM_POST' AND column_name = 'SCREENING_THREAD';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE fl_forum_post ADD (screening_thread NUMBER(1) DEFAULT 0 NOT NULL
            CONSTRAINT fl_forum_screening_ck CHECK (screening_thread IN (0, 1)))';
    END IF;

    SELECT COUNT(*) INTO present FROM user_indexes WHERE index_name = 'FL_FORUM_SCREENING_UQ';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX fl_forum_screening_uq ON fl_forum_post
            (CASE WHEN screening_thread = 1 THEN work_id END)';
    END IF;
END;
/
