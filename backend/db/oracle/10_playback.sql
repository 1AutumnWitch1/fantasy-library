-- Apply after 09_personal_library.sql in the application schema.
CREATE TABLE fl_episode (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    work_id NUMBER(18) NOT NULL REFERENCES fl_anime (work_id) ON DELETE CASCADE,
    edition VARCHAR2(7 CHAR) NOT NULL CHECK (edition IN ('EPISODE', 'PREVIEW')),
    episode_no NUMBER(6) NOT NULL CHECK (episode_no > 0),
    title VARCHAR2(180 CHAR) NOT NULL,
    duration_seconds NUMBER(7) NOT NULL CHECK (duration_seconds > 0),
    CONSTRAINT fl_episode_order_uq UNIQUE (work_id, edition, episode_no)
);

CREATE TABLE fl_episode_source (
    id NUMBER(18) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    episode_id NUMBER(18) NOT NULL REFERENCES fl_episode (id) ON DELETE CASCADE,
    label VARCHAR2(100 CHAR) NOT NULL,
    quality VARCHAR2(40 CHAR) NOT NULL,
    media_url VARCHAR2(1000 CHAR) NOT NULL,
    format VARCHAR2(4 CHAR) NOT NULL CHECK (format IN ('MP4', 'WEBM')),
    sort_order NUMBER(6) DEFAULT 0 NOT NULL CHECK (sort_order >= 0),
    CONSTRAINT fl_ep_source_order_uq UNIQUE (episode_id, sort_order)
);

INSERT INTO fl_episode (work_id, edition, episode_no, title, duration_seconds)
SELECT w.id, 'PREVIEW', clip.episode_no, clip.title, clip.duration_seconds
FROM fl_work w JOIN fl_anime a ON a.work_id = w.id
CROSS JOIN (
    SELECT 1 AS episode_no, '月下的来信' AS title, 14 AS duration_seconds FROM dual
    UNION ALL SELECT 2, '点亮灯塔', 12 FROM dual
) clip
WHERE w.slug = 'anime-mist-harbor';

INSERT INTO fl_episode_source (episode_id, label, quality, media_url, format, sort_order)
SELECT e.id, node.label, node.quality,
       '/api/media/playback/mist-harbor-' || e.episode_no || '-' || node.suffix || '.webm',
       'WEBM', node.sort_order
FROM fl_episode e JOIN fl_work w ON w.id = e.work_id
CROSS JOIN (
    SELECT '馆内节点' AS label, '720p' AS quality, 'standard' AS suffix, 0 AS sort_order FROM dual
    UNION ALL SELECT '轻量节点', '360p', 'light', 1 FROM dual
) node
WHERE w.slug = 'anime-mist-harbor' AND e.edition = 'PREVIEW' AND e.episode_no IN (1, 2);

COMMIT;
