-- Additive fictional editions for the anime and game books. Apply after 02_seed.sql.
-- Existing work IDs, metadata, covers and associations are preserved on replay.
DECLARE
    PROCEDURE add_tag(p_slug VARCHAR2, p_name VARCHAR2) IS
    BEGIN
        INSERT INTO fl_tag (slug, name)
        SELECT p_slug, p_name FROM dual
        WHERE NOT EXISTS (SELECT 1 FROM fl_tag WHERE slug = p_slug);
    END;

    PROCEDURE link_tags(p_work VARCHAR2, p_tags SYS.ODCIVARCHAR2LIST) IS
    BEGIN
        INSERT INTO fl_work_tag (work_id, tag_id)
        SELECT w.id, t.id FROM fl_work w CROSS JOIN fl_tag t
        WHERE w.slug = p_work AND t.slug IN (SELECT column_value FROM TABLE(p_tags))
          AND NOT EXISTS (SELECT 1 FROM fl_work_tag wt WHERE wt.work_id = w.id AND wt.tag_id = t.id);
    END;

    FUNCTION add_work(p_kind VARCHAR2, p_slug VARCHAR2, p_title VARCHAR2, p_summary VARCHAR2,
                      p_date DATE, p_category VARCHAR2, p_cover VARCHAR2) RETURN NUMBER IS
        v_id fl_work.id%TYPE;
    BEGIN
        INSERT INTO fl_work (work_type, slug, title, summary, release_date, category_id)
        SELECT p_kind, p_slug, p_title, p_summary, p_date, c.id FROM fl_category c
        WHERE c.slug = p_category AND NOT EXISTS (SELECT 1 FROM fl_work WHERE slug = p_slug);
        SELECT id INTO v_id FROM fl_work WHERE slug = p_slug AND work_type = p_kind;
        INSERT INTO fl_work_image (work_id, image_kind, resource_url, alt_text, sort_order)
        SELECT v_id, 'COVER', '/api/media/catalog/editions/' || p_cover || '.webp', p_title || '封面', 0 FROM dual
        WHERE NOT EXISTS (SELECT 1 FROM fl_work_image WHERE work_id = v_id AND image_kind = 'COVER' AND sort_order = 0);
        RETURN v_id;
    END;

    PROCEDURE anime(p_slug VARCHAR2, p_title VARCHAR2, p_summary VARCHAR2, p_date DATE, p_category VARCHAR2,
                    p_cover VARCHAR2, p_episodes NUMBER, p_studio VARCHAR2, p_tags SYS.ODCIVARCHAR2LIST) IS
        v_id NUMBER;
    BEGIN
        v_id := add_work('ANIME', p_slug, p_title, p_summary, p_date, p_category, p_cover);
        INSERT INTO fl_anime (work_id, episode_count, studio, airing_status)
        SELECT v_id, p_episodes, p_studio, 'COMPLETED' FROM dual
        WHERE NOT EXISTS (SELECT 1 FROM fl_anime WHERE work_id = v_id);
        link_tags(p_slug, p_tags);
    END;

    PROCEDURE game(p_slug VARCHAR2, p_title VARCHAR2, p_summary VARCHAR2, p_date DATE, p_category VARCHAR2,
                   p_cover VARCHAR2, p_developer VARCHAR2, p_platforms SYS.ODCIVARCHAR2LIST, p_tags SYS.ODCIVARCHAR2LIST) IS
        v_id NUMBER;
    BEGIN
        v_id := add_work('GAME', p_slug, p_title, p_summary, p_date, p_category, p_cover);
        INSERT INTO fl_game (work_id, developer)
        SELECT v_id, p_developer FROM dual WHERE NOT EXISTS (SELECT 1 FROM fl_game WHERE work_id = v_id);
        INSERT INTO fl_game_platform (work_id, platform_id)
        SELECT v_id, p.id FROM fl_platform p WHERE p.slug IN (SELECT column_value FROM TABLE(p_platforms))
          AND NOT EXISTS (SELECT 1 FROM fl_game_platform gp WHERE gp.work_id = v_id AND gp.platform_id = p.id);
        link_tags(p_slug, p_tags);
    END;
