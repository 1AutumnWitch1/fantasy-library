-- Fictional catalog examples with local artwork. Re-running adds missing rows.
-- Existing rows with matching identifiers keep their original values.
MERGE INTO fl_category target
USING (
    SELECT 'fantasy' AS slug, '奇幻' AS name, 10 AS sort_order FROM dual
    UNION ALL
    SELECT 'mystery', '悬疑推理', 20 FROM dual
    UNION ALL
    SELECT 'action', '动作冒险', 30 FROM dual
    UNION ALL
    SELECT 'puzzle', '探索解谜', 40 FROM dual
    UNION ALL
    SELECT 'simulation', '模拟经营', 50 FROM dual
) seed
ON (target.slug = seed.slug)
WHEN NOT MATCHED THEN INSERT (slug, name, sort_order)
    VALUES (seed.slug, seed.name, seed.sort_order);

MERGE INTO fl_tag target
USING (
    SELECT 'adventure' AS slug, '冒险' AS name FROM dual
    UNION ALL
    SELECT 'cozy', '治愈' FROM dual
    UNION ALL
    SELECT 'fantasy', '奇幻' FROM dual
    UNION ALL
    SELECT 'folklore', '民俗' FROM dual
    UNION ALL
    SELECT 'mystery', '悬疑' FROM dual
    UNION ALL
    SELECT 'puzzle', '解谜' FROM dual
) seed
ON (target.slug = seed.slug)
WHEN NOT MATCHED THEN INSERT (slug, name)
    VALUES (seed.slug, seed.name);

MERGE INTO fl_platform target
USING (
    SELECT 'pc' AS slug, 'PC' AS name, 10 AS sort_order FROM dual
    UNION ALL
    SELECT 'ps5', 'PS5', 20 FROM dual
    UNION ALL
    SELECT 'switch', 'Switch', 30 FROM dual
    UNION ALL
    SELECT 'xbox-series', 'Xbox Series X|S', 40 FROM dual
) seed
ON (target.slug = seed.slug)
WHEN NOT MATCHED THEN INSERT (slug, name, sort_order)
    VALUES (seed.slug, seed.name, seed.sort_order);

MERGE INTO fl_work target
USING (
    SELECT src.slug, src.work_type, src.title, src.summary, src.release_date, c.id AS category_id
    FROM (
        SELECT 'anime-mist-harbor' AS slug, 'ANIME' AS work_type, '雾港来信' AS title, '潮水退去之前，年轻邮差必须把一封寄自昨日的信送到灯塔。' AS summary, DATE '2026-06-21' AS release_date, 'fantasy' AS category_slug FROM dual
        UNION ALL
        SELECT 'anime-star-court', 'ANIME', '星庭遗录', '观星学徒发现一颗每夜都会移动的星，开始追查旧观星台失落的日志。', DATE '2026-04-12', 'mystery' FROM dual
        UNION ALL
        SELECT 'anime-bamboo-night', 'ANIME', '青岚夜行', '旅人循着一盏灯穿过竹林，沿途听白狐讲起村落的旧传说。', DATE '2025-12-06', 'fantasy' FROM dual
        UNION ALL
        SELECT 'game-ash-vigil', 'GAME', '余火守夜人', '在黑曜石废城中寻找火种的动作冒险游戏，支持 PC 与 PS5。', DATE '2026-08-20', 'action' FROM dual
        UNION ALL
        SELECT 'game-tide-ruins', 'GAME', '空潮遗迹', '利用地下水位开启古代机关，在晶石地窟中探索地图。', DATE '2026-05-14', 'puzzle' FROM dual
        UNION ALL
        SELECT 'game-moss-greenhouse', 'GAME', '苔光温室', '在玻璃花房播种和改造温室，照料一株会在夜里发光的花。', DATE '2026-03-01', 'simulation' FROM dual
    ) src
    JOIN fl_category c ON c.slug = src.category_slug
) seed
ON (target.slug = seed.slug)
WHEN NOT MATCHED THEN INSERT (slug, work_type, title, summary, release_date, category_id)
    VALUES (seed.slug, seed.work_type, seed.title, seed.summary, seed.release_date, seed.category_id);

MERGE INTO fl_anime target
USING (
    SELECT w.id AS work_id, src.episode_count, src.studio, src.airing_status
    FROM (
        SELECT 'anime-mist-harbor' AS work_slug, 12 AS episode_count, '潮汐动画工坊' AS studio, 'COMPLETED' AS airing_status FROM dual
        UNION ALL
        SELECT 'anime-star-court', 8, '星庭映画', 'COMPLETED' FROM dual
        UNION ALL
        SELECT 'anime-bamboo-night', 10, '青岚作画室', 'COMPLETED' FROM dual
    ) src
    JOIN fl_work w ON w.slug = src.work_slug AND w.work_type = 'ANIME'
) seed
ON (target.work_id = seed.work_id)
WHEN NOT MATCHED THEN INSERT (work_id, episode_count, studio, airing_status)
    VALUES (seed.work_id, seed.episode_count, seed.studio, seed.airing_status);

