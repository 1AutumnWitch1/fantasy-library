WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF
DECLARE
    PROCEDURE create_object(ddl VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE ddl;
    EXCEPTION
        WHEN OTHERS THEN IF SQLCODE <> -955 THEN RAISE; END IF;
    END;
BEGIN
    create_object(q'[
        CREATE TABLE fl_episode_progress (
            user_id NUMBER(18) REFERENCES fl_account(id) ON DELETE CASCADE,
            work_id NUMBER(18) REFERENCES fl_anime(work_id) ON DELETE CASCADE,
            episode_no NUMBER(6),
            episode_id NUMBER(18) REFERENCES fl_episode(id) ON DELETE SET NULL,
            position_ms NUMBER(12) NOT NULL,
            duration_ms NUMBER(12) NOT NULL,
            coverage CLOB NOT NULL,
            completed NUMBER(1) NOT NULL CHECK (completed IN (0,1)),
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
            PRIMARY KEY (user_id, work_id, episode_no)
        )]');
    create_object('CREATE INDEX fl_episode_progress_work_ix ON fl_episode_progress(work_id)');
    create_object('CREATE INDEX fl_episode_progress_ep_ix ON fl_episode_progress(episode_id)');
    create_object(q'[
        CREATE TABLE fl_play_session (
            id VARCHAR2(36 CHAR) PRIMARY KEY,
            viewer_key VARCHAR2(70 CHAR) NOT NULL,
            user_id NUMBER(18) REFERENCES fl_account(id) ON DELETE CASCADE,
            source_id NUMBER(18) NOT NULL REFERENCES fl_episode_source(id) ON DELETE CASCADE,
            last_position_ms NUMBER(12) NOT NULL,
            last_sample_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
            watched_ms NUMBER(12) DEFAULT 0 NOT NULL,
            settled NUMBER(1) DEFAULT 0 NOT NULL
        )]');
    create_object('CREATE INDEX fl_play_session_source_ix ON fl_play_session(source_id)');
    create_object('CREATE INDEX fl_play_session_user_ix ON fl_play_session(user_id)');
    create_object('CREATE INDEX fl_play_session_expiry_ix ON fl_play_session(last_sample_at)');
    create_object(q'[
        CREATE TABLE fl_play_total (
            work_id NUMBER(18) REFERENCES fl_anime(work_id) ON DELETE CASCADE,
            edition VARCHAR2(7 CHAR),
            episode_no NUMBER(6),
            viewer_key VARCHAR2(70 CHAR),
            play_count NUMBER(18) DEFAULT 0 NOT NULL,
            last_counted_at TIMESTAMP WITH TIME ZONE,
            PRIMARY KEY (work_id, edition, episode_no, viewer_key)
        )]');
END;
/
