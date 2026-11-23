-- Fantasy Library catalog schema. Run once in the application schema.
-- Oracle 19c or later. Object and constraint names stay within 30 bytes.

CREATE TABLE fl_category (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY,
    slug VARCHAR2(60 CHAR) NOT NULL,
    name VARCHAR2(80 CHAR) NOT NULL,
    sort_order NUMBER(6) DEFAULT 0 NOT NULL,
    CONSTRAINT fl_category_pk PRIMARY KEY (id),
    CONSTRAINT fl_category_slug_uq UNIQUE (slug),
    CONSTRAINT fl_category_order_ck CHECK (sort_order >= 0)
);

CREATE TABLE fl_tag (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY,
    slug VARCHAR2(60 CHAR) NOT NULL,
    name VARCHAR2(80 CHAR) NOT NULL,
    CONSTRAINT fl_tag_pk PRIMARY KEY (id),
    CONSTRAINT fl_tag_slug_uq UNIQUE (slug)
);

CREATE TABLE fl_platform (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY,
    slug VARCHAR2(60 CHAR) NOT NULL,
    name VARCHAR2(80 CHAR) NOT NULL,
    sort_order NUMBER(6) DEFAULT 0 NOT NULL,
    CONSTRAINT fl_platform_pk PRIMARY KEY (id),
    CONSTRAINT fl_platform_slug_uq UNIQUE (slug),
    CONSTRAINT fl_platform_order_ck CHECK (sort_order >= 0)
);

CREATE TABLE fl_work (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY,
    work_type VARCHAR2(5 CHAR) NOT NULL,
    slug VARCHAR2(90 CHAR) NOT NULL,
    title VARCHAR2(180 CHAR) NOT NULL,
    summary VARCHAR2(900 CHAR) NOT NULL,
    release_date DATE,
    category_id NUMBER(18) NOT NULL,
    CONSTRAINT fl_work_pk PRIMARY KEY (id),
    CONSTRAINT fl_work_slug_uq UNIQUE (slug),
    CONSTRAINT fl_work_id_type_uq UNIQUE (id, work_type),
    CONSTRAINT fl_work_type_ck CHECK (work_type IN ('ANIME', 'GAME')),
    CONSTRAINT fl_work_category_fk FOREIGN KEY (category_id) REFERENCES fl_category (id)
);

CREATE TABLE fl_anime (
    work_id NUMBER(18),
    work_type VARCHAR2(5 CHAR) DEFAULT 'ANIME' NOT NULL,
    episode_count NUMBER(5),
    studio VARCHAR2(150 CHAR) NOT NULL,
    airing_status VARCHAR2(10 CHAR) NOT NULL,
    CONSTRAINT fl_anime_pk PRIMARY KEY (work_id),
    CONSTRAINT fl_anime_type_ck CHECK (work_type = 'ANIME'),
    CONSTRAINT fl_anime_episodes_ck CHECK (episode_count > 0),
    CONSTRAINT fl_anime_status_ck CHECK (airing_status IN ('UPCOMING', 'ONGOING', 'COMPLETED')),
    CONSTRAINT fl_anime_work_fk FOREIGN KEY (work_id, work_type) REFERENCES fl_work (id, work_type) ON DELETE CASCADE
);

CREATE TABLE fl_game (
    work_id NUMBER(18),
    work_type VARCHAR2(5 CHAR) DEFAULT 'GAME' NOT NULL,
    developer VARCHAR2(150 CHAR) NOT NULL,
    age_rating VARCHAR2(20 CHAR),
    CONSTRAINT fl_game_pk PRIMARY KEY (work_id),
    CONSTRAINT fl_game_type_ck CHECK (work_type = 'GAME'),
    CONSTRAINT fl_game_work_fk FOREIGN KEY (work_id, work_type) REFERENCES fl_work (id, work_type) ON DELETE CASCADE
);

CREATE TABLE fl_work_tag (
    work_id NUMBER(18),
    tag_id NUMBER(18),
    CONSTRAINT fl_work_tag_pk PRIMARY KEY (work_id, tag_id),
    CONSTRAINT fl_work_tag_work_fk FOREIGN KEY (work_id) REFERENCES fl_work (id) ON DELETE CASCADE,
    CONSTRAINT fl_work_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES fl_tag (id)
);

CREATE TABLE fl_game_platform (
    work_id NUMBER(18),
    platform_id NUMBER(18),
    CONSTRAINT fl_game_platform_pk PRIMARY KEY (work_id, platform_id),
    CONSTRAINT fl_gp_game_fk FOREIGN KEY (work_id) REFERENCES fl_game (work_id) ON DELETE CASCADE,
    CONSTRAINT fl_gp_platform_fk FOREIGN KEY (platform_id) REFERENCES fl_platform (id)
);

CREATE TABLE fl_work_image (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY,
    work_id NUMBER(18) NOT NULL,
    image_kind VARCHAR2(10 CHAR) NOT NULL,
    resource_url VARCHAR2(500 CHAR) NOT NULL,
    alt_text VARCHAR2(180 CHAR) NOT NULL,
    sort_order NUMBER(6) DEFAULT 0 NOT NULL,
    CONSTRAINT fl_work_image_pk PRIMARY KEY (id),
    CONSTRAINT fl_image_work_fk FOREIGN KEY (work_id) REFERENCES fl_work (id) ON DELETE CASCADE,
    CONSTRAINT fl_image_order_uq UNIQUE (work_id, image_kind, sort_order),
    CONSTRAINT fl_image_kind_ck CHECK (image_kind IN ('COVER', 'GALLERY')),
    CONSTRAINT fl_image_order_ck CHECK (sort_order >= 0 AND (image_kind <> 'COVER' OR sort_order = 0))
);

CREATE INDEX fl_work_type_release_ix ON fl_work (work_type, release_date DESC, id DESC);
CREATE INDEX fl_work_cat_release_ix ON fl_work (category_id, work_type, release_date DESC, id DESC);
CREATE INDEX fl_work_tag_tag_ix ON fl_work_tag (tag_id, work_id);
CREATE INDEX fl_gp_platform_ix ON fl_game_platform (platform_id, work_id);
