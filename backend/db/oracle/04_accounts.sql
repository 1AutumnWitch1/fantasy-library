-- Upgrade an existing catalogue once in its application schema.
CREATE TABLE fl_account (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY,
    username VARCHAR2(40 CHAR) NOT NULL,
    password_hash VARCHAR2(96 CHAR) NOT NULL,
    display_name VARCHAR2(60 CHAR) NOT NULL,
    bio VARCHAR2(240 CHAR),
    CONSTRAINT fl_account_pk PRIMARY KEY (id),
    CONSTRAINT fl_account_username_uq UNIQUE (username)
);

CREATE TABLE fl_login_token (
    digest VARCHAR2(64 CHAR) NOT NULL,
    user_id NUMBER(18) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fl_login_token_pk PRIMARY KEY (digest),
    CONSTRAINT fl_login_token_user_fk FOREIGN KEY (user_id) REFERENCES fl_account (id) ON DELETE CASCADE
);

CREATE INDEX fl_login_token_user_ix ON fl_login_token (user_id, expires_at);

CREATE TABLE fl_saved_work (
    user_id NUMBER(18) NOT NULL,
    work_id NUMBER(18) NOT NULL,
    work_type VARCHAR2(5 CHAR) NOT NULL,
    favorite NUMBER(1) NOT NULL,
    wishlist NUMBER(1) NOT NULL,
    watch_status VARCHAR2(16 CHAR),
    watched_episodes NUMBER(5) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fl_saved_work_pk PRIMARY KEY (user_id, work_id),
    CONSTRAINT fl_saved_work_user_fk FOREIGN KEY (user_id) REFERENCES fl_account (id) ON DELETE CASCADE,
    CONSTRAINT fl_saved_work_item_fk FOREIGN KEY (work_id, work_type)
        REFERENCES fl_work (id, work_type) ON DELETE CASCADE,
    CONSTRAINT fl_saved_work_shape_ck CHECK (
        (work_type = 'ANIME' AND favorite = 1 AND wishlist = 0
            AND watch_status IS NOT NULL
            AND watch_status IN ('PLAN_TO_WATCH','WATCHING','COMPLETED','ON_HOLD')
            AND watched_episodes >= 0)
        OR
        (work_type = 'GAME' AND favorite IN (0,1) AND wishlist IN (0,1)
            AND favorite + wishlist >= 1 AND watch_status IS NULL AND watched_episodes = 0)
    )
);

CREATE INDEX fl_saved_work_item_ix ON fl_saved_work (work_id, work_type);