BEGIN
    add_tag('passion', '热血');
    add_tag('battle', '战斗');
    add_tag('romance', '恋爱');
    add_tag('campus', '校园');
    add_tag('daily', '日常');
    add_tag('comedy', '搞笑');
    add_tag('cozy', '治愈');
    add_tag('tearjerker', '催泪');
    add_tag('mystery', '悬疑');
    add_tag('detective', '推理');
    add_tag('horror', '恐怖');
    add_tag('fantasy', '奇幻');
    add_tag('adventure', '冒险');
    add_tag('isekai', '异世界');
    add_tag('time-travel', '穿越');
    add_tag('magic', '魔法');
    add_tag('scifi', '科幻');
    add_tag('mecha', '机战');
    add_tag('athletics', '运动');
    add_tag('music', '音乐');
    add_tag('idol', '偶像');
    add_tag('history', '历史');
    add_tag('period', '古风');
    add_tag('wuxia', '武侠');
    add_tag('xianxia', '仙侠');
    add_tag('food', '美食');
    add_tag('inspiration', '励志');
    add_tag('workplace', '职场');
    add_tag('ensemble', '群像');
    add_tag('folklore', '民俗');
    add_tag('puzzle', '解谜');
    add_tag('action', '动作');
    add_tag('rpg', '角色扮演');
    add_tag('open-world', '开放世界');
    add_tag('shooter', '射击');
    add_tag('strategy', '策略');
    add_tag('simulation', '模拟经营');
    add_tag('fps', 'FPS');
    add_tag('tps', 'TPS');
    add_tag('moba', 'MOBA');
    add_tag('mmorpg', 'MMORPG');
    add_tag('turn-based', '回合制');
    add_tag('rts', '即时战略');
    add_tag('tactics', '战棋');
    add_tag('deckbuilding', '卡牌');
    add_tag('fighting', '格斗');
    add_tag('racing', '竞速');
    add_tag('sports', '体育');
    add_tag('rhythm', '音乐节奏');
    add_tag('platformer', '平台跳跃');
    add_tag('survival', '生存');
    add_tag('sandbox', '沙盒');
    add_tag('building', '建造');
    add_tag('farming', '种田');
    add_tag('roguelike', '肉鸽');
    add_tag('soulslike', '类魂');
    add_tag('dungeon', '地牢');
    add_tag('visual-novel', '视觉小说');
    add_tag('story', '剧情');
    add_tag('management', '养成');
    add_tag('exploration', '探索');
    add_tag('casual', '休闲');
    add_tag('party', '派对');
    add_tag('single-player', '单人');
    add_tag('multiplayer', '多人联机');
    add_tag('co-op', '多人合作');
    add_tag('competitive', '竞技');
    add_tag('tower-defense', '塔防');

    INSERT INTO fl_platform (slug, name, sort_order)
    SELECT 'android', 'Android', 50 FROM dual WHERE NOT EXISTS (SELECT 1 FROM fl_platform WHERE slug = 'android');
    INSERT INTO fl_platform (slug, name, sort_order)
    SELECT 'ios', 'iOS', 60 FROM dual WHERE NOT EXISTS (SELECT 1 FROM fl_platform WHERE slug = 'ios');
    INSERT INTO fl_category (slug, name, sort_order)
    SELECT 'drama', '青春日常', 60 FROM dual WHERE NOT EXISTS (SELECT 1 FROM fl_category WHERE slug = 'drama');
    INSERT INTO fl_category (slug, name, sort_order)
    SELECT 'scifi', '科幻未来', 70 FROM dual WHERE NOT EXISTS (SELECT 1 FROM fl_category WHERE slug = 'scifi');
    INSERT INTO fl_category (slug, name, sort_order)
    SELECT 'history', '历史古风', 80 FROM dual WHERE NOT EXISTS (SELECT 1 FROM fl_category WHERE slug = 'history');
    INSERT INTO fl_category (slug, name, sort_order)
    SELECT 'sports', '运动竞技', 90 FROM dual WHERE NOT EXISTS (SELECT 1 FROM fl_category WHERE slug = 'sports');
    INSERT INTO fl_category (slug, name, sort_order)
    SELECT 'strategy', '策略战术', 100 FROM dual WHERE NOT EXISTS (SELECT 1 FROM fl_category WHERE slug = 'strategy');
    INSERT INTO fl_category (slug, name, sort_order)
    SELECT 'story', '叙事冒险', 110 FROM dual WHERE NOT EXISTS (SELECT 1 FROM fl_category WHERE slug = 'story');

    link_tags('anime-mist-harbor', SYS.ODCIVARCHAR2LIST('time-travel', 'adventure'));
    link_tags('anime-star-court', SYS.ODCIVARCHAR2LIST('detective'));
    link_tags('anime-bamboo-night', SYS.ODCIVARCHAR2LIST('cozy', 'period', 'adventure'));

    anime('anime-relay-last', '接力赛的最后一棒',
          '田径队的替补跑者接下最后一棒，和队友重练雨天的交接。', DATE '2025-10-18', 'sports', 'genres-modern-1', 12, '风铃动画',
          SYS.ODCIVARCHAR2LIST('athletics', 'campus', 'passion', 'inspiration', 'ensemble'));

    anime('anime-idol-debut', '偶像部的第一场演出',
          '五名学生借来旧礼堂，把各自写的一段旋律排成毕业演出。', DATE '2025-10-06', 'drama', 'genres-modern-2', 8, '纸月作画室',
          SYS.ODCIVARCHAR2LIST('music', 'idol', 'campus', 'ensemble'));

    anime('anime-cat-bakery', '猫咪面包店',
          '猫咪面包师在街角收下奇怪的早餐订单，替邻居解开一桩桩小误会。', DATE '2025-09-18', 'drama', 'genres-modern-3', 10, '杏雨动画',
          SYS.ODCIVARCHAR2LIST('food', 'daily', 'comedy', 'cozy'));

    anime('anime-dawn-mecha', '晨光机甲 · 出击',
          '港区维修生修复一架退役机甲，和救援队赶往轨道电梯。', DATE '2025-09-06', 'scifi', 'genres-modern-4', 13, '晨光映画',
          SYS.ODCIVARCHAR2LIST('mecha', 'scifi', 'battle', 'passion'));

    anime('anime-clocktower', '时钟塔的另一侧',
          '修钟学徒误入被定格的异世界，用齿轮魔法找回小镇消失的一天。', DATE '2025-08-18', 'fantasy', 'genres-modern-5', 12, '风铃动画',
          SYS.ODCIVARCHAR2LIST('isekai', 'time-travel', 'magic', 'fantasy', 'adventure'));

    anime('anime-star-ring', '向星环起航',
          '少年航海员接过失联探测器的讯号，穿过星环寻找返航坐标。', DATE '2025-08-06', 'scifi', 'genres-modern-6', 8, '纸月作画室',
          SYS.ODCIVARCHAR2LIST('scifi', 'adventure'));

    anime('anime-pine-duel', '松风桥试剑',
          '渡桥的剑客与旧友约定以一场比试平息两座山庄的恩怨。', DATE '2025-07-18', 'history', 'genres-classic-1', 10, '杏雨动画',
          SYS.ODCIVARCHAR2LIST('wuxia', 'period', 'battle'));

    anime('anime-cloud-sword', '云山问剑',
          '见习剑修沿山道寻找失落的剑谱，一路学会与山灵相处。', DATE '2025-07-06', 'fantasy', 'genres-classic-2', 13, '晨光映画',
          SYS.ODCIVARCHAR2LIST('xianxia', 'period', 'magic', 'battle'));

    anime('anime-old-lanterns', '长街灯会',
          '灯会开市前夜，乐师、厨娘与制灯匠共同赶制一条游灯长街。', DATE '2025-06-18', 'history', 'genres-classic-3', 12, '风铃动画',
          SYS.ODCIVARCHAR2LIST('history', 'period', 'music', 'ensemble', 'food'));

    anime('anime-night-campus', '夜半校舍',
          '值日生发现熄灯后的教室每晚会多出一个座位，约同学追查旧档案。', DATE '2025-06-06', 'mystery', 'genres-classic-4', 8, '纸月作画室',
          SYS.ODCIVARCHAR2LIST('horror', 'mystery', 'campus'));

    anime('anime-drawing-room', '动画工作室的一天',
          '新人原画师赶上片场最忙的一周，从第一张修正稿学起。', DATE '2025-05-18', 'drama', 'genres-classic-5', 10, '杏雨动画',
          SYS.ODCIVARCHAR2LIST('workplace', 'daily', 'ensemble', 'inspiration'));

    anime('anime-last-train', '末班车到站之前',
          '两名毕业生赶赴同一座车站，在末班列车开出前交换迟来的回信。', DATE '2025-05-06', 'drama', 'genres-classic-6', 13, '晨光映画',
          SYS.ODCIVARCHAR2LIST('tearjerker', 'romance', 'cozy'));

    anime('anime-old-post', '邮局里的旧风声',
          '邮局新任档案员从投递戳的差异入手，追查一封失踪多年的挂号信。', DATE '2025-11-22', 'mystery', 'video-places-2', 12, '风铃动画',
          SYS.ODCIVARCHAR2LIST('detective', 'mystery', 'workplace'));

    link_tags('game-ash-vigil', SYS.ODCIVARCHAR2LIST('action', 'rpg', 'soulslike', 'single-player'));

    game('game-north-courier', '北境驿道',
         '冒着风雪翻越吊桥，赶在封山前把信送到。', DATE '2026-07-12', 'story', 'mock-game-covers-2', '驿路工坊',
         SYS.ODCIVARCHAR2LIST('pc', 'switch'), SYS.ODCIVARCHAR2LIST('adventure', 'survival', 'exploration', 'single-player'));

    link_tags('game-tide-ruins', SYS.ODCIVARCHAR2LIST('puzzle', 'adventure', 'dungeon', 'exploration'));

    link_tags('game-moss-greenhouse', SYS.ODCIVARCHAR2LIST('farming', 'simulation', 'casual', 'building'));

    game('game-cloud-fleet', '云海航团',
         '与好友驾驶飞艇往来于浮岛之间，搭建自己的空港。', DATE '2025-10-19', 'fantasy', 'mock-game-covers-5', '铜叶工作室',
         SYS.ODCIVARCHAR2LIST('pc', 'ps5'), SYS.ODCIVARCHAR2LIST('open-world', 'sandbox', 'building', 'co-op', 'exploration'));

    game('game-autumn-kitchen', '秋炉食堂',
         '在晚餐高峰和好友分工，为归来的冒险者准备炖菜。', DATE '2025-10-04', 'simulation', 'mock-game-covers-6', '风屿互动',
         SYS.ODCIVARCHAR2LIST('pc', 'switch'), SYS.ODCIVARCHAR2LIST('simulation', 'casual', 'co-op', 'party'));

    game('game-dune-rally', '赤砂拉力赛',
         '沿着沙海驿道漂移，在最后一个发卡弯抢下领先位置。', DATE '2025-09-19', 'sports', 'game-genres-1', '驿路工坊',
         SYS.ODCIVARCHAR2LIST('pc', 'ps5', 'xbox-series'), SYS.ODCIVARCHAR2LIST('racing', 'sports', 'competitive', 'multiplayer'));

    game('game-dusk-front', '暮河战线',
         '组建五人小队，沿三条河道推进，击破对方的水晶塔。', DATE '2025-09-04', 'strategy', 'game-genres-2', '灯湾互动',
         SYS.ODCIVARCHAR2LIST('pc', 'android', 'ios'), SYS.ODCIVARCHAR2LIST('moba', 'strategy', 'competitive', 'multiplayer'));

    game('game-brass-gambit', '黄铜棋局',
         '构筑战牌，在棋盘地城中指挥骑士逐格突围。', DATE '2025-08-19', 'strategy', 'game-genres-3', '云帆游戏社',
         SYS.ODCIVARCHAR2LIST('pc', 'switch'), SYS.ODCIVARCHAR2LIST('strategy', 'tactics', 'deckbuilding', 'turn-based', 'roguelike'));

    game('game-lantern-duel', '灯影对决',
         '在屋脊的灯火间拆招，挑战各地擂台的守擂人。', DATE '2025-08-04', 'action', 'game-genres-4', '铜叶工作室',
         SYS.ODCIVARCHAR2LIST('pc', 'ps5'), SYS.ODCIVARCHAR2LIST('fighting', 'action', 'competitive', 'multiplayer'));

    game('game-starport-zero', '零号星港',
         '从第一人称视角护送队友穿过封锁线，在警报响起前撤离。', DATE '2025-07-19', 'action', 'game-genres-5', '风屿互动',
         SYS.ODCIVARCHAR2LIST('pc', 'xbox-series'), SYS.ODCIVARCHAR2LIST('shooter', 'fps', 'survival', 'co-op'));

    game('game-string-walker', '星弦步旅',
         '跟随弓弦的拍点，在月光照亮的浮石间跳跃。', DATE '2025-07-04', 'action', 'game-genres-6', '驿路工坊',
         SYS.ODCIVARCHAR2LIST('pc', 'switch', 'ios'), SYS.ODCIVARCHAR2LIST('rhythm', 'platformer', 'adventure', 'casual'));

    game('game-wind-relay', '逆风接力',
         '训练校队，掌握交接棒时机，与好友比拼最后一百米。', DATE '2025-06-19', 'sports', 'genres-modern-1', '灯湾互动',
         SYS.ODCIVARCHAR2LIST('pc', 'switch'), SYS.ODCIVARCHAR2LIST('sports', 'competitive', 'management', 'party'));

    game('game-mech-line', '机甲防线',
         '切换肩后视角拦截敌机，部署防御塔掩护工程队。', DATE '2025-06-04', 'action', 'genres-modern-4', '云帆游戏社',
         SYS.ODCIVARCHAR2LIST('pc', 'ps5'), SYS.ODCIVARCHAR2LIST('shooter', 'tps', 'tower-defense', 'rts', 'co-op'));

    game('game-sky-covenant', '九霄灵契',
         '拜入山门，与同服伙伴结队挑战云海深处的秘境。', DATE '2025-05-19', 'fantasy', 'genres-classic-2', '铜叶工作室',
         SYS.ODCIVARCHAR2LIST('pc', 'android'), SYS.ODCIVARCHAR2LIST('mmorpg', 'rpg', 'open-world', 'multiplayer', 'management'));

    game('game-midnight-hall', '零点回廊',
         '夜巡校舍，借走廊回声破解门锁，躲开巡游的影子。', DATE '2025-05-04', 'mystery', 'genres-classic-4', '风屿互动',
         SYS.ODCIVARCHAR2LIST('pc', 'switch'), SYS.ODCIVARCHAR2LIST('horror', 'puzzle', 'survival', 'story'));

    game('game-platform-letters', '月台来信',
         '在末班车到站前选择回信，解开两个人错过的往事。', DATE '2025-04-19', 'story', 'genres-classic-6', '驿路工坊',
         SYS.ODCIVARCHAR2LIST('pc', 'switch', 'ios'), SYS.ODCIVARCHAR2LIST('romance', 'visual-novel', 'story', 'single-player'));

    game('game-ring-pioneer', '星环拓荒记',
         '采集小行星上的材料，建设一个能够自给自足的空间站。', DATE '2025-04-04', 'scifi', 'genres-modern-6', '灯湾互动',
         SYS.ODCIVARCHAR2LIST('pc', 'xbox-series'), SYS.ODCIVARCHAR2LIST('sandbox', 'survival', 'building', 'open-world'));

    game('game-time-cogs', '逆时齿轮',
         '在每次重置的钟楼里拾取新齿轮，组合技能闯过机关。', DATE '2025-03-19', 'puzzle', 'genres-modern-5', '云帆游戏社',
         SYS.ODCIVARCHAR2LIST('pc', 'switch'), SYS.ODCIVARCHAR2LIST('roguelike', 'action', 'platformer', 'dungeon'));

    game('game-lantern-market', '长街百业',
         '规划灯会摊位、招募艺人，为旧城搭起一年一度的大集。', DATE '2025-03-04', 'simulation', 'genres-classic-3', '铜叶工作室',
         SYS.ODCIVARCHAR2LIST('pc', 'android', 'ios'), SYS.ODCIVARCHAR2LIST('simulation', 'building', 'strategy', 'casual'));
END;
/
COMMIT;

