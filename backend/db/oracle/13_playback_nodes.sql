-- Forward migration for existing installations. Existing episode/source IDs are retained.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF

DECLARE
    present NUMBER;
BEGIN
    SELECT COUNT(*) INTO present FROM user_tables WHERE table_name = 'FL_PLAYBACK_NODE';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE fl_playback_node (
            id NUMBER(18) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            work_id NUMBER(18) NOT NULL REFERENCES fl_anime (work_id) ON DELETE CASCADE,
            label VARCHAR2(100 CHAR) NOT NULL,
            is_listed NUMBER(1) DEFAULT 1 NOT NULL CHECK (is_listed IN (0, 1)),
            CONSTRAINT fl_playback_node_label_uq UNIQUE (work_id, label),
            CONSTRAINT fl_playback_node_work_uq UNIQUE (work_id, id)
        )';
    END IF;

    SELECT COUNT(*) INTO present FROM user_tab_columns
    WHERE table_name = 'FL_EPISODE' AND column_name = 'NODE_ID';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE fl_episode ADD (node_id NUMBER(18))';
    END IF;
END;
/

INSERT INTO fl_playback_node (work_id, label, is_listed)
SELECT DISTINCT e.work_id, '默认线路', 1
FROM fl_episode e
WHERE e.node_id IS NULL
    AND NOT EXISTS (SELECT 1 FROM fl_playback_node n WHERE n.work_id = e.work_id);

UPDATE fl_episode e
SET node_id = (SELECT MIN(n.id) FROM fl_playback_node n WHERE n.work_id = e.work_id)
WHERE e.node_id IS NULL;

DECLARE
    present NUMBER;
BEGIN
    SELECT COUNT(*) INTO present FROM user_tab_columns
    WHERE table_name = 'FL_EPISODE' AND column_name = 'NODE_ID' AND nullable = 'Y';
    IF present > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE fl_episode MODIFY (node_id NOT NULL)';
    END IF;

    SELECT COUNT(*) INTO present FROM user_constraints WHERE constraint_name = 'FL_EPISODE_NODE_WORK_FK';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE fl_episode ADD CONSTRAINT fl_episode_node_work_fk
            FOREIGN KEY (work_id, node_id) REFERENCES fl_playback_node (work_id, id) ON DELETE CASCADE';
    END IF;

    SELECT COUNT(*) INTO present FROM user_constraints WHERE constraint_name = 'FL_EPISODE_NODE_ORDER_UQ';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE fl_episode ADD CONSTRAINT fl_episode_node_order_uq
            UNIQUE (work_id, node_id, edition, episode_no)';
    END IF;

    SELECT COUNT(*) INTO present FROM user_constraints WHERE constraint_name = 'FL_EPISODE_ORDER_UQ';
    IF present > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE fl_episode DROP CONSTRAINT fl_episode_order_uq';
    END IF;
END;
/

COMMIT;
