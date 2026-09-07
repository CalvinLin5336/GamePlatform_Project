package com.example.demo.modules.game.rpg.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.modules.game.rpg.dto.RpgBattleActionRequest;
import com.example.demo.modules.game.rpg.dto.RpgBattleView;
import com.example.demo.modules.game.rpg.dto.RpgCharacterView;
import com.example.demo.modules.game.rpg.dto.RpgCreateCharacterRequest;
import com.example.demo.modules.game.rpg.dto.RpgProfessionView;
import com.example.demo.modules.game.rpg.dto.RpgInventoryItemView;
import com.example.demo.modules.game.rpg.dto.RpgSkillView;
import com.example.demo.modules.game.rpg.dto.RpgSkillConfigurationView;
import com.example.demo.modules.game.rpg.dto.RpgStageView;
import com.example.demo.modules.game.rpg.dto.RpgStartBattleRequest;
import com.example.demo.modules.game.rpg.dto.RpgUpdateEquippedSkillsRequest;
import com.example.demo.modules.game.rpg.dto.RpgUseItemRequest;
import com.example.demo.modules.game.rpg.dto.RpgUseItemResult;
import com.example.demo.modules.game.rpg.dto.RpgEquipRequest;
import com.example.demo.modules.game.rpg.dto.RpgEquipmentResult;
import com.example.demo.modules.game.rpg.dto.RpgEquipmentView;
import com.example.demo.modules.game.rpg.dto.RpgShopActionRequest;
import com.example.demo.modules.game.rpg.dto.RpgShopActionResult;
import com.example.demo.modules.game.rpg.dto.RpgShopView;
import com.example.demo.modules.game.rpg.service.RpgGameService;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.service.LoginSessionService;

@RestController
@RequestMapping("/api/games/rpg")
public class RpgGameController {
    private final RpgGameService gameService;
    private final LoginSessionService sessions;

    public RpgGameController(RpgGameService gameService, LoginSessionService sessions) {
        this.gameService = gameService;
        this.sessions = sessions;
    }

    @GetMapping("/professions")
    public List<RpgProfessionView> professions(Authentication authentication) {
        requireUser(authentication);
        return gameService.findProfessions();
    }

    @GetMapping("/characters")
    public List<RpgCharacterView> characters(Authentication authentication) {
        return gameService.findCharacters(requireUser(authentication).id());
    }

    @PostMapping("/characters")
    public RpgCharacterView createCharacter(
            @RequestBody RpgCreateCharacterRequest request,
            Authentication authentication) {
        UserResponse user = requireUser(authentication);
        return gameService.createCharacter(user.id(), request.characterName(), request.professionCode());
    }

    @GetMapping("/characters/{characterId}/stages")
    public List<RpgStageView> stages(
            @PathVariable long characterId,
            Authentication authentication) {
        return gameService.findStages(requireUser(authentication).id(), characterId);
    }

    @GetMapping("/characters/{characterId}/skills")
    public List<RpgSkillView> skills(
            @PathVariable long characterId,
            Authentication authentication) {
        return gameService.findSkills(requireUser(authentication).id(), characterId);
    }

    @GetMapping("/characters/{characterId}/skill-configuration")
    public RpgSkillConfigurationView skillConfiguration(
            @PathVariable long characterId,
            Authentication authentication) {
        return gameService.findSkillConfiguration(requireUser(authentication).id(), characterId);
    }

    @PutMapping("/characters/{characterId}/skill-configuration")
    public RpgSkillConfigurationView saveSkillConfiguration(
            @PathVariable long characterId,
            @RequestBody RpgUpdateEquippedSkillsRequest request,
            Authentication authentication) {
        return gameService.saveEquippedSkills(requireUser(authentication).id(),
                characterId, request.skillCodes());
    }

    @GetMapping("/characters/{characterId}/inventory")
    public List<RpgInventoryItemView> inventory(
            @PathVariable long characterId,
            Authentication authentication) {
        return gameService.findInventory(requireUser(authentication).id(), characterId);
    }

    @PostMapping("/characters/{characterId}/items/use")
    public RpgUseItemResult useItem(
            @PathVariable long characterId,
            @RequestBody RpgUseItemRequest request,
            Authentication authentication) {
        return gameService.useItem(requireUser(authentication).id(), characterId, request.itemCode());
    }

    @GetMapping("/characters/{characterId}/equipment")
    public List<RpgEquipmentView> equipment(@PathVariable long characterId, Authentication authentication) {
        return gameService.findEquipment(requireUser(authentication).id(), characterId);
    }

    @PutMapping("/characters/{characterId}/equipment")
    public RpgEquipmentResult equip(@PathVariable long characterId, @RequestBody RpgEquipRequest request,
            Authentication authentication) {
        return gameService.equip(requireUser(authentication).id(), characterId,
                request.ownedEquipmentId(), request.slot());
    }

    @PostMapping("/characters/{characterId}/equipment/{ownedId}/unequip")
    public RpgEquipmentResult unequip(@PathVariable long characterId, @PathVariable long ownedId,
            Authentication authentication) {
        return gameService.unequip(requireUser(authentication).id(), characterId, ownedId);
    }

    @GetMapping("/characters/{characterId}/shop")
    public RpgShopView shop(@PathVariable long characterId, Authentication authentication) {
        return gameService.findShop(requireUser(authentication).id(), characterId);
    }

    @PostMapping("/characters/{characterId}/shop/actions")
    public RpgShopActionResult shopAction(@PathVariable long characterId,
            @RequestBody RpgShopActionRequest request, Authentication authentication) {
        return gameService.shopAction(requireUser(authentication).id(), characterId,
                request.action(), request.productId());
    }

    @PostMapping("/battles")
    public RpgBattleView startBattle(
            @RequestBody RpgStartBattleRequest request,
            Authentication authentication) {
        UserResponse user = requireUser(authentication);
        return gameService.startBattle(user.id(), request.characterId(), request.stageCode());
    }

    @PostMapping("/battles/{battleId}/items")
    public RpgBattleView useBattleItem(@PathVariable String battleId,
            @RequestBody RpgUseItemRequest request, Authentication authentication) {
        return gameService.useBattleItem(requireUser(authentication).id(), battleId, request.itemCode());
    }

    @GetMapping("/battles/{battleId}")
    public RpgBattleView battle(
            @PathVariable String battleId,
            Authentication authentication) {
        return gameService.findBattle(requireUser(authentication).id(), battleId);
    }

    @PostMapping("/battles/{battleId}/actions")
    public RpgBattleView act(
            @PathVariable String battleId,
            @RequestBody RpgBattleActionRequest request,
            Authentication authentication) {
        UserResponse user = requireUser(authentication);
        return gameService.act(user.id(), battleId, request.skillCode());
    }

    @PostMapping("/battles/{battleId}/abandon")
    public void abandon(
            @PathVariable String battleId,
            Authentication authentication) {
        gameService.abandonBattle(requireUser(authentication).id(), battleId);
    }

    private UserResponse requireUser(Authentication authentication) {
        return sessions.requireUserFromAuthentication(authentication);
    }
}
