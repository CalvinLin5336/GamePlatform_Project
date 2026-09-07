(function () {
    'use strict';

    const API_BASE = window.location.port === '8080'
        ? window.location.origin
        : 'http://' + window.location.hostname + ':8080';
    const ADMIN_API = '/api/admin/game-management';

    let games = [];
    let selectedGameId = null;

    const byId = id => document.getElementById(id);
    const gameModal = byId('gameModal');
    const modeModal = byId('modeModal');

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
            } catch (_) { /* 使用預設訊息。 */ }
            const error = new Error(message);
            error.status = response.status;
            throw error;
        }
        if (response.status === 204) return null;
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    }

    function showMessage(element, text, type) {
        element.textContent = text || '';
        element.className = 'message' + (type ? ' ' + type : '');
        element.hidden = !text;
    }

    function setBusy(button, busy, busyText) {
        if (!button.dataset.originalText) button.dataset.originalText = button.textContent;
        button.disabled = busy;
        button.textContent = busy ? busyText : button.dataset.originalText;
    }

    function findGame(gameId) {
        return games.find(game => String(game.gameId) === String(gameId));
    }

    function updateStats() {
        const modes = games.flatMap(game => Array.isArray(game.modes) ? game.modes : []);
        byId('totalGames').textContent = games.length;
        byId('enabledGames').textContent = games.filter(game => game.enabled).length;
        byId('totalModes').textContent = modes.length;
        byId('enabledModes').textContent = modes.filter(mode => mode.enabled).length;
    }

    function pathLink(path) {
        if (!path) return '<span class="path-text">—</span>';
        const safe = escapeHtml(path);
        if (!path.startsWith('/')) return '<span class="path-text">' + safe + '</span>';
        return '<a class="path-link" href="' + safe + '" target="_blank" rel="noopener">' + safe + '</a>';
    }

    function escapeHtml(value) {
        const node = document.createElement('div');
        node.textContent = value == null ? '' : String(value);
        return node.innerHTML;
    }

    function renderGames() {
        const keyword = byId('gameSearch').value.trim().toLowerCase();
        const status = byId('gameStatusFilter').value;
        const filtered = games.filter(game => {
            const matchesText = !keyword
                || String(game.gameName || '').toLowerCase().includes(keyword)
                || String(game.gameCode || '').toLowerCase().includes(keyword);
            const matchesStatus = status === 'ALL'
                || (status === 'ENABLED' && game.enabled)
                || (status === 'DISABLED' && !game.enabled);
            return matchesText && matchesStatus;
        });

        if (!filtered.length) {
            byId('gamesTableBody').innerHTML = '<tr><td colspan="6" class="empty">找不到符合條件的遊戲。</td></tr>';
            return;
        }

        byId('gamesTableBody').innerHTML = filtered.map(game => {
            const modeCount = Array.isArray(game.modes) ? game.modes.length : 0;
            return '<tr>'
                + '<td>#' + escapeHtml(game.gameId) + '</td>'
                + '<td><span class="game-name">' + escapeHtml(game.gameName) + '</span><span class="game-code">' + escapeHtml(game.gameCode) + '</span></td>'
                + '<td><span class="badge ' + (game.enabled ? 'enabled' : 'disabled') + '">' + (game.enabled ? '已上架' : '已下架') + '</span></td>'
                + '<td>' + modeCount + '</td>'
                + '<td>' + pathLink(game.frontendPath) + '</td>'
                + '<td><div class="actions">'
                + '<button class="button compact secondary manage-modes" data-id="' + game.gameId + '" type="button">管理模式</button>'
                + '<button class="button compact secondary edit-game" data-id="' + game.gameId + '" type="button">編輯</button>'
                + '<button class="button compact warning toggle-game" data-id="' + game.gameId + '" type="button">' + (game.enabled ? '下架' : '上架') + '</button>'
                + '</div></td></tr>';
        }).join('');
    }

    function renderModes() {
        const game = findGame(selectedGameId);
        if (!game) {
            byId('modesPanel').hidden = true;
            return;
        }

        byId('modesPanel').hidden = false;
        byId('modesTitle').textContent = game.gameName + '－遊戲模式';
        byId('modesDescription').textContent = game.gameCode + '，模式代碼建立後會鎖定，避免破壞遊戲整合。';
        const modes = Array.isArray(game.modes) ? game.modes : [];
        byId('modesTableBody').innerHTML = modes.length ? modes.map(mode => {
            return '<tr>'
                + '<td>#' + escapeHtml(mode.modeId) + '</td>'
                + '<td><span class="game-name">' + escapeHtml(mode.modeName) + '</span><span class="game-code">' + escapeHtml(mode.modeCode) + '</span></td>'
                + '<td>' + escapeHtml(mode.minPlayers) + '－' + escapeHtml(mode.maxPlayers) + '</td>'
                + '<td>' + escapeHtml(mode.computerPlayers) + '</td>'
                + '<td><span class="badge ' + (mode.enabled ? 'enabled' : 'disabled') + '">' + (mode.enabled ? '已啟用' : '已停用') + '</span></td>'
                + '<td><div class="actions">'
                + '<button class="button compact secondary edit-mode" data-id="' + mode.modeId + '" type="button">編輯</button>'
                + '<button class="button compact warning toggle-mode" data-id="' + mode.modeId + '" type="button">' + (mode.enabled ? '停用' : '啟用') + '</button>'
                + '</div></td></tr>';
        }).join('') : '<tr><td colspan="6" class="empty">這款遊戲尚未建立模式。</td></tr>';
    }

    async function verifyAdmin() {
        if (!token()) throw new Error('請先使用管理員帳號登入。');
        const user = await request('/api/user/auth/me');
        if (!user || String(user.role).toUpperCase() !== 'ADMIN') {
            const error = new Error('目前帳號沒有遊戲管理權限。');
            error.status = 403;
            throw error;
        }
        return user;
    }

    async function loadGames() {
        showMessage(byId('gamesMessage'), '正在載入遊戲資料……');
        try {
            const data = await request(ADMIN_API + '/games');
            games = Array.isArray(data) ? data : [];
            updateStats();
            renderGames();
            renderModes();
            showMessage(byId('gamesMessage'), '');
        } catch (error) {
            showMessage(byId('gamesMessage'), error.message, 'error');
        }
    }

    function openGameModal(game) {
        byId('gameForm').reset();
        byId('gameId').value = game ? game.gameId : '';
        byId('gameModalTitle').textContent = game ? '編輯遊戲' : '新增遊戲';
        byId('gameCode').value = game ? game.gameCode : '';
        byId('gameCode').readOnly = Boolean(game);
        byId('gameName').value = game ? game.gameName : '';
        byId('gameDescription').value = game && game.description ? game.description : '';
        byId('frontendPath').value = game ? game.frontendPath : '';
        byId('backendPath').value = game ? game.backendPath : '';
        byId('imagePath').value = game && game.imagePath ? game.imagePath : '';
        byId('gameEnabled').checked = game ? game.enabled : false;
        showMessage(byId('gameFormMessage'), '');
        gameModal.hidden = false;
        byId('gameName').focus();
    }

    function closeGameModal() {
        gameModal.hidden = true;
    }

    function gamePayload(source, enabledOverride) {
        if (source) {
            return {
                gameCode: source.gameCode,
                gameName: source.gameName,
                description: source.description,
                frontendPath: source.frontendPath,
                backendPath: source.backendPath,
                imagePath: source.imagePath,
                enabled: enabledOverride == null ? source.enabled : enabledOverride
            };
        }
        return {
            gameCode: byId('gameCode').value.trim(),
            gameName: byId('gameName').value.trim(),
            description: byId('gameDescription').value.trim() || null,
            frontendPath: byId('frontendPath').value.trim(),
            backendPath: byId('backendPath').value.trim(),
            imagePath: byId('imagePath').value.trim() || null,
            enabled: byId('gameEnabled').checked
        };
    }

    function openModeModal(mode) {
        byId('modeForm').reset();
        byId('modeId').value = mode ? mode.modeId : '';
        byId('modeModalTitle').textContent = mode ? '編輯遊戲模式' : '新增遊戲模式';
        byId('modeCode').value = mode ? mode.modeCode : '';
        byId('modeCode').readOnly = Boolean(mode);
        byId('modeName').value = mode ? mode.modeName : '';
        byId('minPlayers').value = mode ? mode.minPlayers : 1;
        byId('maxPlayers').value = mode ? mode.maxPlayers : 1;
        byId('computerPlayers').value = mode ? mode.computerPlayers : 0;
        byId('modeEnabled').checked = mode ? mode.enabled : false;
        showMessage(byId('modeFormMessage'), '');
        modeModal.hidden = false;
        byId('modeName').focus();
    }

    function closeModeModal() {
        modeModal.hidden = true;
    }

    function modePayload(source, enabledOverride) {
        if (source) {
            return {
                modeCode: source.modeCode,
                modeName: source.modeName,
                minPlayers: source.minPlayers,
                maxPlayers: source.maxPlayers,
                computerPlayers: source.computerPlayers,
                enabled: enabledOverride == null ? source.enabled : enabledOverride
            };
        }
        return {
            modeCode: byId('modeCode').value.trim(),
            modeName: byId('modeName').value.trim(),
            minPlayers: Number(byId('minPlayers').value),
            maxPlayers: Number(byId('maxPlayers').value),
            computerPlayers: Number(byId('computerPlayers').value),
            enabled: byId('modeEnabled').checked
        };
    }

    byId('gameSearch').addEventListener('input', renderGames);
    byId('gameStatusFilter').addEventListener('change', renderGames);
    byId('refreshGamesBtn').addEventListener('click', loadGames);
    byId('createGameBtn').addEventListener('click', () => openGameModal(null));
    byId('createModeBtn').addEventListener('click', () => openModeModal(null));

    document.querySelectorAll('.close-modal').forEach(button => button.addEventListener('click', closeGameModal));
    document.querySelectorAll('.close-mode-modal').forEach(button => button.addEventListener('click', closeModeModal));
    gameModal.addEventListener('click', event => { if (event.target === gameModal) closeGameModal(); });
    modeModal.addEventListener('click', event => { if (event.target === modeModal) closeModeModal(); });

    byId('gamesTableBody').addEventListener('click', async event => {
        const button = event.target.closest('button[data-id]');
        if (!button) return;
        const game = findGame(button.dataset.id);
        if (!game) return;

        if (button.classList.contains('manage-modes')) {
            selectedGameId = game.gameId;
            renderModes();
            byId('modesPanel').scrollIntoView({ behavior: 'smooth', block: 'start' });
        } else if (button.classList.contains('edit-game')) {
            openGameModal(game);
        } else if (button.classList.contains('toggle-game')) {
            const action = game.enabled ? '下架' : '上架';
            if (!window.confirm('確定要' + action + '「' + game.gameName + '」嗎？')) return;
            setBusy(button, true, action + '中……');
            try {
                await request(ADMIN_API + '/games/' + game.gameId, {
                    method: 'PUT', body: JSON.stringify(gamePayload(game, !game.enabled))
                });
                await loadGames();
            } catch (error) {
                showMessage(byId('gamesMessage'), error.message, 'error');
            } finally {
                setBusy(button, false, '');
            }
        }
    });

    byId('modesTableBody').addEventListener('click', async event => {
        const button = event.target.closest('button[data-id]');
        const game = findGame(selectedGameId);
        if (!button || !game) return;
        const mode = (game.modes || []).find(item => String(item.modeId) === String(button.dataset.id));
        if (!mode) return;

        if (button.classList.contains('edit-mode')) {
            openModeModal(mode);
        } else if (button.classList.contains('toggle-mode')) {
            const action = mode.enabled ? '停用' : '啟用';
            if (!window.confirm('確定要' + action + '「' + mode.modeName + '」嗎？')) return;
            setBusy(button, true, action + '中……');
            try {
                await request(ADMIN_API + '/games/' + game.gameId + '/modes/' + mode.modeId, {
                    method: 'PUT', body: JSON.stringify(modePayload(mode, !mode.enabled))
                });
                await loadGames();
            } catch (error) {
                showMessage(byId('gamesMessage'), error.message, 'error');
            } finally {
                setBusy(button, false, '');
            }
        }
    });

    byId('gameForm').addEventListener('submit', async event => {
        event.preventDefault();
        const id = byId('gameId').value;
        const payload = gamePayload();
        if (!payload.frontendPath.startsWith('/') || !payload.backendPath.startsWith('/')) {
            showMessage(byId('gameFormMessage'), '前端與後端入口必須使用以 / 開頭的相對路徑。', 'error');
            return;
        }
        const button = byId('saveGameBtn');
        setBusy(button, true, '儲存中……');
        try {
            await request(ADMIN_API + '/games' + (id ? '/' + id : ''), {
                method: id ? 'PUT' : 'POST', body: JSON.stringify(payload)
            });
            closeGameModal();
            await loadGames();
            showMessage(byId('gamesMessage'), id ? '遊戲資料已更新。' : '遊戲已建立。', 'success');
        } catch (error) {
            showMessage(byId('gameFormMessage'), error.message, 'error');
        } finally {
            setBusy(button, false, '');
        }
    });

    byId('modeForm').addEventListener('submit', async event => {
        event.preventDefault();
        const game = findGame(selectedGameId);
        if (!game) return;
        const id = byId('modeId').value;
        const payload = modePayload();
        if (payload.minPlayers > payload.maxPlayers) {
            showMessage(byId('modeFormMessage'), '最少真人玩家不能大於最多真人玩家。', 'error');
            return;
        }
        const button = byId('saveModeBtn');
        setBusy(button, true, '儲存中……');
        try {
            await request(ADMIN_API + '/games/' + game.gameId + '/modes' + (id ? '/' + id : ''), {
                method: id ? 'PUT' : 'POST', body: JSON.stringify(payload)
            });
            closeModeModal();
            await loadGames();
            showMessage(byId('gamesMessage'), id ? '遊戲模式已更新。' : '遊戲模式已建立。', 'success');
        } catch (error) {
            showMessage(byId('modeFormMessage'), error.message, 'error');
        } finally {
            setBusy(button, false, '');
        }
    });

    async function initialize() {
        try {
            await verifyAdmin();
            byId('managementContent').hidden = false;
            await loadGames();
        } catch (error) {
            showMessage(byId('accessMessage'), error.message, 'error');
        }
    }

    initialize();
})();
