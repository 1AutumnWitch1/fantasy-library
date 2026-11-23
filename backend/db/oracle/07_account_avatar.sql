-- Apply once after 06_community_views.sql.
ALTER TABLE fl_account ADD (
    avatar_key VARCHAR2(40 CHAR) CONSTRAINT fl_account_avatar_uq UNIQUE
);
