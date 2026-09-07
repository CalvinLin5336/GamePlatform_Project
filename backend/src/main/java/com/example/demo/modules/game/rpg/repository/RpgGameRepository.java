package com.example.demo.modules.game.rpg.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RpgGameRepository {
    private final JdbcTemplate jdbc;

    public RpgGameRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProfessionData> findProfessions() {
        return jdbc.query("SELECT * FROM rpg_professions ORDER BY profession_code", (rs, row) -> mapProfession(rs));
    }

    public Optional<ProfessionData> findProfession(String code) {
        return jdbc.query("SELECT * FROM rpg_professions WHERE profession_code = ?",
                (rs, row) -> mapProfession(rs), code).stream().findFirst();
    }

    public List<CharacterData> findCharacters(long userId) {
        return jdbc.query("""
                SELECT c.*, p.profession_name FROM rpg_characters c
                JOIN rpg_professions p ON p.profession_code = c.profession_code
                WHERE c.user_id = ? ORDER BY c.character_id
                """, (rs, row) -> mapCharacter(rs), userId);
    }

    public Optional<CharacterData> findCharacter(long userId, long characterId) {
        return jdbc.query("""
                SELECT c.*, p.profession_name FROM rpg_characters c
                JOIN rpg_professions p ON p.profession_code = c.profession_code
                WHERE c.user_id = ? AND c.character_id = ?
                """, (rs, row) -> mapCharacter(rs), userId, characterId).stream().findFirst();
    }

    public boolean characterNameExists(long userId, String name) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rpg_characters WHERE user_id = ? AND lower(character_name) = lower(?)",
                Integer.class, userId, name);
        return count != null && count > 0;
    }

    public long createCharacter(long userId, String name, String professionCode) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO rpg_characters
                    (user_id, character_name, profession_code, level, experience, gold)
                    VALUES (?, ?, ?, 1, 0, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setString(3, professionCode);
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("無法取得新角色 ID");
        long characterId = key.longValue();
        jdbc.update("""
                INSERT OR IGNORE INTO rpg_character_stage_progress
                (character_id, stage_code, unlocked, clear_count)
                SELECT ?, stage_code, 1, 0 FROM rpg_stages WHERE stage_order = 1
                """, characterId);
        jdbc.update("""
                INSERT OR IGNORE INTO rpg_character_skills
                (character_id, skill_code, equipped, slot_number)
                VALUES (?, 'S001', 1, 1)
                """, characterId);
        jdbc.update("""
                INSERT OR IGNORE INTO rpg_character_skills
                (character_id, skill_code, equipped, slot_number)
                SELECT ?, skill_code, 1, 2 FROM rpg_skills
                WHERE profession_code = ? AND required_level = 1
                ORDER BY skill_code LIMIT 1
                """, characterId, professionCode);
        jdbc.update("""
                INSERT OR IGNORE INTO rpg_character_items(character_id,item_code,quantity)
                VALUES (?, 'I001', 5)
                """, characterId);
        jdbc.update("""
                INSERT OR IGNORE INTO rpg_character_items(character_id,item_code,quantity)
                VALUES (?, 'I002', 3)
                """, characterId);
        return characterId;
    }

    public List<StageData> findStages(long characterId) {
        return findStages(characterId, null);
    }

    private List<StageData> findStages(long characterId, Integer monsterLevelOverride) {
        return jdbc.query("""
                SELECT s.*, r.description AS region_description,
                       r.min_level AS region_min_level, r.max_level AS region_max_level,
                       m.monster_name, m.description AS monster_description,
                       m.rank_code, m.race_code, m.max_hp, m.max_mp, m.attack_value,
                       m.magic_value, m.defense_value, m.magic_defense_value, m.speed_value,
                       m.growth_hp, m.growth_mp, m.growth_atk, m.growth_ap,
                       m.growth_def, m.growth_mdef, m.growth_speed,
                       m.reward_exp, m.reward_exp_growth_rate, m.reward_gold, m.reward_gold_growth,
                       m.image_path AS monster_image_path,
                       COALESCE((SELECT SUM(t.percent_value) FROM rpg_monster_traits t
                                 WHERE t.monster_code=m.monster_code AND t.stat_name='HP'), 0) AS hp_bonus,
                       COALESCE((SELECT SUM(t.percent_value) FROM rpg_monster_traits t
                                 WHERE t.monster_code=m.monster_code AND t.stat_name='ATK'), 0) AS atk_bonus,
                       COALESCE((SELECT SUM(t.percent_value) FROM rpg_monster_traits t
                                 WHERE t.monster_code=m.monster_code AND t.stat_name='AP'), 0) AS ap_bonus,
                       COALESCE((SELECT SUM(t.percent_value) FROM rpg_monster_traits t
                                 WHERE t.monster_code=m.monster_code AND t.stat_name='DEF'), 0) AS def_bonus,
                       COALESCE((SELECT SUM(t.percent_value) FROM rpg_monster_traits t
                                 WHERE t.monster_code=m.monster_code AND t.stat_name='MDEF'), 0) AS mdef_bonus,
                       COALESCE((SELECT SUM(t.percent_value) FROM rpg_monster_traits t
                                 WHERE t.monster_code=m.monster_code AND t.stat_name='SPEED'), 0) AS speed_bonus,
                       COALESCE(p.unlocked, 0) AS unlocked,
                       COALESCE(p.clear_count, 0) AS clear_count,
                       p.best_turns
                FROM rpg_stages s
                JOIN rpg_regions r ON r.region_code = s.region_code
                JOIN rpg_monsters m ON m.monster_code = s.monster_code
                LEFT JOIN rpg_character_stage_progress p
                  ON p.stage_code = s.stage_code AND p.character_id = ?
                WHERE s.enabled = 1 ORDER BY s.stage_order
                """, (rs, row) -> {
                    int level = monsterLevelOverride == null
                            ? rs.getInt("recommended_level") : monsterLevelOverride;
                    int hp = scaledStat(rs.getInt("max_hp"), rs.getInt("growth_hp"), level, rs.getInt("hp_bonus"));
                    int mp = scaledStat(rs.getInt("max_mp"), rs.getInt("growth_mp"), level, 0);
                    int atk = scaledStat(rs.getInt("attack_value"), rs.getInt("growth_atk"), level, rs.getInt("atk_bonus"));
                    int ap = scaledStat(rs.getInt("magic_value"), rs.getInt("growth_ap"), level, rs.getInt("ap_bonus"));
                    int def = scaledStat(rs.getInt("defense_value"), rs.getInt("growth_def"), level, rs.getInt("def_bonus"));
                    int mdef = scaledStat(rs.getInt("magic_defense_value"), rs.getInt("growth_mdef"), level, rs.getInt("mdef_bonus"));
                    int speed = scaledStat(rs.getInt("speed_value"), rs.getInt("growth_speed"), level, rs.getInt("speed_bonus"));
                    int exp = (int) Math.ceil(rs.getInt("reward_exp") * Math.pow(rs.getDouble("reward_exp_growth_rate"), level - 1));
                    int gold = rs.getInt("reward_gold") + rs.getInt("reward_gold_growth") * (level - 1);
                    return new StageData(
                            rs.getString("stage_code"), rs.getInt("stage_order"),
                            rs.getString("region_code"), rs.getString("region_name"),
                            rs.getString("region_description"), rs.getInt("region_min_level"),
                            rs.getInt("region_max_level"), rs.getString("stage_name"), level,
                            rs.getInt("monster_min_level"), rs.getInt("monster_max_level"),
                            rs.getString("description"), rs.getString("image_path"),
                            rs.getString("monster_code"), rs.getString("monster_name"),
                            rs.getString("monster_description"), rs.getString("rank_code"),
                            rs.getString("race_code"), hp, mp, atk, ap, def, mdef, speed,
                            exp, gold, rs.getString("monster_image_path"), rs.getBoolean("unlocked"),
                            rs.getInt("clear_count"), (Integer) rs.getObject("best_turns"));
                }, characterId);
    }

    public Optional<StageData> findStage(long characterId, String stageCode) {
        return findStages(characterId).stream()
                .filter(stage -> stage.stageCode().equals(stageCode)).findFirst();
    }

    public Optional<StageData> findStageForBattle(long characterId, String stageCode) {
        Optional<StageData> definition = findStage(characterId, stageCode);
        if (definition.isEmpty()) return Optional.empty();
        StageData stage = definition.get();
        int maximum = Math.max(stage.monsterMinLevel(), stage.monsterMaxLevel());
        int level = ThreadLocalRandom.current().nextInt(stage.monsterMinLevel(), maximum + 1);
        return findStage(characterId, stageCode, level);
    }

    public Optional<StageData> findStage(long characterId, String stageCode, int monsterLevel) {
        return findStages(characterId, monsterLevel).stream()
                .filter(stage -> stage.stageCode().equals(stageCode)).findFirst();
    }

    public List<SkillData> findEquippedSkills(long characterId) {
        return jdbc.query("""
                SELECT s.* FROM rpg_character_skills cs
                JOIN rpg_skills s ON s.skill_code = cs.skill_code
                WHERE cs.character_id = ? AND cs.equipped = 1
                ORDER BY cs.slot_number
                """, (rs, row) -> mapSkill(rs), characterId);
    }

    public List<SkillData> findLearnedSkills(long characterId, String professionCode, int level) {
        return jdbc.query("""
                SELECT DISTINCT s.* FROM rpg_skills s
                LEFT JOIN rpg_character_skills cs
                  ON cs.skill_code = s.skill_code AND cs.character_id = ?
                WHERE cs.character_id IS NOT NULL
                   OR (s.profession_code = ? AND s.required_level <= ?)
                ORDER BY s.required_level, s.skill_code
                """, (rs, row) -> mapSkill(rs), characterId, professionCode, level);
    }

    public Optional<SkillData> findEquippedSkill(long characterId, String skillCode) {
        return jdbc.query("""
                SELECT s.* FROM rpg_character_skills cs
                JOIN rpg_skills s ON s.skill_code = cs.skill_code
                WHERE cs.character_id = ? AND cs.skill_code = ? AND cs.equipped = 1
                """, (rs, row) -> mapSkill(rs), characterId, skillCode).stream().findFirst();
    }

    public List<SkillData> learnProfessionSkills(long characterId, String professionCode,
            int oldLevel, int newLevel, int slotLimit) {
        List<SkillData> newlyAvailable = jdbc.query("""
                SELECT * FROM rpg_skills
                WHERE profession_code = ? AND required_level > ? AND required_level <= ?
                ORDER BY required_level, skill_code
                """, (rs, row) -> mapSkill(rs), professionCode, oldLevel, newLevel);
        for (SkillData skill : newlyAvailable) {
            Integer equippedCount = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM rpg_character_skills
                    WHERE character_id = ? AND equipped = 1
                    """, Integer.class, characterId);
            boolean equip = equippedCount != null && equippedCount < slotLimit;
            jdbc.update("""
                    INSERT OR IGNORE INTO rpg_character_skills
                    (character_id, skill_code, equipped, slot_number)
                    VALUES (?, ?, ?, ?)
                    """, characterId, skill.code(), equip,
                    equip ? equippedCount + 1 : 0);
        }
        return newlyAvailable;
    }

    public void saveEquippedSkills(long characterId, List<String> skillCodes) {
        for (String skillCode : skillCodes) {
            jdbc.update("""
                    INSERT OR IGNORE INTO rpg_character_skills
                    (character_id, skill_code, equipped, slot_number)
                    VALUES (?, ?, 0, 0)
                    """, characterId, skillCode);
        }
        jdbc.update("""
                UPDATE rpg_character_skills
                SET equipped = 0, slot_number = 0
                WHERE character_id = ?
                """, characterId);
        for (int index = 0; index < skillCodes.size(); index++) {
            jdbc.update("""
                    UPDATE rpg_character_skills
                    SET equipped = 1, slot_number = ?
                    WHERE character_id = ? AND skill_code = ?
                    """, index + 1, characterId, skillCodes.get(index));
        }
    }

    public List<MonsterSkillData> findMonsterSkills(String monsterCode) {
        return jdbc.query("""
                SELECT s.*, ms.use_weight, ms.priority
                FROM rpg_monster_skills ms
                JOIN rpg_skills s ON s.skill_code = ms.skill_code
                WHERE ms.monster_code = ?
                ORDER BY ms.priority, ms.skill_code
                """, (rs, row) -> new MonsterSkillData(mapSkill(rs), rs.getInt("use_weight"), rs.getInt("priority")),
                monsterCode);
    }

    public List<MonsterTraitData> findMonsterTraits(String monsterCode) {
        return jdbc.query("""
                SELECT * FROM rpg_monster_traits WHERE monster_code = ?
                ORDER BY CASE source_type WHEN 'RACE' THEN 0 ELSE 1 END, trait_code
                """, (rs, row) -> new MonsterTraitData(rs.getString("trait_code"), rs.getString("source_type"),
                        rs.getString("trait_name"), rs.getString("description"), rs.getString("stat_name"),
                        rs.getInt("percent_value")), monsterCode);
    }

    public void abandonActiveBattles(long userId, long characterId) {
        jdbc.update("""
                UPDATE rpg_battles SET battle_status = 'ABANDONED', updated_at = datetime('now')
                WHERE user_id = ? AND character_id = ? AND battle_status = 'ACTIVE'
                """, userId, characterId);
    }

    public void createBattle(BattleData battle) {
        jdbc.update("""
                INSERT INTO rpg_battles
                (battle_id, user_id, character_id, stage_code, monster_level, player_hp, max_player_hp,
                 player_mp, max_player_mp, player_atk, player_ap, player_def, player_mdef,
                 player_speed, player_action,
                 monster_hp, max_monster_hp, monster_mp, max_monster_mp, monster_atk, monster_ap,
                 monster_def, monster_mdef, monster_speed, monster_action,
                 turn_number, battle_status, log_text, effect_state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, battle.battleId(), battle.userId(), battle.characterId(), battle.stageCode(),
                battle.monsterLevel(),
                battle.playerHp(), battle.maxPlayerHp(), battle.playerMp(), battle.maxPlayerMp(),
                battle.playerAtk(), battle.playerAp(), battle.playerDef(), battle.playerMdef(),
                battle.playerSpeed(), battle.playerAction(), battle.monsterHp(),
                battle.maxMonsterHp(), battle.monsterMp(), battle.maxMonsterMp(), battle.monsterAtk(),
                battle.monsterAp(), battle.monsterDef(), battle.monsterMdef(), battle.monsterSpeed(),
                battle.monsterAction(), battle.turnNumber(), battle.status(), battle.logText(), battle.effectState());
    }

    public Optional<BattleData> findBattle(long userId, String battleId) {
        return jdbc.query("SELECT * FROM rpg_battles WHERE user_id = ? AND battle_id = ?",
                (rs, row) -> new BattleData(rs.getString("battle_id"), rs.getLong("user_id"),
                        rs.getLong("character_id"), rs.getString("stage_code"),
                        rs.getInt("monster_level"),
                        rs.getInt("player_hp"), rs.getInt("max_player_hp"),
                        rs.getInt("player_mp"), rs.getInt("max_player_mp"),
                        rs.getInt("player_atk"), rs.getInt("player_ap"), rs.getInt("player_def"),
                        rs.getInt("player_mdef"), rs.getInt("player_speed"), rs.getDouble("player_action"),
                        rs.getInt("monster_hp"), rs.getInt("max_monster_hp"),
                        rs.getInt("monster_mp"), rs.getInt("max_monster_mp"), rs.getInt("monster_atk"),
                        rs.getInt("monster_ap"), rs.getInt("monster_def"), rs.getInt("monster_mdef"),
                        rs.getInt("monster_speed"), rs.getDouble("monster_action"),
                        rs.getInt("turn_number"), rs.getString("battle_status"),
                        rs.getString("log_text"), rs.getString("effect_state")), userId, battleId).stream().findFirst();
    }

    public void updateBattle(BattleData battle) {
        jdbc.update("""
                UPDATE rpg_battles SET player_hp = ?, player_mp = ?, monster_hp = ?, monster_mp = ?,
                    player_action = ?, monster_action = ?, turn_number = ?,
                    battle_status = ?, log_text = ?, effect_state = ?,
                    updated_at = datetime('now')
                WHERE battle_id = ? AND user_id = ?
                """, battle.playerHp(), battle.playerMp(), battle.monsterHp(), battle.monsterMp(),
                battle.playerAction(), battle.monsterAction(), battle.turnNumber(),
                battle.status(), battle.logText(), battle.effectState(),
                battle.battleId(), battle.userId());
    }

    public List<ItemData> findInventory(long characterId) {
        return jdbc.query("""
                SELECT i.*, ci.quantity FROM rpg_character_items ci
                JOIN rpg_items i ON i.item_code=ci.item_code
                WHERE ci.character_id=? AND ci.quantity>0
                ORDER BY CASE i.item_type WHEN 'CONSUMABLE' THEN 0
                    WHEN 'SKILL_SCROLL' THEN 1 WHEN 'MATERIAL' THEN 2 ELSE 3 END,
                    i.item_code
                """, (rs, row) -> mapItem(rs, rs.getInt("quantity")), characterId);
    }

    public List<EquipmentData> findEquipment(long characterId) {
        return jdbc.query("""
                SELECT ce.id owned_id, ce.equipped_slot, e.*
                FROM rpg_character_equipment ce JOIN rpg_equipment e
                  ON e.equipment_id=ce.equipment_id
                WHERE ce.character_id=?
                ORDER BY ce.equipped_slot IS NULL, ce.equipment_id, ce.id
                """, (rs, row) -> mapEquipment(rs, rs.getLong("owned_id"),
                        rs.getString("equipped_slot")), characterId);
    }

    public Optional<EquipmentData> findOwnedEquipment(long characterId, long ownedId) {
        return jdbc.query("""
                SELECT ce.id owned_id, ce.equipped_slot, e.*
                FROM rpg_character_equipment ce JOIN rpg_equipment e
                  ON e.equipment_id=ce.equipment_id
                WHERE ce.character_id=? AND ce.id=?
                """, (rs, row) -> mapEquipment(rs, rs.getLong("owned_id"),
                        rs.getString("equipped_slot")), characterId, ownedId).stream().findFirst();
    }

    public List<String> findEquipmentProfessions(String equipmentCode) {
        return jdbc.query("SELECT profession_id FROM rpg_equipment_profession WHERE equipment_id=? ORDER BY profession_id",
                (rs, row) -> rs.getString(1), equipmentCode);
    }

    public List<EquipmentStatData> findEquipmentStats(String equipmentCode) {
        return jdbc.query("""
                SELECT stat_type,modifier_type,modifier_value,NULL equipment_type FROM rpg_equipment_stat
                WHERE equipment_id=? ORDER BY id
                """, (rs, row) -> new EquipmentStatData(rs.getString(1), rs.getString(2), rs.getDouble(3), null),
                equipmentCode);
    }

    public List<EquipmentStatData> findEquippedStats(long characterId) {
        return jdbc.query("""
                SELECT es.stat_type,es.modifier_type,es.modifier_value,e.equipment_type
                FROM rpg_character_equipment ce JOIN rpg_equipment_stat es
                  ON es.equipment_id=ce.equipment_id
                JOIN rpg_equipment e ON e.equipment_id=ce.equipment_id
                WHERE ce.character_id=? AND ce.equipped_slot IS NOT NULL ORDER BY es.id
                """, (rs, row) -> new EquipmentStatData(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getString(4)),
                characterId);
    }

    public boolean hasEquipped(long characterId, String equipmentCode) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM rpg_character_equipment
                WHERE character_id=? AND equipment_id=? AND equipped_slot IS NOT NULL
                """, Integer.class, characterId, equipmentCode);
        return count != null && count > 0;
    }

    public void equip(long characterId, long ownedId, String slot) {
        jdbc.update("UPDATE rpg_character_equipment SET equipped_slot=NULL WHERE character_id=? AND equipped_slot=?",
                characterId, slot);
        jdbc.update("UPDATE rpg_character_equipment SET equipped_slot=? WHERE character_id=? AND id=?",
                slot, characterId, ownedId);
    }

    public boolean unequip(long characterId, long ownedId) {
        return jdbc.update("UPDATE rpg_character_equipment SET equipped_slot=NULL WHERE character_id=? AND id=?",
                characterId, ownedId) == 1;
    }

    public void addEquipment(long characterId, String equipmentCode) {
        jdbc.update("INSERT INTO rpg_character_equipment(character_id,equipment_id) VALUES (?,?)",
                characterId, equipmentCode);
    }

    public List<EquipmentDropData> findMonsterEquipmentDrops(String monsterCode) {
        return jdbc.query("""
                SELECT d.equipment_id,e.equipment_name,d.drop_rate
                FROM rpg_monster_equipment_drop d JOIN rpg_equipment e
                  ON e.equipment_id=d.equipment_id WHERE d.monster_id=? ORDER BY d.id
                """, (rs, row) -> new EquipmentDropData(rs.getString(1), rs.getString(2), rs.getDouble(3)),
                monsterCode);
    }

    public void recordBattleEquipmentDrop(String battleId, String equipmentCode, String equipmentName) {
        jdbc.update("INSERT INTO rpg_battle_equipment_drops(battle_id,equipment_id,equipment_name) VALUES (?,?,?)",
                battleId, equipmentCode, equipmentName);
    }

    public List<BattleEquipmentDropData> findBattleEquipmentDrops(String battleId) {
        return jdbc.query("""
                SELECT equipment_id,equipment_name FROM rpg_battle_equipment_drops
                WHERE battle_id=? ORDER BY rowid
                """, (rs, row) -> new BattleEquipmentDropData(rs.getString(1), rs.getString(2)), battleId);
    }

    public Optional<ShopData> findShop(String shopCode) {
        return jdbc.query("SELECT shop_id,shop_name FROM rpg_shop WHERE shop_id=?",
                (rs, row) -> new ShopData(rs.getString(1), rs.getString(2)), shopCode).stream().findFirst();
    }

    public List<ShopProductData> findShopItems(String shopCode, long characterId) {
        return jdbc.query("""
                SELECT si.id,si.item_id,i.item_name,i.description,si.buy_price,
                       COALESCE(ci.quantity,0) quantity,i.item_type
                FROM rpg_shop_item si JOIN rpg_items i ON i.item_code=si.item_id
                LEFT JOIN rpg_character_items ci ON ci.item_code=i.item_code AND ci.character_id=?
                WHERE si.shop_id=? ORDER BY si.id
                """, (rs, row) -> new ShopProductData(rs.getLong(1), "ITEM", rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getInt(5), rs.getInt(6), null, 0,
                        rs.getString(7), null), characterId, shopCode);
    }

    public List<ShopProductData> findShopEquipment(String shopCode, long characterId) {
        return jdbc.query("""
                SELECT se.id,se.equipment_id,e.equipment_name,e.description,se.buy_price,
                       se.purchase_limit,COALESCE(cp.quantity,0),e.*
                FROM rpg_shop_equipment se JOIN rpg_equipment e ON e.equipment_id=se.equipment_id
                LEFT JOIN rpg_character_shop_purchase cp ON cp.shop_equipment_id=se.id AND cp.character_id=?
                WHERE se.shop_id=? ORDER BY se.id
                """, (rs, row) -> new ShopProductData(rs.getLong(1), "EQUIPMENT", rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getInt(5), 0, rs.getInt(6), rs.getInt(7),
                        null, mapEquipment(rs, 0, null)), characterId, shopCode);
    }

    public List<ShopMaterialData> findShopMaterials(String shopCode, String type, String code, long characterId) {
        return jdbc.query("""
                SELECT r.material_item_id,i.item_name,r.required_quantity,COALESCE(ci.quantity,0)
                FROM rpg_shop_material_requirement r JOIN rpg_items i ON i.item_code=r.material_item_id
                LEFT JOIN rpg_character_items ci ON ci.item_code=r.material_item_id AND ci.character_id=?
                WHERE r.shop_id=? AND r.product_type=? AND r.product_id=? ORDER BY r.id
                """, (rs, row) -> new ShopMaterialData(rs.getString(1), rs.getString(2),
                        rs.getInt(3), rs.getInt(4)), characterId, shopCode, type, code);
    }

    public List<ShopProductData> findSellableItems(long characterId) {
        return jdbc.query("""
                SELECT ci.rowid,ci.item_code,i.item_name,i.description,i.sell_price,ci.quantity,i.item_type
                FROM rpg_character_items ci JOIN rpg_items i ON i.item_code=ci.item_code
                WHERE ci.character_id=? AND ci.quantity>0 AND i.sell_price>0 AND i.item_type<>'QUEST'
                ORDER BY ci.rowid
                """, (rs, row) -> new ShopProductData(rs.getLong(1), "ITEM", rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getInt(5), rs.getInt(6), null, 0,
                        rs.getString(7), null), characterId);
    }

    public List<ShopProductData> findSellableEquipment(long characterId) {
        return jdbc.query("""
                SELECT ce.id,e.equipment_id,e.equipment_name,e.description,e.sell_price,e.*
                FROM rpg_character_equipment ce JOIN rpg_equipment e ON e.equipment_id=ce.equipment_id
                WHERE ce.character_id=? AND ce.equipped_slot IS NULL AND e.sell_price>0 ORDER BY ce.id
                """, (rs, row) -> new ShopProductData(rs.getLong(1), "EQUIPMENT", rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getInt(5), 1, null, 0, null,
                        mapEquipment(rs, rs.getLong(1), null)), characterId);
    }

    public List<ShopProductData> findBuybacks(long characterId) {
        return jdbc.query("""
                SELECT b.*,COALESCE(i.item_name,e.equipment_name) product_name,
                       COALESCE(i.description,e.description) product_description,i.item_type,e.*
                FROM rpg_shop_buyback b
                LEFT JOIN rpg_items i ON b.product_type='ITEM' AND i.item_code=b.product_id
                LEFT JOIN rpg_equipment e ON b.product_type='EQUIPMENT' AND e.equipment_id=b.product_id
                WHERE b.character_id=? ORDER BY b.id DESC
                """, (rs, row) -> new ShopProductData(rs.getLong("id"), rs.getString("product_type"),
                        rs.getString("product_id"), rs.getString("product_name"),
                        rs.getString("product_description"), rs.getInt("buyback_price"),
                        rs.getInt("quantity"), null, 0, rs.getString("item_type"),
                        "EQUIPMENT".equals(rs.getString("product_type")) ? mapEquipment(rs, 0, null) : null),
                characterId);
    }

    public Optional<ShopProductData> findShopProduct(String type, long id, long characterId) {
        return ("ITEM".equals(type) ? findShopItems("SH001", characterId) : findShopEquipment("SH001", characterId))
                .stream().filter(p -> p.id() == id).findFirst();
    }

    public void deductPayment(long characterId, String shopCode, String type, String code, int price) {
        jdbc.update("UPDATE rpg_characters SET gold=gold-? WHERE character_id=?", price, characterId);
        for (ShopMaterialData material : findShopMaterials(shopCode, type, code, characterId)) {
            jdbc.update("UPDATE rpg_character_items SET quantity=quantity-? WHERE character_id=? AND item_code=?",
                    material.requiredQuantity(), characterId, material.itemCode());
        }
    }

    public void recordEquipmentPurchase(long characterId, long shopProductId) {
        jdbc.update("""
                INSERT INTO rpg_character_shop_purchase(character_id,shop_equipment_id,quantity) VALUES (?,?,1)
                ON CONFLICT(character_id,shop_equipment_id) DO UPDATE SET quantity=quantity+1
                """, characterId, shopProductId);
    }

    public boolean sellItem(long characterId, long rowId, String code, int price) {
        int changed = jdbc.update("UPDATE rpg_character_items SET quantity=quantity-1 WHERE rowid=? AND character_id=? AND quantity>0",
                rowId, characterId);
        if (changed == 0) return false;
        jdbc.update("UPDATE rpg_characters SET gold=gold+? WHERE character_id=?", price, characterId);
        addBuyback(characterId, "ITEM", code, 1, price);
        return true;
    }

    public boolean sellEquipment(long characterId, long ownedId, String code, int price) {
        int changed = jdbc.update("DELETE FROM rpg_character_equipment WHERE id=? AND character_id=? AND equipped_slot IS NULL",
                ownedId, characterId);
        if (changed == 0) return false;
        jdbc.update("UPDATE rpg_characters SET gold=gold+? WHERE character_id=?", price, characterId);
        addBuyback(characterId, "EQUIPMENT", code, 1, price);
        return true;
    }

    private void addBuyback(long characterId, String type, String code, int quantity, int price) {
        jdbc.update("""
                INSERT INTO rpg_shop_buyback(character_id,product_type,product_id,quantity,sell_price,buyback_price)
                VALUES (?,?,?,?,?,?)
                """, characterId, type, code, quantity, price, price);
    }

    public Optional<ShopProductData> findBuyback(long characterId, long id) {
        return findBuybacks(characterId).stream().filter(item -> item.id() == id).findFirst();
    }

    public void removeBuyback(long characterId, long id, int price) {
        jdbc.update("UPDATE rpg_characters SET gold=gold-? WHERE character_id=?", price, characterId);
        jdbc.update("DELETE FROM rpg_shop_buyback WHERE id=? AND character_id=?", id, characterId);
    }

    public Optional<ItemData> findInventoryItem(long characterId, String itemCode) {
        return jdbc.query("""
                SELECT i.*, ci.quantity FROM rpg_character_items ci
                JOIN rpg_items i ON i.item_code=ci.item_code
                WHERE ci.character_id=? AND ci.item_code=? AND ci.quantity>0
                """, (rs, row) -> mapItem(rs, rs.getInt("quantity")), characterId, itemCode)
                .stream().findFirst();
    }

    public Optional<ItemData> findItem(long characterId, String itemCode) {
        return jdbc.query("""
                SELECT i.*,COALESCE(ci.quantity,0) quantity FROM rpg_items i
                LEFT JOIN rpg_character_items ci ON ci.item_code=i.item_code AND ci.character_id=?
                WHERE i.item_code=?
                """, (rs, row) -> mapItem(rs, rs.getInt("quantity")), characterId, itemCode)
                .stream().findFirst();
    }

    public List<ItemEffectData> findItemEffects(String itemCode) {
        return jdbc.query("""
                SELECT * FROM rpg_item_effects WHERE item_code=? ORDER BY effect_order
                """, (rs, row) -> new ItemEffectData(rs.getString("effect_type"),
                        rs.getInt("flat_value"), rs.getDouble("percent_value")), itemCode);
    }

    public boolean consumeItem(long characterId, String itemCode) {
        return jdbc.update("""
                UPDATE rpg_character_items SET quantity=quantity-1
                WHERE character_id=? AND item_code=? AND quantity>0
                """, characterId, itemCode) == 1;
    }

    public boolean learnSkill(long characterId, String skillCode) {
        return jdbc.update("""
                INSERT OR IGNORE INTO rpg_character_skills
                (character_id,skill_code,equipped,slot_number) VALUES(?,?,0,0)
                """, characterId, skillCode) == 1;
    }

    public void addItem(long characterId, String itemCode, int quantity) {
        jdbc.update("""
                INSERT INTO rpg_character_items(character_id,item_code,quantity)
                VALUES(?,?,?) ON CONFLICT(character_id,item_code) DO UPDATE SET
                quantity=MIN((SELECT max_stack FROM rpg_items WHERE item_code=excluded.item_code),
                             rpg_character_items.quantity+excluded.quantity)
                """, characterId, itemCode, quantity);
    }

    public List<ItemDropData> findMonsterItemDrops(String monsterCode) {
        return jdbc.query("""
                SELECT d.*, i.item_name FROM rpg_monster_item_drops d
                JOIN rpg_items i ON i.item_code=d.item_code
                WHERE d.monster_code=? ORDER BY d.item_code
                """, (rs, row) -> new ItemDropData(rs.getString("item_code"),
                        rs.getString("item_name"), rs.getDouble("drop_rate"),
                        rs.getInt("min_quantity"), rs.getInt("max_quantity")), monsterCode);
    }

    public void recordBattleItemDrop(String battleId, String itemCode, int quantity) {
        jdbc.update("""
                INSERT OR IGNORE INTO rpg_battle_item_drops(battle_id,item_code,quantity)
                VALUES(?,?,?)
                """, battleId, itemCode, quantity);
    }

    public List<BattleItemDropData> findBattleItemDrops(String battleId) {
        return jdbc.query("""
                SELECT d.item_code,i.item_name,d.quantity FROM rpg_battle_item_drops d
                JOIN rpg_items i ON i.item_code=d.item_code
                WHERE d.battle_id=? ORDER BY d.item_code
                """, (rs, row) -> new BattleItemDropData(rs.getString("item_code"),
                        rs.getString("item_name"), rs.getInt("quantity")), battleId);
    }

    public void saveCharacterState(long userId, long characterId, int level, int experience, int gold,
            int currentHp, int currentMp) {
        jdbc.update("""
                UPDATE rpg_characters SET level = ?, experience = ?, gold = ?,
                    current_hp = ?, current_mp = ?, updated_at = datetime('now')
                WHERE user_id = ? AND character_id = ?
                """, level, experience, gold, currentHp, currentMp, userId, characterId);
    }

    public void recordStageClear(long characterId, String stageCode, int turns) {
        jdbc.update("""
                INSERT OR IGNORE INTO rpg_character_stage_progress
                (character_id, stage_code, unlocked, clear_count, best_turns)
                VALUES (?, ?, 1, 0, NULL)
                """, characterId, stageCode);
        jdbc.update("""
                UPDATE rpg_character_stage_progress
                SET clear_count = clear_count + 1,
                    best_turns = CASE WHEN best_turns IS NULL OR ? < best_turns THEN ? ELSE best_turns END,
                    updated_at = datetime('now')
                WHERE character_id = ? AND stage_code = ?
                """, turns, turns, characterId, stageCode);
        jdbc.update("""
                INSERT OR IGNORE INTO rpg_character_stage_progress
                (character_id, stage_code, unlocked, clear_count)
                SELECT ?, stage_code, 1, 0 FROM rpg_stages
                WHERE stage_order = (SELECT stage_order + 1 FROM rpg_stages WHERE stage_code = ?)
                """, characterId, stageCode);
    }

    private ProfessionData mapProfession(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ProfessionData(rs.getString("profession_code"), rs.getString("profession_name"),
                rs.getString("description"), rs.getInt("base_hp"), rs.getInt("base_mp"),
                rs.getInt("base_atk"), rs.getInt("base_ap"), rs.getInt("base_def"), rs.getInt("base_mdef"),
                rs.getInt("base_speed"), rs.getInt("growth_hp"), rs.getInt("growth_mp"),
                rs.getInt("growth_atk"), rs.getInt("growth_ap"), rs.getInt("growth_def"),
                rs.getInt("growth_mdef"), rs.getInt("growth_speed"));
    }

    private CharacterData mapCharacter(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new CharacterData(rs.getLong("character_id"), rs.getLong("user_id"),
                rs.getString("character_name"), rs.getString("profession_code"),
                rs.getString("profession_name"), rs.getInt("level"),
                rs.getInt("experience"), rs.getInt("gold"),
                nullableInt(rs, "current_hp"), nullableInt(rs, "current_mp"));
    }

    private Integer nullableInt(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private SkillData mapSkill(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SkillData(rs.getString("skill_code"), rs.getString("profession_code"),
                rs.getString("skill_name"), rs.getString("description"),
                rs.getString("skill_type"), rs.getInt("mp_cost"),
                rs.getInt("base_power"), rs.getInt("scaling_percent"),
                rs.getInt("required_level"));
    }

    private ItemData mapItem(java.sql.ResultSet rs, int quantity) throws java.sql.SQLException {
        return new ItemData(rs.getString("item_code"), rs.getString("item_name"),
                rs.getString("item_type"), rs.getString("description"), quantity,
                rs.getInt("max_stack"), rs.getInt("sell_price"));
    }

    private EquipmentData mapEquipment(java.sql.ResultSet rs, long ownedId, String slot) throws java.sql.SQLException {
        return new EquipmentData(ownedId, rs.getString("equipment_id"), rs.getString("equipment_name"),
                rs.getString("equipment_type"), rs.getString("usage_type"), rs.getInt("required_level"),
                rs.getString("description"), rs.getInt("sell_price"), slot);
    }

    private int scaledStat(int base, int growth, int level, int bonusPercent) {
        int leveled = base + growth * (level - 1);
        return (int) (leveled * (1 + bonusPercent / 100.0));
    }

    public record ProfessionData(String code, String name, String description,
            int baseHp, int baseMp, int baseAtk, int baseAp, int baseDef, int baseMdef, int baseSpeed,
            int growthHp, int growthMp, int growthAtk, int growthAp, int growthDef, int growthMdef, int growthSpeed) { }
    public record CharacterData(long id, long userId, String name, String professionCode,
            String professionName, int level, int experience, int gold,
            Integer currentHp, Integer currentMp) { }
    public record SkillData(String code, String professionCode, String name, String description,
            String type, int mpCost, int basePower, int scalingPercent, int requiredLevel) { }
    public record MonsterSkillData(SkillData skill, int useWeight, int priority) { }
    public record MonsterTraitData(String traitCode, String sourceType, String name, String description,
            String statName, int percentValue) { }
    public record ItemData(String code, String name, String type, String description,
            int quantity, int maxStack, int sellPrice) { }
    public record ItemEffectData(String type, int flatValue, double percentValue) { }
    public record ItemDropData(String itemCode, String itemName, double dropRate,
            int minQuantity, int maxQuantity) { }
    public record BattleItemDropData(String itemCode, String itemName, int quantity) { }
    public record EquipmentData(long ownedId, String code, String name, String type, String usageType,
            int requiredLevel, String description, int sellPrice, String equippedSlot) { }
    public record EquipmentStatData(String statType, String modifierType, double modifierValue, String equipmentType) { }
    public record EquipmentDropData(String equipmentCode, String equipmentName, double dropRate) { }
    public record BattleEquipmentDropData(String equipmentCode, String equipmentName) { }
    public record ShopData(String code, String name) { }
    public record ShopMaterialData(String itemCode, String itemName, int requiredQuantity, int ownedQuantity) { }
    public record ShopProductData(long id, String productType, String productCode, String productName,
            String description, int price, int quantity, Integer purchaseLimit, int purchasedQuantity,
            String itemType, EquipmentData equipment) { }
    public record StageData(String stageCode, int stageOrder, String regionCode, String regionName,
            String regionDescription, int regionMinLevel, int regionMaxLevel,
            String stageName, int recommendedLevel, int monsterMinLevel, int monsterMaxLevel,
            String description, String imagePath,
            String monsterCode, String monsterName, String monsterDescription, String monsterRankCode,
            String monsterRaceCode, int monsterMaxHp, int monsterMaxMp, int monsterAttack, int monsterMagic,
            int monsterDefense, int monsterMagicDefense, int monsterSpeed,
            int rewardExp, int rewardGold, String monsterImagePath, boolean unlocked,
            int clearCount, Integer bestTurns) { }
    public record BattleData(String battleId, long userId, long characterId, String stageCode, int monsterLevel,
            int playerHp, int maxPlayerHp, int playerMp, int maxPlayerMp, int playerAtk,
            int playerAp, int playerDef, int playerMdef, int playerSpeed, double playerAction,
            int monsterHp, int maxMonsterHp,
            int monsterMp, int maxMonsterMp, int monsterAtk, int monsterAp, int monsterDef,
            int monsterMdef, int monsterSpeed, double monsterAction,
            int turnNumber, String status, String logText, String effectState) { }
}
