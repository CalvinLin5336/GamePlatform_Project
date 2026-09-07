package com.example.demo.modules.game.rpg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.demo.modules.user.dto.UserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.security.JwtService;
import com.example.demo.modules.user.service.UserService;
import com.example.demo.modules.game.rpg.config.RpgDataInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
class RpgGameIntegrationTests {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        String url = "jdbc:sqlite:" + Files.createTempDirectory("rpg-game-test-").resolve("test.db");
        properties.add("spring.datasource.url", () -> url);
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @Autowired UserService users;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired RpgDataInitializer initializer;
    final ObjectMapper json = new ObjectMapper();
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        // Keep formula-focused tests deterministic. A dedicated test below verifies
        // Homework03's stage monster level range.
        jdbc.update("""
                UPDATE rpg_stages
                SET monster_min_level=recommended_level,
                    monster_max_level=recommended_level
                """);
    }

    @Test
    void newCharacterReceivesCanonicalStarterItemsAndCanUseThem() throws Exception {
        UserResponse user = user("inventory");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"背包測試\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();

        mvc.perform(get("/api/games/rpg/characters/" + characterId + "/inventory")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemCode").value("I001"))
                .andExpect(jsonPath("$[0].quantity").value(5))
                .andExpect(jsonPath("$[1].itemCode").value("I002"))
                .andExpect(jsonPath("$[1].quantity").value(3));

        jdbc.update("UPDATE rpg_characters SET current_hp=100 WHERE character_id=?", characterId);
        mvc.perform(post("/api/games/rpg/characters/" + characterId + "/items/use")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemCode\":\"I001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.character.currentHp").value(225));
        Integer quantity = jdbc.queryForObject("""
                SELECT quantity FROM rpg_character_items WHERE character_id=? AND item_code='I001'
                """, Integer.class, characterId);
        assertEquals(4, quantity);
    }

    @Test
    void skillScrollTeachesCanonicalUniversalSkillWithoutConsumingDuplicates() throws Exception {
        UserResponse user = user("scroll");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"卷軸測試\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        jdbc.update("INSERT INTO rpg_character_items(character_id,item_code,quantity) VALUES(?, 'I301', 1)",
                characterId);

        mvc.perform(post("/api/games/rpg/characters/" + characterId + "/items/use")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemCode\":\"I301\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("暴風脈動")));
        Integer learned = jdbc.queryForObject("""
                SELECT COUNT(*) FROM rpg_character_skills WHERE character_id=? AND skill_code='S040'
                """, Integer.class, characterId);
        assertEquals(1, learned);
    }

    @Test
    void victoryRollsCanonicalMonsterDropsAndPersistsGuaranteedRewards() throws Exception {
        UserResponse user = user("drops");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"掉落測試\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        jdbc.update("""
                INSERT INTO rpg_character_stage_progress(character_id,stage_code,unlocked,clear_count)
                VALUES(?, 'ST003', 1, 0)
                """, characterId);
        MvcResult started = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST003\"}"))
                .andExpect(status().isOk()).andReturn();
        String battleId = json.readTree(started.getResponse().getContentAsString()).get("battleId").asText();
        jdbc.update("UPDATE rpg_battles SET monster_hp=1,player_action=10000 WHERE battle_id=?", battleId);

        mvc.perform(post("/api/games/rpg/battles/" + battleId + "/actions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCode\":\"S002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VICTORY"))
                .andExpect(jsonPath("$.drops[?(@.itemCode == 'I201')].quantity").value(1))
                .andExpect(jsonPath("$.drops[?(@.itemCode == 'I107')].quantity").value(1));
    }

    @Test
    void canonicalEquipmentCanBeEquippedAndChangesCharacterStats() throws Exception {
        UserResponse user = user("equipment");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"裝備測試\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode original = json.readTree(created.getResponse().getContentAsString());
        long characterId = original.get("characterId").asLong();
        jdbc.update("INSERT INTO rpg_character_equipment(character_id,equipment_id) VALUES(?, 'EQ001')", characterId);
        Long ownedId = jdbc.queryForObject("SELECT id FROM rpg_character_equipment WHERE character_id=?",
                Long.class, characterId);

        mvc.perform(put("/api/games/rpg/characters/{id}/equipment", characterId)
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownedEquipmentId\":" + ownedId + ",\"slot\":\"WEAPON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.character.attack").value(original.get("attack").asInt() + 14))
                .andExpect(jsonPath("$.equipment[0].equippedSlot").value("WEAPON"));
    }

    @Test
    void canonicalShopLoadsAndPurchasesWithExistingCharacterGold() throws Exception {
        UserResponse user = user("shop");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"商店測試\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        jdbc.update("UPDATE rpg_characters SET gold=10000 WHERE character_id=?", characterId);
        MvcResult shop = mvc.perform(get("/api/games/rpg/characters/{id}/shop", characterId)
                        .header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.shopName").value("旅途橡木商會"))
                .andExpect(jsonPath("$.equipment.length()").value(15))
                .andExpect(jsonPath("$.items.length()").value(3)).andReturn();
        JsonNode first = json.readTree(shop.getResponse().getContentAsString()).get("items").get(0);
        mvc.perform(post("/api/games/rpg/characters/{id}/shop/actions", characterId)
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"BUY_ITEM\",\"productId\":" + first.get("id").asLong() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("購買成功，商品已放入背包。"));
    }

    @Test
    void battleConsumableRestoresSnapshotAndConsumesTurn() throws Exception {
        UserResponse user = user("battleitem");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"戰鬥道具\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        MvcResult started = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk()).andReturn();
        String battleId = json.readTree(started.getResponse().getContentAsString()).get("battleId").asText();
        jdbc.update("UPDATE rpg_battles SET player_hp=100,player_action=10000,monster_action=0 WHERE battle_id=?", battleId);
        mvc.perform(post("/api/games/rpg/battles/{id}/items", battleId)
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemCode\":\"I001\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.player.currentHp").value(225));
        assertEquals(4, jdbc.queryForObject("SELECT quantity FROM rpg_character_items WHERE character_id=? AND item_code='I001'",
                Integer.class, characterId));
    }

    @Test
    void stageBattleChoosesAndPersistsMonsterLevelFromHomeworkRange() throws Exception {
        jdbc.update("UPDATE rpg_stages SET monster_min_level=1, monster_max_level=3 WHERE stage_code='ST001'");
        UserResponse user = user("stagelevel");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"關卡等級\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();

        MvcResult started = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk()).andReturn();
        String battleId = json.readTree(started.getResponse().getContentAsString()).get("battleId").asText();
        Integer monsterLevel = jdbc.queryForObject(
                "SELECT monster_level FROM rpg_battles WHERE battle_id=?", Integer.class, battleId);
        Integer maximumHp = jdbc.queryForObject(
                "SELECT max_monster_hp FROM rpg_battles WHERE battle_id=?", Integer.class, battleId);

        assertTrue(monsterLevel != null && monsterLevel >= 1 && monsterLevel <= 3);
        assertEquals(180 + 25 * (monsterLevel - 1), maximumHp);
    }

    @Test
    void existingPlatformLoginOwnsCharactersAndCanCompleteFirstStage() throws Exception {
        mvc.perform(get("/api/games/rpg/characters"))
                .andExpect(status().isUnauthorized());

        UserResponse user = users.create(new UserRequest(
                "rpg" + UUID.randomUUID().toString().replace("-", ""),
                "RpgTest123", "RPG 測試玩家", null, null, "PLAYER", "Active"), "TEST");
        String token = "Bearer " + jwt.generateToken(user.id(), user.account(), user.role());

        mvc.perform(get("/api/games/rpg/professions").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));

        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"characterName":"測試勇者","professionCode":"E001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionCode").value("E001"))
                .andExpect(jsonPath("$.currentHp").value(500))
                .andExpect(jsonPath("$.maxHp").value(500))
                .andExpect(jsonPath("$.currentMp").value(80))
                .andExpect(jsonPath("$.maxMp").value(80))
                .andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();

        mvc.perform(get("/api/games/rpg/characters/{id}/stages", characterId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stageCode").value("ST001"))
                .andExpect(jsonPath("$[0].unlocked").value(true));

        mvc.perform(get("/api/games/rpg/characters/{id}/skills", characterId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].skillCode").value("S001"))
                .andExpect(jsonPath("$[0].skillName").value("急救"))
                .andExpect(jsonPath("$[0].mpCost").value(30))
                .andExpect(jsonPath("$[0].basePower").value(80))
                .andExpect(jsonPath("$[0].scalingPercent").value(10))
                .andExpect(jsonPath("$[1].skillCode").value("S002"))
                .andExpect(jsonPath("$[1].skillName").value("劈砍"))
                .andExpect(jsonPath("$[1].mpCost").value(0))
                .andExpect(jsonPath("$[1].basePower").value(15))
                .andExpect(jsonPath("$[1].scalingPercent").value(100));

        MvcResult started = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        JsonNode battle = json.readTree(started.getResponse().getContentAsString());
        String battleId = battle.get("battleId").asText();

        for (int turn = 0; turn < 10 && "ACTIVE".equals(battle.get("status").asText()); turn++) {
            MvcResult action = mvc.perform(post("/api/games/rpg/battles/{id}/actions", battleId)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"skillCode\":\"S002\"}"))
                    .andExpect(status().isOk())
                    .andReturn();
            battle = json.readTree(action.getResponse().getContentAsString());
        }

        assertTrue("VICTORY".equals(battle.get("status").asText()));
        assertTrue(battle.get("rewardExp").asInt() > 0);
        mvc.perform(get("/api/games/rpg/characters").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].experience").value(20))
                .andExpect(jsonPath("$[0].gold").value(8));
        mvc.perform(get("/api/games/rpg/characters/{id}/stages", characterId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clearCount").value(1));
    }

    @Test
    void oneUserCannotReadAnotherUsersRpgCharacter() throws Exception {
        UserResponse owner = user("owner");
        UserResponse other = user("other");
        String ownerToken = bearer(owner);
        String otherToken = bearer(other);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"專屬角色\",\"professionCode\":\"E002\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();

        mvc.perform(get("/api/games/rpg/characters/{id}/stages", characterId)
                        .header("Authorization", otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void battleSkillListContainsOnlyTheSkillsActuallyEquippedAtCreation() throws Exception {
        UserResponse user = user("skills");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"技能勇者\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        mvc.perform(get("/api/games/rpg/characters/{id}/skills", characterId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].skillCode").value("S001"))
                .andExpect(jsonPath("$[0].skillName").value("急救"))
                .andExpect(jsonPath("$[1].skillCode").value("S002"))
                .andExpect(jsonPath("$[1].skillName").value("劈砍"));
    }

    @Test
    void learnedSkillsCanBeConfiguredAndOnlyEquippedSkillsAreAvailableInBattle() throws Exception {
        UserResponse user = user("skillconfig");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"配技勇者\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        jdbc.update("UPDATE rpg_characters SET level=3 WHERE character_id=?", characterId);

        mvc.perform(get("/api/games/rpg/characters/{id}/skill-configuration", characterId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotCount").value(4))
                .andExpect(jsonPath("$.learnedSkills.length()").value(3))
                .andExpect(jsonPath("$.learnedSkills[2].skillCode").value("S003"))
                .andExpect(jsonPath("$.equippedSkills.length()").value(2));

        mvc.perform(put("/api/games/rpg/characters/{id}/skill-configuration", characterId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCodes\":[\"S003\",\"S001\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equippedSkills.length()").value(2))
                .andExpect(jsonPath("$.equippedSkills[0].skillCode").value("S003"))
                .andExpect(jsonPath("$.equippedSkills[1].skillCode").value("S001"));

        mvc.perform(get("/api/games/rpg/characters/{id}/skills", characterId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].skillCode").value("S003"))
                .andExpect(jsonPath("$[1].skillCode").value("S001"));

        mvc.perform(put("/api/games/rpg/characters/{id}/skill-configuration", characterId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCodes\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/games/rpg/characters/{id}/skill-configuration", characterId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCodes\":[\"S003\",\"S003\"]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/games/rpg/characters/{id}/skill-configuration", characterId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCodes\":[\"S004\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void monsterFallsBackToBasicAttackWithoutSpendingUnavailableMp() throws Exception {
        UserResponse user = user("nomp");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"耐力勇者\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        jdbc.update("UPDATE rpg_characters SET level=10 WHERE character_id=?", characterId);
        jdbc.update("""
                INSERT INTO rpg_character_stage_progress(character_id, stage_code, unlocked, clear_count)
                VALUES (?, 'ST009', 1, 0)
                """, characterId);
        MvcResult started = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST009\"}"))
                .andExpect(status().isOk()).andReturn();
        String battleId = json.readTree(started.getResponse().getContentAsString()).get("battleId").asText();
        jdbc.update("UPDATE rpg_battles SET monster_mp=0, monster_hp=10000, max_monster_hp=10000 WHERE battle_id=?",
                battleId);

        mvc.perform(post("/api/games/rpg/battles/{id}/actions", battleId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCode\":\"S002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monster.currentMp").value(0))
                .andExpect(jsonPath("$.logs").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("魔力不足，改用普通攻擊"))));
    }

    @Test
    void professionTraitsAreIncludedInDisplayedCharacterStats() throws Exception {
        assertCharacterStats("E002", "法師角色", "maxMp", 318);
        assertCharacterStats("E004", "聖騎角色", "maxHp", 564);
        assertCharacterStats("E004", "聖騎攻擊", "attack", 28);
        assertCharacterStats("E004", "聖騎魔力", "magic", 45);
        assertCharacterStats("E004", "聖騎防禦", "defense", 45);
        assertCharacterStats("E005", "遊俠角色", "speed", 132);
        assertCharacterStats("E006", "牧師角色", "magic", 63);
        assertCharacterStats("E001", "戰士角色", "magicDefense", 28);
    }

    @Test
    void fasterMonsterActsBeforeThePlayerAndActionGaugeStopsAtPlayerTurn() throws Exception {
        UserResponse user = user("speed");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"速度測試\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        jdbc.update("UPDATE rpg_characters SET level=3 WHERE character_id=?", characterId);
        jdbc.update("""
                INSERT INTO rpg_character_stage_progress(character_id, stage_code, unlocked, clear_count)
                VALUES (?, 'ST004', 1, 0)
                """, characterId);

        mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST004\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnNumber").value(2))
                .andExpect(jsonPath("$.player.actionPercent").value(100))
                .andExpect(jsonPath("$.player.currentHp").value(org.hamcrest.Matchers.lessThan(620)))
                .andExpect(jsonPath("$.logs").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("第 1 回合：輪到 洞窟蝙蝠"))))
                .andExpect(jsonPath("$.logs").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("第 2 回合：輪到 速度測試"))));
    }

    @Test
    void actionRetainingSkillUsesHomeworkActionGaugeRule() throws Exception {
        UserResponse user = user("action");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"行動法師\",\"professionCode\":\"E002\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        MvcResult started = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk()).andReturn();
        String battleId = json.readTree(started.getResponse().getContentAsString()).get("battleId").asText();

        mvc.perform(post("/api/games/rpg/battles/{id}/actions", battleId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCode\":\"S004\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnNumber").value(3))
                .andExpect(jsonPath("$.player.actionPercent").value(100))
                .andExpect(jsonPath("$.monster.actionPercent").value(org.hamcrest.Matchers.lessThan(80)))
                .andExpect(jsonPath("$.logs").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("保留 10% 行動值"))));
    }

    @Test
    void physicalAndMagicalDamageUseHomeworkDefenseRules() throws Exception {
        UserResponse warrior = user("physical");
        String warriorToken = bearer(warrior);
        MvcResult warriorCreated = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", warriorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"物傷測試\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long warriorId = json.readTree(warriorCreated.getResponse().getContentAsString())
                .get("characterId").asLong();
        MvcResult warriorBattle = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", warriorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + warriorId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk()).andReturn();
        String warriorBattleId = json.readTree(warriorBattle.getResponse().getContentAsString())
                .get("battleId").asText();
        mvc.perform(post("/api/games/rpg/battles/{id}/actions", warriorBattleId)
                        .header("Authorization", warriorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCode\":\"S002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monster.currentHp").value(133))
                .andExpect(jsonPath("$.logs").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("造成 47 點生命傷害"))));

        UserResponse mage = user("magical");
        String mageToken = bearer(mage);
        MvcResult mageCreated = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", mageToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"魔傷測試\",\"professionCode\":\"E002\"}"))
                .andExpect(status().isOk()).andReturn();
        long mageId = json.readTree(mageCreated.getResponse().getContentAsString())
                .get("characterId").asLong();
        MvcResult mageBattle = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", mageToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + mageId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk()).andReturn();
        String mageBattleId = json.readTree(mageBattle.getResponse().getContentAsString())
                .get("battleId").asText();
        mvc.perform(post("/api/games/rpg/battles/{id}/actions", mageBattleId)
                        .header("Authorization", mageToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCode\":\"S004\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monster.currentHp").value(102))
                .andExpect(jsonPath("$.logs").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("造成 78 點生命傷害"))));
    }

    @Test
    void equalActionTimesGiveThePlayerTheFirstTurn() throws Exception {
        jdbc.update("UPDATE rpg_monsters SET speed_value=100 WHERE monster_code='M001'");
        try {
            UserResponse user = user("tie");
            String token = bearer(user);
            MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"characterName\":\"同速測試\",\"professionCode\":\"E001\"}"))
                    .andExpect(status().isOk()).andReturn();
            long characterId = json.readTree(created.getResponse().getContentAsString())
                    .get("characterId").asLong();

            mvc.perform(post("/api/games/rpg/battles")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST001\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.turnNumber").value(1))
                    .andExpect(jsonPath("$.player.actionPercent").value(100))
                    .andExpect(jsonPath("$.monster.actionPercent").value(100))
                    .andExpect(jsonPath("$.logs").value(org.hamcrest.Matchers.hasItem(
                            org.hamcrest.Matchers.containsString("第 1 回合：輪到 同速測試"))));
        } finally {
            jdbc.update("UPDATE rpg_monsters SET speed_value=90 WHERE monster_code='M001'");
        }
    }

    @Test
    void defeatAppliesHomeworkExperiencePenaltyOnce() throws Exception {
        UserResponse user = user("defeat");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"戰敗測試\",\"professionCode\":\"E002\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        jdbc.update("UPDATE rpg_characters SET experience=50 WHERE character_id=?", characterId);

        MvcResult started = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk()).andReturn();
        String battleId = json.readTree(started.getResponse().getContentAsString()).get("battleId").asText();
        jdbc.update("""
                UPDATE rpg_battles SET player_hp=1, player_action=10000,
                    monster_action=9999, monster_hp=10000, max_monster_hp=10000
                WHERE battle_id=?
                """, battleId);

        mvc.perform(post("/api/games/rpg/battles/{id}/actions", battleId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCode\":\"S004\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEFEAT"))
                .andExpect(jsonPath("$.logs").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("失去 10 點經驗值"))));

        mvc.perform(get("/api/games/rpg/characters").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].experience").value(40))
                .andExpect(jsonPath("$[0].currentHp").value(300))
                .andExpect(jsonPath("$[0].currentMp").value(318));

        mvc.perform(post("/api/games/rpg/battles/{id}/actions", battleId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCode\":\"S004\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/games/rpg/characters").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].experience").value(40));
    }

    @Test
    void abandoningBattleKeepsRemainingVitalsForTheNextBattle() throws Exception {
        UserResponse user = user("abandon");
        String token = bearer(user);
        MvcResult created = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"撤退測試\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long characterId = json.readTree(created.getResponse().getContentAsString()).get("characterId").asLong();
        MvcResult started = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk()).andReturn();
        String battleId = json.readTree(started.getResponse().getContentAsString()).get("battleId").asText();
        jdbc.update("UPDATE rpg_battles SET player_hp=321, player_mp=54 WHERE battle_id=?", battleId);

        mvc.perform(post("/api/games/rpg/battles/{id}/abandon", battleId)
                        .header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/games/rpg/characters").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentHp").value(321))
                .andExpect(jsonPath("$[0].currentMp").value(54));
        mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + characterId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.currentHp").value(321))
                .andExpect(jsonPath("$.player.currentMp").value(54));
    }

    @Test
    void victoryKeepsRemainingVitalsAndLevelUpRefillsNewMaximums() throws Exception {
        UserResponse survivor = user("victoryvitals");
        String survivorToken = bearer(survivor);
        MvcResult survivorCreated = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", survivorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"續戰測試\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long survivorId = json.readTree(survivorCreated.getResponse().getContentAsString())
                .get("characterId").asLong();
        MvcResult survivorStarted = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", survivorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + survivorId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk()).andReturn();
        String survivorBattleId = json.readTree(survivorStarted.getResponse().getContentAsString())
                .get("battleId").asText();
        jdbc.update("""
                UPDATE rpg_battles SET player_hp=321, player_mp=54,
                    player_action=10000, monster_action=0, monster_hp=1
                WHERE battle_id=?
                """, survivorBattleId);
        mvc.perform(post("/api/games/rpg/battles/{id}/actions", survivorBattleId)
                        .header("Authorization", survivorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCode\":\"S002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VICTORY"));
        mvc.perform(get("/api/games/rpg/characters").header("Authorization", survivorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentHp").value(322))
                .andExpect(jsonPath("$[0].currentMp").value(54));

        UserResponse leveling = user("levelupvitals");
        String levelingToken = bearer(leveling);
        MvcResult levelingCreated = mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", levelingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"升級測試\",\"professionCode\":\"E001\"}"))
                .andExpect(status().isOk()).andReturn();
        long levelingId = json.readTree(levelingCreated.getResponse().getContentAsString())
                .get("characterId").asLong();
        jdbc.update("UPDATE rpg_characters SET level=2, experience=125, current_hp=1, current_mp=1 WHERE character_id=?",
                levelingId);
        MvcResult levelingStarted = mvc.perform(post("/api/games/rpg/battles")
                        .header("Authorization", levelingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterId\":" + levelingId + ",\"stageCode\":\"ST001\"}"))
                .andExpect(status().isOk()).andReturn();
        String levelingBattleId = json.readTree(levelingStarted.getResponse().getContentAsString())
                .get("battleId").asText();
        jdbc.update("UPDATE rpg_battles SET player_action=10000, monster_action=0, monster_hp=1 WHERE battle_id=?",
                levelingBattleId);
        mvc.perform(post("/api/games/rpg/battles/{id}/actions", levelingBattleId)
                        .header("Authorization", levelingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCode\":\"S002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VICTORY"))
                .andExpect(jsonPath("$.levelUp").value(true));
        mvc.perform(get("/api/games/rpg/characters").header("Authorization", levelingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].level").value(3))
                .andExpect(jsonPath("$[0].experience").value(15))
                .andExpect(jsonPath("$[0].currentHp").value(620))
                .andExpect(jsonPath("$[0].maxHp").value(620))
                .andExpect(jsonPath("$[0].currentMp").value(96))
                .andExpect(jsonPath("$[0].maxMp").value(96));
        mvc.perform(get("/api/games/rpg/characters/{id}/skills", levelingId)
                        .header("Authorization", levelingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].skillCode").value("S003"));
    }

    @Test
    void initializerCanRunAgainWithoutDuplicatingSeedData() {
        int professionsBefore = count("rpg_professions");
        int skillsBefore = count("rpg_skills");
        int stagesBefore = count("rpg_stages");
        int monstersBefore = count("rpg_monsters");
        int monsterSkillsBefore = count("rpg_monster_skills");
        int monsterTraitsBefore = count("rpg_monster_traits");
        int equipmentBefore = count("rpg_equipment");
        int equipmentStatsBefore = count("rpg_equipment_stat");

        Long characterId = jdbc.query("SELECT character_id FROM rpg_characters ORDER BY character_id LIMIT 1",
                (rs, row) -> rs.getLong(1)).stream().findFirst().orElse(null);
        if (characterId != null) {
            jdbc.update("UPDATE rpg_characters SET current_hp=NULL, current_mp=NULL WHERE character_id=?",
                    characterId);
        }

        initializer.initialize();

        assertEquals(6, professionsBefore);
        assertEquals(33, skillsBefore);
        assertEquals(24, jdbc.queryForObject(
                "SELECT COUNT(*) FROM rpg_skills WHERE profession_code IS NOT NULL", Integer.class));
        assertEquals(15, stagesBefore);
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM rpg_regions", Integer.class));
        assertEquals(3, jdbc.queryForObject(
                "SELECT COUNT(*) FROM rpg_stages WHERE region_code='A001'", Integer.class));
        assertEquals(15, monstersBefore);
        assertEquals(30, monsterSkillsBefore);
        assertEquals(19, monsterTraitsBefore);
        assertEquals(71, equipmentBefore);
        assertEquals(145, equipmentStatsBefore);
        assertEquals(99, count("rpg_equipment_profession"));
        assertEquals(56, count("rpg_monster_equipment_drop"));
        assertEquals(15, count("rpg_shop_equipment"));
        assertEquals(3, count("rpg_shop_item"));
        assertEquals(21, count("rpg_shop_material_requirement"));
        assertEquals(180, jdbc.queryForObject(
                "SELECT max_hp FROM rpg_monsters WHERE monster_code='M001'", Integer.class));
        assertEquals(30, jdbc.queryForObject(
                "SELECT max_mp FROM rpg_monsters WHERE monster_code='M001'", Integer.class));
        assertEquals(90, jdbc.queryForObject(
                "SELECT speed_value FROM rpg_monsters WHERE monster_code='M001'", Integer.class));
        assertEquals(professionsBefore, count("rpg_professions"));
        assertEquals(skillsBefore, count("rpg_skills"));
        assertEquals(stagesBefore, count("rpg_stages"));
        assertEquals(monstersBefore, count("rpg_monsters"));
        assertEquals(monsterSkillsBefore, count("rpg_monster_skills"));
        assertEquals(monsterTraitsBefore, count("rpg_monster_traits"));
        if (characterId != null) {
            assertEquals(0, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM rpg_characters
                    WHERE character_id=? AND (current_hp IS NULL OR current_mp IS NULL)
                    """, Integer.class, characterId));
        }
    }

    private int count(String tableName) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private void assertCharacterStats(String profession, String characterName,
            String property, int expected) throws Exception {
        UserResponse user = user(profession.toLowerCase());
        String token = bearer(user);
        mvc.perform(post("/api/games/rpg/characters")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterName\":\"" + characterName
                                + "\",\"professionCode\":\"" + profession + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + property).value(expected));
    }

    private UserResponse user(String prefix) {
        return users.create(new UserRequest(
                prefix + UUID.randomUUID().toString().replace("-", ""),
                "RpgTest123", prefix, null, null, "PLAYER", "Active"), "TEST");
    }

    private String bearer(UserResponse user) {
        return "Bearer " + jwt.generateToken(user.id(), user.account(), user.role());
    }
}
