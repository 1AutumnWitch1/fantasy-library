-- Split only the intact bundled demo. Custom sources and already-migrated data are left alone.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF

DECLARE
    work NUMBER;
    origin NUMBER;
    target NUMBER;
    visible NUMBER;
    episodes NUMBER;
    original_sources NUMBER;
    seeded_sources NUMBER;
BEGIN
    SELECT MIN(n.work_id), MIN(n.id), MIN(n.is_listed) INTO work, origin, visible
    FROM fl_playback_node n JOIN fl_work w ON w.id = n.work_id
    WHERE w.slug = 'anime-mist-harbor' AND n.label = '默认线路'
        AND NOT EXISTS (
            SELECT 1 FROM fl_playback_node named WHERE named.work_id = n.work_id
                AND named.label IN ('馆内节点', '轻量节点')
        );

    IF origin IS NOT NULL THEN
        SELECT COUNT(*) INTO episodes FROM fl_episode WHERE node_id = origin;

        SELECT COUNT(*), COUNT(CASE WHEN e.edition = 'PREVIEW' AND e.episode_no IN (1, 2) AND (
            (s.label = '馆内节点' AND s.media_url = '/api/media/playback/mist-harbor-' || e.episode_no || '-standard.webm')
            OR
            (s.label = '轻量节点' AND s.media_url = '/api/media/playback/mist-harbor-' || e.episode_no || '-light.webm')
        ) THEN 1 END)
        INTO original_sources, seeded_sources
        FROM fl_episode_source s JOIN fl_episode e ON e.id = s.episode_id
        WHERE e.node_id = origin;

        IF episodes = 2 AND original_sources = 4 AND seeded_sources = 4 THEN
            UPDATE fl_playback_node SET label = '馆内节点' WHERE id = origin;
            INSERT INTO fl_playback_node (work_id, label, is_listed) VALUES (work, '轻量节点', visible)
            RETURNING id INTO target;

            FOR episode IN (SELECT id, work_id, edition, episode_no, title, duration_seconds
                            FROM fl_episode WHERE node_id = origin) LOOP
                DECLARE
                    copied NUMBER;
                BEGIN
                    INSERT INTO fl_episode (work_id, node_id, edition, episode_no, title, duration_seconds)
                    VALUES (episode.work_id, target, episode.edition, episode.episode_no, episode.title, episode.duration_seconds)
                    RETURNING id INTO copied;
                    UPDATE fl_episode_source SET episode_id = copied
                    WHERE episode_id = episode.id AND label = '轻量节点';
                END;
            END LOOP;
        END IF;
    END IF;
END;
/

COMMIT;
