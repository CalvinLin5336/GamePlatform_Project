package com.example.demo.modules.game.rpg.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.modules.game.rpg.dto.RpgBattleView;
import com.example.demo.modules.game.rpg.dto.RpgCharacterView;
import com.example.demo.modules.game.rpg.dto.RpgCombatantView;
import com.example.demo.modules.game.rpg.dto.RpgInventoryItemView;
import com.example.demo.modules.game.rpg.dto.RpgItemEffectView;
import com.example.demo.modules.game.rpg.dto.RpgDropView;
import com.example.demo.modules.game.rpg.dto.RpgEquipmentResult;
import com.example.demo.modules.game.rpg.dto.RpgEquipmentStatView;
import com.example.demo.modules.game.rpg.dto.RpgEquipmentView;
import com.example.demo.modules.game.rpg.dto.RpgProfessionView;
import com.example.demo.modules.game.rpg.dto.RpgPossibleDropView;
import com.example.demo.modules.game.rpg.dto.RpgSkillConfigurationView;
import com.example.demo.modules.game.rpg.dto.RpgSkillView;
import com.example.demo.modules.game.rpg.dto.RpgStageView;
import com.example.demo.modules.game.rpg.dto.RpgUseItemResult;
import com.example.demo.modules.game.rpg.dto.RpgShopActionResult;
import com.example.demo.modules.game.rpg.dto.RpgShopMaterialView;
import com.example.demo.modules.game.rpg.dto.RpgShopProductView;
import com.example.demo.modules.game.rpg.dto.RpgShopView;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.BattleData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.CharacterData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.MonsterSkillData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.MonsterTraitData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.ItemData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.ItemEffectData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.ItemDropData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.EquipmentData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.EquipmentStatData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.EquipmentDropData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.ShopProductData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.ProfessionData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.SkillData;
import com.example.demo.modules.game.rpg.repository.RpgGameRepository.StageData;

@Service
public class RpgGameService {
    private static final int MAX_CHARACTERS = 3;
    private static final double ACTION_FINISH = 10_000.0;
    private static final double ACTION_EPSILON = 0.0001;
    private final RpgGameRepository repository;

    public RpgGameService(RpgGameRepository repository) {
        this.repository = repository;
    }

    public List<RpgProfessionView> findProfessions() {
        return repository.findProfessions().stream().map(this::toProfessionView).toList();
    }

    public List<RpgCharacterView> findCharacters(long userId) {
        return repository.findCharacters(userId).stream().map(this::toCharacterView).toList();
    }