MERGE INTO fl_game target
USING (
    SELECT w.id AS work_id, src.developer, src.age_rating
    FROM (
        SELECT 'game-ash-vigil' AS work_slug, '余火工坊' AS developer, '16+' AS age_rating FROM dual
        UNION ALL
        SELECT 'game-tide-ruins', '逆潮互动', '12+' FROM dual
        UNION ALL
        SELECT 'game-moss-greenhouse', '春苔工作室', '全年龄' FROM dual
    ) src
    JOIN fl_work w ON w.slug = src.work_slug AND w.work_type = 'GAME'
) seed
ON (target.work_id = seed.work_id)
WHEN NOT MATCHED THEN INSERT (work_id, developer, age_rating)
    VALUES (seed.work_id, seed.developer, seed.age_rating);

MERGE INTO fl_work_tag target
USING (
    SELECT w.id AS work_id, t.id AS tag_id
    FROM (
        SELECT 'anime-mist-harbor' AS work_slug, 'fantasy' AS tag_slug FROM dual
        UNION ALL
        SELECT 'anime-mist-harbor', 'mystery' FROM dual
        UNION ALL
        SELECT 'anime-star-court', 'mystery' FROM dual
        UNION ALL
        SELECT 'anime-star-court', 'puzzle' FROM dual
        UNION ALL
        SELECT 'anime-bamboo-night', 'folklore' FROM dual
        UNION ALL
        SELECT 'anime-bamboo-night', 'fantasy' FROM dual
        UNION ALL
        SELECT 'game-ash-vigil', 'fantasy' FROM dual
        UNION ALL
        SELECT 'game-ash-vigil', 'adventure' FROM dual
        UNION ALL
        SELECT 'game-tide-ruins', 'puzzle' FROM dual
        UNION ALL
        SELECT 'game-tide-ruins', 'adventure' FROM dual
        UNION ALL
        SELECT 'game-moss-greenhouse', 'cozy' FROM dual
    ) src
    JOIN fl_work w ON w.slug = src.work_slug
    JOIN fl_tag t ON t.slug = src.tag_slug
) seed
ON (target.work_id = seed.work_id AND target.tag_id = seed.tag_id)
WHEN NOT MATCHED THEN INSERT (work_id, tag_id)
    VALUES (seed.work_id, seed.tag_id);

MERGE INTO fl_game_platform target
USING (
    SELECT g.work_id, p.id AS platform_id
    FROM (
        SELECT 'game-ash-vigil' AS work_slug, 'pc' AS platform_slug FROM dual
        UNION ALL
        SELECT 'game-ash-vigil', 'ps5' FROM dual
        UNION ALL
        SELECT 'game-tide-ruins', 'pc' FROM dual
        UNION ALL
        SELECT 'game-tide-ruins', 'xbox-series' FROM dual
        UNION ALL
        SELECT 'game-moss-greenhouse', 'pc' FROM dual
        UNION ALL
        SELECT 'game-moss-greenhouse', 'switch' FROM dual
    ) src
    JOIN fl_work w ON w.slug = src.work_slug
    JOIN fl_game g ON g.work_id = w.id
    JOIN fl_platform p ON p.slug = src.platform_slug
) seed
ON (target.work_id = seed.work_id AND target.platform_id = seed.platform_id)
WHEN NOT MATCHED THEN INSERT (work_id, platform_id)
    VALUES (seed.work_id, seed.platform_id);

MERGE INTO fl_work_image target
USING (
    SELECT w.id AS work_id, src.image_kind, src.resource_url, src.alt_text, src.sort_order
    FROM (
        SELECT 'anime-mist-harbor' AS work_slug, 'COVER' AS image_kind, '/api/media/catalog/mist-harbor.webp' AS resource_url, '邮差手持盖着火漆的信站在雾港码头' AS alt_text, 0 AS sort_order FROM dual
        UNION ALL
        SELECT 'anime-mist-harbor', 'GALLERY', '/api/media/catalog/mist-harbor-banner.webp', '月色下的雾港和远处的灯火', 1 FROM dual
        UNION ALL
        SELECT 'anime-star-court', 'COVER', '/api/media/catalog/star-court.webp', '观星学徒站在浑仪和星空下', 0 FROM dual
        UNION ALL
        SELECT 'anime-bamboo-night', 'COVER', '/api/media/catalog/bamboo-night.webp', '提灯旅人与白狐来到竹林古寺', 0 FROM dual
        UNION ALL
        SELECT 'game-ash-vigil', 'COVER', '/api/media/catalog/ash-vigil.webp', '提剑的守夜人走过燃着火盆的废城', 0 FROM dual
        UNION ALL
        SELECT 'game-tide-ruins', 'COVER', '/api/media/catalog/tide-ruins.webp', '探索者停在晶石地窟的水边', 0 FROM dual
        UNION ALL
        SELECT 'game-moss-greenhouse', 'COVER', '/api/media/catalog/moss-greenhouse.webp', '园艺师在阳光下照料温室里的花', 0 FROM dual
    ) src
    JOIN fl_work w ON w.slug = src.work_slug
) seed
ON (target.work_id = seed.work_id AND target.image_kind = seed.image_kind AND target.sort_order = seed.sort_order)
WHEN NOT MATCHED THEN INSERT (work_id, image_kind, resource_url, alt_text, sort_order)
    VALUES (seed.work_id, seed.image_kind, seed.resource_url, seed.alt_text, seed.sort_order);

COMMIT;
