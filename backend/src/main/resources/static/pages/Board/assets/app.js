(function ($) {
    'use strict';

    const SERVER_BASE = UserApi.API_BASE;
    const API_BASE = SERVER_BASE + '/board';
    const app = $('#app');
    let verifiedMember = null, sessionToken = '', sessionReady = false, sessionVersion = 0, sessionError = '';
    let viewVersion = 0, refreshVisible = null, summaryTimer = null, summaryRequest = 0;
    const postFilters = {keyword:'', status:'', gameId:'', modeId:'', startFrom:'', startTo:'', page:0};
    const noticeState = {category:'CAPTAIN', page:0};
    const categoryLabels = {CAPTAIN:'我的隊伍', APPLICANT:'我申請加入', WATCHING:'我關注的公告'};
    const realtime = new BoardRealtime({base:SERVER_BASE,
        onStatus: text => $('#boardLiveStatus').text((currentUser() ? '即時訊息：' : '公開公告更新：') + text),
        onAuthError: () => UserApi.checkLogin().fail(function(){}),
        onEvent: event => {
            if (['NOTIFICATION','NOTIFICATIONS_READ','RESYNC'].includes(event.type)) scheduleSummary();
            if (event.type === 'NOTIFICATION') notify(event.title + '：' + event.message);
            if (refreshVisible) refreshVisible(event);
        }
    });
    function scheduleSummary() { clearTimeout(summaryTimer); summaryTimer = setTimeout(refreshSummary, 150); }
    function refreshSummary() {
        const user = currentUser(), version = sessionVersion, requestVersion = ++summaryRequest;
        if (!user) { $('#notificationBadge').addClass('hidden').text(''); return; }
        api.noticeSummary().done(summary => {
            if (version !== sessionVersion || requestVersion !== summaryRequest || !currentUser()) return;
            $('#notificationBadge').text(summary.unread > 99 ? '99+' : summary.unread).toggleClass('hidden', !summary.unread)
                .attr('aria-label', summary.unread + ' 則未讀通知');
            $('#noticeSummary').text('未讀通知 ' + summary.unread + ' 則，其中新留言 ' + summary.unreadComments + ' 則');
            Object.keys(categoryLabels).forEach(key => $('[data-count="'+key+'"]').text(summary.categories[key] || 0));
        });
    }

    function currentUser() {
        const session = UserApi.getLoginSession();
        return session && sessionToken === UserApi.getToken() && verifiedMember?.platformUserId === session.userId ? verifiedMember : null;
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
        return UserApi.request({
            method: method,
            url: (base === API_BASE ? '/board' : '') + path,
            contentType: 'application/json; charset=UTF-8',
            dataType: 'json',
            data: data === undefined ? undefined : JSON.stringify(data)
        }).catch(function (xhr) {
            const message = xhr.responseJSON?.message || xhr.responseText || `HTTP 錯誤：${xhr.status}`;
            return $.Deferred().reject(new Error(message)).promise();
        });
    }

    const api = {
        games: () => request('GET', '/api/game-management/games', undefined, SERVER_BASE),
        posts: (keyword='', status='', gameId='', modeId='') => {
            const query = new URLSearchParams({keyword});
            if (status) query.set('status', status);
            if (gameId) query.set('gameId', gameId);
            if (modeId) query.set('modeId', modeId);
            return request('GET', '/team-posts?' + query);
        },
        postPage: filters => request('GET', '/team-posts/page?' + new URLSearchParams(Object.entries(filters).filter(([,v]) => v !== '' && v != null))),
        post: id => request('GET', `/team-posts/${id}`),
        createPost: data => request('POST', '/team-posts', data),
        updatePost: (id,data) => request('PUT', `/team-posts/${id}`, data),
        deletePost: id => UserApi.request({method:'DELETE',url:`/board/team-posts/${id}`}),
        captainPosts: id => request('GET', `/team-posts/captain/${id}`),
        join: (id,data) => request('POST', `/team-posts/${id}/join`, data),
        myApplications: id => request('GET', `/applications/member/${id}`),
        captainRequests: id => request('GET', `/applications/captain/${id}`),
        review: (id,status) => request('PUT', `/applications/${id}/${status}?captainId=${currentUser().id}`),
        comments: (id,page=0) => request('GET', `/team-posts/${id}/comments/page?page=${page}`),
        comment: (id,data) => request('POST', `/team-posts/${id}/comments`, data),
        deleteComment: id => UserApi.request({method:'DELETE',url:`/board/comments/${id}`}),
        favorite: (postId,memberId) => request('POST', `/team-posts/${postId}/favorite/${memberId}`),
        favorites: id => request('GET', `/favorites/member/${id}`),
        notifications: (category,page=0) => request('GET', '/notifications?' + new URLSearchParams({category,page})),
        noticeSummary: () => request('GET', '/notifications/summary'),
        readNotices: ids => request('PUT', '/notifications/read', ids),
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
        $('#notificationLink').attr('title', user ? '查看我的通知' : '登入後查看我的通知');
        if (!user) $('#notificationBadge').addClass('hidden').text('');
    }

    function requireLogin(returnHash = location.hash || '#home') {
        if (currentUser()) return true;
        if (UserApi.isLoggedIn()) { notify(sessionError || '會員資料載入中，請稍候', true); return false; }
        sessionStorage.setItem('afterLoginHash', returnHash);
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
            <div class="meta"><span>◷ 開始時間：${dateText(post.startTime)}</span><span>👥 ${post.currentPlayers || 0}/${post.maxPlayers}</span><span>隊長：${escapeHtml(post.captain?.nickname || '未知')}</span></div></div>
            <div class="post-side"><span class="status ${status}">${statusText(post.status)}</span><div class="actions"><a class="primary" href="#post/${post.id}">查看詳情</a></div></div>
        </article>`;
    }

    function homePage() {
        app.html(`<section class="hero"><div><h1>找到你的隊友，<br>一起挑戰遊戲世界！</h1><p>撲克牌、趣味問答、圖靈解密，揪齊隊友立即開局。</p><a class="primary" href="#posts">搜尋公告</a></div><div class="hero-games"><div class="game-tile poker"><span class="icon">🂡</span><b>撲克牌</b><small>策略對決</small></div><div class="game-tile quiz"><span class="icon">❓</span><b>問答遊戲</b><small>知識挑戰</small></div><div class="game-tile turing"><span class="icon">⌘</span><b>圖靈解密</b><small>協力破譯</small></div></div></section><section class="feature-grid"><article><b>🂡 撲克牌對戰</b><p>找齊牌友，展開策略與心理的較量。</p></article><article><b>❓ 問答挑戰</b><p>集合不同專長的隊友一起破解題目。</p></article><article><b>⌘ 圖靈解密</b><p>組隊分析線索，完成程式與邏輯挑戰。</p></article></section>`);
    }

    function loginPage() {
        if (currentUser()) { location.hash = '#home'; return; }
        const target = new URL(window.location.href);
        target.hash = sessionStorage.getItem('afterLoginHash') || '#home';
        sessionStorage.removeItem('afterLoginHash');
        UserApi.redirectToLogin(target.href);
    }

    function registerPage() {
        const target = new URL(window.location.href);
        target.hash = '#home';
        UserApi.redirectToLogin(target.href, 'register');
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
        const version = viewVersion;
        app.html(`<section><div class="title-row"><div><h1>組隊公告</h1><p>搜尋適合你的遊戲隊伍</p></div><a class="primary protected-link" href="#form/new">＋建立公告</a></div><div class="list-layout"><aside class="card filter"><h3>搜尋／篩選</h3><label>關鍵字<input id="keyword" placeholder="遊戲、模式或關鍵字"></label><label>遊戲<select id="filterGame" disabled><option value="">載入遊戲中...</option></select></label><label>遊戲模式<select id="filterMode" disabled><option value="">全部模式</option></select></label><label>狀態<select id="status"><option value="">全部</option><option value="RECRUITING">招募中</option><option value="FULL">已滿</option><option value="STARTING">遊戲中</option><option value="FINISHED">已結束</option></select></label><label>開始時間（從）<input id="startFrom" type="datetime-local"></label><label>開始時間（至）<input id="startTo" type="datetime-local"></label><p class="filter-hint">依隊伍預定開始時間篩選</p><p id="filterMessage" class="error hidden"></p><button id="searchButton" class="primary" type="button">搜尋</button></aside><div id="postList" class="posts"><p class="loading">載入中...</p></div></div></section>`);
        const list = $('#postList'), message = $('#filterMessage');
        $('#keyword').val(postFilters.keyword); $('#status').val(postFilters.status);
        $('#startFrom').val(postFilters.startFrom); $('#startTo').val(postFilters.startTo);
        let searchVersion = 0, refreshTimer;
        const load = function () {
            const search = ++searchVersion;
            api.postPage(postFilters).done(data => {
                if (version !== viewVersion || search !== searchVersion) return;
                if (data.page > 0 && data.page >= data.totalPages) { postFilters.page = Math.max(0,data.totalPages-1); load(); return; }
                list.html((data.content.length ? data.content.map(postCard).join('') : '<p class="card empty">目前沒有符合條件的公告</p>') + BoardUi.pagination(data,'posts'));
            }).fail(error => { if (version === viewVersion && search === searchVersion) list.html('<p class="error">'+escapeHtml(error.message)+'</p>'); });
        };
        api.games().done(games => {
            if (version !== viewVersion) return;
            bindGameSelectors(games,$('#filterGame'),$('#filterMode'),()=>{},postFilters.gameId,postFilters.modeId,true);
        }).fail(error => { if (version === viewVersion) message.removeClass('hidden').text(error.message); });
        const search = function () {
            const from = $('#startFrom').val(), to = $('#startTo').val();
            if (from && to && to < from) { message.removeClass('hidden').text('時間範圍的結束不能早於開始'); return; }
            message.addClass('hidden');
            Object.assign(postFilters,{keyword:$('#keyword').val(),status:$('#status').val(),gameId:$('#filterGame').val() || '',modeId:$('#filterMode').val() || '',startFrom:from,startTo:to,page:0});
            load();
        };
        $('#searchButton').on('click',search);
        $('#keyword').on('keydown',event=>{if(event.key==='Enter')search();});
        list.on('click','.page-button',function(){postFilters.page=Number($(this).data('page'));load();});
        refreshVisible = event => { if (['POST_CHANGED','RESYNC'].includes(event.type)) { clearTimeout(refreshTimer); refreshTimer=setTimeout(()=>{if(version===viewVersion)load();},200); } };
        load();
    }

    function postDetailPage(id) {
        const version = viewVersion;
        let commentPage = 0, commentVersion = 0, wantLastCommentPage = false;
        app.html('<p class="loading">載入公告中...</p>');
        function facts(post) {
            return `<span>🎮 ${escapeHtml(post.gameName)}</span><span>⚔ ${escapeHtml(post.modeName || '尚未設定模式')}</span><span>🤖 電腦：${post.computerPlayers ?? '未設定'} 人</span><span>◷ 開始時間：${dateText(post.startTime)}</span><span>結束時間：${dateText(post.endTime)}</span><span>👥 ${post.currentPlayers}/${post.maxPlayers}</span><span>🎙 ${post.voiceRequired?'需要':'不需要'}</span><span>隊長：${escapeHtml(post.captain?.nickname)}</span>`;
        }
        function updatePost(post) {
            $('#detailTitle').text(post.title); $('#detailDescription').text(post.description);
            $('#postFacts').html(facts(post));
            $('#postStatus').attr('class','status '+String(post.status).toLowerCase()).text(statusText(post.status));
            $('#joinButton').prop('disabled',post.status!=='RECRUITING'||!post.modeId||post.captain?.id===currentUser()?.id || $('#joinButton').data('applied'));
            $('#detailRoomActions').html(roomButton(post));
        }
        function loadComments(lastPage=false) {
            wantLastCommentPage = wantLastCommentPage || lastPage;
            const search = ++commentVersion;
            api.comments(id,commentPage).done(data=>{
                if(version!==viewVersion || search!==commentVersion)return;
                if(wantLastCommentPage && data.totalPages>0 && commentPage!==data.totalPages-1){commentPage=data.totalPages-1;loadComments();return;}
                if(data.page>0 && data.page>=data.totalPages){commentPage=Math.max(0,data.totalPages-1);loadComments();return;}
                wantLastCommentPage=false;
                $('#commentHeading').text('留言（'+data.totalElements+'）');
                $('#commentList').html((data.content.length ? data.content.map(c=>`<div class="comment" id="comment-${c.id}"><div class="comment-head"><b>${escapeHtml(c.member?.nickname)}</b>${currentUser()?.id===c.member?.id?`<button class="danger delete-comment" data-id="${c.id}">刪除</button>`:''}</div><p>${escapeHtml(c.content)}</p><small>${dateText(c.createdAt)}</small></div>`).join('') : '<p class="empty">目前沒有留言</p>')+BoardUi.pagination(data,'comments'));
            }).fail(error=>{if(version===viewVersion)notify(error.message,true);});
        }
        api.post(id).done(post=>{
            if(version!==viewVersion)return;
            app.html(`<section><a class="primary" href="#posts">← 返回列表</a><div class="banner">🎮 ⚔️</div><div class="card detail-card"><div class="title-row" style="color:inherit"><h1 id="detailTitle" style="color:inherit"></h1><span id="postStatus"></span></div><div id="postFacts" class="facts"></div><p id="detailDescription"></p><div class="actions"><button id="joinButton" class="primary">我要加入</button><span id="detailRoomActions"></span><button id="favoriteButton" class="secondary">☆ 收藏／取消收藏</button><button id="shareButton" class="secondary">分享</button></div><form id="joinForm" class="hidden"><label>申請留言（可留空）<textarea name="message" maxlength="255" placeholder="向隊長介紹自己"></textarea></label><button class="primary" type="submit">送出加入申請</button></form></div><div class="card comments"><h2 id="commentHeading">留言</h2><div id="commentList"></div><form id="commentForm" class="comment-form"><input name="content" placeholder="輸入留言..." maxlength="1000" required><button class="primary">送出</button></form><p class="filter-hint">收藏或留言後，可在「我關注的公告」收到後續留言通知。</p></div></section>`);
            updatePost(post); loadComments();
            $('#joinButton').on('click',()=>{if(requireLogin())$('#joinForm').toggleClass('hidden').find('textarea').trigger('focus');});
            $('#joinForm').on('submit',function(event){
                event.preventDefault();if(!requireLogin())return;
                const form=$(this),button=form.find('button').prop('disabled',true);
                api.join(id,{memberId:currentUser().id,message:this.message.value.trim()}).done(()=>{
                    if(version!==viewVersion)return;
                    notify('申請已送出');form.addClass('hidden');$('#joinButton').data('applied',true).prop('disabled',true).text('已送出申請');
                }).fail(e=>notify(e.message,true)).always(()=>button.prop('disabled',false));
            });
            $('#favoriteButton').on('click',()=>{if(requireLogin())api.favorite(id,currentUser().id).done(r=>notify(r.favorite?'已收藏，將收到後續留言通知':'已取消收藏')).fail(e=>notify(e.message,true));});
            $('#shareButton').on('click',()=>{navigator.clipboard?.writeText(location.href);notify('網址已複製');});
            $('#commentForm').on('submit',function(event){
                event.preventDefault();if(!requireLogin())return;
                const field=this.content,content=field.value.trim(),button=$(this).find('button');
                if(!content)return;
                button.prop('disabled',true);
                api.comment(id,{memberId:currentUser().id,content}).done(()=>{if(version!==viewVersion)return;field.value='';loadComments(true);})
                    .fail(e=>notify(e.message,true)).always(()=>button.prop('disabled',false));
            });
            $('#commentList').on('click','.delete-comment',function(){if(confirm('確定刪除留言？'))api.deleteComment($(this).data('id')).done(()=>loadComments()).fail(e=>notify(e.message,true));});
            $('#commentList').on('click','.page-button',function(){wantLastCommentPage=false;commentPage=Number($(this).data('page'));loadComments();});
            refreshVisible=event=>{
                if(event.type!=='RESYNC' && String(event.postId)!==String(id))return;
                if(['COMMENTS_CHANGED','RESYNC'].includes(event.type))loadComments();
                if(['POST_CHANGED','RESYNC'].includes(event.type))api.post(id).done(p=>{if(version===viewVersion)updatePost(p);}).fail(e=>notify(e.message,true));
            };
        }).fail(error=>{if(version===viewVersion)app.html('<p class="error">'+escapeHtml(error.message)+'</p>');});
    }

    function postFormPage(id) {
        if (!requireLogin()) return;
        const isEdit = id !== 'new';
        app.html(`<section class="card form-card"><h1>${isEdit?'編輯公告':'建立隊伍'}</h1><p id="formMessage" class="error hidden"></p><form id="postForm"><div class="form-grid"><label>遊戲<select name="gameId" required disabled><option value="">載入遊戲中...</option></select></label><label>遊戲模式<select name="modeId" required disabled><option value="">請先選擇遊戲</option></select></label><label class="wide">遊玩人數（含隊長）<select name="playerCount" required disabled><option value="">請先選擇遊戲模式</option></select></label><div id="modeInfo" class="mode-info wide" aria-live="polite">請選擇遊戲與模式，系統會自動帶入人數。</div><label class="wide">公告標題<input name="title" maxlength="100" required></label><label>活動類型<input name="activityType" required></label><label>開始時間<input name="startTime" type="datetime-local" required></label><label>結束時間<input name="endTime" type="datetime-local"></label><label>語音需求<select name="voiceRequired"><option value="true">需要</option><option value="false">不需要</option></select></label><label>段位條件（選填）<input name="rankRequirement" placeholder="留空表示不限段位"></label><label class="wide">標籤<input name="tags" placeholder="新手友善,語音"></label><label class="wide">詳細說明<textarea name="description" required></textarea></label></div><div class="actions"><a class="secondary" href="#posts">取消</a><button class="primary" type="submit" disabled>${isEdit?'更新公告':'建立隊伍'}</button></div></form></section>`);
        const form = $('#postForm'), f = form[0], message = $('#formMessage'), info = $('#modeInfo');
        const button = form.find('button[type="submit"]');
        const label = isEdit ? '更新公告' : '建立隊伍';
        let ready = false, selectedMode = null, originalStart = null;
        f.startTime.min = BoardUi.nextMinute();
        function updateTimeLimits() {
            const unchangedPast = originalStart && f.startTime.value === originalStart.slice(0,16) && new Date(originalStart) < new Date();
            f.startTime.min = unchangedPast ? originalStart.slice(0,16) : BoardUi.nextMinute();
            f.endTime.min = f.startTime.value || f.startTime.min;
        }
        $(f.startTime).on("focus input change",updateTimeLimits);
        $(f.endTime).on("focus",updateTimeLimits);
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
                if (post.startTime) { originalStart=post.startTime; f.startTime.value=post.startTime.slice(0,16); }
                updateTimeLimits();
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
            const startTime = originalStart && f.startTime.value === originalStart.slice(0,16) ? originalStart : f.startTime.value;
            const timeError = BoardUi.timeError(startTime,f.endTime.value,originalStart);
            if(timeError){message.removeClass('hidden').text(timeError);updateTimeLimits();return;}
            const data = {title:f.title.value.trim(), gameId:Number(f.gameId.value), modeId:Number(f.modeId.value), playerCount:Number(f.playerCount.value), activityType:f.activityType.value.trim(), startTime, endTime:f.endTime.value||null, voiceRequired:f.voiceRequired.value==='true', rankRequirement:f.rankRequirement.value.trim() || null, description:f.description.value.trim(), tags:f.tags.value.trim(), captainId:currentUser().id};
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
        const version = viewVersion;
        let requestVersion = 0;
        app.html(`<section><h1 class="page-title">${title}</h1><div id="manageList" class="card manager"><p class="loading">載入中...</p></div></section>`);
        const load = function () {
            const sequence = ++requestVersion;
            loader().done(data=>{
                if(version===viewVersion && sequence===requestVersion) $('#manageList').html(data?.length?data.map(renderer).join(''):'<p class="table-empty">目前沒有資料</p>');
            }).fail(error=>{if(version===viewVersion)$('#manageList').html('<p class="error">'+escapeHtml(error.message)+'</p>');});
        };
        refreshVisible=event=>{if(['POST_CHANGED','RESYNC'].includes(event.type))load();};
        load();
    }

    function applicationsPage(){const user=currentUser();listPage('我的申請',()=>api.myApplications(user.id),x=>`<div class="manage-row"><div><b><a href="#post/${x.post?.id}">${escapeHtml(x.post?.title)}</a></b><p>${escapeHtml(x.post?.gameName)}・${escapeHtml(x.post?.modeName)}・${dateText(x.createdAt)}</p></div><div><span class="status ${String(x.status).toLowerCase()}">${statusText(x.status)}</span> ${x.status==='APPROVED'?roomButton(x.post):''}</div></div>`);}

    function favoritesPage(){const user=currentUser();listPage('我的收藏',()=>api.favorites(user.id),x=>postCard(x.post));}
    function notificationsPage(){
        if(!requireLogin())return;
        const version=viewVersion;
        app.html('<section><div class="title-row"><h1>我的通知</h1><button id="refreshNotices" class="secondary">重新整理</button></div><p id="noticeSummary" class="notice-summary">載入未讀通知中...</p><div class="notice-tabs" role="tablist" aria-label="通知分類">'+Object.entries(categoryLabels).map(([key,label])=>'<button class="notice-tab" role="tab" data-category="'+key+'" aria-selected="'+(key===noticeState.category)+'">'+label+' <span class="notification-badge" data-count="'+key+'">0</span></button>').join('')+'</div><p id="categoryHint" class="notice-summary"></p><div class="actions"><a id="noticeManageLink" class="secondary" href="#captain">管理我的隊伍</a><button id="readPage" class="secondary" disabled>本頁全部標為已讀</button></div><div id="noticeList" class="card manager"><p>載入中...</p></div></section>');
        let requestVersion=0,visibleIds=[];
        function load(){
            const sequence=++requestVersion;
            const hints={CAPTAIN:'你擔任隊長的公告：加入申請、隊員狀態與新留言。',APPLICANT:'你已申請或加入的隊伍：審核結果、開始遊戲與新留言。',WATCHING:'你收藏或曾留言、尚未申請加入的公告：後續新留言。'};
            $('#categoryHint').text(hints[noticeState.category]);
            $('#noticeManageLink').attr('href',noticeState.category==='CAPTAIN'?'#captain':noticeState.category==='APPLICANT'?'#applications':'#favorites').text(noticeState.category==='CAPTAIN'?'管理我的隊伍':noticeState.category==='APPLICANT'?'查看我的申請':'查看收藏');
            $('.notice-tab').attr('aria-selected','false').filter('[data-category="'+noticeState.category+'"]').attr('aria-selected','true');
            api.notifications(noticeState.category,noticeState.page).done(data=>{
                if(version!==viewVersion||sequence!==requestVersion)return;
                if(data.page>0&&data.page>=data.totalPages){noticeState.page=Math.max(0,data.totalPages-1);load();return;}
                visibleIds=data.content.filter(n=>!n.readFlag).map(n=>n.id);
                $('#readPage').prop('disabled',!visibleIds.length);
                $('#noticeList').html((data.content.length?data.content.map(n=>`<article class="notice ${n.readFlag?'':'unread'}"><b>${n.commentId?'💬':'🔔'} ${escapeHtml(n.title)} ${n.readFlag?'':'<span class="unread-label">未讀</span>'}</b><p>${escapeHtml(n.message)}</p><small>${dateText(n.createdAt)}</small><div class="actions">${n.postId?`<a class="secondary notice-link" data-id="${n.id}" href="#post/${n.postId}">查看公告與留言</a>`:''}${!n.readFlag?`<button class="secondary read-notice" data-id="${n.id}">標為已讀</button>`:''}</div></article>`).join(''):'<p class="empty">此分類目前沒有通知</p>')+BoardUi.pagination(data,'notices'));
            }).fail(e=>{if(version===viewVersion)$('#noticeList').html('<p class="error">'+escapeHtml(e.message)+'</p>');});
            refreshSummary();
        }
        function read(ids){api.readNotices(ids).done(()=>{if(version===viewVersion)load();scheduleSummary();}).fail(e=>notify(e.message,true));}
        $('.notice-tab').on('click',function(){noticeState.category=$(this).data('category');noticeState.page=0;load();});
        $('#noticeList').on('click','.page-button',function(){noticeState.page=Number($(this).data('page'));load();});
        $('#noticeList').on('click','.read-notice',function(){read([Number($(this).data('id'))]);});
        $('#noticeList').on('click','.notice-link',function(){read([Number($(this).data('id'))]);});
        $('#readPage').on('click',()=>read(visibleIds));$('#refreshNotices').on('click',load);
        refreshVisible=event=>{if(['NOTIFICATION','NOTIFICATIONS_READ','RESYNC'].includes(event.type))load();};
        load();
    }
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
        const version=viewVersion;
        if (!requireLogin()) return;
        const user = currentUser();
        app.html(`<section><div class="title-row"><h1>${showNotices ? '我的通知' : '隊長管理'}</h1><button class="secondary refresh-team">重新整理</button></div><div id="teamManager"><p class="loading">載入隊伍中...</p></div>${showNotices ? '<div id="noticeList" class="card manager"><h2>通知紀錄</h2></div>' : ''}</section>`);
        const manager = $('#teamManager'), noticeList = $('#noticeList');
        refreshVisible = event => { if (['POST_CHANGED','RESYNC'].includes(event.type) && version===viewVersion) renderTeamManager(false); };
        $.when(api.captainPosts(user.id), api.captainRequests(user.id)).done(function(postResult, requestResult) {
            const posts = Array.isArray(postResult) && postResult.length === 3 && typeof postResult[1] === 'string' ? postResult[0] : postResult;
            const requests = Array.isArray(requestResult) && requestResult.length === 3 && typeof requestResult[1] === 'string' ? requestResult[0] : requestResult;
            manager.html(posts.length ? posts.map(p => teamManagementCard(p, requests)).join('') : '<p class="empty">你目前沒有擔任隊長的隊伍。</p>');
        }).fail(e => manager.html(`<p class="error">${escapeHtml(e.message)}</p>`));

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
        viewVersion++;
        refreshVisible = null;
        if (!sessionReady) { app.html('<p class="loading">確認會員登入狀態中...</p>'); return; }
        if (sessionError) {
            app.html(`<section class="card"><h1>無法載入會員資料</h1><p class="error">${escapeHtml(sessionError)}</p><button class="primary retry-session">重新連線</button></section>`);
            return;
        }
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
    }

    function loadSession() {
        if (sessionVersion && !sessionReady && sessionToken === UserApi.getToken()) return;
        const version = ++sessionVersion;
        realtime.stop();
        clearTimeout(summaryTimer);
        sessionToken = UserApi.getToken();
        verifiedMember = null;
        sessionReady = !sessionToken;
        sessionError = '';
        renderMember();
        route();
        if (!sessionToken) { realtime.connect(''); return; }
        UserApi.checkLogin().then(() => UserApi.getBoardSession()).done(function(member) {
            if (version !== sessionVersion) return;
            verifiedMember = member;
            sessionReady = true;
            realtime.connect(sessionToken);
            scheduleSummary();
            renderMember();
            route();
        }).fail(function(xhr) {
            if (version !== sessionVersion) return;
            if (xhr.status === 401 || xhr.status === 403) { UserApi.clearLoginSession(); return; }
            sessionReady = true;
            sessionError = xhr.responseJSON?.message || '無法連線至會員服務，請確認後端已啟動後重試。';
            renderMember();
            route();
        });
    }

    $(document).on('click','.retry-session',loadSession);
    $(document).on('click','.protected-link',function(event){if(!currentUser()){event.preventDefault();requireLogin($(this).attr('href'));}});
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
    $('#quickBattle').on('click',function(){api.posts('','RECRUITING').done(function(posts){if(posts?.length){const post=posts[Math.floor(Math.random()*posts.length)];location.hash=`#post/${post.id}`;}else notify('目前沒有招募中的公告',true);}).fail(e=>notify(e.message,true));});
    $(window).on('hashchange',route);
    $(window).on('user-session-changed',function(){
        if (sessionToken !== UserApi.getToken()) loadSession();
        else renderMember();
    });
    $(window).on('storage',function(event){
        if (event.originalEvent.key === 'token' || event.originalEvent.key === null) loadSession();
    });
    $(window).on('focus',function(){
        if (sessionToken !== UserApi.getToken() || sessionError) { loadSession(); return; }
        // 返回分頁時只確認登入，不重繪表單，以保留尚未送出的輸入。
        if (sessionToken && sessionReady) UserApi.checkLogin().fail(function(){});
    });
    setInterval(function(){if(UserApi.getToken() && !UserApi.isLoggedIn()) UserApi.clearLoginSession();},30000);
    $(window).on('pagehide',()=>realtime.stop());
    $(window).on('pageshow',event=>{if(event.originalEvent?.persisted)loadSession();});
    loadSession();
})(jQuery);
