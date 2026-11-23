-- Allow the existing durable resource queue to also retire catalog images.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF

DECLARE
    present NUMBER;
BEGIN
    SELECT COUNT(*) INTO present FROM user_constraints
    WHERE constraint_name = 'FL_RESOURCE_RETIREMENT_KIND_CK';
    IF present = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE fl_resource_retirement ADD CONSTRAINT fl_resource_retirement_kind_ck
            CHECK (reference_kind IN (''PLAYBACK'', ''DOWNLOAD'', ''CATALOG''))';
    END IF;

    FOR old_check IN (
        SELECT constraint_name FROM user_constraints
        WHERE table_name = 'FL_RESOURCE_RETIREMENT'
            AND constraint_type = 'C'
            AND constraint_name <> 'FL_RESOURCE_RETIREMENT_KIND_CK'
            AND UPPER(search_condition_vc) LIKE '%REFERENCE_KIND%PLAYBACK%DOWNLOAD%'
    ) LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE fl_resource_retirement DROP CONSTRAINT '
            || DBMS_ASSERT.ENQUOTE_NAME(old_check.constraint_name, FALSE);
    END LOOP;
END;
/
