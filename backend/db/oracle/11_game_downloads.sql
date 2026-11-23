-- Apply after 10_playback.sql. Register only files and links you are authorized to distribute.
CREATE TABLE fl_game_download (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    work_id NUMBER(18) NOT NULL,
    platform_id NUMBER(18) NOT NULL,
    label VARCHAR2(100 CHAR) NOT NULL,
    external_url VARCHAR2(1000 CHAR),
    file_name VARCHAR2(180 CHAR),
    sort_order NUMBER(6) DEFAULT 0 NOT NULL CHECK (sort_order >= 0),
    CONSTRAINT fl_game_download_platform_fk FOREIGN KEY (work_id, platform_id)
        REFERENCES fl_game_platform (work_id, platform_id) ON DELETE CASCADE,
    CONSTRAINT fl_game_download_location_ck CHECK (
        (external_url IS NOT NULL AND file_name IS NULL) OR (file_name IS NOT NULL AND external_url IS NULL)
    ),
    CONSTRAINT fl_game_download_label_uq UNIQUE (work_id, platform_id, label)
);
