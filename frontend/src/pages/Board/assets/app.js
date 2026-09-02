(function ($) {
    'use strict';

    const SERVER_BASE = `http://${window.location.hostname || 'localhost'}:8080`;
    const API_BASE = SERVER_BASE + '/board';
    const app = $('#app');

    function currentUser() {
        try { return JSON.parse(localStorage.getItem('sgpUser') || 'null'); }
        catch (_) { return null; }
    }

    function saveUser(user) {
        if (user) localStorage.setItem('sgpUser', JSON.stringify(user));
        else localStorage.removeItem('sgpUser');
        renderMember();
    }

    function escapeHtml(value) {
        return $('<div>').text(value == null ? '' : String(value)).html();
    }

    function dateText(value) {
        return value ? String(value).replace('T', ' ').slice(0, 16) : '未指定';
    }

    function statusText(status) {
        return ({RECRUITING:'招募中',FULL:'已滿',STARTING:'遊戲中',FINISHED:'已結束',CLOSED:'已關閉',PENDING:'等待審核',APPROVED:'已同意',REJECTED:'已拒絕',CANCELLED:'已取消'})[status] || status || '未指定';
    }

    function request(method, path, data, base = API_BASE) {
        return $.ajax({
            method: method,
            url: base + path,
            contentType: 'application/json; charset=UTF-8',
            dataType: 'json',
            data: data === undefined ? undefined : JSON.stringify(data)
        }).catch(function (xhr) {
            const message = xhr.responseJSON?.message || xhr.responseText || `HTTP 錯誤：${xhr.status}`;
            return $.Deferred().reject(new Error(message)).promise();
        });
    }

    const api = {
        login: data => request('POST', '/auth/login', data),
        register: data => request('POST', '/auth/register', data),
        games: () => request('GET', '/api/game-management/games', undefined, SERVER_BASE),
        posts: (keyword='', status='', gameId='', modeId='') => {
            const query = new URLSearchParams({keyword});
            if (status) query.set('status', status);
            if (gameId) query.set('gameId', gameId);
            if (modeId) query.set('modeId', modeId);
            return request('GET', '/team-posts?' + query);
        },
        post: id => request('GET', `/team-posts/${id}`),
        createPost: data => request('POST', '/team-posts', data),
        updatePost: (id,data) => request('PUT', `/team-posts/${id}`, data),
        deletePost: id => $.ajax({method:'DELETE',url:API_BASE+`/team-posts/${id}`}),
        captainPosts: id => request('GET', `/team-posts/captain/${id}`),
        join: (id,data) => request('POST', `/team-posts/${id}/join`, data),
        myApplications: id => request('GET', `/applications/member/${id}`),
        captainRequests: id => request('GET', `/applications/captain/${id}`),
        review: (id,status) => request('PUT', `/applications/${id}/${status}?captainId=${currentUser().id}`),
        comments: id => request('GET', `/team-posts/${id}/comments`),
        comment: (id,data) => request('POST', `/team-posts/${id}/comments`, data),
        deleteComment: id => $.ajax({method:'DELETE',url:API_BASE+`/comments/${id}`}),
        favorite: (postId,memberId) => request('POST', `/team-posts/${postId}/favorite/${memberId}`),
        favorites: id => request('GET', `/favorites/member/${id}`),
        notifications: id => request('GET', `/notifications/member/${id}`),
        roomByPost: postId => request('GET', `/team-posts/${postId}/game?memberId=${currentUser().id}`),
        startTeam: postId => request('POST', `/team-posts/${postId}/start?captainId=${currentUser().id}`),
        kickMember: (postId, account) => request('POST', `/team-posts/${postId}/kick?captainId=${currentUser().id}`, {account})

    };

    function notify(message, isError=false) {
        $('.toast').remove();
        const toast = $('<div>').addClass(`toast${isError ? ' error-toast' : ''}`).text(message).appendTo('body');
        setTimeout(() => toast.fadeOut(250, () => toast.remove()), 3200);
    }

    function renderMember() {
        const user = currentUser();
        $('[data-login-only]').toggleClass('hidden', !user);
        $('#memberArea').html(user
            ? `<span class="nickname">👤 ${escapeHtml(user.nickname || user.account)}</span><button id="logoutButton" class="secondary" type="button">登出</button>`
            : '<a class="primary" href="#login">登入</a>');
    }

    function requireLogin() {
        if (currentUser()) return true;
        sessionStorage.setItem('afterLoginHash', location.hash || '#home');
        location.hash = '#login';
        notify('請先登入會員', true);
        return false;
    }

    function postCard(post) {
        const status = String(post.status || '').toLowerCase();
        return `<article class="card post-card">
            <div><div class="meta"><span class="chip">🎮 ${escapeHtml(post.gameName)}</span><span class="chip">${escapeHtml(post.modeName || '尚未設定模式')}</span><span class="chip">${escapeHtml(post.activityType)}</span></div>
            <h2><a href="#post/${post.id}">${escapeHtml(post.title)}</a></h2>
            <p>${escapeHtml(post.description || '尚無說明')}</p>
            <div class="meta"><span>◷ ${dateText(post.startTime)}</span><span>👥 ${post.currentPlayers || 0}/${post.maxPlayers}</span><span>隊長：${escapeHtml(post.captain?.nickname || '未知')}</span></div></div>
            <div class="post-side"><span class="status ${status}">${statusText(post.status)}</span><div class="actions"><a class="primary" href="#post/${post.id}">查看詳情</a></div></div>
        </article>`;
    }

    function homePage() {
        app.html(`<section class="hero"><div><h1>找到你的隊友，<br>一起挑戰遊戲世界！</h1><p>撲克牌、趣味問答、圖靈解密，揪齊隊友立即開局。</p><a class="primary" href="#posts">搜尋公告</a></div><div class="hero-games"><div class="game-tile poker"><span class="icon">🂡</span><b>撲克牌</b><small>策略對決</small></div><div class="game-tile quiz"><span class="icon">❓</span><b>問答遊戲</b><small>知識挑戰</small></div><div class="game-tile turing"><span class="icon">⌘</span><b>圖靈解密</b><small>協力破譯</small></div></div></section><section class="feature-grid"><article><b>🂡 撲克牌對戰</b><p>找齊牌友，展開策略與心理的較量。</p></article><article><b>❓ 問答挑戰</b><p>集合不同專長的隊友一起破解題目。</p></article><article><b>⌘ 圖靈解密</b><p>組隊分析線索，完成程式與邏輯挑戰。</p></article></section>`);
    }

    function loginPage() {
        app.html(`<section class="card auth-card"><h1>會員登入</h1><p id="formMessage" class="error hidden"></p><form id="loginForm"><label>帳號／Email<input name="account" value="player02" autocomplete="username" required></label><label>密碼<input name="password" type="password" value="password" autocomplete="current-password" required></label><button class="primary" type="submit">登入</button></form><p>還沒有帳號？ <a href="#register">註冊帳號</a></p><small>測試：player02 / password、teamleader / password</small></section>`);
        $('#loginForm').on('submit', function (event) {
            event.preventDefault();
            const button = $(this).find('button').prop('disabled', true).text('登入中...');
            api.login({account:this.account.value.trim(), password:this.password.value})
                .done(function (user) { saveUser(user); location.hash = sessionStorage.getItem('afterLoginHash') || '#home'; sessionStorage.removeItem('afterLoginHash'); })
                .fail(function (error) { $('#formMessage').removeClass('hidden').text(error.message); })
                .always(function () { button.prop('disabled', false).text('登入'); });
        });
    }

    function registerPage() {
        app.html(`<section class="card auth-card"><h1>註冊會員</h1><p id="formMessage" class="error hidden"></p><form id="registerForm"><label>帳號<input name="account" required></label><label>暱稱<input name="nickname" required></label><label>Email<input name="email" type="email" required></label><label>密碼<input name="password" type="password" minlength="4" required></label><button class="primary" type="submit">建立帳號</button></form></section>`);
        $('#registerForm').on('submit', function (event) {
            event.preventDefault();
            const form = this;
            api.register({account:form.account.value.trim(),nickname:form.nickname.value.trim(),email:form.email.value.trim(),password:form.password.value})
                .done(function () { notify('註冊完成，請登入'); location.hash='#login'; })
                .fail(function (error) { $('#formMessage').removeClass('hidden').text(error.message); });
        });
    }

    function bindGameSelectors(games, gameSelect, modeSelect, onModeChange, selectedGame, selectedMode, filtering = false) {
        const gamePlaceholder = filtering ? '全部遊戲' : '請選擇遊戲';
        const modePlaceholder = filtering ? '全部模式' : '請選擇模式';
        gameSelect.empty().append($('<option>').val('').text(gamePlaceholder));
        games.forEach(game => gameSelect.append($('<option>').val(game.gameId).text(game.gameName)));
        gameSelect.prop('disabled', !games.length);
        function updateMode() {
            const game = games.find(g => String(g.gameId) === gameSelect.val());
            const modes = game?.modes || [];
            modeSelect.empty().append($('<option>').val('').text(!game && !filtering ? '請先選擇遊戲' : modePlaceholder));
            modes.forEach(mode => modeSelect.append($('<option>').val(mode.modeId).text(mode.modeName)));
            modeSelect.prop('disabled', !modes.length);
            onModeChange(null);
        }
        gameSelect.on('change', updateMode);
        modeSelect.on('change', function () {
            const game = games.find(g => String(g.gameId) === gameSelect.val());
            onModeChange(game?.modes?.find(m => String(m.modeId) === modeSelect.val()) || null);
        });
        if (selectedGame) gameSelect.val(String(selectedGame));
        updateMode();
        if (selectedMode) modeSelect.val(String(selectedMode)).trigger('change');
    }

    function modeInfo(mode, count) {
        if (!mode) return '請選擇遊戲與模式，再選擇遊玩人數。';
        const max = mode.modeMaxPlayers ?? mode.maxPlayers;
        const selected = count || mode.maxPlayers;
        const range = mode.minPlayers === max ? `此模式固定 ${max} 人` : `此模式可選 ${mode.minPlayers}～${max} 人`;
        return `${range}（含隊長） · 本隊伍：${selected} 人 · 電腦玩家：${mode.computerPlayers} 人。${selected === 1 ? '建立後立即建房，由隊長選擇開始遊戲。' : '達到選定人數後建房，由隊長決定何時開始。'}`;
    }

    function roomButton(post) {
        return post?.roomId ? `<button class="primary enter-room" data-id="${post.id}">${post.status==='STARTING'?'進入遊戲':'進入房間'}</button>` : '';
    }

    function postsPage() {
        app.html(`<section><div class="title-row"><div><h1>組隊公告</h1><p>搜尋適合你的遊戲隊伍</p></div><a class="primary protected-link" href="#form/new">＋建立公告</a></div><div class="list-layout"><aside class="card filter"><h3>搜尋／篩選</h3><label>關鍵字<input id="keyword" placeholder="遊戲、模式或關鍵字"></label><label>遊戲<select id="filterGame" disabled><option value="">載入遊戲中...</option></select></label><label>遊戲模式<select id="filterMode" disabled><option value="">全部模式</option></select></label><p id="filterMessage" class="error hidden"></p><label>狀態<select id="status"><option value="">全部</option><option value="RECRUITING">招募中</option><option value="FULL">已滿</option><option value="FINISHED">已結束</option></select></label><button id="searchButton" class="primary" type="button">搜尋</button></aside><div id="postList" class="posts"><p class="loading">載入中...</p></div></div></section>`);
        const list = $('#postList');
        let searchVersion = 0;
        const load = function () {
            const version = ++searchVersion;
            list.html('<p class="loading">載入中...</p>');
            api.posts($('#keyword').val(), $('#status').val(), $('#filterGame').val(), $('#filterMode').val()).done(function (data) {
                if (version !== searchVersion) return;
                list.html(data?.length ? data.map(postCard).join('') : '<p class="card empty">目前沒有符合條件的公告</p>');
            }).fail(error => { if (version === searchVersion) list.html(`<p class="error">${escapeHtml(error.message)}</p>`); });
        };
        const gameSelect = $('#filterGame'), modeSelect = $('#filterMode'), message = $('#filterMessage');
        api.games().done(games => bindGameSelectors(games, gameSelect, modeSelect, () => {}, null, null, true))
            .fail(error => { gameSelect.find('option').text('遊戲載入失敗'); message.removeClass('hidden').text(error.message); });
        $('#searchButton').on('click', load);
        $('#keyword').on('keydown', event => { if (event.key === 'Enter') load(); });
        load();
    }

    function postDetailPage(id) {
        app.html('<p class="loading">載入公告中...</p>');
        $.when(api.post(id), api.comments(id)).done(function (postResult, commentResult) {
            const post = Array.isArray(postResult) && postResult.length === 3 ? postResult[0] : postResult;
            const comments = Array.isArray(commentResult) && commentResult.length === 3 ? commentResult[0] : commentResult;
            const status = String(post.status || '').toLowerCase();
            app.html(`<section><a class="primary" href="#posts">← 返回列表</a><div class="banner">🎮 ⚔️</div><div class="card detail-card"><div class="title-row" style="color:inherit"><h1 style="color:inherit">${escapeHtml(post.title)}</h1><span class="status ${status}">${statusText(post.status)}</span></div><div class="facts"><span>🎮 ${escapeHtml(post.gameName)}</span><span>⚔ ${escapeHtml(post.modeName || '尚未設定模式')}</span><span>🤖 電腦：${post.computerPlayers ?? '未設定'} 人</span><span>▣ ${escapeHtml(post.activityType)}</span><span>◷ ${dateText(post.startTime)}</span><span>👥 ${post.currentPlayers}/${post.maxPlayers}</span><span>🎙 ${post.voiceRequired?'需要':'不需要'}</span><span>隊長：${escapeHtml(post.captain?.nickname)}</span></div><p>${escapeHtml(post.description)}</p><div class="actions"><button id="joinButton" class="primary" ${post.status!=='RECRUITING'||!post.modeId||post.captain?.id===currentUser()?.id?'disabled':''}>我要加入</button>${roomButton(post)}<button id="favoriteButton" class="secondary">☆ 收藏</button><button id="shareButton" class="secondary">分享</button></div><form id="joinForm" class="hidden"><label>申請留言（可留空）<textarea name="message" maxlength="255" placeholder="向隊長介紹自己"></textarea></label><button class="primary" type="submit">送出加入申請</button></form></div><div class="card comments"><h2>留言（${comments?.length || 0}）</h2><div id="commentList">${comments?.length ? comments.map(comment => `<div class="comment"><div class="comment-head"><b>${escapeHtml(comment.member?.nickname)}</b>${currentUser()?.id===comment.member?.id?`<button class="danger delete-comment" data-id="${comment.id}">刪除</button>`:''}</div><p>${escapeHtml(comment.content)}</p></div>`).join('') : '<p class="empty">目前沒有留言</p>'}</div><form id="commentForm" class="comment-form"><input name="content" placeholder="輸入留言..." required><button class="primary">送出</button></form></div></section>`);
            $('#joinButton').on('click', function () { if(!requireLogin())return; $('#joinForm').toggleClass('hidden').find('textarea').trigger('focus'); });
            $('#joinForm').on('submit', function(event) {
                event.preventDefault();
                if (!requireLogin()) return;
                const form = $(this), button = form.find('button').prop('disabled', true);
                api.join(id, {memberId:currentUser().id, message:this.message.value.trim()})
                    .done(() => { notify('申請已送出'); form.addClass('hidden'); $('#joinButton').prop('disabled', true).text('已送出申請'); })
                    .fail(e => notify(e.message, true)).always(() => button.prop('disabled', false));
            });
            $('#favoriteButton').on('click', function () { if(!requireLogin())return; api.favorite(id,currentUser().id).done(r=>notify(r.favorite?'已加入收藏':'已取消收藏')).fail(e=>notify(e.message,true)); });
            $('#shareButton').on('click', function () { navigator.clipboard?.writeText(location.href); notify('網址已複製'); });
            $('#commentForm').on('submit', function(event){event.preventDefault();if(!requireLogin())return;api.comment(id,{memberId:currentUser().id,content:this.content.value.trim()}).done(()=>postDetailPage(id)).fail(e=>notify(e.message,true));});
            $('.delete-comment').on('click', function(){if(confirm('確定刪除留言？'))api.deleteComment($(this).data('id')).done(()=>postDetailPage(id));});
        }).fail(error => app.html(`<p class="error">${escapeHtml(error.message)}</p>`));
    }

    function postFormPage(id) {
        if (!requireLogin()) return;
        const isEdit = id !== 'new';
        app.html(`<section class="card form-card"><h1>${isEdit?'編輯公告':'建立隊伍'}</h1><p id="formMessage" class="error hidden"></p><form id="postForm"><div class="form-grid"><label>遊戲<select name="gameId" required disabled><option value="">載入遊戲中...</option></select></label><label>遊戲模式<select name="modeId" required disabled><option value="">請先選擇遊戲</option></select></label><label class="wide">遊玩人數（含隊長）<select name="playerCount" required disabled><option value="">請先選擇遊戲模式</option></select></label><div id="modeInfo" class="mode-info wide" aria-live="polite">請選擇遊戲與模式，系統會自動帶入人數。</div><label class="wide">公告標題<input name="title" maxlength="100" required></label><label>活動類型<input name="activityType" required></label><label>開始時間<input name="startTime" type="datetime-local" required></label><label>結束時間<input name="endTime" type="datetime-local"></label><label>語音需求<select name="voiceRequired"><option value="true">需要</option><option value="false">不需要</option></select></label><label>段位條件（選填）<input name="rankRequirement" placeholder="留空表示不限段位"></label><label class="wide">標籤<input name="tags" placeholder="新手友善,語音"></label><label class="wide">詳細說明<textarea name="description" required></textarea></label></div><div class="actions"><a class="secondary" href="#posts">取消</a><button class="primary" type="submit" disabled>${isEdit?'更新公告':'建立隊伍'}</button></div></form></section>`);
        const form = $('#postForm'), f = form[0], message = $('#formMessage'), info = $('#modeInfo');
        const button = form.find('button[type="submit"]');
        const label = isEdit ? '更新公告' : '建立隊伍';
        let ready = false, selectedMode = null;
        const playerSelect = $(f.playerCount);
        function setPlayerCounts(mode, selectedCount, locked = false) {
            playerSelect.empty();
            if (!mode) {
                playerSelect.append($('<option>').val('').text('請先選擇遊戲模式')).prop('disabled', true);
                info.text(modeInfo(null));
                return;
            }
            const min = locked ? selectedCount : mode.minPlayers;
            const max = locked ? selectedCount : (mode.modeMaxPlayers ?? mode.maxPlayers);
            for (let count = min; count <= max; count++) {
                playerSelect.append($('<option>').val(count).text(count + ' 人' + (min === max ? '（固定）' : '')));
            }
            playerSelect.val(String(selectedCount || max)).prop('disabled', locked);
            info.text(modeInfo(mode, Number(playerSelect.val())));
        }
        playerSelect.on('change', () => info.text(modeInfo(selectedMode, Number(playerSelect.val()))));
        const postPromise = isEdit ? api.post(id) : $.Deferred().resolve(null).promise();
        $.when(api.games(), postPromise).done(function (gameResult, postResult) {
            const games = Array.isArray(gameResult) && gameResult.length === 3 && typeof gameResult[1] === 'string' ? gameResult[0] : gameResult;
            const post = Array.isArray(postResult) && postResult.length === 3 ? postResult[0] : postResult;
            if (post && post.captain?.id !== currentUser()?.id) { message.removeClass('hidden').text('只有隊長可以編輯公告'); return; }
            if (post) {
                Object.keys(post).forEach(key => { if (f.elements[key]) $(f.elements[key]).val(typeof post[key] === 'boolean' ? String(post[key]) : post[key]); });
                if (post.startTime) f.startTime.value = post.startTime.slice(0,16);
                if (post.endTime) f.endTime.value = post.endTime.slice(0,16);
            }
            // 已建房或已招募隊員的公告保留當時的模式，即使遊戲日後停用仍可改說明。
            const locked = post && (post.roomId || post.currentPlayers > 1) && post.modeId;
            if (locked) {
                $(f.gameId).empty().append($('<option>').val(post.gameId).text(post.gameName));
                $(f.modeId).empty().append($('<option>').val(post.modeId).text(post.modeName));
                selectedMode = post;
                setPlayerCounts(post, post.maxPlayers, true);
                info.append(' 遊戲、模式與人數已固定。');
            } else {
                bindGameSelectors(games, $(f.gameId), $(f.modeId), mode => {
                    selectedMode = mode;
                    setPlayerCounts(mode, mode && post?.modeId === mode.modeId ? post.maxPlayers : null);
                    button.prop('disabled', !ready || !mode);
                }, post?.gameId, post?.modeId);
            }
            ready = true;
            button.prop('disabled', !selectedMode);
            if (!games.length && !locked) message.removeClass('hidden').text('目前沒有可選擇的遊戲，請先由遊戲管理員啟用遊戲與模式。');
            if (post && !post.modeId) message.removeClass('hidden').text('這是尚未關聯遊戲模式的舊公告；若已有隊員，請重新建立隊伍。');
        }).fail(error => message.removeClass('hidden').text(error.message));
        form.on('submit', function(event) {
            event.preventDefault();
            if (!ready || !selectedMode || button.prop('disabled')) return;
            const data = {title:f.title.value.trim(), gameId:Number(f.gameId.value), modeId:Number(f.modeId.value), playerCount:Number(f.playerCount.value), activityType:f.activityType.value.trim(), startTime:f.startTime.value, endTime:f.endTime.value||null, voiceRequired:f.voiceRequired.value==='true', rankRequirement:f.rankRequirement.value.trim() || null, description:f.description.value.trim(), tags:f.tags.value.trim(), captainId:currentUser().id};
            button.prop('disabled', true).text('儲存中...');
            message.addClass('hidden');
            const action = isEdit ? api.updatePost(id,data) : api.createPost(data);
            action.done(post => { notify(post.roomId ? '隊伍已滿，房間已建立' : '公告已儲存，等待隊員加入'); location.hash = `#post/${post.id}`; })
                .fail(error => message.removeClass('hidden').text(error.message))
                .always(() => button.prop('disabled', !selectedMode).text(label));
        });
    }

    function listPage(title, loader, renderer) {
        if (!requireLogin()) return;
        app.html(`<section><h1 class="page-title">${title}</h1><div id="manageList" class="card manager"><p class="loading">載入中...</p></div></section>`);
        loader().done(data=>$('#manageList').html(data?.length?data.map(renderer).join(''):'<p class="table-empty">目前沒有資料</p>')).fail(error=>$('#manageList').html(`<p class="error">${escapeHtml(error.message)}</p>`));
    }

    function applicationsPage(){const user=currentUser();listPage('我的申請',()=>api.myApplications(user.id),x=>`<div class="manage-row"><div><b><a href="#post/${x.post?.id}">${escapeHtml(x.post?.title)}</a></b><p>${escapeHtml(x.post?.gameName)}・${escapeHtml(x.post?.modeName)}・${dateText(x.createdAt)}</p></div><div><span class="status ${String(x.status).toLowerCase()}">${statusText(x.status)}</span> ${x.status==='APPROVED'?roomButton(x.post):''}</div></div>`);}

    function favoritesPage(){const user=currentUser();listPage('我的收藏',()=>api.favorites(user.id),x=>postCard(x.post));}
    function notificationsPage(){renderTeamManager(true);}
    function captainPage(){renderTeamManager(false);}

    function teamManagementCard(post, requests) {
        const pending = requests.filter(r => r.post?.id === post.id && r.status === 'PENDING');
        const approved = requests.filter(r => r.post?.id === post.id && r.status === 'APPROVED');
        const editable = ['RECRUITING', 'FULL'].includes(post.status);
        const canStart = post.status === 'FULL' && post.currentPlayers === post.maxPlayers && post.roomId;
        return `<article class="card manager team-manager" id="team-${post.id}">
            <div class="team-title"><div><h2><a href="#post/${post.id}">${escapeHtml(post.title)}</a></h2><p>${escapeHtml(post.gameName)} · ${escapeHtml(post.modeName || '尚未設定模式')} · ${post.currentPlayers}/${post.maxPlayers} 人</p></div><span class="status ${String(post.status).toLowerCase()}">${statusText(post.status)}</span></div>
            <div class="actions">${post.status !== 'STARTING' ? `<button class="primary start-team" data-id="${post.id}" ${canStart ? '' : 'disabled'}>開始遊戲</button>` : ''}${roomButton(post)}<a class="secondary" href="#form/${post.id}">編輯公告</a><button class="danger delete-post" data-id="${post.id}">刪除公告</button></div>
            <p class="team-hint">${post.status === 'STARTING' ? '遊戲已開始，可從上方返回遊戲。' : canStart ? '人數已到齊。你可以開始遊戲，或先調整隊員。' : '先核准申請並達到選定人數，才能開始遊戲。'}</p>
            <h3>隊員</h3><div class="manage-row"><b>👑 ${escapeHtml(post.captain?.nickname || post.captain?.account)}</b><span>隊長</span></div>
            ${approved.map(r => `<div class="manage-row"><div><b>${escapeHtml(r.applicant?.nickname)}</b><p>${escapeHtml(r.applicant?.account)}</p></div>${editable ? `<button class="danger kick-member" data-id="${post.id}" data-account="${encodeURIComponent(r.applicant.account)}">踢除隊員</button>` : '<span>遊戲中</span>'}</div>`).join('')}
            <h3>加入申請（${pending.length}）</h3>
            ${pending.length ? pending.map(r => `<div class="manage-row"><div><b>${escapeHtml(r.applicant?.nickname)}</b><p>${escapeHtml(r.message || '未填寫留言')}</p></div><div><button class="ok review" data-id="${r.id}" data-status="APPROVED" ${post.status === 'RECRUITING' ? '' : 'disabled'}>同意加入</button><button class="danger review" data-id="${r.id}" data-status="REJECTED">拒絕申請</button></div></div>`).join('') : '<p class="team-hint">目前沒有待審核申請</p>'}
        </article>`;
    }

    function renderTeamManager(showNotices) {
        if (!requireLogin()) return;
        const user = currentUser();
        app.html(`<section><div class="title-row"><h1>${showNotices ? '我的通知' : '隊長管理'}</h1><button class="secondary refresh-team">重新整理</button></div><div id="teamManager"><p class="loading">載入隊伍中...</p></div>${showNotices ? '<div id="noticeList" class="card manager"><h2>通知紀錄</h2></div>' : ''}</section>`);
        const manager = $('#teamManager'), noticeList = $('#noticeList');
        $.when(api.captainPosts(user.id), api.captainRequests(user.id)).done(function(postResult, requestResult) {
            const posts = Array.isArray(postResult) && postResult.length === 3 && typeof postResult[1] === 'string' ? postResult[0] : postResult;
            const requests = Array.isArray(requestResult) && requestResult.length === 3 && typeof requestResult[1] === 'string' ? requestResult[0] : requestResult;
            manager.html(posts.length ? posts.map(p => teamManagementCard(p, requests)).join('') : '<p class="empty">你目前沒有擔任隊長的隊伍。</p>');
        }).fail(e => manager.html(`<p class="error">${escapeHtml(e.message)}</p>`));
        if (showNotices) api.notifications(user.id).done(notices => {
            noticeList.html('<h2>通知紀錄</h2>' + (notices.length ? notices.map(n => `<div class="notice"><b>🔔 ${escapeHtml(n.title)}</b><p>${escapeHtml(n.message)}</p><small>${dateText(n.createdAt)}</small><div class="actions">${n.postId ? `<a class="secondary" href="#post/${n.postId}">查看隊伍</a>` : '<a class="secondary" href="#captain">管理申請與隊伍</a>'}</div></div>`).join('') : '<p class="team-hint">目前沒有通知</p>'));
        }).fail(e => noticeList.html(`<p class="error">${escapeHtml(e.message)}</p>`));
    }

    function refreshTeamPage() {
        if (location.hash === '#notifications') notificationsPage();
        else captainPage();
    }

    function goToGame(entry) {
        if (!entry.gameUrl) { notify('尚未取得遊戲入口，請重新整理後再試', true); return; }
        localStorage.setItem('account', currentUser().account);
        window.location.assign(new URL(entry.gameUrl, window.location.origin).href);
    }

    function enterRoom(postId, button) {
        if (!requireLogin()) return;
        button.prop('disabled', true);
        api.roomByPost(postId).done(function(room) {
            if (room.gameUrl) { goToGame(room); return; }
            localStorage.setItem('account', currentUser().account);
            // Lobby 等待頁使用 account 與 ?room=，成員已由後端加入，不重複呼叫 join-room。
            const url = new URL('../Lobby/waiting_room.html', window.location.href);
            url.hash = '';
            url.search = new URLSearchParams({room: room.roomId, boardPostId: postId}).toString();
            window.location.assign(url.href);
        }).fail(e => notify(e.message, true)).always(() => button.prop('disabled', false));
    }

    function notFoundPage(){app.html('<section class="card not-found"><strong>404</strong><h1>找不到這個頁面</h1><p>網址可能已變更，或頁面不存在。</p><a class="primary" href="#home">回到大廳</a></section>');}

    function route() {
        const hash = (location.hash || '#home').slice(1);
        const parts = hash.split('/');
        window.scrollTo(0,0);
        if(parts[0]==='home'||!parts[0])homePage();
        else if(parts[0]==='login')loginPage();
        else if(parts[0]==='register')registerPage();
        else if(parts[0]==='posts')postsPage();
        else if(parts[0]==='post'&&parts[1])postDetailPage(parts[1]);
        else if(parts[0]==='form'&&parts[1])postFormPage(parts[1]);
        else if(parts[0]==='applications')applicationsPage();
        else if(parts[0]==='favorites')favoritesPage();
        else if(parts[0]==='notifications')notificationsPage();
        else if(parts[0]==='captain')captainPage();
        else notFoundPage();
        $('#mainNav').removeClass('open');
    }

    $(document).on('click','#logoutButton',function(){saveUser(null);location.hash='#home';notify('已登出');});
    $(document).on('click','.protected-link',function(event){if(!currentUser()){event.preventDefault();requireLogin();}});
    $(document).on('click','.review',function(){const button=$(this).prop('disabled',true);api.review(button.data('id'),button.data('status')).done(result=>{if(result.post?.roomId)notify('隊伍已滿，可以選擇開始遊戲');refreshTeamPage();}).fail(e=>notify(e.message,true)).always(()=>button.prop('disabled',false));});
    $(document).on('click','.refresh-team',refreshTeamPage);
    $(document).on('click','.start-team',function(){
        if(!requireLogin())return;
        const button=$(this).prop('disabled',true);
        api.startTeam(button.data('id')).done(goToGame).fail(e=>notify(e.message,true)).always(()=>button.prop('disabled',false));
    });
    $(document).on('click','.kick-member',function(){
        if(!requireLogin())return;
        const button=$(this);
        if(!button.data('confirmed')){button.data('confirmed',true).text('確認踢除');return;}
        button.prop('disabled',true);
        api.kickMember(button.data('id'),decodeURIComponent(button.attr('data-account')))
            .done(()=>{notify('已移出隊員，隊伍繼續招募');refreshTeamPage();})
            .fail(e=>notify(e.message,true)).always(()=>button.prop('disabled',false));
    });
    $(document).on('click','.enter-room',function(){enterRoom($(this).data('id'),$(this));});
    $(document).on('click','.delete-post',function(){if(confirm('確定刪除公告？'))api.deletePost($(this).data('id')).done(captainPage).fail(e=>notify(e.message,true));});
    $('#menuButton').on('click',()=>$('#mainNav').toggleClass('open'));
    $('#quickBattle').on('click',function(){api.posts('','RECRUITING').done(function(posts){if(posts?.length){const post=posts[Math.floor(Math.random()*posts.length)];location.hash=`#post/${post.id}`;}else notify('目前沒有招募中的公告',true);}).fail(e=>notify(e.message,true));});
    $(window).on('hashchange',route);
    renderMember();
    route();
})(jQuery);