    @Transactional
    public RpgCharacterView createCharacter(long userId, String rawName, String rawProfessionCode) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() < 2 || name.length() > 20) {
            throw badRequest("角色名稱長度必須為 2 到 20 個字");
        }
        if (!name.matches("[\\p{L}\\p{N}_]+")) {
            throw badRequest("角色名稱只能使用文字、數字或底線");
        }
        if (repository.findCharacters(userId).size() >= MAX_CHARACTERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "每個帳號最多建立 3 名角色");
        }
        if (repository.characterNameExists(userId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "你已經有同名角色");
        }
        String professionCode = canonicalProfessionCode(
                normalizeCode(rawProfessionCode, "請選擇職業"));
        ProfessionData profession = repository.findProfession(professionCode)
                .orElseThrow(() -> badRequest("找不到指定職業"));
        long characterId = repository.createCharacter(userId, name, professionCode);
        CharacterData created = requireCharacter(userId, characterId);
        CharacterStats stats = characterStats(created, profession);
        repository.saveCharacterState(userId, characterId, 1, 0, 0, stats.hp(), stats.mp());
        return toCharacterView(requireCharacter(userId, characterId));
    }

    public List<RpgStageView> findStages(long userId, long characterId) {
        requireCharacter(userId, characterId);
        return repository.findStages(characterId).stream().map(this::toStageView).toList();
    }

    public List<RpgSkillView> findSkills(long userId, long characterId) {
        CharacterData character = requireCharacter(userId, characterId);
        return repository.findEquippedSkills(character.id()).stream()
                .map(skill -> toSkillView(skill, true))
                .toList();
    }

    public List<RpgInventoryItemView> findInventory(long userId, long characterId) {
        requireCharacter(userId, characterId);
        return repository.findInventory(characterId).stream().map(this::toInventoryItemView).toList();
    }

    public List<RpgEquipmentView> findEquipment(long userId, long characterId) {
        CharacterData character = requireCharacter(userId, characterId);
        return repository.findEquipment(characterId).stream()
                .map(item -> toEquipmentView(item, character.professionCode())).toList();
    }

    @Transactional
    public RpgEquipmentResult equip(long userId, long characterId, long ownedId, String rawSlot) {
        CharacterData character = requireCharacter(userId, characterId);
        EquipmentData equipment = repository.findOwnedEquipment(characterId, ownedId)
                .orElseThrow(() -> badRequest("裝備背包中沒有這件裝備"));
        String slot = normalizeCode(rawSlot, "請選擇裝備部位");
        boolean slotMatches = "ACCESSORY".equals(equipment.type())
                ? Set.of("ACCESSORY_1", "ACCESSORY_2").contains(slot)
                : equipment.type().equals(slot);
        if (!slotMatches) throw badRequest("這件裝備不能穿戴在選擇的部位");
        if (character.level() < equipment.requiredLevel()) throw badRequest("角色等級不足，需要 Lv"
                + equipment.requiredLevel());
        if (!professionAllowed(equipment, character.professionCode())) throw badRequest("目前職業不能使用這件裝備");
        repository.equip(characterId, ownedId, slot);
        clampCharacterVitals(userId, characterId);
        return equipmentResult(userId, characterId, "已將「" + equipment.name() + "」穿戴至" + slotName(slot) + "。");
    }

    @Transactional
    public RpgEquipmentResult unequip(long userId, long characterId, long ownedId) {
        EquipmentData equipment = repository.findOwnedEquipment(characterId, ownedId)
                .orElseThrow(() -> badRequest("找不到這件裝備"));
        requireCharacter(userId, characterId);
        if (equipment.equippedSlot() == null || !repository.unequip(characterId, ownedId)) {
            throw badRequest("這件裝備目前沒有穿戴");
        }
        clampCharacterVitals(userId, characterId);
        return equipmentResult(userId, characterId, "已卸下「" + equipment.name() + "」。");
    }

    public RpgShopView findShop(long userId, long characterId) {
        CharacterData character = requireCharacter(userId, characterId);
        var shop = repository.findShop("SH001").orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到冒險者商店"));
        return new RpgShopView(shop.code(), shop.name(), toCharacterView(character),
                mapShopProducts(repository.findShopEquipment(shop.code(), characterId), character),
                mapShopProducts(repository.findShopItems(shop.code(), characterId), character),
                mapShopProducts(repository.findSellableItems(characterId), character),
                mapShopProducts(repository.findSellableEquipment(characterId), character),
                mapShopProducts(repository.findBuybacks(characterId), character));
    }

    @Transactional
    public RpgShopActionResult shopAction(long userId, long characterId, String rawAction, long productId) {
        CharacterData character = requireCharacter(userId, characterId);
        String action = normalizeCode(rawAction, "請選擇商店操作");
        String message;
        switch (action) {
            case "BUY_ITEM", "BUY_EQUIPMENT" -> {
                String type = action.substring(4);
                ShopProductData product = repository.findShopProduct(type, productId, characterId)
                        .orElseThrow(() -> badRequest("找不到這項商品"));
                if (product.purchaseLimit() != null && product.purchaseLimit() > 0
                        && product.purchasedQuantity() >= product.purchaseLimit()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "這件商會限定裝備已達購買上限");
                }
                if (character.gold() < product.price()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "金幣不足，無法購買");
                var materials = repository.findShopMaterials("SH001", type, product.productCode(), characterId);
                if (materials.stream().anyMatch(m -> m.ownedQuantity() < m.requiredQuantity())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "素材不足，無法購買");
                }
                if ("ITEM".equals(type)) {
                    ItemData definition = repository.findItem(characterId, product.productCode()).orElseThrow();
                    if (definition.quantity() >= definition.maxStack())
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "道具持有數量已達上限");
                }
                repository.deductPayment(characterId, "SH001", type, product.productCode(), product.price());
                if ("ITEM".equals(type)) repository.addItem(characterId, product.productCode(), 1);
                else {
                    repository.addEquipment(characterId, product.productCode());
                    if (product.purchaseLimit() != null && product.purchaseLimit() > 0)
                        repository.recordEquipmentPurchase(characterId, product.id());
                }
                message = "購買成功，商品已放入背包。";
            }
            case "SELL_ITEM" -> {
                ShopProductData product = repository.findSellableItems(characterId).stream()
                        .filter(p -> p.id() == productId).findFirst().orElseThrow(() -> badRequest("找不到這項背包物品"));
                if (!repository.sellItem(characterId, productId, product.productCode(), product.price()))
                    throw badRequest("這項物品無法出售");
                message = "出售成功，可在買回清單中取回。";
            }
            case "SELL_EQUIPMENT" -> {
                ShopProductData product = repository.findSellableEquipment(characterId).stream()
                        .filter(p -> p.id() == productId).findFirst().orElseThrow(() -> badRequest("找不到這件未穿戴裝備"));
                if (!repository.sellEquipment(characterId, productId, product.productCode(), product.price()))
                    throw badRequest("這件裝備無法出售");
                message = "出售成功，可在買回清單中取回。";
            }
            case "BUYBACK" -> {
                ShopProductData product = repository.findBuyback(characterId, productId)
                        .orElseThrow(() -> badRequest("找不到這筆買回資料"));
                if (character.gold() < product.price()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "金幣不足，無法買回");
                if ("ITEM".equals(product.productType())) {
                    ItemData definition = repository.findItem(characterId, product.productCode()).orElseThrow();
                    if (definition.quantity() + product.quantity() > definition.maxStack())
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "道具持有數量已達上限");
                    repository.addItem(characterId, product.productCode(), product.quantity());
                }
                else repository.addEquipment(characterId, product.productCode());
                repository.removeBuyback(characterId, productId, product.price());
                message = "買回成功，物品已放回背包。";
            }
            default -> throw badRequest("不支援的商店操作");
        }
        return new RpgShopActionResult(message, findShop(userId, characterId));
    }

    @Transactional
    public RpgUseItemResult useItem(long userId, long characterId, String rawItemCode) {
        CharacterData character = requireCharacter(userId, characterId);
        String itemCode = normalizeCode(rawItemCode, "請選擇道具");
        ItemData item = repository.findInventoryItem(characterId, itemCode)
                .orElseThrow(() -> badRequest("背包中沒有這個道具"));
        if ("SKILL_SCROLL".equals(item.type())) {
            String skillCode = switch (item.code()) {
                case "I301" -> "S040";
                case "I302" -> "S041";
                case "I303" -> "S042";
                case "I304" -> "S043";
                default -> null;
            };
            if (skillCode == null) throw badRequest("這張卷軸的內容無法辨識");
            if (!repository.learnSkill(characterId, skillCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "這名角色已經學會該技能，卷軸沒有被消耗");
            }
            if (!repository.consumeItem(characterId, itemCode)) {
                throw new IllegalStateException("卷軸使用失敗");
            }
            SkillData skill = repository.findLearnedSkills(characterId,
                    character.professionCode(), character.level()).stream()
                    .filter(value -> value.code().equals(skillCode)).findFirst()
                    .orElseThrow();
            return new RpgUseItemResult("使用「" + item.name() + "」，學會通用技能「"
                    + skill.name() + "」。請到攜帶技能頁面裝備。",
                    toCharacterView(requireCharacter(userId, characterId)));
        }
        if (!"CONSUMABLE".equals(item.type())) throw badRequest("素材與任務道具不能直接使用");
        ProfessionData profession = requireProfession(character.professionCode());
        CharacterStats stats = characterStats(character, profession);
        int oldHp = currentValue(character.currentHp(), stats.hp());
        int oldMp = currentValue(character.currentMp(), stats.mp());
        int hp = oldHp;
        int mp = oldMp;
        for (ItemEffectData effect : repository.findItemEffects(itemCode)) {
            if ("RECOVER_HP".equals(effect.type())) {
                int recover = effect.flatValue() + (int) (stats.hp() * effect.percentValue() / 100.0);
                hp = Math.min(stats.hp(), hp + recover);
            } else if ("RECOVER_MP".equals(effect.type())) {
                int recover = effect.flatValue() + (int) (stats.mp() * effect.percentValue() / 100.0);
                mp = Math.min(stats.mp(), mp + recover);
            }
        }
        if (hp == oldHp && mp == oldMp) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "目前的 HP、MP 不需要恢復");
        if (!repository.consumeItem(characterId, itemCode)) throw badRequest("道具數量不足");
        repository.saveCharacterState(userId, characterId, character.level(), character.experience(),
                character.gold(), hp, mp);
        String message = "使用「" + item.name() + "」";
        if (hp > oldHp) message += "，恢復 " + (hp - oldHp) + " HP";
        if (mp > oldMp) message += "，恢復 " + (mp - oldMp) + " MP";
        return new RpgUseItemResult(message + "。", toCharacterView(requireCharacter(userId, characterId)));
    }

    public RpgSkillConfigurationView findSkillConfiguration(long userId, long characterId) {
        CharacterData character = requireCharacter(userId, characterId);
        List<RpgSkillView> learned = repository.findLearnedSkills(character.id(),
                character.professionCode(), character.level()).stream()
                .map(skill -> toSkillView(skill, true)).toList();
        List<RpgSkillView> equipped = repository.findEquippedSkills(character.id()).stream()
                .map(skill -> toSkillView(skill, true)).toList();
        return new RpgSkillConfigurationView(4, learned, equipped);
    }

    @Transactional
    public RpgSkillConfigurationView saveEquippedSkills(long userId, long characterId,
            List<String> rawSkillCodes) {
        CharacterData character = requireCharacter(userId, characterId);
        if (rawSkillCodes == null || rawSkillCodes.isEmpty()) {
            throw badRequest("至少需要攜帶一個技能");
        }
        LinkedHashSet<String> requested = new LinkedHashSet<>();
        for (String code : rawSkillCodes) requested.add(normalizeCode(code, "技能代碼不得空白"));
        if (requested.size() != rawSkillCodes.size()) throw badRequest("同一技能不能重複攜帶");
        if (requested.size() > 4) throw badRequest("技能欄位已滿");
        Set<String> learned = repository.findLearnedSkills(character.id(),
                        character.professionCode(), character.level()).stream()
                .map(SkillData::code).collect(java.util.stream.Collectors.toSet());
        if (!learned.containsAll(requested)) throw badRequest("只能攜帶已習得的技能");
        repository.saveEquippedSkills(character.id(), List.copyOf(requested));
        return findSkillConfiguration(userId, characterId);
    }

    @Transactional
    public synchronized RpgBattleView startBattle(long userId, Long requestedCharacterId, String rawStageCode) {
        if (requestedCharacterId == null || requestedCharacterId <= 0) throw badRequest("角色 ID 不正確");
        CharacterData character = requireCharacter(userId, requestedCharacterId);
        String stageCode = normalizeCode(rawStageCode, "請選擇關卡");
        StageData stage = repository.findStageForBattle(character.id(), stageCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到指定關卡"));
        if (!stage.unlocked()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "這個關卡尚未解鎖");

        ProfessionData profession = requireProfession(character.professionCode());
        CharacterStats stats = characterStats(character, profession);
        int currentHp = currentValue(character.currentHp(), stats.hp());
        int currentMp = currentValue(character.currentMp(), stats.mp());
        repository.abandonActiveBattles(userId, character.id());
        StringBuilder log = new StringBuilder("冒險開始：" + character.name() + " 遭遇了 Lv."
                + stage.recommendedLevel() + " " + stage.monsterName() + "！");
        for (MonsterTraitData trait : repository.findMonsterTraits(stage.monsterCode())) {
            appendLog(log, "【敵方特性】" + trait.name() + "：" + trait.description());
        }
        double startingAction = repository.hasEquipped(character.id(), "EQ068") ? ACTION_FINISH * 0.15 : 0;
        BattleData baseBattle = new BattleData(UUID.randomUUID().toString(), userId, character.id(), stageCode,
                stage.recommendedLevel(), currentHp, stats.hp(), currentMp, stats.mp(), stats.attack(), stats.magic(), stats.defense(),
                stats.magicDefense(), stats.speed(), startingAction, stage.monsterMaxHp(), stage.monsterMaxHp(), stage.monsterMaxMp(),
                stage.monsterMaxMp(), stage.monsterAttack(), stage.monsterMagic(), stage.monsterDefense(),
                stage.monsterMagicDefense(), stage.monsterSpeed(), 0, 0, "ACTIVE", log.toString(), "");
        EffectState effects = new EffectState();
        if (repository.hasEquipped(character.id(), "EQ010")) {
            effects.reflectPercent = 10;
            effects.equipmentReflect = 1;
        }
        TurnProgress progress = advanceToPlayerTurn(stage, character, baseBattle, effects,
                new TurnProgress(baseBattle.playerHp(), baseBattle.monsterHp(), baseBattle.monsterMp(),
                        baseBattle.playerAction(), baseBattle.monsterAction(), baseBattle.turnNumber()), log);
        String status = progress.playerHp() <= 0 ? "DEFEAT" : "ACTIVE";
        if ("DEFEAT".equals(status)) settleDefeat(userId, character, log);
        BattleData battle = withProgress(baseBattle, progress, baseBattle.playerMp(), status,
                log.toString(), effects.serialize());
        repository.createBattle(battle);
        return toBattleView(battle, character, stage, false, null, null);
    }

    public RpgBattleView findBattle(long userId, String battleId) {
        BattleData battle = requireBattle(userId, battleId);
        CharacterData character = requireCharacter(userId, battle.characterId());
        StageData stage = repository.findStage(character.id(), battle.stageCode(), battle.monsterLevel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到關卡資料"));
        return toBattleView(battle, character, stage, false,
                "VICTORY".equals(battle.status()) ? stage.rewardExp() : null,
                "VICTORY".equals(battle.status()) ? stage.rewardGold() : null);
    }

    @Transactional
    public synchronized RpgBattleView act(long userId, String battleId, String rawSkillCode) {
        BattleData battle = requireBattle(userId, battleId);
        if (!"ACTIVE".equals(battle.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "這場戰鬥已經結束");
        }
        if (battle.playerAction() < ACTION_FINISH - ACTION_EPSILON) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "目前還沒輪到玩家行動");
        }
        CharacterData character = requireCharacter(userId, battle.characterId());
        StageData stage = repository.findStage(character.id(), battle.stageCode(), battle.monsterLevel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到關卡資料"));
        String skillCode = normalizeCode(rawSkillCode, "請選擇技能");
        SkillData skill = repository.findEquippedSkill(character.id(), skillCode)
                .orElseThrow(() -> badRequest("角色無法使用這個技能"));
        if (skill.mpCost() > battle.playerMp()) throw badRequest("魔力不足，無法使用這個技能");

        EffectState effects = EffectState.parse(battle.effectState());
        int playerHp = battle.playerHp();
        int playerMp = battle.playerMp() - skill.mpCost();
        int monsterHp = battle.monsterHp();
        int monsterMp = battle.monsterMp();
        double playerAction = battle.playerAction();
        double monsterAction = battle.monsterAction();
        int nextTurn = battle.turnNumber();
        String status = "ACTIVE";
        StringBuilder log = new StringBuilder(battle.logText());
        appendLog(log, "第 " + battle.turnNumber() + " 回合：" + character.name() + " 使用「" + skill.name() + "」。");

        int effectiveAtk = percentValue(battle.playerAtk(), effects.atkBonus);
        int effectiveAp = percentValue(battle.playerAp(), effects.apBonus);
        if ("S012".equals(skill.code()) || "S013".equals(skill.code())) {
            int costPercent = "S012".equals(skill.code()) ? 10 : 20;
            int hpCost = Math.min(playerHp - 1, (int) Math.ceil(playerHp * costPercent / 100.0));
            playerHp -= Math.max(0, hpCost);
            appendLog(log, "【代價】消耗 " + hpCost + " 點生命值，最低保留 1 HP。");
        }

        if ("HEAL".equals(skill.type())) {
            int healing = Math.max(1, skill.basePower() + effectiveAp * skill.scalingPercent() / 100);
            int actualHealing = Math.min(healing, battle.maxPlayerHp() - playerHp);
            playerHp += actualHealing;
            appendLog(log, "【治療】恢復 " + actualHealing + " 點生命值。");
        } else if (!"SUPPORT".equals(skill.type())) {
            int monsterDefense = "MAGICAL".equals(skill.type())
                    ? effectiveMonsterMdef(battle, effects) : effectiveMonsterDef(battle, effects);
            if ("PHYSICAL".equals(skill.type())) {
                int penetration = "E005".equals(character.professionCode()) ? 10 : 0;
                if ("S022".equals(skill.code())) penetration += 30;
                if (penetration > 0) {
                    monsterDefense = monsterDefense * (100 - penetration) / 100;
                    appendLog(log, "【穿透】忽略敵人 " + penetration + "% 物理防禦。");
                }
            }
            int playerDamage;
            if ("S013".equals(skill.code())) {
                int lostHp = battle.maxPlayerHp() - playerHp;
                playerDamage = Math.max(1, 10 + (int) Math.ceil(lostHp * 0.3)
                        + (int) Math.ceil(effectiveAtk * 0.3) - monsterDefense);
            } else {
                playerDamage = calculateDamage(skill, effectiveAtk, effectiveAp, monsterDefense);
            }
            if ("PHYSICAL".equals(skill.type())) {
                int criticalChance = baseCriticalChance(character.professionCode()) + effects.criticalBonus;
                if ("S007".equals(skill.code())) criticalChance += 40;
                if ("S016".equals(skill.code())) criticalChance = 100;
                if (ThreadLocalRandom.current().nextInt(100) < criticalChance) {
                    playerDamage = (int) Math.ceil(playerDamage * 1.5);
                    appendLog(log, "【爆擊】本次物理傷害提升為 1.5 倍。");
                    if ("E003".equals(character.professionCode())) {
                        effects.killingIntent = Math.min(3, effects.killingIntent + 1);
                        appendLog(log, "【殺意】爆擊使殺意提升至 " + effects.killingIntent + " / 3。");
                    }
                }
            }
            if (effects.nextAttackBonus > 0) {
                playerDamage = (int) Math.ceil(playerDamage * (100 + effects.nextAttackBonus) / 100.0);
                appendLog(log, "【標記】引爆獵鷹標記，傷害提升 " + effects.nextAttackBonus + "% 。");
                effects.nextAttackBonus = 0;
            }
            if ("S017".equals(skill.code()) && monsterHp * 100 <= battle.maxMonsterHp() * 30) {
                // Homework03 replaces the skill's base damage with the target's
                // current HP.  Shield absorption is still resolved afterwards.
                playerDamage = monsterHp;
                appendLog(log, "【斬殺】敵人生命值不高於 30%，直接斬殺。");
            }
            int[] hitPercents = hitPercents(skill.code());
            int incomingDamage = 0;
            for (int percent : hitPercents) {
                incomingDamage += (int) Math.ceil(playerDamage * percent / 100.0);
            }
            int reflected = effects.monsterReflectPercent > 0 && effects.monsterShield > 0
                    ? (int) Math.ceil(incomingDamage * effects.monsterReflectPercent / 100.0) : 0;
            int actualDamage = 0;
            for (int index = 0; index < hitPercents.length && actualDamage < monsterHp; index++) {
                int segmentDamage = (int) Math.ceil(playerDamage * hitPercents[index] / 100.0);
                DamageResult hit = absorb(segmentDamage, effects.monsterShield);
                effects.monsterShield = hit.remainingShield();
                int segmentActual = Math.min(hit.hpDamage(), monsterHp - actualDamage);
                actualDamage += segmentActual;
                if (hit.absorbed() > 0) {
                    appendLog(log, "【敵方護盾】第 " + (index + 1) + " 段吸收 "
                            + hit.absorbed() + " 點傷害。");
                }
                if (hitPercents.length > 1) {
                    appendLog(log, "【連擊】第 " + (index + 1) + " 段造成 "
                            + segmentActual + " 點生命傷害。");
                }
            }
            int trueDamage = "E003".equals(character.professionCode())
                    ? (int) Math.ceil(actualDamage * effects.killingIntent * 0.10) : 0;
            monsterHp -= Math.min(monsterHp, actualDamage + trueDamage);
            appendLog(log, "【傷害】對 " + stage.monsterName() + " 造成 " + actualDamage + " 點生命傷害。");
            if (trueDamage > 0) appendLog(log, "【殺意】追加 " + trueDamage + " 點真實傷害。");
            if (reflected > 0) {
                playerHp = Math.max(0, playerHp - reflected);
                appendLog(log, "【敵方反傷】反射 " + reflected + " 點傷害。");
            }
            if ("E001".equals(character.professionCode()) && "PHYSICAL".equals(skill.type())) {
                double missingRatio = (battle.maxPlayerHp() - playerHp) / (double) battle.maxPlayerHp();
                double lifeStealRate = Math.min(0.9, Math.max(0, missingRatio)) * 0.20;
                int restored = Math.min((int) Math.ceil(actualDamage * lifeStealRate), battle.maxPlayerHp() - playerHp);
                if (restored > 0) {
                    playerHp += restored;
                    appendLog(log, "【浴血奮戰】物理吸血恢復 " + restored + " 點生命值。");
                }
            }
        }

        applyPlayerSkillEffects(skill.code(), effects, battle.maxPlayerHp() - playerHp, log);
        int retainedAction = retainedActionPercent(skill.code());
        playerAction = ACTION_FINISH * retainedAction / 100.0;
        if (retainedAction > 0) {
            appendLog(log, "【行動值】行動結束後保留 " + retainedAction + "% 行動值。");
        }
        effects.advancePlayerTurn();
        if (repository.hasEquipped(character.id(), "EQ017")) {
            int recovered = Math.min(15, battle.maxPlayerMp() - playerMp);
            playerMp += recovered;
            if (recovered > 0) appendLog(log, "【魔泉湧動】回合結束恢復 " + recovered + " MP。");
        }

        boolean levelUp = false;
        Integer rewardExp = null;
        Integer rewardGold = null;
        if (playerHp <= 0 && monsterHp > 0) {
            status = "DEFEAT";
            settleDefeat(userId, character, log);
        } else if (monsterHp <= 0) {
            status = "VICTORY";
            RewardResult reward = rewardVictory(userId, character, stage, battle.battleId(), battle.turnNumber(),
                    playerHp, playerMp, log);
            rewardExp = reward.exp();
            rewardGold = reward.gold();
            levelUp = reward.levelUp();
        } else {
            TurnProgress progress = advanceToPlayerTurn(stage, character, battle, effects,
                    new TurnProgress(playerHp, monsterHp, monsterMp, playerAction, monsterAction, nextTurn), log);
            playerHp = progress.playerHp();
            monsterHp = progress.monsterHp();
            monsterMp = progress.monsterMp();
            playerAction = progress.playerAction();
            monsterAction = progress.monsterAction();
            nextTurn = progress.turnNumber();
            if (monsterHp <= 0) {
                status = "VICTORY";
                RewardResult reward = rewardVictory(userId, character, stage, battle.battleId(), nextTurn,
                        playerHp, playerMp, log);
                rewardExp = reward.exp();
                rewardGold = reward.gold();
                levelUp = reward.levelUp();
            } else if (playerHp <= 0) {
                status = "DEFEAT";
                settleDefeat(userId, character, log);
            }
        }

        BattleData updated = new BattleData(battle.battleId(), battle.userId(), battle.characterId(),
                battle.stageCode(), battle.monsterLevel(), playerHp, battle.maxPlayerHp(), playerMp, battle.maxPlayerMp(),
                battle.playerAtk(), battle.playerAp(), battle.playerDef(), battle.playerMdef(),
                battle.playerSpeed(), playerAction, monsterHp,
                battle.maxMonsterHp(), monsterMp, battle.maxMonsterMp(), battle.monsterAtk(), battle.monsterAp(),
                battle.monsterDef(), battle.monsterMdef(), battle.monsterSpeed(), monsterAction, nextTurn, status,
                log.toString(), effects.serialize());
        repository.updateBattle(updated);
        CharacterData refreshed = requireCharacter(userId, character.id());
        return toBattleView(updated, refreshed, stage, levelUp, rewardExp, rewardGold);
    }

    @Transactional
    public synchronized RpgBattleView useBattleItem(long userId, String battleId, String rawItemCode) {
        BattleData battle = requireBattle(userId, battleId);
        if (!"ACTIVE".equals(battle.status())) throw new ResponseStatusException(HttpStatus.CONFLICT, "這場戰鬥已經結束");
        if (battle.playerAction() < ACTION_FINISH - ACTION_EPSILON)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "目前還沒輪到玩家使用道具");
        CharacterData character = requireCharacter(userId, battle.characterId());
        StageData stage = repository.findStage(character.id(), battle.stageCode(), battle.monsterLevel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到關卡資料"));
        String itemCode = normalizeCode(rawItemCode, "請選擇道具");
        ItemData item = repository.findInventoryItem(character.id(), itemCode)
                .filter(value -> "CONSUMABLE".equals(value.type()))
                .orElseThrow(() -> badRequest("戰鬥中只能使用背包內的消耗品"));
        int hp = battle.playerHp();
        int mp = battle.playerMp();
        for (ItemEffectData effect : repository.findItemEffects(itemCode)) {
            if ("RECOVER_HP".equals(effect.type())) hp = Math.min(battle.maxPlayerHp(), hp
                    + effect.flatValue() + (int) (battle.maxPlayerHp() * effect.percentValue() / 100.0));
            if ("RECOVER_MP".equals(effect.type())) mp = Math.min(battle.maxPlayerMp(), mp
                    + effect.flatValue() + (int) (battle.maxPlayerMp() * effect.percentValue() / 100.0));
        }
        if (hp == battle.playerHp() && mp == battle.playerMp())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "目前的 HP、MP 不需要恢復");
        if (!repository.consumeItem(character.id(), itemCode)) throw badRequest("道具數量不足");
        StringBuilder log = new StringBuilder(battle.logText());
        appendLog(log, "【道具】使用「" + item.name() + "」，恢復生命與魔力。");
        appendLog(log, "【行動】使用道具已消耗本回合行動。");
        EffectState effects = EffectState.parse(battle.effectState());
        effects.advancePlayerTurn();
        if (repository.hasEquipped(character.id(), "EQ017")) {
            int recovered = Math.min(15, battle.maxPlayerMp() - mp);
            mp += recovered;
            if (recovered > 0) appendLog(log, "【魔泉湧動】回合結束恢復 " + recovered + " MP。");
        }
        TurnProgress progress = advanceToPlayerTurn(stage, character, battle, effects,
                new TurnProgress(hp, battle.monsterHp(), battle.monsterMp(), 0,
                        battle.monsterAction(), battle.turnNumber()), log);
        String status = progress.playerHp() <= 0 ? "DEFEAT" : "ACTIVE";
        if ("DEFEAT".equals(status)) settleDefeat(userId, character, log);
        BattleData updated = withProgress(battle, progress, mp, status, log.toString(), effects.serialize());
        repository.updateBattle(updated);
        return toBattleView(updated, requireCharacter(userId, character.id()), stage, false, null, null);
    }

    private TurnProgress advanceToPlayerTurn(StageData stage, CharacterData character, BattleData battle,
            EffectState effects, TurnProgress initial, StringBuilder log) {
        TurnProgress state = initial;
        for (int safety = 0; safety < 100 && state.playerHp() > 0 && state.monsterHp() > 0; safety++) {
            int playerSpeed = Math.max(1, percentValue(battle.playerSpeed(), effects.speedBonus));
            int monsterSpeed = Math.max(1, effectiveMonsterSpeed(battle, effects));
            double playerTime = Math.max(0, ACTION_FINISH - state.playerAction()) / playerSpeed;
            double monsterTime = Math.max(0, ACTION_FINISH - state.monsterAction()) / monsterSpeed;
            double passedTime = Math.min(playerTime, monsterTime);
            double playerPosition = Math.min(ACTION_FINISH,
                    state.playerAction() + playerSpeed * passedTime);
            double monsterPosition = Math.min(ACTION_FINISH,
                    state.monsterAction() + monsterSpeed * passedTime);
            int turn = state.turnNumber() + 1;

            if (playerPosition >= ACTION_FINISH - ACTION_EPSILON) {
                appendLog(log, "第 " + turn + " 回合：輪到 " + character.name() + " 行動。");
                return new TurnProgress(state.playerHp(), state.monsterHp(), state.monsterMp(),
                        ACTION_FINISH, monsterPosition, turn);
            }

            appendLog(log, "第 " + turn + " 回合：輪到 " + stage.monsterName() + " 行動。");
            MonsterActionResult result = monsterAct(stage, battle, effects, state.playerHp(),
                    state.monsterHp(), state.monsterMp(), log);
            effects.advanceMonsterTurn();
            state = new TurnProgress(result.playerHp(), result.monsterHp(), result.monsterMp(),
                    playerPosition, ACTION_FINISH * result.retainedActionPercent() / 100.0, turn);
        }
        return state;
    }

    private BattleData withProgress(BattleData battle, TurnProgress progress, int playerMp,
            String status, String logText, String effectState) {
        return new BattleData(battle.battleId(), battle.userId(), battle.characterId(), battle.stageCode(),
                battle.monsterLevel(), progress.playerHp(), battle.maxPlayerHp(), playerMp, battle.maxPlayerMp(), battle.playerAtk(),
                battle.playerAp(), battle.playerDef(), battle.playerMdef(), battle.playerSpeed(),
                progress.playerAction(), progress.monsterHp(), battle.maxMonsterHp(), progress.monsterMp(),
                battle.maxMonsterMp(), battle.monsterAtk(), battle.monsterAp(), battle.monsterDef(),
                battle.monsterMdef(), battle.monsterSpeed(), progress.monsterAction(), progress.turnNumber(),
                status, logText, effectState);
    }

    private MonsterActionResult monsterAct(StageData stage, BattleData battle, EffectState effects,
            int playerHp, int monsterHp, int monsterMp, StringBuilder log) {
        List<MonsterSkillData> options = repository.findMonsterSkills(stage.monsterCode());
        if (options.isEmpty()) {
            int damage = Math.max(1, effectiveMonsterAtk(battle, effects)
                    - percentValue(battle.playerDef(), effects.defBonus));
            return applyMonsterDamage(stage.monsterName() + " 發動普通攻擊", damage, playerHp, monsterHp,
                    monsterMp, 0, effects, log);
        }
        MonsterSkillData chosen = chooseMonsterSkill(options, monsterMp);
        if (chosen == null) {
            int damage = Math.max(1, effectiveMonsterAtk(battle, effects)
                    - percentValue(battle.playerDef(), effects.defBonus));
            return applyMonsterDamage(stage.monsterName() + " 魔力不足，改用普通攻擊", damage,
                    playerHp, monsterHp, monsterMp, 0, effects, log);
        }
        SkillData skill = chosen.skill();
        monsterMp -= skill.mpCost();
        appendLog(log, "【敵方技能】" + stage.monsterName() + " 使用「" + skill.name() + "」。");

        int defense = "MAGICAL".equals(skill.type())
                ? battle.playerMdef() : percentValue(battle.playerDef(), effects.defBonus);
        int damage = calculateDamage(skill, effectiveMonsterAtk(battle, effects),
                effectiveMonsterAp(battle, effects), defense);
        applyMonsterSkillEffects(skill.code(), effects, log);
        return applyMonsterDamage("「" + skill.name() + "」", damage, playerHp, monsterHp, monsterMp,
                retainedActionPercent(skill.code()), effects, log);
    }

    private MonsterActionResult applyMonsterDamage(String actionName, int damage, int playerHp, int monsterHp,
            int monsterMp, int retainedActionPercent, EffectState effects, StringBuilder log) {
        int reflected = effects.reflectPercent > 0 && (effects.shield > 0 || effects.equipmentReflect > 0)
                ? (int) Math.ceil(damage * effects.reflectPercent / 100.0) : 0;
        DamageResult hit = absorb(damage, effects.shield);
        effects.shield = hit.remainingShield();
        int actualDamage = Math.min(hit.hpDamage(), playerHp);
        playerHp -= actualDamage;
        appendLog(log, actionName + "造成 " + actualDamage + " 點生命傷害"
                + (hit.absorbed() > 0 ? "，另有 " + hit.absorbed() + " 點被護盾吸收。" : "。"));
        if (reflected > 0) {
            monsterHp = Math.max(0, monsterHp - reflected);
            appendLog(log, "【壁壘反傷】反射 " + reflected + " 點傷害。");
        }
        return new MonsterActionResult(playerHp, monsterHp, monsterMp, retainedActionPercent);
    }

    private MonsterSkillData chooseMonsterSkill(List<MonsterSkillData> skills, int currentMp) {
        int total = skills.stream().filter(s -> s.skill().mpCost() <= currentMp)
                .mapToInt(MonsterSkillData::useWeight).sum();
        if (total <= 0) return null;
        int roll = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (MonsterSkillData setting : skills) {
            if (setting.skill().mpCost() > currentMp) continue;
            cumulative += setting.useWeight();
            if (roll < cumulative) return setting;
        }
        return null;
    }

    private int calculateDamage(SkillData skill, int atk, int ap, int defense) {
        int attackValue = "MAGICAL".equals(skill.type()) ? ap : atk;
        double value = skill.basePower() + attackValue * skill.scalingPercent() / 100.0;
        if ("MAGICAL".equals(skill.type())) {
            double reduction = Math.min(95, Math.max(0, defense));
            value = value * (100.0 - reduction) / 100.0;
        } else {
            value -= defense;
        }
        return Math.max(1, (int) value);
    }

    private int effectiveMonsterAtk(BattleData battle, EffectState effects) {
        return percentValue(battle.monsterAtk(), effects.monsterAtkBonus);
    }

    private int effectiveMonsterAp(BattleData battle, EffectState effects) {
        return percentValue(battle.monsterAp(), effects.monsterApBonus);
    }

    private int effectiveMonsterDef(BattleData battle, EffectState effects) {
        return percentValue(battle.monsterDef(), effects.monsterDefBonus);
    }

    private int effectiveMonsterMdef(BattleData battle, EffectState effects) {
        return percentValue(battle.monsterMdef(), effects.monsterMdefBonus);
    }

    private int effectiveMonsterSpeed(BattleData battle, EffectState effects) {
        return percentValue(battle.monsterSpeed(), effects.monsterSpeedBonus);
    }

    private int retainedActionPercent(String skillCode) {
        return switch (skillCode) {
            case "S004", "S006" -> 10;
            case "S007" -> 20;
            case "S023" -> 15;
            case "S040" -> 50;
            default -> 0;
        };
    }

    private int[] hitPercents(String skillCode) {
        return switch (skillCode) {
            case "S006" -> new int[] {50, 50};
            case "S023" -> new int[] {40, 40, 40};
            default -> new int[] {100};
        };
    }

    private int baseCriticalChance(String professionCode) {
        return switch (professionCode) {
            case "E003" -> 25;
            case "E005" -> 10;
            default -> 0;
        };
    }

    private DamageResult absorb(int damage, int shield) {
        int absorbed = Math.min(Math.max(0, shield), Math.max(0, damage));
        return new DamageResult(damage - absorbed, absorbed, shield - absorbed);
    }

    private RewardResult rewardVictory(long userId, CharacterData character, StageData stage,
            String battleId, int turns,
            int currentHp, int currentMp, StringBuilder log) {
        int rewardExp = repository.hasEquipped(character.id(), "EQ071")
                ? (int) Math.ceil(stage.rewardExp() * 1.10) : stage.rewardExp();
        int rewardGold = repository.hasEquipped(character.id(), "EQ070")
                ? (int) Math.ceil(stage.rewardGold() * 1.15) : stage.rewardGold();
        LevelResult result = addExperience(character.level(), character.experience(), rewardExp);
        boolean levelUp = result.level() > character.level();
        int savedHp = currentHp;
        int savedMp = currentMp;
        if (levelUp) {
            CharacterData leveled = new CharacterData(character.id(), character.userId(), character.name(),
                    character.professionCode(), character.professionName(), result.level(), result.experience(),
                    character.gold() + rewardGold, currentHp, currentMp);
            CharacterStats leveledStats = characterStats(leveled, requireProfession(character.professionCode()));
            savedHp = leveledStats.hp();
            savedMp = leveledStats.mp();
        }
        if (repository.hasEquipped(character.id(), "EQ067")) {
            CharacterData target = new CharacterData(character.id(), character.userId(), character.name(),
                    character.professionCode(), character.professionName(), result.level(), result.experience(),
                    character.gold() + rewardGold, savedHp, savedMp);
            CharacterStats targetStats = characterStats(target, requireProfession(character.professionCode()));
            savedHp = Math.min(targetStats.hp(), savedHp + (int) (targetStats.hp() * 0.08));
            savedMp = Math.min(targetStats.mp(), savedMp + (int) (targetStats.mp() * 0.08));
        }
        repository.saveCharacterState(userId, character.id(), result.level(), result.experience(),
                character.gold() + rewardGold, savedHp, savedMp);
        if (levelUp) {
            List<SkillData> learned = repository.learnProfessionSkills(character.id(),
                    character.professionCode(), character.level(), result.level(), 4);
            for (SkillData skill : learned) {
                appendLog(log, "【習得】學會技能「" + skill.name() + "」。");
            }
        }
        repository.recordStageClear(character.id(), stage.stageCode(), turns);
        rollAndAddItemDrops(character.id(), stage.monsterCode(), battleId, log);
        rollAndAddEquipmentDrops(character.id(), stage.monsterCode(), battleId, log);
        appendLog(log, "戰鬥勝利！獲得 " + rewardExp + " 經驗與 " + rewardGold + " 金幣。"
                + (levelUp ? "角色升級了！" : ""));
        return new RewardResult(rewardExp, rewardGold, levelUp);
    }

    private void rollAndAddItemDrops(long characterId, String monsterCode,
            String battleId, StringBuilder log) {
        double bonus = repository.hasEquipped(characterId, "EQ069") ? 1.15 : 1.0;
        for (ItemDropData drop : repository.findMonsterItemDrops(monsterCode)) {
            if (ThreadLocalRandom.current().nextDouble(100.0) >= Math.min(100.0, drop.dropRate() * bonus)) continue;
            int maximum = Math.max(drop.minQuantity(), drop.maxQuantity());
            int quantity = ThreadLocalRandom.current().nextInt(drop.minQuantity(), maximum + 1);
            repository.addItem(characterId, drop.itemCode(), quantity);
            repository.recordBattleItemDrop(battleId, drop.itemCode(), quantity);
            appendLog(log, "獲得「" + drop.itemName() + "」× " + quantity + "。");
        }
    }

    private void rollAndAddEquipmentDrops(long characterId, String monsterCode,
            String battleId, StringBuilder log) {
        List<EquipmentDropData> drops = repository.findMonsterEquipmentDrops(monsterCode);
        double bonus = repository.hasEquipped(characterId, "EQ069") ? 1.15 : 1.0;
        double totalRate = drops.stream().mapToDouble(drop -> drop.dropRate() * bonus).sum();
        int count = (int) (totalRate / 100.0);
        if (ThreadLocalRandom.current().nextDouble(100.0) < totalRate - count * 100.0) count++;
        for (int number = 0; number < count && !drops.isEmpty(); number++) {
            double roll = ThreadLocalRandom.current().nextDouble(totalRate);
            EquipmentDropData selected = drops.get(drops.size() - 1);
            double cumulative = 0;
            for (EquipmentDropData drop : drops) {
                cumulative += drop.dropRate() * bonus;
                if (roll < cumulative) { selected = drop; break; }
            }
            repository.addEquipment(characterId, selected.equipmentCode());
            repository.recordBattleEquipmentDrop(battleId, selected.equipmentCode(), selected.equipmentName());
            appendLog(log, "獲得裝備「" + selected.equipmentName() + "」。");
        }
    }

    private int settleDefeat(long userId, CharacterData character, StringBuilder log) {
        int penalty = Math.min(character.experience(),
                (int) Math.ceil(experienceForLevel(character.level()) * 0.10));
        CharacterStats stats = characterStats(character, requireProfession(character.professionCode()));
        repository.saveCharacterState(userId, character.id(), character.level(),
                character.experience() - penalty, character.gold(), stats.hp(), stats.mp());
        appendLog(log, "戰鬥失敗。失去 " + penalty
                + " 點經驗值，返回據點後生命與魔力已恢復。");
        return penalty;
    }

    @Transactional
    public void abandonBattle(long userId, String battleId) {
        BattleData battle = requireBattle(userId, battleId);
        if (!"ACTIVE".equals(battle.status())) return;
        BattleData abandoned = new BattleData(battle.battleId(), battle.userId(), battle.characterId(),
                battle.stageCode(), battle.monsterLevel(), battle.playerHp(), battle.maxPlayerHp(), battle.playerMp(),
                battle.maxPlayerMp(), battle.playerAtk(), battle.playerAp(), battle.playerDef(), battle.playerMdef(),
                battle.playerSpeed(), battle.playerAction(),
                battle.monsterHp(), battle.maxMonsterHp(), battle.monsterMp(), battle.maxMonsterMp(),
                battle.monsterAtk(), battle.monsterAp(), battle.monsterDef(), battle.monsterMdef(),
                battle.monsterSpeed(), battle.monsterAction(), battle.turnNumber(), "ABANDONED",
                battle.logText() + "\n玩家離開了這場戰鬥。", battle.effectState());
        repository.updateBattle(abandoned);
        CharacterData character = requireCharacter(userId, battle.characterId());
        repository.saveCharacterState(userId, character.id(), character.level(), character.experience(),
                character.gold(), battle.playerHp(), battle.playerMp());
    }

    private RpgBattleView toBattleView(BattleData battle, CharacterData character, StageData stage,
            boolean levelUp, Integer rewardExp, Integer rewardGold) {
        EffectState effects = EffectState.parse(battle.effectState());
        List<RpgSkillView> skills = repository.findEquippedSkills(character.id()).stream()
                .map(skill -> toSkillView(skill,
                        "ACTIVE".equals(battle.status()) && skill.mpCost() <= battle.playerMp()))
                .toList();
        List<String> logs = Arrays.stream(battle.logText().split("\\R"))
                .filter(line -> !line.isBlank()).toList();
        List<RpgDropView> drops = new java.util.ArrayList<>();
        drops.addAll(repository.findBattleItemDrops(battle.battleId()).stream()
                .map(drop -> RpgDropView.item(drop.itemCode(), drop.itemName(), drop.quantity())).toList());
        drops.addAll(repository.findBattleEquipmentDrops(battle.battleId()).stream()
                .map(drop -> RpgDropView.equipment(drop.equipmentCode(), drop.equipmentName())).toList());
        return new RpgBattleView(battle.battleId(), battle.status(), battle.turnNumber(), battle.monsterLevel(),
                new RpgCombatantView(character.professionCode(), character.name(), battle.playerHp(), battle.maxPlayerHp(),
                        battle.playerMp(), battle.maxPlayerMp(), effects.shield,
                        actionPercent(battle.playerAction()), null),
                new RpgCombatantView(stage.monsterCode(), stage.monsterName(), battle.monsterHp(), battle.maxMonsterHp(),
                        battle.monsterMp(), battle.maxMonsterMp(), effects.monsterShield,
                        actionPercent(battle.monsterAction()), stage.monsterImagePath()),
                skills, logs, rewardExp, rewardGold, levelUp, drops);
    }

    private RpgCharacterView toCharacterView(CharacterData character) {
        ProfessionData profession = requireProfession(character.professionCode());
        CharacterStats stats = characterStats(character, profession);
        int currentHp = currentValue(character.currentHp(), stats.hp());
        int currentMp = currentValue(character.currentMp(), stats.mp());
        return new RpgCharacterView(character.id(), character.name(), character.professionCode(),
                character.professionName(), character.level(), character.experience(),
                experienceForLevel(character.level()), character.gold(), currentHp, stats.hp(), currentMp, stats.mp(),
                stats.attack(), stats.magic(), stats.defense(), stats.magicDefense(), stats.speed());
    }

    private RpgSkillView toSkillView(SkillData skill, boolean available) {
        return new RpgSkillView(skill.code(), skill.name(), skill.description(), skill.type(),
                skill.mpCost(), skill.basePower(), skill.scalingPercent(), skill.requiredLevel(), available);
    }

    private RpgInventoryItemView toInventoryItemView(ItemData item) {
        List<RpgItemEffectView> effects = repository.findItemEffects(item.code()).stream()
                .map(effect -> new RpgItemEffectView(effect.type(), effect.flatValue(),
                        effect.percentValue())).toList();
        return new RpgInventoryItemView(item.code(), item.name(), item.type(),
                item.description(), item.quantity(), item.maxStack(), item.sellPrice(), effects);
    }

    private RpgEquipmentView toEquipmentView(EquipmentData equipment, String professionCode) {
        List<String> professions = repository.findEquipmentProfessions(equipment.code());
        return new RpgEquipmentView(equipment.ownedId(), equipment.code(), equipment.name(), equipment.type(),
                equipment.usageType(), equipment.requiredLevel(), equipment.description(), equipment.sellPrice(),
                equipment.equippedSlot(), "UNIVERSAL".equals(equipment.usageType()) || professions.contains(professionCode),
                professions, repository.findEquipmentStats(equipment.code()).stream()
                        .map(stat -> new RpgEquipmentStatView(stat.statType(), stat.modifierType(), stat.modifierValue()))
                        .toList());
    }

    private boolean professionAllowed(EquipmentData equipment, String professionCode) {
        return "UNIVERSAL".equals(equipment.usageType())
                || repository.findEquipmentProfessions(equipment.code()).contains(professionCode);
    }

    private RpgEquipmentResult equipmentResult(long userId, long characterId, String message) {
        CharacterData refreshed = requireCharacter(userId, characterId);
        return new RpgEquipmentResult(message, toCharacterView(refreshed),
                repository.findEquipment(characterId).stream()
                        .map(item -> toEquipmentView(item, refreshed.professionCode())).toList());
    }

    private void clampCharacterVitals(long userId, long characterId) {
        CharacterData character = requireCharacter(userId, characterId);
        CharacterStats stats = characterStats(character, requireProfession(character.professionCode()));
        repository.saveCharacterState(userId, characterId, character.level(), character.experience(), character.gold(),
                currentValue(character.currentHp(), stats.hp()), currentValue(character.currentMp(), stats.mp()));
    }

    private List<RpgShopProductView> mapShopProducts(List<ShopProductData> products, CharacterData character) {
        return products.stream().map(product -> new RpgShopProductView(product.id(), product.productType(),
                product.productCode(), product.productName(), product.description(), product.price(), product.quantity(),
                product.purchaseLimit(), product.purchasedQuantity(), product.itemType(),
                product.equipment() == null ? null : toEquipmentView(product.equipment(), character.professionCode()),
                product.productType().equals("ITEM") || product.productType().equals("EQUIPMENT")
                        ? repository.findShopMaterials("SH001", product.productType(), product.productCode(), character.id())
                            .stream().map(m -> new RpgShopMaterialView(m.itemCode(), m.itemName(),
                                    m.requiredQuantity(), m.ownedQuantity())).toList()
                        : List.of())).toList();
    }

    private RpgProfessionView toProfessionView(ProfessionData profession) {
        return new RpgProfessionView(profession.code(), profession.name(), profession.description(),
                professionTraitDescription(profession.code()),
                profession.baseHp(), profession.baseMp(), profession.baseAtk(), profession.baseAp(),
                profession.baseDef(), profession.baseMdef(), profession.baseSpeed());
    }

    private String professionTraitDescription(String professionCode) {
        return switch (professionCode) {
            case "E001" -> "【浴血奮戰】根據已損失生命值獲得物理吸血，血量越低吸血越高，最高 18%。";
            case "E002" -> "【魔力轉換】最大 MP 增加實際 AP 的 30%。";
            case "E003" -> "【殺意】爆擊時獲得 1 層殺意，最高 3 層；每層追加 10% 真實傷害。\n"
                    + "【暗殺訓練】爆擊機率增加 25%。";
            case "E004" -> "【守護誓言】最大 HP 增加 20%。";
            case "E005" -> "【自然步伐】SPEED 增加 12%。\n"
                    + "【鷹眼】物理穿透增加 10%，爆擊機率增加 10%。";
            case "E006" -> "【聖恩】AP 增加 15%。";
            default -> "無";
        };
    }

    private RpgStageView toStageView(StageData stage) {
        List<RpgPossibleDropView> possibleDrops = new java.util.ArrayList<>();
        possibleDrops.addAll(repository.findMonsterItemDrops(stage.monsterCode()).stream()
                .map(drop -> new RpgPossibleDropView("ITEM", drop.itemCode(), drop.itemName(),
                        drop.dropRate(), drop.minQuantity(), drop.maxQuantity())).toList());
        possibleDrops.addAll(repository.findMonsterEquipmentDrops(stage.monsterCode()).stream()
                .map(drop -> new RpgPossibleDropView("EQUIPMENT", drop.equipmentCode(), drop.equipmentName(),
                        drop.dropRate(), 1, 1)).toList());
        return new RpgStageView(stage.stageCode(), stage.stageOrder(), stage.regionCode(), stage.regionName(),
                stage.regionDescription(), stage.regionMinLevel(), stage.regionMaxLevel(),
                stage.stageName(), stage.recommendedLevel(), stage.description(), stage.imagePath(),
                stage.unlocked(), stage.clearCount(), stage.bestTurns(),
                stage.monsterCode(), stage.monsterName(), stage.monsterDescription(), stage.monsterImagePath(),
                stage.monsterMaxHp(), stage.rewardExp(), stage.rewardGold(), possibleDrops);
    }

    private CharacterData requireCharacter(long userId, long characterId) {
        return repository.findCharacter(userId, characterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到這名角色"));
    }

    private ProfessionData requireProfession(String code) {
        return repository.findProfession(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到職業資料"));
    }

    private BattleData requireBattle(long userId, String battleId) {
        if (battleId == null || battleId.isBlank()) throw badRequest("戰鬥 ID 不正確");
        return repository.findBattle(userId, battleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到這場戰鬥"));
    }

    private CharacterStats characterStats(CharacterData character, ProfessionData profession) {
        int growth = character.level() - 1;
        int[] values = { profession.baseHp() + growth * profession.growthHp(),
                profession.baseMp() + growth * profession.growthMp(),
                profession.baseAtk() + growth * profession.growthAtk(),
                profession.baseAp() + growth * profession.growthAp(),
                profession.baseDef() + growth * profession.growthDef(),
                profession.baseMdef() + growth * profession.growthMdef(),
                profession.baseSpeed() + growth * profession.growthSpeed() };
        int[] additive = new int[values.length];
        int[] baseBonus = new int[values.length];
        double[] multiplier = new double[values.length];
        switch (character.professionCode()) {
            case "E002" -> additive[1] += (int) (values[3] * 0.30);
            case "E004" -> multiplier[0] += 20;
            case "E005" -> multiplier[6] += 12;
            case "E006" -> multiplier[3] += 15;
            default -> { }
        }
        String[] names = { "HP", "MP", "ATK", "AP", "DEF", "MDEF", "SPEED" };
        for (EquipmentStatData stat : repository.findEquippedStats(character.id())) {
            int index = java.util.Arrays.asList(names).indexOf(stat.statType());
            if (index < 0) continue;
            if ("MULTIPLIER".equals(stat.modifierType())) multiplier[index] += stat.modifierValue();
            else if (index == 2 && "WEAPON".equals(stat.equipmentType())) baseBonus[index] += (int) stat.modifierValue();
            else additive[index] += (int) stat.modifierValue();
        }
        for (int index = 0; index < values.length; index++) {
            values[index] = (int) ((values[index] + baseBonus[index]) * (1 + multiplier[index] / 100.0)
                    + additive[index]);
        }
        return new CharacterStats(values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
    }

    private String slotName(String slot) {
        return switch (slot) {
            case "WEAPON" -> "武器";
            case "OFF_HAND" -> "副手";
            case "ARMOR" -> "護甲";
            case "SHOES" -> "鞋子";
            case "ACCESSORY_1" -> "飾品一";
            case "ACCESSORY_2" -> "飾品二";
            default -> slot;
        };
    }

    private int actionPercent(double position) {
        return (int) Math.round(Math.max(0, Math.min(ACTION_FINISH, position)) * 100 / ACTION_FINISH);
    }

    private LevelResult addExperience(int level, int experience, int gained) {
        int newLevel = level;
        int newExperience = experience + gained;
        while (newExperience >= experienceForLevel(newLevel)) {
            newExperience -= experienceForLevel(newLevel);
            newLevel++;
        }
        return new LevelResult(newLevel, newExperience);
    }

    private int experienceForLevel(int level) {
        return (int) Math.ceil(100 * Math.pow(1.3, level - 1));
    }

    private String canonicalProfessionCode(String code) {
        return switch (code) {
            case "WARRIOR" -> "E001";
            case "MAGE" -> "E002";
            case "ASSASSIN" -> "E003";
            case "PALADIN" -> "E004";
            case "RANGER" -> "E005";
            case "PRIEST" -> "E006";
            default -> code;
        };
    }

    private int percentValue(int base, int bonusPercent) {
        return (int) (base * (100 + bonusPercent) / 100.0);
    }

    private int currentValue(Integer stored, int maximum) {
        return stored == null ? maximum : Math.max(0, Math.min(stored, maximum));
    }

    private void applyPlayerSkillEffects(String skillCode, EffectState effects, int lostHp, StringBuilder log) {
        switch (skillCode) {
            case "S003" -> {
                int shield = (int) Math.ceil(lostHp * 0.10);
                if (shield > 0) effects.addShield(shield, 2);
                if (shield > 0) appendLog(log, "【護盾】依已損失生命值獲得 " + shield + " 點護盾，持續 2 回合。");
            }
            case "S005" -> effects.setApBonus(20, 2);
            case "S006" -> effects.setCriticalBonus(30, 2);
            case "S012" -> effects.setAtkBonus(25, 3);
            case "S014" -> {
                effects.speedBonus = 20;
                effects.speedTurns = 3;
                appendLog(log, "【速度提升】SPEED 提升 20%，持續 2 回合。");
            }
            case "S015" -> effects.setApBonus(30, 2);
            case "S019" -> effects.setDefBonus(15, 2);
            case "S020" -> {
                effects.addShield(150, 3);
                effects.reflectPercent = 30;
                effects.reflectTurns = 4;
            }
            case "S021" -> effects.addShield(80, 2);
            case "S024" -> effects.nextAttackBonus = 25;
            case "S025" -> {
                effects.monsterSpeedBonus = -40;
                effects.monsterSpeedTurns = 3;
                appendLog(log, "【速度降低】敵人 SPEED 降低 40%，持續 3 回合。");
            }
            case "S026" -> effects.addShield(30, 2);
            case "S027" -> effects.setApBonus(15, 2);
            case "S028" -> {
                effects.addShield(80, 3);
                effects.setDefBonus(20, 3);
            }
            case "S029" -> effects.setApBonus(25, 2);
            default -> { }
        }
        if (List.of("S005", "S006", "S012", "S015", "S019", "S020", "S021",
                "S024", "S026", "S027", "S028", "S029").contains(skillCode)) {
            appendLog(log, "【狀態】" + effectSummary(skillCode));
        }
    }

    private void applyMonsterSkillEffects(String skillCode, EffectState effects, StringBuilder log) {
        switch (skillCode) {
            case "S014" -> {
                effects.monsterSpeedBonus = 20;
                effects.monsterSpeedTurns = 3;
                appendLog(log, "【敵方狀態】SPEED 提升 20%，持續 2 回合。");
            }
            case "S015" -> {
                effects.monsterApBonus = 30;
                effects.monsterApTurns = 3;
                appendLog(log, "【敵方狀態】AP 提升 30%，持續 2 回合。");
            }
            case "S020" -> {
                effects.addMonsterShield(150, 3);
                effects.monsterReflectPercent = 30;
                effects.monsterReflectTurns = 4;
                appendLog(log, "【敵方狀態】獲得 150 點護盾與 30% 反傷，持續 3 回合。");
            }
            case "S021" -> {
                effects.addMonsterShield(80, 2);
                appendLog(log, "【敵方護盾】獲得 80 點護盾，持續 2 回合。");
            }
            default -> { }
        }
    }

    private String effectSummary(String skillCode) {
        return switch (skillCode) {
            case "S005" -> "AP 提升 20%，持續 2 回合。";
            case "S006" -> "爆擊率提升 30%，持續 2 回合。";
            case "S012" -> "ATK 提升 25%，持續 3 回合。";
            case "S015" -> "AP 提升 30%，持續 2 回合。";
            case "S019" -> "DEF 提升 15%，持續 2 回合。";
            case "S020" -> "獲得 150 點護盾與 30% 反傷，持續 3 回合。";
            case "S021" -> "獲得 80 點護盾，持續 2 回合。";
            case "S024" -> "留下獵鷹標記，下一次攻擊傷害提升 25%。";
            case "S026" -> "獲得 30 點護盾，持續 2 回合。";
            case "S027" -> "AP 提升 15%，持續 2 回合。";
            case "S028" -> "獲得 80 點護盾與 20% DEF，持續 3 回合。";
            case "S029" -> "AP 提升 25%，持續 2 回合。";
            default -> "";
        };
    }

    private String normalizeCode(String value, String emptyMessage) {
        if (value == null || value.isBlank()) throw badRequest(emptyMessage);
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void appendLog(StringBuilder log, String message) {
        if (!log.isEmpty()) log.append('\n');
        log.append(message);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record CharacterStats(int hp, int mp, int attack, int magic, int defense, int magicDefense, int speed) { }
    private record LevelResult(int level, int experience) { }
    private record DamageResult(int hpDamage, int absorbed, int remainingShield) { }
    private record MonsterActionResult(int playerHp, int monsterHp, int monsterMp, int retainedActionPercent) { }
    private record RewardResult(int exp, int gold, boolean levelUp) { }
    private record TurnProgress(int playerHp, int monsterHp, int monsterMp,
            double playerAction, double monsterAction, int turnNumber) { }

    private static final class EffectState {
        int shield;
        int shieldTurns;
        int atkBonus;
        int atkTurns;
        int apBonus;
        int apTurns;
        int defBonus;
        int defTurns;
        int criticalBonus;
        int criticalTurns;
        int speedBonus;
        int speedTurns;
        int killingIntent;
        int nextAttackBonus;
        int reflectPercent;
        int reflectTurns;
        int monsterShield;
        int monsterShieldTurns;
        int monsterAtkBonus;
        int monsterAtkTurns;
        int monsterApBonus;
        int monsterApTurns;
        int monsterDefBonus;
        int monsterDefTurns;
        int monsterMdefBonus;
        int monsterMdefTurns;
        int monsterSpeedBonus;
        int monsterSpeedTurns;
        int monsterReflectPercent;
        int monsterReflectTurns;
        int equipmentReflect;

        static EffectState parse(String encoded) {
            EffectState state = new EffectState();
            if (encoded == null || encoded.isBlank()) return state;
            for (String part : encoded.split(";")) {
                String[] pair = part.split("=", 2);
                if (pair.length != 2) continue;
                try {
                    int value = Integer.parseInt(pair[1]);
                    switch (pair[0]) {
                        case "shield" -> state.shield = value;
                        case "shieldTurns" -> state.shieldTurns = value;
                        case "atkBonus" -> state.atkBonus = value;
                        case "atkTurns" -> state.atkTurns = value;
                        case "apBonus" -> state.apBonus = value;
                        case "apTurns" -> state.apTurns = value;
                        case "defBonus" -> state.defBonus = value;
                        case "defTurns" -> state.defTurns = value;
                        case "criticalBonus" -> state.criticalBonus = value;
                        case "criticalTurns" -> state.criticalTurns = value;
                        case "speedBonus" -> state.speedBonus = value;
                        case "speedTurns" -> state.speedTurns = value;
                        case "killingIntent" -> state.killingIntent = value;
                        case "nextAttackBonus" -> state.nextAttackBonus = value;
                        case "reflectPercent" -> state.reflectPercent = value;
                        case "reflectTurns" -> state.reflectTurns = value;
                        case "monsterShield" -> state.monsterShield = value;
                        case "monsterShieldTurns" -> state.monsterShieldTurns = value;
                        case "monsterAtkBonus" -> state.monsterAtkBonus = value;
                        case "monsterAtkTurns" -> state.monsterAtkTurns = value;
                        case "monsterApBonus" -> state.monsterApBonus = value;
                        case "monsterApTurns" -> state.monsterApTurns = value;
                        case "monsterDefBonus" -> state.monsterDefBonus = value;
                        case "monsterDefTurns" -> state.monsterDefTurns = value;
                        case "monsterMdefBonus" -> state.monsterMdefBonus = value;
                        case "monsterMdefTurns" -> state.monsterMdefTurns = value;
                        case "monsterSpeedBonus" -> state.monsterSpeedBonus = value;
                        case "monsterSpeedTurns" -> state.monsterSpeedTurns = value;
                        case "monsterReflectPercent" -> state.monsterReflectPercent = value;
                        case "monsterReflectTurns" -> state.monsterReflectTurns = value;
                        case "equipmentReflect" -> state.equipmentReflect = value;
                        default -> { }
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore malformed values from an interrupted development database.
                }
            }
            return state;
        }

        void addShield(int value, int turns) {
            shield += value;
            shieldTurns = Math.max(shieldTurns, turns + 1);
        }

        void addMonsterShield(int value, int turns) {
            monsterShield += value;
            monsterShieldTurns = Math.max(monsterShieldTurns, turns + 1);
        }

        void setAtkBonus(int value, int turns) { atkBonus = value; atkTurns = turns + 1; }
        void setApBonus(int value, int turns) { apBonus = value; apTurns = turns + 1; }
        void setDefBonus(int value, int turns) { defBonus = value; defTurns = turns + 1; }
        void setCriticalBonus(int value, int turns) { criticalBonus = value; criticalTurns = turns + 1; }

        void advancePlayerTurn() {
            if (shieldTurns > 0 && --shieldTurns == 0) shield = 0;
            if (atkTurns > 0 && --atkTurns == 0) atkBonus = 0;
            if (apTurns > 0 && --apTurns == 0) apBonus = 0;
            if (defTurns > 0 && --defTurns == 0) defBonus = 0;
            if (criticalTurns > 0 && --criticalTurns == 0) criticalBonus = 0;
            if (speedTurns > 0 && --speedTurns == 0) speedBonus = 0;
            if (reflectTurns > 0 && --reflectTurns == 0) reflectPercent = 0;
        }

        void advanceMonsterTurn() {
            if (monsterShieldTurns > 0 && --monsterShieldTurns == 0) monsterShield = 0;
            if (monsterAtkTurns > 0 && --monsterAtkTurns == 0) monsterAtkBonus = 0;
            if (monsterApTurns > 0 && --monsterApTurns == 0) monsterApBonus = 0;
            if (monsterDefTurns > 0 && --monsterDefTurns == 0) monsterDefBonus = 0;
            if (monsterMdefTurns > 0 && --monsterMdefTurns == 0) monsterMdefBonus = 0;
            if (monsterSpeedTurns > 0 && --monsterSpeedTurns == 0) monsterSpeedBonus = 0;
            if (monsterReflectTurns > 0 && --monsterReflectTurns == 0) monsterReflectPercent = 0;
        }

        String serialize() {
            return "shield=" + shield + ";shieldTurns=" + shieldTurns
                    + ";atkBonus=" + atkBonus + ";atkTurns=" + atkTurns
                    + ";apBonus=" + apBonus + ";apTurns=" + apTurns
                    + ";defBonus=" + defBonus + ";defTurns=" + defTurns
                    + ";criticalBonus=" + criticalBonus + ";criticalTurns=" + criticalTurns
                    + ";speedBonus=" + speedBonus + ";speedTurns=" + speedTurns
                    + ";killingIntent=" + killingIntent
                    + ";nextAttackBonus=" + nextAttackBonus
                    + ";reflectPercent=" + reflectPercent + ";reflectTurns=" + reflectTurns
                    + ";monsterShield=" + monsterShield + ";monsterShieldTurns=" + monsterShieldTurns
                    + ";monsterAtkBonus=" + monsterAtkBonus + ";monsterAtkTurns=" + monsterAtkTurns
                    + ";monsterApBonus=" + monsterApBonus + ";monsterApTurns=" + monsterApTurns
                    + ";monsterDefBonus=" + monsterDefBonus + ";monsterDefTurns=" + monsterDefTurns
                    + ";monsterMdefBonus=" + monsterMdefBonus + ";monsterMdefTurns=" + monsterMdefTurns
                    + ";monsterSpeedBonus=" + monsterSpeedBonus + ";monsterSpeedTurns=" + monsterSpeedTurns
                    + ";monsterReflectPercent=" + monsterReflectPercent
                    + ";monsterReflectTurns=" + monsterReflectTurns
                    + ";equipmentReflect=" + equipmentReflect;
        }
    }
}
