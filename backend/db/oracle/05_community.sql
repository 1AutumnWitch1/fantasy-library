-- Apply once after 04_accounts.sql as the library application user.
ALTER TABLE fl_account ADD (
    account_role VARCHAR2(10 CHAR) DEFAULT 'MEMBER' NOT NULL
        CONSTRAINT fl_account_role_ck CHECK (account_role IN ('MEMBER', 'ADMIN'))
);

CREATE SEQUENCE fl_forum_post_seq START WITH 1 CACHE 20;
CREATE SEQUENCE fl_forum_comment_seq START WITH 1 CACHE 20;

CREATE TABLE fl_forum_post (
    id NUMBER(18) NOT NULL,
    author_id NUMBER(18) NOT NULL REFERENCES fl_account (id),
    work_id NUMBER(18) REFERENCES fl_work (id) ON DELETE SET NULL,
    topic VARCHAR2(12 CHAR) NOT NULL,
    title VARCHAR2(100 CHAR) NOT NULL,
    body CLOB NOT NULL,
    state VARCHAR2(8 CHAR) DEFAULT 'VISIBLE' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fl_forum_post_pk PRIMARY KEY (id),
    CONSTRAINT fl_forum_topic_ck CHECK (topic IN ('GENERAL','ANIME','GAME','GUIDE')),
    CONSTRAINT fl_forum_post_state_ck CHECK (state IN ('VISIBLE','HIDDEN','DELETED'))
);
CREATE INDEX fl_forum_work_ix ON fl_forum_post (work_id, state, created_at DESC, id DESC);
CREATE INDEX fl_forum_feed_ix ON fl_forum_post (state, created_at DESC, id DESC);

CREATE TABLE fl_forum_image (
    image_key VARCHAR2(40 CHAR) NOT NULL,
    post_id NUMBER(18) NOT NULL REFERENCES fl_forum_post (id),
    position NUMBER(1) NOT NULL,
    CONSTRAINT fl_forum_image_pk PRIMARY KEY (image_key),
    CONSTRAINT fl_forum_image_pos_uq UNIQUE (post_id, position)
);

CREATE TABLE fl_forum_comment (
    id NUMBER(18) NOT NULL,
    post_id NUMBER(18) NOT NULL REFERENCES fl_forum_post (id),
    author_id NUMBER(18) NOT NULL REFERENCES fl_account (id),
    parent_id NUMBER(18),
    body VARCHAR2(900 CHAR) NOT NULL,
    state VARCHAR2(8 CHAR) DEFAULT 'VISIBLE' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fl_forum_comment_pk PRIMARY KEY (id),
    CONSTRAINT fl_forum_comment_post_uq UNIQUE (id, post_id),
    CONSTRAINT fl_forum_comment_parent_fk FOREIGN KEY (parent_id, post_id)
        REFERENCES fl_forum_comment (id, post_id),
    CONSTRAINT fl_forum_comment_state_ck CHECK (state IN ('VISIBLE','HIDDEN','DELETED'))
);
CREATE INDEX fl_forum_thread_ix ON fl_forum_comment (post_id, state, created_at, id);
CREATE INDEX fl_forum_reply_ix ON fl_forum_comment (parent_id, post_id);

CREATE TABLE fl_forum_like (
    post_id NUMBER(18) NOT NULL REFERENCES fl_forum_post (id),
    user_id NUMBER(18) NOT NULL REFERENCES fl_account (id),
    CONSTRAINT fl_forum_like_pk PRIMARY KEY (post_id, user_id)
);

CREATE TABLE fl_forum_report (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY,
    reporter_id NUMBER(18) NOT NULL REFERENCES fl_account (id),
    target_type VARCHAR2(7 CHAR) NOT NULL,
    target_id NUMBER(18) NOT NULL,
    reason VARCHAR2(240 CHAR) NOT NULL,
    state VARCHAR2(9 CHAR) DEFAULT 'OPEN' NOT NULL,
    handled_by NUMBER(18) REFERENCES fl_account (id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    handled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fl_forum_report_pk PRIMARY KEY (id),
    CONSTRAINT fl_forum_report_target_uq UNIQUE (reporter_id, target_type, target_id),
    CONSTRAINT fl_forum_report_type_ck CHECK (target_type IN ('POST','COMMENT')),
    CONSTRAINT fl_forum_report_state_ck CHECK (state IN ('OPEN','DISMISSED','HIDDEN','DELETED'))
);
CREATE INDEX fl_forum_review_ix ON fl_forum_report (state, created_at, id);
CREATE INDEX fl_forum_report_item_ix ON fl_forum_report (target_type, target_id, state);
