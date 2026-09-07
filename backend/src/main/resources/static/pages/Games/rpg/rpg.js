(function () {
    'use strict';

    const isLiveServer = window.location.port === '5500' || window.location.port === '5501';
    const API_BASE = isLiveServer
        ? 'http://' + window.location.hostname + ':8080'
        : window.location.origin;
    const RPG_API = '/api/games/rpg';
    const state = {
        user: null,
        professions: [],
        characters: [],
        selectedCharacter: null,
        stages: [],
        skills: [],
        inventory: [],
        inventoryFilter: 'CONSUMABLE',
        selectedItemCode: null,
        equipment: [],
        selectedEquipmentId: null,
        shop: null,
        shopTab: 'equipment',
        selectedShopProductId: null,
        skillConfiguration: null,
        selectedConfigSkillCode: null,
        selectedProfessionCode: null,
        selectedProfessionSkillCode: null,
        selectedRegionCode: 'A001',
        selectedStage: null,
        battle: null,
        selectedSkillCode: null
    };
    const byId = id => document.getElementById(id);
    const professionCodes = ['E001', 'E002', 'E003', 'E004', 'E005', 'E006'];
    const monsterCodes = Array.from({ length: 15 }, (_, index) => 'M' + String(index + 1).padStart(3, '0'));

    function token() {
        return localStorage.getItem('token') || '';
    }

    async function request(path, options) {
        const settings = Object.assign({}, options || {});
        settings.headers = Object.assign({}, settings.headers || {}, {
            Authorization: 'Bearer ' + token()
        });
        if (settings.body) settings.headers['Content-Type'] = 'application/json';

        const response = await fetch(API_BASE + path, settings);
        if (!response.ok) {
            let message = '操作失敗（HTTP ' + response.status + '）';
            try {
                const data = await response.json();
                message = data.message || data.detail || data.error || message;
            } catch (_) {
                // 保留 HTTP 狀態訊息。
            }
            const error = new Error(message);
            error.status = response.status;
            throw error;
        }
        if (response.status === 204) return null;
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    }

    function escapeHtml(value) {
        const node = document.createElement('div');
        node.textContent = value == null ? '' : String(value);
        return node.innerHTML;
    }

    function showMessage(text, type) {
        const message = byId('message');
        message.textContent = text || '';
        message.className = 'message' + (type ? ' ' + type : '');
        message.hidden = !text;
    }

    function setBusy(button, busy, busyText) {
        if (!button.dataset.label) button.dataset.label = button.textContent;
        button.disabled = busy;
        button.textContent = busy ? busyText : button.dataset.label;
    }

    function percent(current, max) {
        if (!max) return 0;
        return Math.max(0, Math.min(100, current * 100 / max));
    }

    function setBar(barId, current, max) {
        byId(barId).style.width = percent(current, max) + '%';
    }

    function setProfessionSprite(element, code) {
        professionCodes.forEach(value => element.classList.remove('profession-' + value));
        element.classList.add('profession-' + (professionCodes.includes(code) ? code : 'E001'));
    }

    function setMonsterSprite(element, code) {
        monsterCodes.forEach(value => element.classList.remove('monster-' + value));
        const normalized = monsterCodes.includes(code) ? code : 'M001';
        element.classList.add('monster-' + normalized);
        const index = Math.max(0, Number(normalized.slice(1)) - 1);
        element.style.backgroundPosition = (index % 4) * 100 / 3 + '% '
            + Math.floor(index / 4) * 100 / 3 + '%';
    }

    function skillIconStyle(skillCode) {
        const number = /^S\d{3}$/.test(skillCode) ? Number(skillCode.slice(1)) : 1;
        const entry = number >= 40 && number <= 44
            ? ['skill-icons-03.png', number - 40]
            : number >= 17
                ? ['skill-icons-02.png', Math.min(15, number - 17)]
                : ['skill-icons-01.png', Math.max(0, number - 1)];
        const column = entry[1] % 4;
        const row = Math.floor(entry[1] / 4);
        return 'background-image:url(../../../assets/Games/rpg/' + entry[0] + ');'
            + 'background-position:' + (column * 100 / 3) + '% ' + (row * 100 / 3) + '%';
    }

    function skillIcon(skill) {
        return '<span class="skill-icon" style="' + skillIconStyle(skill.skillCode) + '"></span>';
    }

    function itemIconStyle(itemCode) {
        const firstAtlas = ['I001', 'I002', 'I003', 'I004', 'I005', 'I006', 'I101', 'I102',
            'I103', 'I104', 'I105', 'I106', 'I201', 'I202', 'I203', 'I204'];
        const secondAtlas = ['I107', 'I108', 'I109', 'I110', 'I111', 'I301', 'I302', 'I303', 'I304', 'I305'];
        let file = 'item-icons.png';
        let index = firstAtlas.indexOf(itemCode);
        if (index < 0) {
            file = 'item-icons-02.png';
            index = Math.max(0, secondAtlas.indexOf(itemCode));
        }
        return 'background-image:url(../../../assets/Games/rpg/' + file + ');'
            + 'background-position:' + ((index % 4) * 100 / 3) + '% '
            + (Math.floor(index / 4) * 100 / 3) + '%';
    }

    function equipmentIconStyle(equipmentCode) {
        const number = /^EQ\d{3}$/.test(equipmentCode) ? Number(equipmentCode.slice(2)) : 1;
        let file = 'equipment-icons.png';
        let index = Math.max(0, number - 1);
        let columns = 5;
        let rows = 5;
        if (number >= 24) {
            file = 'equipment-icons-02.png'; columns = 8; rows = 6;
            const exceptions = { 48: 41, 54: 31, 55: 32, 56: 43, 61: 38, 62: 39,
                63: 40, 64: 39, 65: 40, 66: 42, 67: 43, 68: 44, 69: 45, 70: 46, 71: 47 };
            index = Object.prototype.hasOwnProperty.call(exceptions, number) ? exceptions[number] : number - 24;
        }
        return 'background-image:url(../../../assets/Games/rpg/' + file + ');'
            + 'background-size:' + (columns * 100) + '% ' + (rows * 100) + '%;'
            + 'background-position:' + ((index % columns) * 100 / (columns - 1)) + '% '
            + (Math.floor(index / columns) * 100 / (rows - 1)) + '%';
    }

    function showScreen(name) {
        const subtitles = {
            characterScreen: '選擇出征角色',
            adventureScreen: '冒險地圖',
            stageScreen: '出征準備',
            battleScreen: '戰鬥'
        };
        ['characterScreen', 'adventureScreen', 'stageScreen', 'battleScreen'].forEach(id => {
            byId(id).hidden = id !== name;
        });
        byId('pageSubtitle').textContent = subtitles[name] || '';
        window.scrollTo({ top: 0, behavior: 'auto' });
    }

    function professionByCode(code) {
        return state.professions.find(item => item.code === code);
    }

    function renderProfessions() {
        byId('professionGrid').innerHTML = state.professions.map(item =>
            '<button class="profession-card' + (item.code === state.selectedProfessionCode ? ' selected' : '')
            + '" data-code="' + escapeHtml(item.code) + '" type="button">'
            + '<span class="sprite profession-sprite profession-' + escapeHtml(item.code) + '"></span>'
            + '<strong>' + escapeHtml(item.name) + '</strong></button>'
        ).join('');
        renderProfessionDetail();
    }

    function renderProfessionDetail() {
        const profession = professionByCode(state.selectedProfessionCode);
        byId('professionCode').value = profession ? profession.code : '';
        if (!profession) {
            byId('professionDetail').textContent = '請從左側選擇職業。';
            byId('professionSkillList').innerHTML = '';
            byId('professionSkillDetail').textContent = '請選擇職業技能。';
            return;
        }
        byId('professionDetail').innerHTML = '<strong>' + escapeHtml(profession.name) + '</strong><br><br>'
            + escapeHtml(profession.description) + '<br><br>基礎值<br>'
            + 'HP ' + profession.hp + '　MP ' + profession.mp + '<br>'
            + 'ATK ' + profession.attack + '　AP ' + profession.magic + '<br>'
            + 'DEF ' + profession.defense + '　MDEF ' + profession.magicDefense
            + '　SPEED ' + profession.speed + '<br><br>每級成長<br>'
            + 'HP +' + profession.growthHp + '　MP +' + profession.growthMp + '<br>'
            + 'ATK +' + profession.growthAttack + '　AP +' + profession.growthMagic + '<br>'
            + 'DEF +' + profession.growthDefense + '　MDEF +' + profession.growthMagicDefense + '<br>'
            + 'SPEED +' + profession.growthSpeed + '<br><br>職業特性：<br>'
            + escapeHtml(profession.traitDescription).replace(/\n/g, '<br>')
            + '<br><br>額外屬性：<br>' + escapeHtml(profession.extraAttributes).replace(/\n/g, '<br>');
        const skills = profession.skills || [];
        if (!skills.some(skill => skill.skillCode === state.selectedProfessionSkillCode)) {
            state.selectedProfessionSkillCode = skills.length ? skills[0].skillCode : null;
        }
        byId('professionSkillList').innerHTML = skills.map(skill =>
            '<button class="create-skill-entry' + (skill.skillCode === state.selectedProfessionSkillCode ? ' selected' : '')
            + '" data-code="' + escapeHtml(skill.skillCode) + '" type="button">'
            + skillIcon(skill) + '<span>Lv' + skill.requiredLevel + '　<strong>'
            + escapeHtml(skill.skillName) + '</strong>　MP ' + skill.mpCost + '</span></button>'
        ).join('');
        const selectedSkill = skills.find(skill => skill.skillCode === state.selectedProfessionSkillCode);
        byId('professionSkillDetail').innerHTML = selectedSkill
            ? '學習等級：Lv' + selectedSkill.requiredLevel + '<br>' + skillDetailHtml(selectedSkill)
            : '這個職業目前沒有可學習技能。';
    }

    function renderCharacters() {
        const grid = byId('characterGrid');
        if (!state.characters.length) {
            grid.innerHTML = '<div class="parchment empty-state">名冊目前是空的，請建立第一名冒險者。</div>';
            state.selectedCharacter = null;
            renderCharacterDetail();
        } else {
            grid.innerHTML = state.characters.map(character =>
                '<button class="character-entry' + (state.selectedCharacter
                    && state.selectedCharacter.characterId === character.characterId ? ' selected' : '')
                + '" data-id="' + character.characterId + '" type="button">'
                + '<span class="sprite profession-sprite profession-' + escapeHtml(character.professionCode) + '"></span>'
                + '<span><strong>' + escapeHtml(character.characterName) + '　Lv' + character.level + '</strong>'
                + escapeHtml(character.professionName) + '<br>HP ' + character.currentHp + '/' + character.maxHp
                + '　MP ' + character.currentMp + '/' + character.maxMp + '</span></button>'
            ).join('');
            if (!state.selectedCharacter
                    || !state.characters.some(item => item.characterId === state.selectedCharacter.characterId)) {
                state.selectedCharacter = state.characters[0];
            }
            renderCharacterDetail();
        }
        byId('openCreateBtn').hidden = state.characters.length >= 3;
    }

    function renderCharacterDetail() {
        const character = state.selectedCharacter;
        byId('characterEmpty').hidden = Boolean(character);
        byId('characterSelected').hidden = !character;
        if (!character) return;

        setProfessionSprite(byId('characterPortrait'), character.professionCode);
        byId('detailCharacterName').textContent = character.characterName;
        byId('detailProfession').textContent = character.professionName;
        byId('detailLevel').textContent = character.level;
        byId('detailGold').textContent = character.gold;
        byId('detailSkillSlots').textContent = character.skillSlotCount;
        byId('detailHp').textContent = character.currentHp + ' / ' + character.maxHp;
        byId('detailMp').textContent = character.currentMp + ' / ' + character.maxMp;
        byId('detailExp').textContent = character.experience + ' / ' + character.experienceToNextLevel;
        setBar('detailHpBar', character.currentHp, character.maxHp);
        setBar('detailMpBar', character.currentMp, character.maxMp);
        setBar('detailExpBar', character.experience, character.experienceToNextLevel);
        byId('detailAttack').textContent = character.attack;
        byId('detailMagic').textContent = character.magic;
        byId('detailDefense').textContent = character.defense;
        byId('detailSpeed').textContent = character.speed;
        const profession = professionByCode(character.professionCode);
        byId('detailProfessionText').textContent = profession
            ? '職業特性　' + profession.traitDescription : '';
    }

    function renderCharacterStatus() {
        const character = state.selectedCharacter;
        if (!character) return;
        const profession = professionByCode(character.professionCode);
        setProfessionSprite(byId('statusPortrait'), character.professionCode);
        byId('statusName').textContent = character.characterName;
        byId('statusProfession').textContent = character.professionName;
        byId('statusLevel').textContent = character.level;
        byId('statusHp').textContent = character.currentHp + ' / ' + character.maxHp;
        byId('statusMp').textContent = character.currentMp + ' / ' + character.maxMp;
        byId('statusExp').textContent = character.experience + ' / ' + character.experienceToNextLevel;
        setBar('statusHpBar', character.currentHp, character.maxHp);
        setBar('statusMpBar', character.currentMp, character.maxMp);
        setBar('statusExpBar', character.experience, character.experienceToNextLevel);
        byId('statusGold').textContent = character.gold;
        byId('statusSkillSlots').textContent = character.skillSlotCount;
        const rows = [
            ['ATK', character.baseAttack + ' + ' + character.attackBaseBonus],
            ['AP', character.magic],
            ['DEF', character.defense],
            ['MDEF', character.magicDefense],
            ['SPEED', character.speed],
            ['separator', ''],
            ['物理穿透', character.physicalPenetrationPercent + '% + ' + character.physicalPenetrationFlat],
            ['爆擊機率', character.criticalRate + '%'],
            ['物理吸血', character.physicalLifesteal + '%']
        ];
        byId('statusAttributeList').innerHTML = rows.map(row => row[0] === 'separator'
            ? '<hr>' : '<div><span>' + row[0] + '</span><strong>' + row[1] + '</strong></div>').join('');
        byId('statusTrait').innerHTML = '職業特性　'
            + escapeHtml(profession ? profession.traitDescription : '無').replace(/\n/g, '<br>');
    }

    function skillDetailHtml(skill) {
        if (!skill) return '請選擇技能。';
        return '<strong>【' + escapeHtml(skill.skillName) + '】　'
            + skillTypeName(skill.skillType) + '　｜　MP 消耗 ' + skill.mpCost + '</strong><br>'
            + (skill.skillType === 'SUPPORT' ? '' : '技能效果：基礎威力 ' + skill.basePower + '＋'
            + skill.scalingPercent + '% ' + (skill.skillType === 'PHYSICAL' ? 'ATK' : 'AP') + '<br>')
            + '解鎖等級：Lv' + skill.requiredLevel + '<br>說明：' + escapeHtml(skill.description);
    }

    function renderSkillConfiguration() {
        const config = state.skillConfiguration;
        if (!config) return;
        const equippedCodes = new Set(config.equippedSkills.map(skill => skill.skillCode));
        byId('skillConfigTitle').textContent = state.selectedCharacter.characterName + '的技能配置';
        byId('equippedSkillTitle').textContent = '已攜帶技能　' + config.equippedSkills.length
            + ' / ' + config.slotCount;
        byId('learnedSkillList').innerHTML = config.learnedSkills.map(skill =>
            '<button class="config-skill-entry'
            + (skill.skillCode === state.selectedConfigSkillCode ? ' selected' : '')
            + '" data-code="' + escapeHtml(skill.skillCode) + '" type="button">'
            + skillIcon(skill) + '<span><strong>' + escapeHtml(skill.skillName) + '</strong>'
            + '<small>MP ' + skill.mpCost + (equippedCodes.has(skill.skillCode) ? '　已攜帶' : '')
            + '</small></span></button>'
        ).join('');
        byId('equippedSkillList').innerHTML = config.equippedSkills.map((skill, index) =>
            '<div class="config-equipped-row"><button class="config-skill-entry'
            + (skill.skillCode === state.selectedConfigSkillCode ? ' selected' : '')
            + '" data-code="' + escapeHtml(skill.skillCode) + '" type="button">'
            + skillIcon(skill) + '<span><strong>' + (index + 1) + '. '
            + escapeHtml(skill.skillName) + '</strong><small>MP ' + skill.mpCost + '</small></span></button></div>'
        ).join('');
        const selected = config.learnedSkills.find(skill => skill.skillCode === state.selectedConfigSkillCode);
        byId('configSkillDetail').innerHTML = skillDetailHtml(selected);
    }

    function selectConfigurationSkill(code) {
        const config = state.skillConfiguration;
        if (!config || !code) return;
        state.selectedConfigSkillCode = code;
        document.querySelectorAll('#learnedSkillList .config-skill-entry, #equippedSkillList .config-skill-entry')
            .forEach(button => button.classList.toggle('selected', button.dataset.code === code));
        const selected = config.learnedSkills.find(skill => skill.skillCode === code);
        byId('configSkillDetail').innerHTML = skillDetailHtml(selected);
    }

    async function openSkillConfiguration() {
        const character = state.selectedCharacter;
        if (!character) return;
        state.skillConfiguration = await request(RPG_API + '/characters/'
            + character.characterId + '/skill-configuration');
        state.selectedConfigSkillCode = state.skillConfiguration.learnedSkills.length
            ? state.skillConfiguration.learnedSkills[0].skillCode : null;
        renderSkillConfiguration();
        byId('skillConfigModal').hidden = false;
    }

    async function persistSkillConfiguration(codes) {
        const character = state.selectedCharacter;
        state.skillConfiguration = await request(RPG_API + '/characters/'
            + character.characterId + '/skill-configuration', {
                method: 'PUT',
                body: JSON.stringify({ skillCodes: codes })
            });
        state.skills = state.skillConfiguration.equippedSkills;
        renderSkillConfiguration();
        if (state.selectedStage) renderPreparationSkills();
    }

    async function equipSelectedSkill() {
        const config = state.skillConfiguration;
        const code = state.selectedConfigSkillCode;
        if (!config || !code) return;
        if (config.equippedSkills.some(skill => skill.skillCode === code)) {
            showMessage('這個技能已經攜帶。', 'error');
            return;
        }
        if (config.equippedSkills.length >= config.slotCount) {
            showMessage('技能欄位已滿。', 'error');
            return;
        }
        const learnedOrder = config.learnedSkills.map(skill => skill.skillCode);
        const codes = config.equippedSkills.map(skill => skill.skillCode).concat(code)
            .sort((left, right) => learnedOrder.indexOf(left) - learnedOrder.indexOf(right));
        await persistSkillConfiguration(codes);
        showMessage('技能配置已保存。', 'success');
    }

    async function unequipSkill(code) {
        const config = state.skillConfiguration;
        if (!config) return;
        if (config.equippedSkills.length <= 1) {
            showMessage('至少需要攜帶一個技能。', 'error');
            return;
        }
        await persistSkillConfiguration(config.equippedSkills
            .map(skill => skill.skillCode).filter(skillCode => skillCode !== code));
        showMessage('技能配置已保存。', 'success');
    }

    function selectCharacter(characterId) {
        state.selectedCharacter = state.characters.find(item => String(item.characterId) === String(characterId)) || null;
        renderCharacters();
    }

    async function loadCharacters() {
        state.characters = await request(RPG_API + '/characters');
        renderCharacters();
    }

    async function enterWorld() {
        const character = state.selectedCharacter;
        if (!character) return;
        const result = await Promise.all([
            request(RPG_API + '/characters/' + character.characterId + '/stages'),
            request(RPG_API + '/characters/' + character.characterId + '/skills')
        ]);
        state.stages = result[0];
        state.skills = result[1];
        state.selectedRegionCode = state.stages.find(stage => stage.unlocked)?.regionCode || 'A001';
        state.selectedStage = null;
        renderMapCharacter();
        renderStages();
        showMessage('');
        showScreen('adventureScreen');
    }

    function renderMapCharacter() {
        const character = state.selectedCharacter;
        setProfessionSprite(byId('mapPortrait'), character.professionCode);
        byId('mapCharacterName').textContent = character.characterName;
        byId('mapCharacterClass').textContent = character.professionName + '　Lv' + character.level;
        byId('mapHp').textContent = character.currentHp + ' / ' + character.maxHp;
        byId('mapMp').textContent = character.currentMp + ' / ' + character.maxMp;
        byId('mapExp').textContent = character.experience + ' / ' + character.experienceToNextLevel;
        setBar('mapHpBar', character.currentHp, character.maxHp);
        setBar('mapMpBar', character.currentMp, character.maxMp);
        setBar('mapExpBar', character.experience, character.experienceToNextLevel);
        byId('mapStats').textContent = 'ATK ' + character.attack + '　AP ' + character.magic
            + '　DEF ' + character.defense + '　SPEED ' + character.speed;
    }

    function renderStages() {
        const regionStages = state.stages.filter(stage => stage.regionCode === state.selectedRegionCode);
        const region = regionStages[0];
        document.querySelectorAll('.map-hotspot').forEach(button => {
            const hasUnlockedStage = state.stages.some(stage => stage.regionCode === button.dataset.region
                && stage.unlocked);
            button.classList.toggle('selected', button.dataset.region === state.selectedRegionCode);
            button.classList.toggle('locked', !hasUnlockedStage);
            button.disabled = !hasUnlockedStage;
        });
        if (region) {
            byId('regionTitle').textContent = region.regionName;
            byId('regionDescription').textContent = region.regionDescription
                + '\n建議等級：Lv' + region.regionMinLevel + '～' + region.regionMaxLevel;
        }
        byId('stageGrid').innerHTML = regionStages.map(stage =>
            '<button class="stage-entry" data-code="' + escapeHtml(stage.stageCode) + '" type="button" '
            + (stage.unlocked ? '' : 'disabled') + '><strong>' + escapeHtml(stage.stageName)
            + '　' + (stage.clearCount ? '已通關' : stage.unlocked ? '可挑戰' : '未解鎖')
            + '</strong><small>建議 Lv' + stage.recommendedLevel
            + '　｜　' + escapeHtml(stage.monsterName) + '</small></button>'
        ).join('');
    }

    function renderPreparationSkills() {
        byId('stageSkillList').innerHTML = state.skills.map((skill, index) =>
            '<div class="preparation-skill">' + skillIcon(skill)
            + '<span>' + (index + 1) + '. <strong>' + escapeHtml(skill.skillName)
            + '</strong><br><small>MP ' + skill.mpCost + '　'
            + skillTypeName(skill.skillType) + '</small></span></div>'
        ).join('');
    }

    function openStage(stageCode) {
        const stage = state.stages.find(item => item.stageCode === stageCode);
        if (!stage || !stage.unlocked) return;
        state.selectedStage = stage;
        const character = state.selectedCharacter;

        byId('stageTitle').textContent = stage.stageName;
        byId('stageSubtitle').textContent = stage.regionName + '　｜　建議等級 Lv' + stage.recommendedLevel;
        setProfessionSprite(byId('stageCharacterPortrait'), character.professionCode);
        byId('stageCharacterName').textContent = character.characterName;
        byId('stageCharacterClass').textContent = character.professionName + '　Lv' + character.level;
        byId('stageCharacterStats').textContent = 'HP ' + character.currentHp + '/' + character.maxHp
            + '　MP ' + character.currentMp + '/' + character.maxMp
            + '　ATK ' + character.attack + '　AP ' + character.magic
            + '　DEF ' + character.defense + '　MDEF ' + character.magicDefense;
        byId('stageDescription').textContent = stage.description;
        setMonsterSprite(byId('stageMonsterPortrait'), stage.monsterCode);
        byId('stageMonsterName').textContent = stage.monsterName;
        byId('stageMonsterDescription').textContent = stage.monsterDescription;
        byId('stageReward').innerHTML = '勝利獎勵：EXP ' + stage.rewardExp + '　金幣 ' + stage.rewardGold
            + '<br>可能掉落：' + ((stage.possibleDrops || []).map(drop => escapeHtml(drop.name)
                + ' ' + drop.dropRate + '%').join('、') || '無');
        renderPreparationSkills();
        showScreen('stageScreen');
    }

    async function startBattle() {
        if (!state.selectedStage) return;
        const button = byId('confirmBattleBtn');
        setBusy(button, true, '進入戰鬥……');
        try {
            state.battle = await request(RPG_API + '/battles', {
                method: 'POST',
                body: JSON.stringify({
                    characterId: state.selectedCharacter.characterId,
                    stageCode: state.selectedStage.stageCode
                })
            });
            state.selectedSkillCode = state.battle.skills.length ? state.battle.skills[0].skillCode : null;
            renderBattle();
            showMessage('');
            showScreen('battleScreen');
        } catch (error) {
            showMessage(error.message, 'error');
        } finally {
            setBusy(button, false, '');
        }
    }

    function renderBattle() {
        const battle = state.battle;
        const character = state.selectedCharacter;
        const stage = state.selectedStage;
        const finished = battle.status !== 'ACTIVE';

        byId('battleMeta').textContent = '區域：' + stage.regionName + '・' + stage.stageName
            + '　｜　Lv' + character.level + '　｜　行動回合：' + battle.turnNumber;
        byId('turnBadge').textContent = String(battle.turnNumber);
        setProfessionSprite(byId('playerPortrait'), battle.player.code);
        byId('playerName').textContent = battle.player.name + '　等級 ' + character.level;
        byId('playerStats').textContent = 'ATK ' + character.attack + '　AP ' + character.magic
            + '　DEF ' + character.defense + '　MDEF ' + character.magicDefense
            + '　SPEED ' + character.speed;
        byId('playerHpText').textContent = battle.player.currentHp + ' / ' + battle.player.maxHp;
        byId('playerMpText').textContent = battle.player.currentMp + ' / ' + battle.player.maxMp;
        byId('playerShieldText').textContent = battle.player.shield > 0 ? '護盾 ' + battle.player.shield : '';
        setBar('playerHpBar', battle.player.currentHp, battle.player.maxHp);
        setBar('playerMpBar', battle.player.currentMp, battle.player.maxMp);
        byId('playerActionText').textContent = battle.player.actionPercent + '%';
        setBar('playerActionBar', battle.player.actionPercent, 100);

        setMonsterSprite(byId('monsterPortrait'), battle.monster.code);
        byId('monsterName').textContent = battle.monster.name + '　等級 ' + battle.monsterLevel;
        byId('monsterHpText').textContent = battle.monster.currentHp + ' / ' + battle.monster.maxHp;
        byId('monsterMpText').textContent = battle.monster.currentMp + ' / ' + battle.monster.maxMp;
        byId('monsterShieldText').textContent = battle.monster.shield > 0 ? '護盾 ' + battle.monster.shield : '';
        setBar('monsterHpBar', battle.monster.currentHp, battle.monster.maxHp);
        setBar('monsterMpBar', battle.monster.currentMp, battle.monster.maxMp);
        byId('monsterActionText').textContent = battle.monster.actionPercent + '%';
        setBar('monsterActionBar', battle.monster.actionPercent, 100);

        byId('skillGrid').innerHTML = battle.skills.map((skill, index) =>
            '<button class="skill-button' + (skill.skillCode === state.selectedSkillCode ? ' selected' : '')
            + '" data-code="' + escapeHtml(skill.skillCode) + '" data-available="' + skill.available
            + '" type="button">' + skillIcon(skill)
            + '<span><strong>' + (index + 1) + '. ' + escapeHtml(skill.skillName)
            + '</strong><span>MP ' + skill.mpCost + '</span></span></button>'
        ).join('');

        const selected = battle.skills.find(skill => skill.skillCode === state.selectedSkillCode);
        renderSkillDetail(selected);
        byId('useSelectedSkillBtn').textContent = byId('useSelectedSkillBtn').dataset.label
            || '使用選擇的技能';
        byId('useSelectedSkillBtn').disabled = finished || !selected || !selected.available;
        byId('battleLog').innerHTML = battle.logs.map(line => '<li>' + escapeHtml(line) + '</li>').join('');
        byId('battleLog').scrollTop = byId('battleLog').scrollHeight;
        byId('returnToMapBtn').hidden = !finished;
        byId('repeatStageBtn').hidden = !finished;
        byId('abandonBattleBtn').hidden = finished;
        byId('battleStatusTitle').hidden = !finished;

        if (battle.status === 'VICTORY') {
            byId('battleStatusTitle').textContent = '戰鬥勝利';
            showMessage('獲得 ' + battle.rewardExp + ' 經驗與 ' + battle.rewardGold + ' 金幣。'
                + (battle.levelUp ? '角色升級了！' : '')
                + ((battle.drops || []).length ? '　掉落：' + battle.drops.map(drop =>
                    (drop.itemName || drop.equipmentName) + (drop.quantity > 1 ? ' × ' + drop.quantity : '')).join('、') : ''), 'success');
        } else if (battle.status === 'DEFEAT') {
            byId('battleStatusTitle').textContent = '戰鬥失敗';
            showMessage('休整後可再次挑戰。', 'error');
        }
    }

    function renderSkillDetail(skill) {
        if (!skill) {
            byId('skillDetail').textContent = '請從左側選擇技能。';
            return;
        }
        byId('skillDetail').innerHTML = skillDetailHtml(skill)
            + (skill.available ? '' : '<br><strong>目前魔力不足。</strong>');
    }

    function itemTypeName(type) {
        return { CONSUMABLE: '消耗品', MATERIAL: '素材', QUEST: '任務道具',
            SKILL_SCROLL: '技能卷軸' }[type] || type;
    }

    async function openInventory() {
        if (!state.selectedCharacter) return;
        state.inventory = await request(RPG_API + '/characters/'
            + encodeURIComponent(state.selectedCharacter.characterId) + '/inventory');
        state.inventoryFilter = 'CONSUMABLE';
        state.selectedItemCode = null;
        renderInventoryCharacterSummary();
        document.querySelectorAll('.inventory-tab').forEach(tab => {
            tab.classList.toggle('selected', tab.dataset.type === 'CONSUMABLE');
        });
        renderInventory();
        byId('inventoryModal').hidden = false;
    }

    function renderInventoryCharacterSummary() {
        const character = state.selectedCharacter;
        if (!character) return;
        byId('inventoryCharacterName').textContent = state.selectedCharacter.characterName
            + '　Lv' + state.selectedCharacter.level;
        byId('inventoryHp').textContent = state.selectedCharacter.currentHp + ' / ' + state.selectedCharacter.maxHp;
        byId('inventoryMp').textContent = state.selectedCharacter.currentMp + ' / ' + state.selectedCharacter.maxMp;
        setBar('inventoryHpBar', state.selectedCharacter.currentHp, state.selectedCharacter.maxHp);
        setBar('inventoryMpBar', state.selectedCharacter.currentMp, state.selectedCharacter.maxMp);
    }

    function renderInventory() {
        const items = state.inventory.filter(item => item.itemType === state.inventoryFilter);
        if (!items.some(item => item.itemCode === state.selectedItemCode)) {
            state.selectedItemCode = items.length ? items[0].itemCode : null;
        }
        byId('inventoryList').innerHTML = items.length ? items.map(item =>
            '<button class="inventory-entry' + (item.itemCode === state.selectedItemCode ? ' selected' : '')
            + '" data-code="' + escapeHtml(item.itemCode) + '" type="button">'
            + '<span class="item-icon" style="' + itemIconStyle(item.itemCode) + '"></span>'
            + '<span><strong>' + escapeHtml(item.itemName) + '</strong><small>'
            + itemTypeName(item.itemType) + '　× ' + item.quantity + '</small></span></button>'
        ).join('') : '<p class="inventory-empty">這個分類目前沒有道具。</p>';
        const selected = state.inventory.find(item => item.itemCode === state.selectedItemCode);
        if (!selected) {
            byId('inventoryDetail').textContent = '請選擇道具。';
            byId('useInventoryItemBtn').disabled = true;
            return;
        }
        const effects = selected.effects.map(effect => {
            const target = effect.effectType === 'RECOVER_HP' ? 'HP' : 'MP';
            return '恢復 ' + target + '：' + effect.flatValue
                + (effect.percentValue ? ' + 最大值的 ' + effect.percentValue + '%' : '');
        });
        byId('inventoryDetail').innerHTML = '<strong>【' + escapeHtml(selected.itemName) + '】</strong><br>'
            + '種類：' + escapeHtml(itemTypeName(selected.itemType)) + '<br>'
            + '持有數量：' + selected.quantity + ' / ' + selected.maxStack + '<br><br>'
            + escapeHtml(selected.description)
            + (effects.length ? '<br><br>' + effects.map(escapeHtml).join('<br>') : '');
        const inBattle = state.battle && state.battle.status === 'ACTIVE' && !byId('battleScreen').hidden;
        byId('useInventoryItemBtn').disabled = inBattle
            ? selected.itemType !== 'CONSUMABLE'
            : !['CONSUMABLE', 'SKILL_SCROLL'].includes(selected.itemType);
    }

    async function useInventoryItem() {
        if (!state.selectedCharacter || !state.selectedItemCode) return;
        const button = byId('useInventoryItemBtn');
        setBusy(button, true, '使用中……');
        try {
            if (state.battle && state.battle.status === 'ACTIVE' && !byId('battleScreen').hidden) {
                state.battle = await request(RPG_API + '/battles/' + encodeURIComponent(state.battle.battleId)
                    + '/items', { method: 'POST', body: JSON.stringify({ itemCode: state.selectedItemCode }) });
                byId('inventoryModal').hidden = true;
                renderBattle();
                showMessage('道具已使用，並消耗本回合行動。', 'success');
                return;
            }
            const result = await request(RPG_API + '/characters/'
                + encodeURIComponent(state.selectedCharacter.characterId) + '/items/use', {
                method: 'POST', body: JSON.stringify({ itemCode: state.selectedItemCode })
            });
            const characterId = state.selectedCharacter.characterId;
            await loadCharacters();
            selectCharacter(characterId);
            state.inventory = await request(RPG_API + '/characters/' + encodeURIComponent(characterId) + '/inventory');
            renderInventoryCharacterSummary();
            renderInventory();
            showMessage(result.message, 'success');
            if (!byId('adventureScreen').hidden) {
                const character = state.selectedCharacter;
                byId('mapHp').textContent = character.currentHp + ' / ' + character.maxHp;
                byId('mapMp').textContent = character.currentMp + ' / ' + character.maxMp;
                setBar('mapHpBar', character.currentHp, character.maxHp);
                setBar('mapMpBar', character.currentMp, character.maxMp);
            }
        } catch (error) {
            showMessage(error.message, 'error');
        } finally {
            button.textContent = button.dataset.label || '使用選擇的道具';
            renderInventory();
        }
    }

    const equipmentSlots = [
        ['WEAPON', '武器'], ['OFF_HAND', '副手'], ['ARMOR', '護甲'], ['SHOES', '鞋子'],
        ['ACCESSORY_1', '飾品一'], ['ACCESSORY_2', '飾品二']
    ];

    async function openEquipment() {
        if (!state.selectedCharacter) return;
        state.equipment = await request(RPG_API + '/characters/'
            + encodeURIComponent(state.selectedCharacter.characterId) + '/equipment');
        state.selectedEquipmentId = state.equipment.length ? state.equipment[0].ownedEquipmentId : null;
        byId('equipmentCharacterName').textContent = state.selectedCharacter.characterName;
        byId('equipmentModal').hidden = false;
        renderEquipment();
    }

    function renderEquipment() {
        const filter = byId('equipmentProfessionFilter').checked;
        byId('equippedSlots').innerHTML = equipmentSlots.map(slot => {
            const item = state.equipment.find(value => value.equippedSlot === slot[0]);
            return '<button class="equipment-slot' + (item && item.ownedEquipmentId === state.selectedEquipmentId ? ' selected' : '')
                + '" data-id="' + (item ? item.ownedEquipmentId : '') + '" type="button">'
                + (item ? '<span class="equipment-icon" style="' + equipmentIconStyle(item.equipmentCode) + '"></span>' : '')
                + '<span><strong>' + slot[1] + '</strong><small>' + (item ? escapeHtml(item.equipmentName) : '未穿戴')
                + '</small></span></button>';
        }).join('');
        const inventory = state.equipment.filter(item => !item.equippedSlot && (!filter || item.professionAllowed));
        byId('equipmentInventory').innerHTML = inventory.length ? inventory.map(item =>
            '<button class="equipment-entry' + (item.ownedEquipmentId === state.selectedEquipmentId ? ' selected' : '')
            + '" data-id="' + item.ownedEquipmentId + '" type="button"><span class="equipment-icon" style="'
            + equipmentIconStyle(item.equipmentCode) + '"></span><small>' + escapeHtml(item.equipmentName)
            + '</small></button>').join('') : '<p class="inventory-empty">目前沒有符合條件的未穿戴裝備。</p>';
        const selected = state.equipment.find(item => item.ownedEquipmentId === state.selectedEquipmentId);
        if (!selected) {
            byId('equipmentDetail').textContent = '請選擇裝備。';
            byId('equipmentSlotButtons').innerHTML = '';
            return;
        }
        const stats = selected.stats.length ? selected.stats.map(stat => stat.statType + ' '
            + (stat.modifierType === 'MULTIPLIER' ? '+' + stat.modifierValue + '%' : '+' + stat.modifierValue)).join('、') : '無';
        byId('equipmentDetail').innerHTML = '<strong>【' + escapeHtml(selected.equipmentName) + '】</strong><br>裝備部位：'
            + selected.equipmentType + '<br>需求等級：Lv' + selected.requiredLevel + '<br>可用職業：'
            + (selected.usageType === 'UNIVERSAL' ? '全職業' : selected.professions.join('、'))
            + '<br>屬性加成：' + escapeHtml(stats) + '<br><br>' + escapeHtml(selected.description);
        if (selected.equippedSlot) {
            byId('equipmentSlotButtons').innerHTML = '<button class="medieval-button danger" data-unequip="'
                + selected.ownedEquipmentId + '" type="button">卸下裝備</button>';
        } else {
            const slots = selected.equipmentType === 'ACCESSORY' ? equipmentSlots.slice(4) : equipmentSlots.filter(s => s[0] === selected.equipmentType);
            byId('equipmentSlotButtons').innerHTML = slots.map(slot => '<button class="medieval-button" data-equip="'
                + selected.ownedEquipmentId + '" data-slot="' + slot[0] + '" type="button">穿戴至' + slot[1] + '</button>').join('');
        }
    }

    async function changeEquipment(button) {
        const characterId = state.selectedCharacter.characterId;
        const unequip = button.dataset.unequip;
        const result = unequip
            ? await request(RPG_API + '/characters/' + characterId + '/equipment/' + unequip + '/unequip', { method: 'POST' })
            : await request(RPG_API + '/characters/' + characterId + '/equipment', { method: 'PUT',
                body: JSON.stringify({ ownedEquipmentId: Number(button.dataset.equip), slot: button.dataset.slot }) });
        state.equipment = result.equipment;
        state.selectedCharacter = result.character;
        state.characters = state.characters.map(item => item.characterId === result.character.characterId ? result.character : item);
        showMessage(result.message, 'success');
        renderCharacters(); renderEquipment();
    }

    async function openShop() {
        if (!state.selectedCharacter) return;
        state.shop = await request(RPG_API + '/characters/' + state.selectedCharacter.characterId + '/shop');
        state.shopTab = 'equipment';
        byId('shopModal').hidden = false;
        renderShop();
    }

    function renderShop() {
        const products = state.shop[state.shopTab] || [];
        if (!products.some(item => item.id === state.selectedShopProductId)) state.selectedShopProductId = products.length ? products[0].id : null;
        byId('shopTitle').textContent = state.shop.shopName;
        byId('shopGold').textContent = '持有金幣：' + state.shop.character.gold;
        document.querySelectorAll('.shop-tab').forEach(tab => tab.classList.toggle('selected', tab.dataset.shopTab === state.shopTab));
        byId('shopProductList').innerHTML = products.length ? products.map(product =>
            '<button class="shop-product' + (product.id === state.selectedShopProductId ? ' selected' : '')
            + '" data-id="' + product.id + '" type="button">' + (product.equipment
                ? '<span class="equipment-icon" style="' + equipmentIconStyle(product.productCode) + '"></span>'
                : '<span class="item-icon" style="' + itemIconStyle(product.productCode) + '"></span>')
            + '<span><strong>' + escapeHtml(product.productName) + '</strong><small>'
            + (state.shopTab.startsWith('sellable') ? '售價 ' : state.shopTab === 'buybacks' ? '買回 ' : '')
            + product.price + ' 金幣' + (product.quantity > 1 ? '　× ' + product.quantity : '') + '</small></span></button>').join('')
            : '<p class="inventory-empty">目前沒有這類商品。</p>';
        const selected = products.find(item => item.id === state.selectedShopProductId);
        const actionNames = { equipment: ['BUY_EQUIPMENT', '購買選擇裝備'], items: ['BUY_ITEM', '購買選擇道具'],
            sellableItems: ['SELL_ITEM', '出售一個'], sellableEquipment: ['SELL_EQUIPMENT', '出售選擇裝備'], buybacks: ['BUYBACK', '買回選擇物品'] };
        byId('shopActionBtn').disabled = !selected;
        byId('shopActionBtn').textContent = actionNames[state.shopTab][1];
        byId('shopActionBtn').dataset.action = actionNames[state.shopTab][0];
        if (!selected) { byId('shopProductDetail').textContent = '目前沒有這類商品。'; return; }
        const materials = selected.materials.length ? '<br>所需素材：' + selected.materials.map(m => escapeHtml(m.itemName)
            + ' ' + m.ownedQuantity + ' / ' + m.requiredQuantity).join('、') : '';
        const limit = selected.purchaseLimit ? '<br>限定購買：' + selected.purchasedQuantity + ' / ' + selected.purchaseLimit : '';
        byId('shopProductDetail').innerHTML = '<strong>【' + escapeHtml(selected.productName) + '】</strong><br>價格：'
            + selected.price + ' 金幣' + materials + limit + '<br><br>' + escapeHtml(selected.description);
    }

    async function performShopAction() {
        const result = await request(RPG_API + '/characters/' + state.selectedCharacter.characterId + '/shop/actions', {
            method: 'POST', body: JSON.stringify({ action: byId('shopActionBtn').dataset.action,
                productId: state.selectedShopProductId })
        });
        state.shop = result.shop; state.selectedCharacter = result.shop.character;
        state.characters = state.characters.map(item => item.characterId === state.selectedCharacter.characterId ? state.selectedCharacter : item);
        showMessage(result.message, 'success'); renderCharacters(); renderShop();
    }

    function skillTypeName(type) {
        return { PHYSICAL: '物理攻擊', MAGICAL: '魔法攻擊', HEAL: '治療', SUPPORT: '輔助' }[type] || type;
    }

    function selectSkill(skillCode) {
        state.selectedSkillCode = skillCode;
        renderBattle();
    }

    async function useSelectedSkill() {
        const skill = state.battle.skills.find(item => item.skillCode === state.selectedSkillCode);
        if (!skill || !skill.available || state.battle.status !== 'ACTIVE') return;
        const button = byId('useSelectedSkillBtn');
        setBusy(button, true, '行動中……');
        try {
            state.battle = await request(RPG_API + '/battles/'
                + encodeURIComponent(state.battle.battleId) + '/actions', {
                method: 'POST',
                body: JSON.stringify({ skillCode: skill.skillCode })
            });
            renderBattle();
        } catch (error) {
            showMessage(error.message, 'error');
            renderBattle();
        } finally {
            if (state.battle) {
                const selected = state.battle.skills.find(skill => skill.skillCode === state.selectedSkillCode);
                button.textContent = button.dataset.label || '使用選擇的技能';
                button.disabled = state.battle.status !== 'ACTIVE' || !selected || !selected.available;
            }
        }
    }

    async function returnToMap() {
        const characterId = state.selectedCharacter.characterId;
        await loadCharacters();
        selectCharacter(characterId);
        await enterWorld();
        state.battle = null;
        state.selectedSkillCode = null;
        showMessage('');
    }

    byId('characterGrid').addEventListener('click', event => {
        const button = event.target.closest('.character-entry');
        if (button) selectCharacter(button.dataset.id);
    });
    byId('professionGrid').addEventListener('click', event => {
        const button = event.target.closest('.profession-card');
        if (!button) return;
        state.selectedProfessionCode = button.dataset.code;
        state.selectedProfessionSkillCode = null;
        renderProfessions();
    });
    byId('professionSkillList').addEventListener('click', event => {
        const button = event.target.closest('.create-skill-entry');
        if (!button) return;
        state.selectedProfessionSkillCode = button.dataset.code;
        renderProfessionDetail();
    });
    byId('openCreateBtn').addEventListener('click', () => {
        state.selectedProfessionCode = state.professions.length ? state.professions[0].code : null;
        state.selectedProfessionSkillCode = null;
        byId('characterName').value = '';
        renderProfessions();
        byId('createCharacterScreen').hidden = false;
    });
    byId('cancelCreateBtn').addEventListener('click', () => {
        showMessage('');
        byId('createCharacterScreen').hidden = true;
    });
    byId('createCharacterForm').addEventListener('submit', async event => {
        event.preventDefault();
        const button = byId('createCharacterBtn');
        setBusy(button, true, '建立中……');
        try {
            await request(RPG_API + '/characters', {
                method: 'POST',
                body: JSON.stringify({
                    characterName: byId('characterName').value.trim(),
                    professionCode: byId('professionCode').value
                })
            });
            event.target.reset();
            state.selectedCharacter = null;
            await loadCharacters();
            showMessage('角色已加入冒險者名冊。', 'success');
            byId('createCharacterScreen').hidden = true;
        } catch (error) {
            showMessage(error.message, 'error');
        } finally {
            setBusy(button, false, '');
        }
    });

    byId('enterWorldBtn').addEventListener('click', () => {
        enterWorld().catch(error => showMessage(error.message, 'error'));
    });
    function openCharacterInfo() {
        renderCharacterStatus();
        showMessage('');
        byId('characterInfoScreen').hidden = false;
    }
    byId('openCharacterInfoBtn').addEventListener('click', openCharacterInfo);
    byId('mapCharacterInfoBtn').addEventListener('click', openCharacterInfo);
    byId('closeCharacterInfoBtn').addEventListener('click', () => {
        byId('characterInfoScreen').hidden = true;
    });
    ['mapSkillConfigBtn', 'stageSkillConfigBtn'].forEach(id => {
        byId(id).addEventListener('click', () => {
            openSkillConfiguration().catch(error => showMessage(error.message, 'error'));
        });
    });
    ['mapInventoryBtn'].forEach(id => {
        byId(id).addEventListener('click', () => {
            openInventory().catch(error => showMessage(error.message, 'error'));
        });
    });
    ['mapEquipmentBtn'].forEach(id => byId(id).addEventListener('click', () =>
        openEquipment().catch(error => showMessage(error.message, 'error'))));
    ['mapShopBtn'].forEach(id => byId(id).addEventListener('click', () =>
        openShop().catch(error => showMessage(error.message, 'error'))));
    byId('closeEquipmentBtn').addEventListener('click', () => { byId('equipmentModal').hidden = true; showMessage(''); });
    byId('equipmentProfessionFilter').addEventListener('change', renderEquipment);
    ['equippedSlots', 'equipmentInventory'].forEach(id => byId(id).addEventListener('click', event => {
        const button = event.target.closest('[data-id]');
        if (!button || !button.dataset.id) return;
        state.selectedEquipmentId = Number(button.dataset.id); renderEquipment();
    }));
    byId('equipmentSlotButtons').addEventListener('click', event => {
        const button = event.target.closest('[data-equip],[data-unequip]');
        if (button) changeEquipment(button).catch(error => showMessage(error.message, 'error'));
    });
    byId('closeShopBtn').addEventListener('click', () => { byId('shopModal').hidden = true; showMessage(''); });
    document.querySelectorAll('.shop-tab').forEach(tab => tab.addEventListener('click', () => {
        state.shopTab = tab.dataset.shopTab; state.selectedShopProductId = null; renderShop();
    }));
    byId('shopProductList').addEventListener('click', event => {
        const button = event.target.closest('[data-id]');
        if (!button) return; state.selectedShopProductId = Number(button.dataset.id); renderShop();
    });
    byId('shopActionBtn').addEventListener('click', () => performShopAction()
        .catch(error => showMessage(error.message, 'error')));
    byId('battleItemBtn').addEventListener('click', () => openInventory()
        .catch(error => showMessage(error.message, 'error')));
    byId('inventoryList').addEventListener('click', event => {
        const button = event.target.closest('.inventory-entry');
        if (!button) return;
        state.selectedItemCode = button.dataset.code;
        renderInventory();
    });
    document.querySelectorAll('.inventory-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            state.inventoryFilter = tab.dataset.type;
            document.querySelectorAll('.inventory-tab').forEach(item =>
                item.classList.toggle('selected', item === tab));
            renderInventory();
        });
    });
    byId('useInventoryItemBtn').addEventListener('click', useInventoryItem);
    byId('closeInventoryBtn').addEventListener('click', () => {
        byId('inventoryModal').hidden = true;
        showMessage('');
    });
    byId('reloadInventoryBtn').addEventListener('click', async () => {
        try {
            state.inventory = await request(RPG_API + '/characters/'
                + encodeURIComponent(state.selectedCharacter.characterId) + '/inventory');
            renderInventory();
            showMessage('背包資料已重新讀取。', 'success');
        } catch (error) {
            showMessage(error.message, 'error');
        }
    });
    byId('learnedSkillList').addEventListener('click', event => {
        const button = event.target.closest('.config-skill-entry');
        if (!button) return;
        selectConfigurationSkill(button.dataset.code);
    });
    byId('learnedSkillList').addEventListener('dblclick', event => {
        const button = event.target.closest('.config-skill-entry');
        if (!button) return;
        selectConfigurationSkill(button.dataset.code);
        equipSelectedSkill().catch(error => showMessage(error.message, 'error'));
    });
    byId('equippedSkillList').addEventListener('click', event => {
        const button = event.target.closest('.config-skill-entry');
        if (button) selectConfigurationSkill(button.dataset.code);
    });
    byId('equippedSkillList').addEventListener('contextmenu', event => {
        const button = event.target.closest('.config-skill-entry');
        if (!button) return;
        event.preventDefault();
        unequipSkill(button.dataset.code).catch(error => showMessage(error.message, 'error'));
    });
    byId('closeSkillConfigBtn').addEventListener('click', () => {
        byId('skillConfigModal').hidden = true;
        showMessage('');
    });
    byId('changeCharacterBtn').addEventListener('click', () => {
        showMessage('');
        showScreen('characterScreen');
    });
    byId('stageGrid').addEventListener('click', event => {
        const button = event.target.closest('.stage-entry');
        if (button && !button.disabled) openStage(button.dataset.code);
    });
    document.querySelector('.world-map').addEventListener('click', event => {
        const button = event.target.closest('.map-hotspot');
        if (!button || button.disabled) return;
        state.selectedRegionCode = button.dataset.region;
        renderStages();
    });
    byId('backToMapBtn').addEventListener('click', () => showScreen('adventureScreen'));
    byId('confirmBattleBtn').addEventListener('click', startBattle);
    byId('skillGrid').addEventListener('click', event => {
        const button = event.target.closest('.skill-button');
        if (button) selectSkill(button.dataset.code);
    });
    byId('useSelectedSkillBtn').addEventListener('click', useSelectedSkill);
    byId('returnToMapBtn').addEventListener('click', () => {
        returnToMap().catch(error => showMessage(error.message, 'error'));
    });
    byId('repeatStageBtn').addEventListener('click', startBattle);
    byId('abandonBattleBtn').addEventListener('click', async () => {
        if (!window.confirm('確定要撤離這場戰鬥嗎？')) return;
        try {
            await request(RPG_API + '/battles/' + encodeURIComponent(state.battle.battleId)
                + '/abandon', { method: 'POST' });
            await returnToMap();
        } catch (error) {
            showMessage(error.message, 'error');
        }
    });
    byId('leaveGameBtn').addEventListener('click', () => {
        window.location.assign('../../Lobby/jquery_lobby.html');
    });

    async function initialize() {
        document.body.classList.toggle('embedded', window.self !== window.top);
        try {
            if (!token()) throw new Error('請先登入 GamePlatform，再進入維爾薩王國。');
            state.user = await request('/api/user/auth/me');
            byId('accountBadge').textContent = state.user.username + '｜' + state.user.account;
            byId('characterWelcome').textContent = '歡迎歸來，' + state.user.username
                + '。請選擇角色，或在名冊中建立新的冒險者。';
            byId('createWelcome').textContent = state.user.username + '，請為角色命名並選擇職業';
            const results = await Promise.all([
                request(RPG_API + '/professions'),
                request(RPG_API + '/characters')
            ]);
            state.professions = results[0];
            state.characters = results[1];
            renderProfessions();
            renderCharacters();
            showScreen('characterScreen');
        } catch (error) {
            byId('accountBadge').textContent = '尚未登入';
            showMessage(error.message, 'error');
            showScreen('characterScreen');
        }
    }

    initialize();
})();
