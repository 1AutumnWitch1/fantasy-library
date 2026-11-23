-- Serialize reference changes with queue creation. The queue survives a storage outage or restart.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF

DECLARE
    present NUMBER;
BEGIN
    SELECT COUNT(*) INTO present FROM user_tables WHERE table_name = 'FL_RESOURCE_REFERENCE_GUARD';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE fl_resource_reference_guard (
            id NUMBER(1) PRIMARY KEY CHECK (id = 1)
        )';
    END IF;

    SELECT COUNT(*) INTO present FROM user_tables WHERE table_name = 'FL_RESOURCE_RETIREMENT';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE fl_resource_retirement (
            object_key VARCHAR2(1000 CHAR) PRIMARY KEY,
            reference_kind VARCHAR2(8 CHAR) NOT NULL CHECK (reference_kind IN (''PLAYBACK'', ''DOWNLOAD'')),
            retry_after TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
        )';
    END IF;
END;
/

MERGE INTO fl_resource_reference_guard guard
USING (SELECT 1 AS id FROM dual) seed ON (guard.id = seed.id)
WHEN NOT MATCHED THEN INSERT (id) VALUES (seed.id);

COMMIT;
