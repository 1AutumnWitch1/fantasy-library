-- Apply after 11_game_downloads.sql. Existing publication states are preserved on retries.
DECLARE
    object_count NUMBER;

    PROCEDURE add_flag(target_table VARCHAR2, target_column VARCHAR2, initial_value NUMBER) IS
    BEGIN
        SELECT COUNT(*) INTO object_count FROM user_tab_columns
        WHERE table_name = target_table AND column_name = target_column;
        IF object_count = 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || target_table || ' ADD (' || target_column
                || ' NUMBER(1) DEFAULT ' || initial_value || ' NOT NULL CHECK (' || target_column || ' IN (0, 1)))';
        END IF;
    END;
BEGIN
    add_flag('FL_WORK', 'IS_LISTED', 1);
    add_flag('FL_EPISODE_SOURCE', 'IS_LISTED', 1);
    add_flag('FL_GAME_DOWNLOAD', 'IS_LISTED', 1);
    add_flag('FL_ACCOUNT', 'IS_DELETED', 0);

    SELECT COUNT(*) INTO object_count FROM user_tables WHERE table_name = 'FL_COMMUNITY_SETTING';
    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE fl_community_setting (
            id NUMBER(1) PRIMARY KEY CHECK (id = 1),
            is_open NUMBER(1) NOT NULL CHECK (is_open IN (0, 1))
        )';
    END IF;

    EXECUTE IMMEDIATE 'MERGE INTO fl_community_setting setting
        USING (SELECT 1 AS id FROM dual) seed ON (setting.id = seed.id)
        WHEN NOT MATCHED THEN INSERT (id, is_open) VALUES (seed.id, 1)';
END;
/
COMMIT;
