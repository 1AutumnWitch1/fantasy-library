-- Apply after 08_recommendations.sql in the application schema.
CREATE TABLE fl_watch_history (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id NUMBER(18) NOT NULL REFERENCES fl_account (id) ON DELETE CASCADE,
    work_id NUMBER(18) NOT NULL REFERENCES fl_anime (work_id) ON DELETE CASCADE,
    watch_status VARCHAR2(16 CHAR) NOT NULL
        CHECK (watch_status IN ('PLAN_TO_WATCH','WATCHING','COMPLETED','ON_HOLD')),
    watched_episodes NUMBER(5) NOT NULL CHECK (watched_episodes >= 0),
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
);
CREATE INDEX fl_watch_history_feed_ix ON fl_watch_history (user_id, recorded_at DESC, id DESC);
CREATE INDEX fl_watch_history_work_ix ON fl_watch_history (work_id);

CREATE TABLE fl_inbox_notice (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipient_id NUMBER(18) NOT NULL REFERENCES fl_account (id) ON DELETE CASCADE,
    actor_id NUMBER(18) NOT NULL REFERENCES fl_account (id) ON DELETE CASCADE,
    post_id NUMBER(18) NOT NULL REFERENCES fl_forum_post (id) ON DELETE CASCADE,
    comment_id NUMBER(18),
    kind VARCHAR2(13 CHAR) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fl_inbox_comment_fk FOREIGN KEY (comment_id, post_id)
        REFERENCES fl_forum_comment (id, post_id) ON DELETE CASCADE,
    CONSTRAINT fl_inbox_people_ck CHECK (recipient_id <> actor_id),
    CONSTRAINT fl_inbox_kind_ck CHECK (
        (kind = 'POST_LIKE' AND comment_id IS NULL)
        OR (kind IN ('POST_COMMENT', 'COMMENT_REPLY') AND comment_id IS NOT NULL)
    ),
    CONSTRAINT fl_inbox_event_uq UNIQUE (recipient_id, actor_id, post_id, kind, comment_id)
);
CREATE INDEX fl_inbox_feed_ix ON fl_inbox_notice (recipient_id, created_at DESC, id DESC);
CREATE INDEX fl_inbox_post_ix ON fl_inbox_notice (post_id);
CREATE INDEX fl_inbox_comment_ix ON fl_inbox_notice (comment_id, post_id);
CREATE INDEX fl_inbox_actor_ix ON fl_inbox_notice (actor_id);

-- Preserve only the latest known pre-migration progress, with its original update time.
INSERT INTO fl_watch_history (user_id, work_id, watch_status, watched_episodes, recorded_at)
SELECT user_id, work_id, watch_status, watched_episodes, updated_at
FROM fl_saved_work
WHERE work_type = 'ANIME' AND (watched_episodes > 0 OR watch_status IN ('WATCHING', 'COMPLETED'));

-- Import visible comments and replies using their existing timestamps.
-- Pre-migration likes have no recorded timestamps.
INSERT INTO fl_inbox_notice (recipient_id, actor_id, post_id, comment_id, kind, created_at)
SELECT p.author_id, c.author_id, p.id, c.id, 'POST_COMMENT', c.created_at
FROM fl_forum_comment c JOIN fl_forum_post p ON p.id = c.post_id
LEFT JOIN fl_forum_comment parent ON parent.id = c.parent_id AND parent.state = 'VISIBLE'
WHERE p.state = 'VISIBLE' AND c.state = 'VISIBLE' AND p.author_id <> c.author_id
  AND (parent.author_id IS NULL OR parent.author_id <> p.author_id)
UNION ALL
SELECT parent.author_id, c.author_id, p.id, c.id, 'COMMENT_REPLY', c.created_at
FROM fl_forum_comment c JOIN fl_forum_post p ON p.id = c.post_id
JOIN fl_forum_comment parent ON parent.id = c.parent_id AND parent.state = 'VISIBLE'
WHERE p.state = 'VISIBLE' AND c.state = 'VISIBLE' AND parent.author_id <> c.author_id;

COMMIT;
