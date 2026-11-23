-- Organize replies by their first comment and bring back comments saved as posts by the old player.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF
SET SERVEROUTPUT ON

DECLARE
    present NUMBER;
BEGIN
    SELECT COUNT(*) INTO present FROM user_tab_columns
    WHERE table_name = 'FL_FORUM_COMMENT' AND column_name = 'THREAD_ROOT_ID';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE fl_forum_comment ADD (thread_root_id NUMBER(18))';
    END IF;
    SELECT COUNT(*) INTO present FROM user_tab_columns
    WHERE table_name = 'FL_FORUM_POST' AND column_name = 'MIGRATED_COMMENT_ID';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE fl_forum_post ADD (migrated_comment_id NUMBER(18))';
    END IF;
    SELECT COUNT(*) INTO present FROM user_indexes WHERE index_name = 'FL_FORUM_ROOT_REPLIES_IX';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX fl_forum_root_replies_ix
            ON fl_forum_comment (thread_root_id, post_id, state, created_at, id)';
    END IF;
    SELECT COUNT(*) INTO present FROM user_tables WHERE table_name = 'FL_SCREENING_COMMENT_LIKE';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE fl_screening_comment_like (
            comment_id NUMBER(18) NOT NULL REFERENCES fl_forum_comment (id) ON DELETE CASCADE,
            user_id NUMBER(18) NOT NULL REFERENCES fl_account (id) ON DELETE CASCADE,
            CONSTRAINT fl_screening_comment_like_pk PRIMARY KEY (comment_id, user_id)
        )';
    END IF;
END;
/

MERGE INTO fl_forum_comment c
USING (
    SELECT id, CONNECT_BY_ROOT id AS root_id
    FROM (
        SELECT c.id, c.parent_id, c.post_id
        FROM fl_forum_comment c JOIN fl_forum_post p ON p.id = c.post_id
        WHERE p.screening_thread = 1
    )
    START WITH parent_id IS NULL
    CONNECT BY PRIOR id = parent_id AND PRIOR post_id = post_id
) tree ON (tree.id = c.id)
WHEN MATCHED THEN UPDATE SET c.thread_root_id = tree.root_id
    WHERE c.parent_id IS NOT NULL AND c.thread_root_id IS NULL;

DECLARE
    TYPE comment_map IS TABLE OF NUMBER INDEX BY VARCHAR2(24);
    copied comment_map;
    thread_id NUMBER;
    root_id NUMBER;
    new_id NUMBER;
    new_parent NUMBER;
    locked_work NUMBER;
    migrated NUMBER := 0;

    PROCEDURE move_reports(old_type VARCHAR2, old_id NUMBER, new_target NUMBER) IS
    BEGIN
        UPDATE fl_forum_report
        SET target_type = 'COMMENT', target_id = new_target
        WHERE target_type = old_type AND target_id = old_id;
    END;
BEGIN
    -- The old player constructed titles as "<episode label> · <first 48 characters of the body>".
    -- Images and titles written by community authors are outside this migration.
    FOR legacy IN (
        SELECT p.id, p.author_id, p.work_id, p.state, p.created_at, w.title AS work_title,
               DBMS_LOB.SUBSTR(p.body, 900, 1) AS body
        FROM fl_forum_post p JOIN fl_work w ON w.id = p.work_id AND w.work_type = 'ANIME'
        WHERE p.screening_thread = 0 AND p.migrated_comment_id IS NULL AND p.topic = 'ANIME'
          AND p.state IN ('VISIBLE', 'HIDDEN') AND DBMS_LOB.GETLENGTH(p.body) <= 900
          AND NOT EXISTS (SELECT 1 FROM fl_forum_image i WHERE i.post_id = p.id)
          AND REGEXP_LIKE(p.title, '^(第 [0-9]+ 集|预告 [0-9]+|特别篇 [0-9]+) · ')
          AND SUBSTR(p.title, INSTR(p.title, ' · ') + LENGTH(' · ')) =
              SUBSTR(REGEXP_REPLACE(DBMS_LOB.SUBSTR(p.body, 900, 1), '[[:space:]]+', ' '), 1, 48)
        ORDER BY p.work_id, p.created_at, p.id
    ) LOOP
        SELECT id INTO locked_work FROM fl_work WHERE id = legacy.work_id FOR UPDATE;
        SELECT MAX(id) INTO thread_id
        FROM fl_forum_post WHERE work_id = locked_work AND screening_thread = 1;
        IF thread_id IS NULL THEN
            SELECT fl_forum_post_seq.NEXTVAL INTO thread_id FROM dual;
            INSERT INTO fl_forum_post (id, author_id, work_id, topic, title, body, screening_thread)
            VALUES (thread_id, legacy.author_id, locked_work, 'ANIME',
                    SUBSTR(legacy.work_title, 1, 90) || ' · 放映评论', '放映室评论', 1);
        END IF;

        SELECT fl_forum_comment_seq.NEXTVAL INTO root_id FROM dual;
        INSERT INTO fl_forum_comment (id, post_id, author_id, body, state, created_at)
        VALUES (root_id, thread_id, legacy.author_id, legacy.body, legacy.state, legacy.created_at);
        INSERT INTO fl_screening_comment_like (comment_id, user_id)
        SELECT root_id, user_id FROM fl_forum_like WHERE post_id = legacy.id;

        copied.DELETE;
        FOR reply IN (
            SELECT c.id, c.parent_id, c.author_id, c.body, c.state, c.created_at
            FROM (SELECT * FROM fl_forum_comment WHERE post_id = legacy.id) c
            START WITH c.parent_id IS NULL
            CONNECT BY PRIOR c.id = c.parent_id
        ) LOOP
            new_parent := root_id;
            IF reply.parent_id IS NOT NULL THEN
                new_parent := copied(TO_CHAR(reply.parent_id));
            END IF;
            SELECT fl_forum_comment_seq.NEXTVAL INTO new_id FROM dual;
            INSERT INTO fl_forum_comment (id, post_id, author_id, parent_id, thread_root_id, body, state, created_at)
            VALUES (new_id, thread_id, reply.author_id, new_parent, root_id, reply.body, reply.state, reply.created_at);
            copied(TO_CHAR(reply.id)) := new_id;
            move_reports('COMMENT', reply.id, new_id);
        END LOOP;
        move_reports('POST', legacy.id, root_id);
        UPDATE fl_forum_post SET state = 'HIDDEN', migrated_comment_id = root_id WHERE id = legacy.id;
        migrated := migrated + 1;
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('Legacy player comments moved: ' || migrated);
END;
/
COMMIT;
