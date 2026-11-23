-- Apply once after 05_community.sql.
ALTER TABLE fl_forum_post ADD (
    view_count NUMBER(18) DEFAULT 0 NOT NULL
        CONSTRAINT fl_forum_post_views_ck CHECK (view_count >= 0)
);
