package com.example.demo.modules.game.rpg.config;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;

/** Initializes the web RPG with the canonical Homework03 game data. */
@Component
public class RpgDataInitializer {
    private final JdbcTemplate jdbc;

    public RpgDataInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void initialize() {
        createTables();
        migrateDraftSchema();
        seedProfessions();
        migrateDraftProfessionCodes();
        backfillCharacterVitals();
        seedRegions();
        seedSkills();
        seedItems();
        seedItemEffects();
        backfillCharacterSkills();
        seedMonsters();
        seedMonsterItemDrops();
        seedEquipmentAndShopData();
        seedMonsterSkills();
        seedMonsterTraits();
        seedStages();
        removeObsoleteDraftSkills();
    }

    private void createTables() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_professions (
                    profession_code VARCHAR(20) PRIMARY KEY,
                    profession_name VARCHAR(40) NOT NULL,
                    description VARCHAR(300) NOT NULL,
                    base_hp INTEGER NOT NULL,
                    base_mp INTEGER NOT NULL,
                    base_atk INTEGER NOT NULL,
                    base_ap INTEGER NOT NULL,
                    base_def INTEGER NOT NULL,
                    base_mdef INTEGER NOT NULL DEFAULT 0,
                    base_speed INTEGER NOT NULL,
                    growth_hp INTEGER NOT NULL DEFAULT 0,
                    growth_mp INTEGER NOT NULL DEFAULT 0,
                    growth_atk INTEGER NOT NULL DEFAULT 0,
                    growth_ap INTEGER NOT NULL DEFAULT 0,
                    growth_def INTEGER NOT NULL DEFAULT 0,
                    growth_mdef INTEGER NOT NULL DEFAULT 0,
                    growth_speed INTEGER NOT NULL DEFAULT 0
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_regions (
                    region_code VARCHAR(20) PRIMARY KEY,
                    region_order INTEGER NOT NULL UNIQUE,
                    region_name VARCHAR(60) NOT NULL,
                    region_type VARCHAR(30) NOT NULL,
                    description VARCHAR(300) NOT NULL,
                    min_level INTEGER NOT NULL,
                    max_level INTEGER NOT NULL,
                    image_path VARCHAR(240)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_monsters (
                    monster_code VARCHAR(20) PRIMARY KEY,
                    monster_name VARCHAR(60) NOT NULL,
                    description VARCHAR(300) NOT NULL,
                    rank_code VARCHAR(20) NOT NULL DEFAULT 'K001',
                    race_code VARCHAR(20) NOT NULL DEFAULT 'MR001',
                    max_hp INTEGER NOT NULL,
                    max_mp INTEGER NOT NULL DEFAULT 0,
                    attack_value INTEGER NOT NULL,
                    magic_value INTEGER NOT NULL DEFAULT 0,
                    defense_value INTEGER NOT NULL,
                    magic_defense_value INTEGER NOT NULL DEFAULT 0,
                    speed_value INTEGER NOT NULL DEFAULT 100,
                    growth_hp INTEGER NOT NULL DEFAULT 0,
                    growth_mp INTEGER NOT NULL DEFAULT 0,
                    growth_atk INTEGER NOT NULL DEFAULT 0,
                    growth_ap INTEGER NOT NULL DEFAULT 0,
                    growth_def INTEGER NOT NULL DEFAULT 0,
                    growth_mdef INTEGER NOT NULL DEFAULT 0,
                    growth_speed INTEGER NOT NULL DEFAULT 0,
                    reward_exp INTEGER NOT NULL,
                    reward_exp_growth_rate REAL NOT NULL DEFAULT 1.25,
                    reward_gold INTEGER NOT NULL,
                    reward_gold_growth INTEGER NOT NULL DEFAULT 0,
                    image_path VARCHAR(240)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_stages (
                    stage_code VARCHAR(20) PRIMARY KEY,
                    stage_order INTEGER NOT NULL UNIQUE,
                    region_code VARCHAR(20),
                    region_name VARCHAR(60) NOT NULL,
                    stage_name VARCHAR(80) NOT NULL,
                    description VARCHAR(400) NOT NULL,
                    recommended_level INTEGER NOT NULL DEFAULT 1,
                    monster_code VARCHAR(20) NOT NULL,
                    monster_min_level INTEGER NOT NULL DEFAULT 1,
                    monster_max_level INTEGER NOT NULL DEFAULT 1,
                    monster_spawn_weight INTEGER NOT NULL DEFAULT 1,
                    image_path VARCHAR(240),
                    enabled BOOLEAN NOT NULL DEFAULT 1,
                    FOREIGN KEY (region_code) REFERENCES rpg_regions(region_code),
                    FOREIGN KEY (monster_code) REFERENCES rpg_monsters(monster_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_skills (
                    skill_code VARCHAR(30) PRIMARY KEY,
                    profession_code VARCHAR(20),
                    skill_name VARCHAR(60) NOT NULL,
                    description VARCHAR(300) NOT NULL,
                    skill_type VARCHAR(20) NOT NULL,
                    mp_cost INTEGER NOT NULL,
                    base_power INTEGER NOT NULL,
                    scaling_percent INTEGER NOT NULL,
                    required_level INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY (profession_code) REFERENCES rpg_professions(profession_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_monster_skills (
                    monster_code VARCHAR(20) NOT NULL,
                    skill_code VARCHAR(30) NOT NULL,
                    use_weight INTEGER NOT NULL,
                    priority INTEGER NOT NULL,
                    PRIMARY KEY (monster_code, skill_code),
                    FOREIGN KEY (monster_code) REFERENCES rpg_monsters(monster_code),
                    FOREIGN KEY (skill_code) REFERENCES rpg_skills(skill_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_monster_traits (
                    monster_code VARCHAR(20) NOT NULL,
                    trait_code VARCHAR(20) NOT NULL,
                    source_type VARCHAR(20) NOT NULL,
                    trait_name VARCHAR(60) NOT NULL,
                    description VARCHAR(300) NOT NULL,
                    stat_name VARCHAR(20),
                    percent_value INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (monster_code, trait_code, source_type),
                    FOREIGN KEY (monster_code) REFERENCES rpg_monsters(monster_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_characters (
                    character_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id BIGINT NOT NULL,
                    character_name VARCHAR(30) NOT NULL,
                    profession_code VARCHAR(20) NOT NULL,
                    level INTEGER NOT NULL DEFAULT 1,
                    experience INTEGER NOT NULL DEFAULT 0,
                    gold INTEGER NOT NULL DEFAULT 0,
                    current_hp INTEGER,
                    current_mp INTEGER,
                    created_at VARCHAR(19) NOT NULL DEFAULT (datetime('now')),
                    updated_at VARCHAR(19) NOT NULL DEFAULT (datetime('now')),
                    UNIQUE (user_id, character_name),
                    FOREIGN KEY (profession_code) REFERENCES rpg_professions(profession_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_character_stage_progress (
                    character_id INTEGER NOT NULL,
                    stage_code VARCHAR(20) NOT NULL,
                    unlocked BOOLEAN NOT NULL DEFAULT 0,
                    clear_count INTEGER NOT NULL DEFAULT 0,
                    best_turns INTEGER,
                    updated_at VARCHAR(19) NOT NULL DEFAULT (datetime('now')),
                    PRIMARY KEY (character_id, stage_code),
                    FOREIGN KEY (character_id) REFERENCES rpg_characters(character_id),
                    FOREIGN KEY (stage_code) REFERENCES rpg_stages(stage_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_character_skills (
                    character_id INTEGER NOT NULL,
                    skill_code VARCHAR(30) NOT NULL,
                    equipped BOOLEAN NOT NULL DEFAULT 0,
                    slot_number INTEGER NOT NULL DEFAULT 0,
                    learned_at VARCHAR(19) NOT NULL DEFAULT (datetime('now')),
                    PRIMARY KEY (character_id, skill_code),
                    FOREIGN KEY (character_id) REFERENCES rpg_characters(character_id),
                    FOREIGN KEY (skill_code) REFERENCES rpg_skills(skill_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_items (
                    item_code VARCHAR(20) PRIMARY KEY,
                    item_name VARCHAR(60) NOT NULL,
                    item_type VARCHAR(24) NOT NULL,
                    description VARCHAR(300) NOT NULL,
                    max_stack INTEGER NOT NULL DEFAULT 99,
                    sell_price INTEGER NOT NULL DEFAULT 0
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_item_effects (
                    effect_code VARCHAR(20) PRIMARY KEY,
                    item_code VARCHAR(20) NOT NULL,
                    effect_type VARCHAR(30) NOT NULL,
                    effect_order INTEGER NOT NULL DEFAULT 1,
                    flat_value INTEGER NOT NULL DEFAULT 0,
                    percent_value REAL NOT NULL DEFAULT 0,
                    FOREIGN KEY (item_code) REFERENCES rpg_items(item_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_character_items (
                    character_id INTEGER NOT NULL,
                    item_code VARCHAR(20) NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (character_id, item_code),
                    FOREIGN KEY (character_id) REFERENCES rpg_characters(character_id),
                    FOREIGN KEY (item_code) REFERENCES rpg_items(item_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_monster_item_drops (
                    monster_code VARCHAR(20) NOT NULL,
                    item_code VARCHAR(20) NOT NULL,
                    drop_rate REAL NOT NULL DEFAULT 0,
                    min_quantity INTEGER NOT NULL DEFAULT 1,
                    max_quantity INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY (monster_code, item_code),
                    FOREIGN KEY (monster_code) REFERENCES rpg_monsters(monster_code),
                    FOREIGN KEY (item_code) REFERENCES rpg_items(item_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_battle_item_drops (
                    battle_id VARCHAR(36) NOT NULL,
                    item_code VARCHAR(20) NOT NULL,
                    quantity INTEGER NOT NULL,
                    PRIMARY KEY (battle_id, item_code),
                    FOREIGN KEY (battle_id) REFERENCES rpg_battles(battle_id),
                    FOREIGN KEY (item_code) REFERENCES rpg_items(item_code)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_battle_equipment_drops (
                    battle_id VARCHAR(36) NOT NULL,
                    equipment_id VARCHAR(20) NOT NULL,
                    equipment_name VARCHAR(60) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_equipment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    equipment_id VARCHAR(20) NOT NULL UNIQUE,
                    equipment_name VARCHAR(60) NOT NULL,
                    equipment_type VARCHAR(24) NOT NULL,
                    usage_type VARCHAR(20) NOT NULL,
                    required_level INTEGER NOT NULL DEFAULT 1,
                    description VARCHAR(300) NOT NULL,
                    sell_price INTEGER NOT NULL DEFAULT 0
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_equipment_profession (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    equipment_id VARCHAR(20) NOT NULL,
                    profession_id VARCHAR(20) NOT NULL,
                    UNIQUE(equipment_id,profession_id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_equipment_stat (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    equipment_id VARCHAR(20) NOT NULL,
                    stat_type VARCHAR(30) NOT NULL,
                    modifier_type VARCHAR(20) NOT NULL,
                    modifier_value REAL NOT NULL DEFAULT 0
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_character_equipment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    character_id INTEGER NOT NULL,
                    equipment_id VARCHAR(20) NOT NULL,
                    equipped_slot VARCHAR(24)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_monster_equipment_drop (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    monster_id VARCHAR(20) NOT NULL,
                    equipment_id VARCHAR(20) NOT NULL,
                    drop_rate REAL NOT NULL DEFAULT 0,
                    UNIQUE(monster_id,equipment_id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_shop (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    shop_id VARCHAR(20) NOT NULL UNIQUE,
                    shop_name VARCHAR(60) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_shop_equipment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    shop_id VARCHAR(20) NOT NULL,
                    equipment_id VARCHAR(20) NOT NULL,
                    buy_price INTEGER NOT NULL DEFAULT 0,
                    purchase_limit INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(shop_id,equipment_id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_shop_item (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    shop_id VARCHAR(20) NOT NULL,
                    item_id VARCHAR(20) NOT NULL,
                    buy_price INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(shop_id,item_id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_shop_material_requirement (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    shop_id VARCHAR(20) NOT NULL,
                    product_type VARCHAR(20) NOT NULL,
                    product_id VARCHAR(20) NOT NULL,
                    material_item_id VARCHAR(20) NOT NULL,
                    required_quantity INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(shop_id,product_type,product_id,material_item_id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_character_shop_purchase (
                    character_id INTEGER NOT NULL,
                    shop_equipment_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(character_id,shop_equipment_id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_shop_buyback (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    character_id INTEGER NOT NULL,
                    product_type VARCHAR(20) NOT NULL,
                    product_id VARCHAR(20) NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 1,
                    sell_price INTEGER NOT NULL DEFAULT 0,
                    buyback_price INTEGER NOT NULL DEFAULT 0
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rpg_battles (
                    battle_id VARCHAR(36) PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    character_id INTEGER NOT NULL,
                    stage_code VARCHAR(20) NOT NULL,
                    monster_level INTEGER NOT NULL DEFAULT 1,
                    player_hp INTEGER NOT NULL,
                    max_player_hp INTEGER NOT NULL,
                    player_mp INTEGER NOT NULL,
                    max_player_mp INTEGER NOT NULL,
                    player_atk INTEGER NOT NULL,
                    player_ap INTEGER NOT NULL,
                    player_def INTEGER NOT NULL,
                    player_mdef INTEGER NOT NULL DEFAULT 0,
                    player_speed INTEGER NOT NULL DEFAULT 100,
                    player_action REAL NOT NULL DEFAULT 0,
                    monster_hp INTEGER NOT NULL,
                    max_monster_hp INTEGER NOT NULL,
                    monster_mp INTEGER NOT NULL DEFAULT 0,
                    max_monster_mp INTEGER NOT NULL DEFAULT 0,
                    monster_atk INTEGER NOT NULL,
                    monster_ap INTEGER NOT NULL DEFAULT 0,
                    monster_def INTEGER NOT NULL,
                    monster_mdef INTEGER NOT NULL DEFAULT 0,
                    monster_speed INTEGER NOT NULL DEFAULT 100,
                    monster_action REAL NOT NULL DEFAULT 0,
                    turn_number INTEGER NOT NULL DEFAULT 1,
                    battle_status VARCHAR(20) NOT NULL,
                    log_text TEXT NOT NULL,
                    effect_state TEXT NOT NULL DEFAULT '',
                    created_at VARCHAR(19) NOT NULL DEFAULT (datetime('now')),
                    updated_at VARCHAR(19) NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (character_id) REFERENCES rpg_characters(character_id),
                    FOREIGN KEY (stage_code) REFERENCES rpg_stages(stage_code)
                )
                """);
    }

    private void migrateDraftSchema() {
        addColumnIfMissing("rpg_professions", "growth_hp", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_professions", "growth_mp", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_professions", "growth_atk", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_professions", "growth_ap", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_professions", "base_mdef", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_professions", "growth_def", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_professions", "growth_mdef", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_professions", "growth_speed", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_stages", "region_code", "VARCHAR(20)");
        addColumnIfMissing("rpg_stages", "recommended_level", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing("rpg_stages", "monster_min_level", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing("rpg_stages", "monster_max_level", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing("rpg_stages", "monster_spawn_weight", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing("rpg_skills", "required_level", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing("rpg_characters", "current_hp", "INTEGER");
        addColumnIfMissing("rpg_characters", "current_mp", "INTEGER");
        addColumnIfMissing("rpg_monsters", "rank_code", "VARCHAR(20) NOT NULL DEFAULT 'K001'");
        addColumnIfMissing("rpg_monsters", "race_code", "VARCHAR(20) NOT NULL DEFAULT 'MR001'");
        addColumnIfMissing("rpg_monsters", "max_mp", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_monsters", "magic_value", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_monsters", "magic_defense_value", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_monsters", "speed_value", "INTEGER NOT NULL DEFAULT 100");
        addColumnIfMissing("rpg_monsters", "growth_hp", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_monsters", "growth_mp", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_monsters", "growth_atk", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_monsters", "growth_ap", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_monsters", "growth_def", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_monsters", "growth_mdef", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_monsters", "growth_speed", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_monsters", "reward_exp_growth_rate", "REAL NOT NULL DEFAULT 1.25");
        addColumnIfMissing("rpg_monsters", "reward_gold_growth", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_battles", "player_mdef", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_battles", "monster_level", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing("rpg_battles", "player_speed", "INTEGER NOT NULL DEFAULT 100");
        addColumnIfMissing("rpg_battles", "player_action", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_battles", "monster_mp", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_battles", "max_monster_mp", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_battles", "monster_ap", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_battles", "monster_mdef", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_battles", "monster_speed", "INTEGER NOT NULL DEFAULT 100");
        addColumnIfMissing("rpg_battles", "monster_action", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing("rpg_battles", "effect_state", "TEXT NOT NULL DEFAULT ''");
        jdbc.update("""
                UPDATE rpg_battles SET battle_status = 'ABANDONED', updated_at = datetime('now')
                WHERE battle_status = 'ACTIVE' AND player_action = 0 AND monster_action = 0
                """);
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        List<String> columns = jdbc.query("PRAGMA table_info(" + table + ")",
                (rs, row) -> rs.getString("name"));
        if (!columns.contains(column)) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void seedProfessions() {
        // IDs, base values and growth values are copied directly from Homework03.
        seedProfession("E001", "戰士", "擅長物理攻擊與防禦", 500, 80, 50, 10, 40, 28, 100, 60, 8, 7, 2, 6, 0, 1);
        seedProfession("E002", "法師", "擅長使用魔法造成傷害", 300, 300, 15, 60, 20, 35, 100, 35, 45, 2, 8, 3, 0, 1);
        seedProfession("E003", "刺客", "速度較快並能攜帶更多技能", 350, 120, 45, 20, 25, 20, 120, 40, 15, 6, 3, 4, 0, 3);
        seedProfession("E004", "聖騎士", "持盾守護同伴，兼具防禦與神聖力量", 470, 150, 28, 45, 45, 50, 92, 55, 22, 5, 5, 8, 0, 1);
        seedProfession("E005", "遊俠", "善用弓箭與野外知識的敏捷獵手", 365, 130, 48, 22, 28, 25, 118, 42, 18, 7, 3, 4, 0, 3);
        seedProfession("E006", "牧師", "以祈禱治癒傷勢並驅散黑暗", 330, 280, 18, 55, 26, 35, 98, 38, 40, 2, 8, 4, 0, 1);
    }

    private void migrateDraftProfessionCodes() {
        String[][] mappings = {
                {"WARRIOR", "E001"}, {"MAGE", "E002"}, {"ASSASSIN", "E003"},
                {"PALADIN", "E004"}, {"RANGER", "E005"}, {"PRIEST", "E006"}
        };
        for (String[] mapping : mappings) {
            jdbc.update("UPDATE rpg_characters SET profession_code=? WHERE profession_code=?",
                    mapping[1], mapping[0]);
            jdbc.update("UPDATE rpg_skills SET profession_code=? WHERE profession_code=?",
                    mapping[1], mapping[0]);
            jdbc.update("DELETE FROM rpg_professions WHERE profession_code=?", mapping[0]);
        }
    }

    /**
     * Characters created by the earlier web draft did not store their remaining HP/MP.
     * Initialize only those missing values from the same level and profession rules used
     * by the battle service, so an existing database can be upgraded without deletion.
     */
    private void backfillCharacterVitals() {
        jdbc.update("""
                UPDATE rpg_characters AS c
                SET current_hp = COALESCE(current_hp, (
                        SELECT CAST(
                            (p.base_hp + (c.level - 1) * p.growth_hp)
                            * CASE WHEN c.profession_code = 'E004' THEN 1.20 ELSE 1.00 END
                            AS INTEGER)
                        FROM rpg_professions AS p
                        WHERE p.profession_code = c.profession_code
                    )),
                    current_mp = COALESCE(current_mp, (
                        SELECT p.base_mp + (c.level - 1) * p.growth_mp
                            + CASE WHEN c.profession_code = 'E002' THEN CAST(
                                (p.base_ap + (c.level - 1) * p.growth_ap) * 0.30 AS INTEGER)
                              ELSE 0 END
                        FROM rpg_professions AS p
                        WHERE p.profession_code = c.profession_code
                    )),
                    updated_at = datetime('now')
                WHERE current_hp IS NULL OR current_mp IS NULL
                """);
    }

    private void seedRegions() {
        seedRegion("A001", 1, "青苔森林", "FOREST", "潮濕而茂密的森林，適合剛開始冒險的角色", 1, 4, "/assets/Games/rpg/region_01_moss_forest.png");
        seedRegion("A002", 2, "霧影洞窟", "CAVE", "充滿蝙蝠、蜘蛛與礦石魔力的洞窟", 3, 8, "/assets/Games/rpg/region_02_mist_cave.png");
        seedRegion("A003", 3, "荒草古道", "PLAINS", "長年受強風吹拂的荒野與古老道路", 6, 11, "/assets/Games/rpg/region_03_wild_grass_road.png");
        seedRegion("A004", 4, "暮鴉城塞", "RUINS", "陷落多年的邊境城塞，亡者與黑甲騎士仍在巡邏", 10, 16, "/assets/Games/rpg/region_04_broken_ruins.png");
        seedRegion("A005", 5, "赤焰山脊", "VOLCANO", "熔岩流經的古老龍脈，只有資深冒險者能夠深入", 14, 20, "/assets/Games/rpg/region_05_blackstone_mountains.png");
    }

    private void seedSkills() {
        // Homework03 grants first-aid to every new character in addition to the profession skill.
        seedSkill("S001", null, "急救", "立即恢復自身生命值，恢復量會隨 AP 提升。", "HEAL", 30, 80, 10, 1);
        seedSkill("S002", "E001", "劈砍", "以武器攻擊一名敵人，造成穩定的物理傷害。", "PHYSICAL", 0, 15, 100, 1);
        seedSkill("S003", "E001", "重擊", "沉重打擊敵人，並獲得當前已損失生命值10%的護盾，持續2回合。", "PHYSICAL", 10, 35, 130, 3);
        seedSkill("S012", "E001", "旋風斬", "旋轉武器重創敵人，消耗目前HP 10%並提升攻擊力，持續3回合；最低保留1HP。", "PHYSICAL", 20, 55, 150, 6);
        seedSkill("S013", "E001", "裂地斬", "震裂地面重創敵人，消耗目前HP 20%，依已損失生命值提高傷害；最低保留1HP。", "PHYSICAL", 35, 0, 0, 9);
        seedSkill("S004", "E002", "火球術", "發射火球造成魔法傷害，行動後保留10%行動值。", "MAGICAL", 0, 30, 110, 1);
        seedSkill("S005", "E002", "魔力衝擊", "釋放強力魔法衝擊，並使自身 AP 提升20%，持續2回合。", "MAGICAL", 25, 50, 140, 3);
        seedSkill("S014", "E002", "冰槍術", "以冰槍貫穿敵人造成魔法傷害，並提升20%速度2回合。", "MAGICAL", 27, 60, 150, 6);
        seedSkill("S015", "E002", "隕石術", "召喚隕石造成巨大魔法傷害，並提升30% AP 2回合。", "MAGICAL", 45, 100, 190, 9);
        seedSkill("S006", "E003", "雙重斬", "快速揮動武器連斬兩次，並提升30%爆擊率，持續2回合。", "PHYSICAL", 0, 24, 105, 1);
        seedSkill("S007", "E003", "影襲", "從陰影突襲敵人，本次攻擊提高爆擊率並保留行動值；若爆擊可累積殺意。", "PHYSICAL", 12, 35, 130, 3);
        seedSkill("S016", "E003", "刃舞", "進入刃舞直取要害，本次攻擊必定爆擊，並可累積殺意。", "PHYSICAL", 28, 65, 155, 6);
        seedSkill("S017", "E003", "絕命擊", "對瀕死敵人發動致命一擊；目標生命值不高於30%時直接斬殺。", "PHYSICAL", 40, 95, 190, 9);
        seedSkill("S018", "E004", "盾擊", "以聖盾撞擊敵人，造成神聖魔法傷害。", "MAGICAL", 0, 20, 100, 1);
        seedSkill("S019", "E004", "聖光斬", "將聖光附於武器斬擊敵人，造成魔法傷害並提升15%防禦，持續2回合。", "MAGICAL", 14, 38, 115, 3);
        seedSkill("S020", "E004", "王國壁壘", "獲得150點護盾，持續3回合；護盾存在時反射所受直接傷害的30%。", "SUPPORT", 24, 0, 0, 6);
        seedSkill("S021", "E004", "審判之劍", "降下神聖審判重創敵人，並獲得80點護盾2回合。", "MAGICAL", 42, 105, 185, 9);
        seedSkill("S022", "E005", "穿刺箭", "瞄準護甲縫隙射擊，本技能額外獲得30%物理穿透。", "PHYSICAL", 0, 22, 105, 1);
        seedSkill("S023", "E005", "連環箭", "向敵人連射三箭，行動結束後保留15%行動值。", "PHYSICAL", 12, 38, 120, 3);
        seedSkill("S024", "E005", "獵鷹突襲", "命令獵鷹攻擊並留下標記，使下一次攻擊傷害增加25%。", "PHYSICAL", 24, 55, 135, 6);
        seedSkill("S025", "E005", "星落箭雨", "以密集箭雨封鎖敵人的行動，使敵人速度降低40%，持續3回合。", "PHYSICAL", 40, 105, 180, 9);
        seedSkill("S027", "E006", "驅邪聖言", "以聖言造成魔法傷害，並提升15% AP 2回合。", "MAGICAL", 0, 40, 125, 1);
        seedSkill("S026", "E006", "微光治癒", "以聖光恢復自身生命值，並獲得30點護盾2回合。", "HEAL", 12, 42, 95, 3);
        seedSkill("S028", "E006", "大治癒術", "大幅恢復自身生命值，並獲得80點護盾與20%防禦3回合。", "HEAL", 32, 90, 155, 6);
        seedSkill("S029", "E006", "天譴", "召喚天光造成強力魔法傷害，並提升25% AP 2回合。", "MAGICAL", 48, 115, 195, 9);

        // Homework03 monster skills. They are not profession skills, so profession_code is NULL.
        seedSkill("S008", null, "撕咬", "以利齒撕咬目標，造成物理傷害。", "PHYSICAL", 0, 10, 100, 1);
        seedSkill("S009", null, "猛撲", "猛然撲向目標，造成較高的物理傷害。", "PHYSICAL", 8, 25, 120, 1);
        seedSkill("S010", null, "藤蔓抽擊", "以藤蔓抽擊目標，造成物理傷害。", "PHYSICAL", 0, 20, 100, 1);
        seedSkill("S011", null, "自然魔力", "凝聚自然魔力攻擊目標，造成魔法傷害。", "MAGICAL", 15, 35, 120, 1);

        // Equipment/scroll skills are retained as canonical data, but are not granted automatically.
        seedSkill("S040", null, "暴風脈動", "本次行動後獲得50%行動值，之後3次行動結束再獲得50%。", "SUPPORT", 30, 0, 0, 1);
        seedSkill("S041", null, "古樹庇護", "獲得120點護盾與25%防禦，持續3回合。", "SUPPORT", 25, 0, 0, 1);
        seedSkill("S042", null, "晶核共鳴", "造成魔法傷害並提升25%速度、20%攻擊與20%魔力，持續3回合。", "MAGICAL", 27, 55, 120, 1);
        seedSkill("S043", null, "將軍戰意", "獲得90點護盾與25%攻擊，持續3回合。", "SUPPORT", 31, 0, 0, 1);
    }

    private void seedItems() {
        seedItem("I001", "初級恢復藥水", "CONSUMABLE", "恢復少量 HP 的常見藥水", 99, 10);
        seedItem("I002", "初級魔力藥水", "CONSUMABLE", "恢復少量 MP 的常見藥水", 99, 12);
        seedItem("I003", "森林祝福藥水", "CONSUMABLE", "同時恢復 HP 與 MP 的混合藥水", 99, 35);
        seedItem("I004", "騎士恢復藥劑", "CONSUMABLE", "王國騎士團使用的高品質生命藥劑", 99, 45);
        seedItem("I005", "星輝魔力藥劑", "CONSUMABLE", "由星輝花提煉的高品質魔力藥劑", 99, 50);
        seedItem("I006", "聖壇萬靈藥", "CONSUMABLE", "同時大量恢復 HP 與 MP 的稀有藥劑", 99, 120);
        seedItem("I101", "森林狼牙", "MATERIAL", "森林狼留下的尖牙，可作為製作素材", 99, 5);
        seedItem("I102", "水晶蛛絲", "MATERIAL", "帶有微弱魔力的堅韌蛛絲", 99, 14);
        seedItem("I103", "魔像核心碎片", "MATERIAL", "岩窟魔像核心剝落的碎片", 99, 28);
        seedItem("I104", "古堡銀礦", "MATERIAL", "廢棄城塞深處出產的純銀礦石", 99, 32);
        seedItem("I105", "幽魂殘布", "MATERIAL", "仍帶著寒意的古老織物", 99, 38);
        seedItem("I106", "熾龍鱗片", "MATERIAL", "火山亞龍脫落的灼熱鱗片", 99, 75);
        seedItem("I107", "古樹心材", "MATERIAL", "古樹守衛核心留下的堅韌心材，是商會重視的高級素材", 99, 90);
        seedItem("I108", "岩窟晶核", "MATERIAL", "岩窟魔像體內凝結的完整晶核", 99, 130);
        seedItem("I109", "風暴翎羽", "MATERIAL", "風暴巨鷹留下、仍帶有風力的翎羽", 99, 170);
        seedItem("I110", "亡將徽印", "MATERIAL", "亡國將軍持有的古老軍徽", 99, 220);
        seedItem("I111", "赤龍心鱗", "MATERIAL", "赤焰亞龍心口附近最堅硬的鱗片", 99, 300);
        seedItem("I201", "森林守衛徽記", "QUEST", "證明曾協助森林守衛的任務道具", 1, 0);
        seedItem("I202", "洞窟調查筆記", "QUEST", "記錄霧影洞窟異常現象的筆記", 1, 0);
        seedItem("I203", "王國密令", "QUEST", "蓋有王室火漆的機密命令", 1, 0);
        seedItem("I204", "初火聖徽", "QUEST", "傳說能開啟火山聖壇的古老徽記", 1, 0);
        seedItem("I301", "暴風脈動技能卷軸", "SKILL_SCROLL", "使用後學會全職業通用技能「暴風脈動」", 1, 500);
        seedItem("I302", "古樹庇護技能卷軸", "SKILL_SCROLL", "使用後學會全職業通用技能「古樹庇護」", 1, 420);
        seedItem("I303", "晶核共鳴技能卷軸", "SKILL_SCROLL", "使用後學會全職業通用技能「晶核共鳴」", 1, 460);
        seedItem("I304", "將軍戰意技能卷軸", "SKILL_SCROLL", "使用後學會全職業通用技能「將軍戰意」", 1, 560);
    }

    private void seedItemEffects() {
        seedItemEffect("IE001", "I001", "RECOVER_HP", 1, 100, 5);
        seedItemEffect("IE002", "I002", "RECOVER_MP", 1, 60, 0);
        seedItemEffect("IE003", "I003", "RECOVER_HP", 1, 80, 10);
        seedItemEffect("IE004", "I003", "RECOVER_MP", 2, 40, 10);
        seedItemEffect("IE005", "I004", "RECOVER_HP", 1, 260, 12);
        seedItemEffect("IE006", "I005", "RECOVER_MP", 1, 180, 15);
        seedItemEffect("IE007", "I006", "RECOVER_HP", 1, 220, 20);
        seedItemEffect("IE008", "I006", "RECOVER_MP", 2, 140, 20);
    }

    private void seedMonsterItemDrops() {
        seedItemDrop("M001", "I101", 45, 1, 2); seedItemDrop("M002", "I001", 25, 1, 1);
        seedItemDrop("M003", "I201", 100, 1, 1); seedItemDrop("M004", "I002", 20, 1, 1);
        seedItemDrop("M005", "I102", 40, 1, 2); seedItemDrop("M006", "I103", 55, 1, 2);
        seedItemDrop("M007", "I001", 20, 1, 1); seedItemDrop("M008", "I003", 12.5, 1, 1);
        seedItemDrop("M009", "I103", 65, 1, 3); seedItemDrop("M010", "I105", 45, 1, 2);
        seedItemDrop("M011", "I104", 55, 1, 2); seedItemDrop("M012", "I203", 100, 1, 1);
        seedItemDrop("M013", "I106", 35, 1, 1); seedItemDrop("M014", "I005", 22, 1, 1);
        seedItemDrop("M015", "I204", 100, 1, 1); seedItemDrop("M015", "I106", 80, 2, 4);
        seedItemDrop("M002", "I302", 3, 1, 1); seedItemDrop("M003", "I302", 18, 1, 1);
        seedItemDrop("M005", "I303", 3, 1, 1); seedItemDrop("M006", "I303", 18, 1, 1);
        seedItemDrop("M008", "I301", 4, 1, 1); seedItemDrop("M009", "I301", 20, 1, 1);
        seedItemDrop("M011", "I304", 4, 1, 1); seedItemDrop("M012", "I304", 20, 1, 1);
        seedItemDrop("M003", "I107", 100, 1, 1); seedItemDrop("M006", "I108", 100, 1, 1);
        seedItemDrop("M009", "I109", 100, 1, 1); seedItemDrop("M012", "I110", 100, 1, 1);
        seedItemDrop("M015", "I111", 100, 1, 1);
    }

    private void seedEquipmentAndShopData() {
        try {
            String sql = new ClassPathResource("rpg/homework03-equipment-data.sql")
                    .getContentAsString(StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                String executable = statement.replaceAll("(?m)^\\s*--.*$", "").trim();
                if (!executable.isEmpty()) jdbc.execute(executable);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("無法載入 Homework03 裝備與商店資料", exception);
        }
    }

    private void backfillCharacterSkills() {
        jdbc.update("""
                INSERT OR IGNORE INTO rpg_character_skills
                (character_id, skill_code, equipped, slot_number)
                SELECT character_id, 'S001', 1, 1 FROM rpg_characters
                """);
        jdbc.update("""
                INSERT OR IGNORE INTO rpg_character_skills
                (character_id, skill_code, equipped, slot_number)
                SELECT character_id, skill_code,
                       CASE WHEN profession_order <= 3 THEN 1 ELSE 0 END,
                       CASE WHEN profession_order <= 3 THEN profession_order + 1 ELSE 0 END
                FROM (
                    SELECT c.character_id, s.skill_code,
                           ROW_NUMBER() OVER (
                               PARTITION BY c.character_id
                               ORDER BY s.required_level, s.skill_code
                           ) AS profession_order
                    FROM rpg_characters c
                    JOIN rpg_skills s ON s.profession_code = c.profession_code
                    WHERE s.required_level <= c.level
                ) eligible
                """);
    }

    private void seedMonsters() {
        // Values are copied from Homework03 monster/base-growth data.
        seedMonster("M001", "森林狼", "依靠身體能力戰鬥的森林野獸。", "K001", "MR001", 180, 30, 32, 5, 18, 18, 90, 25, 5, 4, 1, 3, 0, 2, 20, 1.25, 8, 2);
        seedMonster("M002", "荊棘獸", "受到自然魔力影響的菁英植物魔獸。", "K002", "MR002", 260, 60, 42, 25, 30, 30, 75, 35, 7, 5, 3, 4, 0, 1, 45, 1.25, 18, 4);
        seedMonster("M003", "古樹守衛", "守護森林核心的首領級古樹。", "K003", "MR002", 520, 120, 58, 48, 48, 48, 55, 55, 10, 7, 5, 6, 0, 1, 120, 1.25, 45, 8);
        seedMonster("M004", "洞窟蝙蝠", "棲息在廢棄礦道中的迅捷野獸。", "K001", "MR001", 150, 35, 30, 8, 16, 16, 110, 22, 5, 4, 1, 2, 0, 2, 20, 1.25, 8, 2);
        seedMonster("M005", "水晶蜘蛛", "盤踞結晶礦脈的菁英蜘蛛。", "K002", "MR001", 240, 70, 40, 30, 28, 28, 92, 32, 8, 5, 4, 4, 0, 2, 45, 1.25, 18, 4);
        seedMonster("M006", "岩窟魔像", "由礦石魔力形成的首領級魔像。", "K003", "MR003", 500, 130, 60, 45, 55, 55, 50, 52, 11, 7, 5, 7, 0, 1, 120, 1.25, 45, 8);
        seedMonster("M007", "草原野豬", "佔據荒野古道的強悍野獸。", "K001", "MR001", 190, 30, 35, 5, 20, 20, 95, 26, 5, 4, 1, 3, 0, 2, 20, 1.25, 8, 2);
        seedMonster("M008", "疾風猛禽", "能操縱疾風的菁英猛禽。", "K002", "MR001", 230, 75, 44, 28, 26, 26, 120, 30, 8, 5, 4, 4, 0, 2, 45, 1.25, 18, 4);
        seedMonster("M009", "風暴巨鷹", "盤旋於雷雲之下的荒野霸主。", "K003", "MR003", 480, 150, 62, 55, 45, 45, 105, 50, 12, 7, 6, 6, 0, 2, 120, 1.25, 45, 8);
        seedMonster("M010", "失魂士兵", "仍在城塞外牆巡守的不死士兵。", "K001", "MR004", 260, 40, 48, 12, 35, 35, 82, 32, 5, 5, 2, 5, 0, 1, 35, 1.25, 14, 3);
        seedMonster("M011", "黑甲騎士", "持續守護破敗中庭的菁英騎士。", "K002", "MR005", 390, 90, 66, 28, 58, 58, 76, 44, 8, 7, 3, 7, 0, 1, 75, 1.25, 32, 6);
        seedMonster("M012", "亡國將軍", "執念延續百年的不死將軍。", "K003", "MR004", 720, 170, 82, 62, 76, 76, 70, 70, 12, 9, 6, 9, 0, 1, 180, 1.25, 75, 12);
        seedMonster("M013", "灰燼蜥蜴", "棲息在灼熱灰燼坡道的龍族生物。", "K001", "MR006", 300, 55, 55, 22, 38, 38, 96, 36, 6, 6, 3, 5, 0, 2, 45, 1.25, 18, 4);
        seedMonster("M014", "熔岩守衛", "鎮守熔岩神殿的古老元素守衛。", "K002", "MR003", 460, 120, 72, 50, 64, 64, 68, 48, 10, 8, 5, 8, 0, 1, 95, 1.25, 40, 7);
        seedMonster("M015", "赤焰亞龍", "在赤焰山脊甦醒的首領級亞龍。", "K003", "MR006", 880, 230, 98, 82, 82, 82, 88, 78, 14, 10, 8, 10, 0, 2, 240, 1.25, 100, 15);
    }

    private void seedMonsterSkills() {
        seedMonsterSkill("M001", "S008", 70, 1); seedMonsterSkill("M001", "S009", 30, 2);
        seedMonsterSkill("M002", "S010", 70, 1); seedMonsterSkill("M002", "S011", 30, 2);
        seedMonsterSkill("M003", "S010", 50, 1); seedMonsterSkill("M003", "S011", 50, 2);
        seedMonsterSkill("M004", "S008", 75, 1); seedMonsterSkill("M004", "S009", 25, 2);
        seedMonsterSkill("M005", "S008", 60, 1); seedMonsterSkill("M005", "S011", 40, 2);
        seedMonsterSkill("M006", "S010", 55, 1); seedMonsterSkill("M006", "S011", 45, 2);
        seedMonsterSkill("M007", "S008", 70, 1); seedMonsterSkill("M007", "S009", 30, 2);
        seedMonsterSkill("M008", "S008", 55, 1); seedMonsterSkill("M008", "S009", 45, 2);
        seedMonsterSkill("M009", "S009", 50, 1); seedMonsterSkill("M009", "S011", 50, 2);
        seedMonsterSkill("M010", "S002", 70, 1); seedMonsterSkill("M010", "S003", 30, 2);
        seedMonsterSkill("M011", "S018", 55, 1); seedMonsterSkill("M011", "S020", 45, 2);
        seedMonsterSkill("M012", "S003", 45, 1); seedMonsterSkill("M012", "S021", 55, 2);
        seedMonsterSkill("M013", "S008", 65, 1); seedMonsterSkill("M013", "S011", 35, 2);
        seedMonsterSkill("M014", "S010", 45, 1); seedMonsterSkill("M014", "S014", 55, 2);
        seedMonsterSkill("M015", "S009", 40, 1); seedMonsterSkill("M015", "S015", 60, 2);
    }

    private void seedMonsterTraits() {
        // Race traits expanded to the monsters that inherit them in Homework03.
        for (String monster : List.of("M001", "M004", "M005", "M007", "M008"))
            seedMonsterTrait(monster, "T004", "RACE", "野性本能", "ATK +10%", "ATK", 10);
        for (String monster : List.of("M010", "M012"))
            seedMonsterTrait(monster, "T008", "RACE", "不死之軀", "HP +25%", "HP", 25);
        seedMonsterTrait("M011", "T003", "RACE", "戰鬥準備", "技能欄位 +1", "SKILL_SLOT", 1);
        for (String monster : List.of("M013", "M015"))
            seedMonsterTrait(monster, "T010", "RACE", "龍血沸騰", "ATK +20%", "ATK", 20);

        seedMonsterTrait("M003", "T009", "MONSTER", "古代護甲", "DEF +25%", "DEF", 25);
        seedMonsterTrait("M006", "T009", "MONSTER", "古代護甲", "DEF +25%", "DEF", 25);
        seedMonsterTrait("M009", "T006", "MONSTER", "自然步伐", "SPEED +12%", "SPEED", 12);
        seedMonsterTrait("M010", "T008", "MONSTER", "不死之軀", "HP +25%", "HP", 25);
        seedMonsterTrait("M011", "T005", "MONSTER", "守護誓言", "最大 HP +20%", "HP", 20);
        seedMonsterTrait("M012", "T008", "MONSTER", "不死之軀", "HP +25%", "HP", 25);
        seedMonsterTrait("M013", "T010", "MONSTER", "龍血沸騰", "ATK +20%", "ATK", 20);
        seedMonsterTrait("M014", "T009", "MONSTER", "古代護甲", "DEF +25%", "DEF", 25);
        seedMonsterTrait("M015", "T010", "MONSTER", "龍血沸騰", "ATK +20%", "ATK", 20);
    }

    private void seedStages() {
        seedStage("ST001", 1, "A001", "青苔森林", "狼跡小徑", "沿著泥濘足跡驅逐在商道徘徊的森林狼。", 1, "M001", 1, 3, 70);
        seedStage("ST002", 2, "A001", "青苔森林", "荊棘庭園", "調查失控荊棘與潛伏其中的菁英魔獸。", 3, "M002", 2, 4, 25);
        seedStage("ST003", 3, "A001", "青苔森林", "古樹聖域", "深入森林核心，面對甦醒的古樹守衛。", 4, "M003", 4, 4, 5);
        seedStage("ST004", 4, "A002", "霧影洞窟", "蝙蝠礦道", "清理廢棄礦道，重新打通洞窟入口。", 3, "M004", 3, 6, 70);
        seedStage("ST005", 5, "A002", "霧影洞窟", "水晶蛛巢", "穿過結晶蛛網，討伐盤踞礦脈的巨蛛。", 6, "M005", 5, 8, 25);
        seedStage("ST006", 6, "A002", "霧影洞窟", "地心祭壇", "在失落祭壇阻止岩窟魔像完全甦醒。", 8, "M006", 8, 8, 5);
        seedStage("ST007", 7, "A003", "荒草古道", "野豬古道", "護送商隊穿越遭野豬群佔據的古道。", 6, "M007", 6, 9, 70);
        seedStage("ST008", 8, "A003", "荒草古道", "風嘯斷崖", "攀上斷崖，迎戰操縱疾風的猛禽。", 9, "M008", 8, 11, 25);
        seedStage("ST009", 9, "A003", "荒草古道", "暴風巢穴", "在雷雲之下挑戰荒野霸主風暴巨鷹。", 11, "M009", 11, 11, 5);
        seedStage("ST010", 10, "A004", "暮鴉城塞", "亡兵外牆", "突破仍由失魂士兵巡守的城塞外牆。", 10, "M010", 10, 13, 70);
        seedStage("ST011", 11, "A004", "暮鴉城塞", "黑甲中庭", "在破敗中庭與黑甲騎士正面交鋒。", 13, "M011", 12, 16, 25);
        seedStage("ST012", 12, "A004", "暮鴉城塞", "將軍王座", "終結亡國將軍延續百年的執念。", 16, "M012", 16, 16, 5);
        seedStage("ST013", 13, "A005", "赤焰山脊", "灰燼坡道", "穿越灼熱灰燼，驅趕成群的火山蜥蜴。", 14, "M013", 14, 17, 70);
        seedStage("ST014", 14, "A005", "赤焰山脊", "熔岩神殿", "進入熔岩神殿，擊倒古老元素守衛。", 17, "M014", 16, 20, 25);
        seedStage("ST015", 15, "A005", "赤焰山脊", "赤龍火口", "登上火山口，迎戰沉睡甦醒的赤焰亞龍。", 20, "M015", 20, 20, 5);
    }

    private void seedProfession(String code, String name, String description,
            int hp, int mp, int atk, int ap, int def, int mdef, int speed,
            int growthHp, int growthMp, int growthAtk, int growthAp, int growthDef, int growthMdef, int growthSpeed) {
        jdbc.update("""
                INSERT INTO rpg_professions
                (profession_code, profession_name, description, base_hp, base_mp, base_atk, base_ap,
                 base_def, base_mdef, base_speed, growth_hp, growth_mp, growth_atk, growth_ap,
                 growth_def, growth_mdef, growth_speed)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(profession_code) DO UPDATE SET profession_name=excluded.profession_name,
                 description=excluded.description, base_hp=excluded.base_hp, base_mp=excluded.base_mp,
                 base_atk=excluded.base_atk, base_ap=excluded.base_ap, base_def=excluded.base_def,
                 base_mdef=excluded.base_mdef, base_speed=excluded.base_speed,
                 growth_hp=excluded.growth_hp, growth_mp=excluded.growth_mp,
                 growth_atk=excluded.growth_atk, growth_ap=excluded.growth_ap,
                 growth_def=excluded.growth_def, growth_mdef=excluded.growth_mdef,
                 growth_speed=excluded.growth_speed
                """, code, name, description, hp, mp, atk, ap, def, mdef, speed,
                growthHp, growthMp, growthAtk, growthAp, growthDef, growthMdef, growthSpeed);
    }

    private void seedRegion(String code, int order, String name, String type, String description,
            int minLevel, int maxLevel, String imagePath) {
        jdbc.update("""
                INSERT INTO rpg_regions
                (region_code, region_order, region_name, region_type, description, min_level, max_level, image_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(region_code) DO UPDATE SET region_order=excluded.region_order,
                 region_name=excluded.region_name, region_type=excluded.region_type,
                 description=excluded.description, min_level=excluded.min_level,
                 max_level=excluded.max_level, image_path=excluded.image_path
                """, code, order, name, type, description, minLevel, maxLevel, imagePath);
    }

    private void seedSkill(String code, String profession, String name, String description,
            String type, int mpCost, int basePower, int scalingPercent, int requiredLevel) {
        jdbc.update("""
                INSERT INTO rpg_skills
                (skill_code, profession_code, skill_name, description, skill_type,
                 mp_cost, base_power, scaling_percent, required_level)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(skill_code) DO UPDATE SET profession_code=excluded.profession_code,
                 skill_name=excluded.skill_name, description=excluded.description,
                 skill_type=excluded.skill_type, mp_cost=excluded.mp_cost,
                 base_power=excluded.base_power, scaling_percent=excluded.scaling_percent,
                 required_level=excluded.required_level
                """, code, profession, name, description, type, mpCost, basePower, scalingPercent, requiredLevel);
    }

    private void seedItem(String code, String name, String type, String description,
            int maxStack, int sellPrice) {
        jdbc.update("""
                INSERT INTO rpg_items(item_code,item_name,item_type,description,max_stack,sell_price)
                VALUES(?,?,?,?,?,?) ON CONFLICT(item_code) DO UPDATE SET
                item_name=excluded.item_name,item_type=excluded.item_type,
                description=excluded.description,max_stack=excluded.max_stack,
                sell_price=excluded.sell_price
                """, code, name, type, description, maxStack, sellPrice);
    }

    private void seedItemEffect(String code, String itemCode, String type, int order,
            int flatValue, double percentValue) {
        jdbc.update("""
                INSERT INTO rpg_item_effects
                (effect_code,item_code,effect_type,effect_order,flat_value,percent_value)
                VALUES(?,?,?,?,?,?) ON CONFLICT(effect_code) DO UPDATE SET
                item_code=excluded.item_code,effect_type=excluded.effect_type,
                effect_order=excluded.effect_order,flat_value=excluded.flat_value,
                percent_value=excluded.percent_value
                """, code, itemCode, type, order, flatValue, percentValue);
    }

    private void seedItemDrop(String monsterCode, String itemCode, double rate,
            int minimum, int maximum) {
        jdbc.update("""
                INSERT INTO rpg_monster_item_drops
                (monster_code,item_code,drop_rate,min_quantity,max_quantity)
                VALUES(?,?,?,?,?) ON CONFLICT(monster_code,item_code) DO UPDATE SET
                drop_rate=excluded.drop_rate,min_quantity=excluded.min_quantity,
                max_quantity=excluded.max_quantity
                """, monsterCode, itemCode, rate, minimum, maximum);
    }

    private void seedMonster(String code, String name, String description, String rankCode,
            String raceCode, int hp, int mp, int attack, int magic, int defense, int magicDefense, int speed,
            int growthHp, int growthMp, int growthAtk, int growthAp, int growthDef, int growthMdef, int growthSpeed,
            int rewardExp, double rewardExpGrowthRate, int rewardGold, int rewardGoldGrowth) {
        jdbc.update("""
                INSERT INTO rpg_monsters
                (monster_code, monster_name, description, rank_code, race_code, max_hp, max_mp,
                 attack_value, magic_value, defense_value, magic_defense_value, speed_value,
                 growth_hp, growth_mp, growth_atk, growth_ap, growth_def, growth_mdef, growth_speed,
                 reward_exp, reward_exp_growth_rate, reward_gold, reward_gold_growth, image_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
                ON CONFLICT(monster_code) DO UPDATE SET monster_name=excluded.monster_name,
                 description=excluded.description, rank_code=excluded.rank_code, race_code=excluded.race_code,
                 max_hp=excluded.max_hp, max_mp=excluded.max_mp, attack_value=excluded.attack_value,
                 magic_value=excluded.magic_value, defense_value=excluded.defense_value,
                 magic_defense_value=excluded.magic_defense_value, speed_value=excluded.speed_value,
                 growth_hp=excluded.growth_hp, growth_mp=excluded.growth_mp, growth_atk=excluded.growth_atk,
                 growth_ap=excluded.growth_ap, growth_def=excluded.growth_def, growth_mdef=excluded.growth_mdef,
                 growth_speed=excluded.growth_speed, reward_exp=excluded.reward_exp,
                 reward_exp_growth_rate=excluded.reward_exp_growth_rate, reward_gold=excluded.reward_gold,
                 reward_gold_growth=excluded.reward_gold_growth, image_path=excluded.image_path
                """, code, name, description, rankCode, raceCode, hp, mp, attack, magic, defense, magicDefense,
                speed, growthHp, growthMp, growthAtk, growthAp, growthDef, growthMdef, growthSpeed,
                rewardExp, rewardExpGrowthRate, rewardGold, rewardGoldGrowth);
    }

    private void seedMonsterSkill(String monsterCode, String skillCode, int useWeight, int priority) {
        jdbc.update("""
                INSERT INTO rpg_monster_skills (monster_code, skill_code, use_weight, priority)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(monster_code, skill_code) DO UPDATE SET
                 use_weight=excluded.use_weight, priority=excluded.priority
                """, monsterCode, skillCode, useWeight, priority);
    }

    private void seedMonsterTrait(String monsterCode, String traitCode, String sourceType,
            String traitName, String description, String statName, int percentValue) {
        jdbc.update("""
                INSERT INTO rpg_monster_traits
                (monster_code, trait_code, source_type, trait_name, description, stat_name, percent_value)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(monster_code, trait_code, source_type) DO UPDATE SET
                 trait_name=excluded.trait_name, description=excluded.description,
                 stat_name=excluded.stat_name, percent_value=excluded.percent_value
                """, monsterCode, traitCode, sourceType, traitName, description, statName, percentValue);
    }

    private void seedStage(String code, int order, String regionCode, String region, String name,
            String description, int recommendedLevel, String monster,
            int monsterMinLevel, int monsterMaxLevel, int monsterSpawnWeight) {
        jdbc.update("""
                INSERT INTO rpg_stages
                (stage_code, stage_order, region_code, region_name, stage_name, description,
                 recommended_level, monster_code, monster_min_level, monster_max_level,
                 monster_spawn_weight, image_path, enabled)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                 (SELECT image_path FROM rpg_regions WHERE region_code = ?), 1)
                ON CONFLICT(stage_code) DO UPDATE SET stage_order=excluded.stage_order,
                 region_code=excluded.region_code, region_name=excluded.region_name,
                 stage_name=excluded.stage_name, description=excluded.description,
                 recommended_level=excluded.recommended_level, monster_code=excluded.monster_code,
                 monster_min_level=excluded.monster_min_level,
                 monster_max_level=excluded.monster_max_level,
                 monster_spawn_weight=excluded.monster_spawn_weight,
                 image_path=excluded.image_path, enabled=1
                """, code, order, regionCode, region, name, description, recommendedLevel,
                monster, monsterMinLevel, monsterMaxLevel, monsterSpawnWeight, regionCode);
    }

    private void removeObsoleteDraftSkills() {
        jdbc.update("""
                DELETE FROM rpg_skills WHERE skill_code IN
                ('BASIC_ATTACK','POWER_SLASH','FIREBALL','SHADOW_STRIKE',
                 'HOLY_STRIKE','PIERCING_ARROW','SMITE')
                """);
    }
}
