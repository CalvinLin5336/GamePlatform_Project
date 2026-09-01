(function ($) {
    'use strict';

    const API_BASE = `http://${window.location.hostname || 'localhost'}:8080/board`;
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
        return ({RECRUITING:'招募中',FULL:'已滿',FINISHED:'已結束',CLOSED:'已關閉',PENDING:'等待審核',APPROVED:'已同意',REJECTED:'已拒絕',CANCELLED:'已取消'})[status] || status || '未指定';
    }

    function request(method, path, data) {
        return $.ajax({
            method: method,
            url: API_BASE + path,
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
        posts: (keyword='', status='') => request('GET', `/team-posts?keyword=${encodeURIComponent(keyword)}${status ? '&status=' + encodeURIComponent(status) : ''}`),
        post: id => request('GET', `/team-posts/${id}`),
        createPost: data => request('POST', '/team-posts', data),
        updatePost: (id,data) => request('PUT', `/team-posts/${id}`, data),
        deletePost: id => $.ajax({method:'DELETE',url:API_BASE+`/team-posts/${id}`}),
        captainPosts: id => request('GET', `/team-posts/captain/${id}`),
        join: (id,data) => request('POST', `/team-posts/${id}/join`, data),
        myApplications: id => request('GET', `/applications/member/${id}`),
        captainRequests: id => request('GET', `/applications/captain/${id}`),
        review: (id,status) => request('PUT', `/applications/${id}/${status}`),
        comments: id => request('GET', `/team-posts/${id}/comments`),
        comment: (id,data) => request('POST', `/team-posts/${id}/comments`, data),
        deleteComment: id => $.ajax({method:'DELETE',url:API_BASE+`/comments/${id}`}),
        favorite: (postId,memberId) => request('POST', `/team-posts/${postId}/favorite/${memberId}`),
        favorites: id => request('GET', `/favorites/member/${id}`),
        notifications: id => request('GET', `/notifications/member/${id}`),
        confirmTeam: (postId,captainId) => request('POST', `/game-rooms/team-posts/${postId}/confirm?captainId=${captainId}`),
        roomByPost: postId => request('GET', `/game-rooms/team-posts/${postId}`),
        roomMembers: roomId => request('GET', `/game-rooms/${roomId}/members`),
        startGame: (roomId,captainId) => request('PUT', `/game-rooms/${roomId}/start?captainId=${captainId}`)
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
            <div><div class="meta"><span class="chip">🎮 ${escapeHtml(post.gameName)}</span><span class="chip">${escapeHtml(post.activityType)}</span></div>
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

    function postsPage() {
        app.html(`<section><div class="title-row"><div><h1>組隊公告</h1><p>搜尋適合你的遊戲隊伍</p></div><a class="primary protected-link" href="#form/new">＋建立公告</a></div><div class="list-layout"><aside class="card filter"><h3>搜尋／篩選</h3><label>關鍵字<input id="keyword" placeholder="遊戲、活動或關鍵字"></label><label>狀態<select id="status"><option value="">全部</option><option value="RECRUITING">招募中</option><option value="FULL">已滿</option><option value="FINISHED">已結束</option></select></label><button id="searchButton" class="primary" type="button">搜尋</button></aside><div id="postList" class="posts"><p class="loading">載入中...</p></div></div></section>`);
        const load = function () {
            $('#postList').html('<p class="loading">載入中...</p>');
            api.posts($('#keyword').val(), $('#status').val()).done(function (data) {
                $('#postList').html(data && data.length ? data.map(postCard).join('') : '<p class="card empty">目前沒有公告</p>');
            }).fail(error => $('#postList').html(`<p class="error">${escapeHtml(error.message)}</p>`));
        };
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
            app.html(`<section><a class="primary" href="#posts">← 返回列表</a><div class="banner">🎮 ⚔️</div><div class="card detail-card"><div class="title-row" style="color:inherit"><h1 style="color:inherit">${escapeHtml(post.title)}</h1><span class="status ${status}">${statusText(post.status)}</span></div><div class="facts"><span>🎮 ${escapeHtml(post.gameName)}</span><span>▣ ${escapeHtml(post.activityType)}</span><span>◷ ${dateText(post.startTime)}</span><span>👥 ${post.currentPlayers}/${post.maxPlayers}</span><span>🎙 ${post.voiceRequired?'需要':'不需要'}</span><span>隊長：${escapeHtml(post.captain?.nickname)}</span></div><p>${escapeHtml(post.description)}</p><div class="actions"><button id="joinButton" class="primary">我要加入</button><button id="favoriteButton" class="secondary">☆ 收藏</button><button id="shareButton" class="secondary">分享</button></div></div><div class="card comments"><h2>留言（${comments?.length || 0}）</h2><div id="commentList">${comments?.length ? comments.map(comment => `<div class="comment"><div class="comment-head"><b>${escapeHtml(comment.member?.nickname)}</b>${currentUser()?.id===comment.member?.id?`<button class="danger delete-comment" data-id="${comment.id}">刪除</button>`:''}</div><p>${escapeHtml(comment.content)}</p></div>`).join('') : '<p class="empty">目前沒有留言</p>'}</div><form id="commentForm" class="comment-form"><input name="content" placeholder="輸入留言..." required><button class="primary">送出</button></form></div></section>`);
            $('#joinButton').on('click', function () { if(!requireLogin())return; const message=window.prompt('請輸入申請留言（可留空）')||''; api.join(id,{memberId:currentUser().id,message}).done(()=>notify('申請已送出')).fail(e=>notify(e.message,true)); });
            $('#favoriteButton').on('click', function () { if(!requireLogin())return; api.favorite(id,currentUser().id).done(r=>notify(r.favorite?'已加入收藏':'已取消收藏')).fail(e=>notify(e.message,true)); });
            $('#shareButton').on('click', function () { navigator.clipboard?.writeText(location.href); notify('網址已複製'); });
            $('#commentForm').on('submit', function(event){event.preventDefault();if(!requireLogin())return;api.comment(id,{memberId:currentUser().id,content:this.content.value.trim()}).done(()=>postDetailPage(id)).fail(e=>notify(e.message,true));});
            $('.delete-comment').on('click', function(){if(confirm('確定刪除留言？'))api.deleteComment($(this).data('id')).done(()=>postDetailPage(id));});
        }).fail(error => app.html(`<p class="error">${escapeHtml(error.message)}</p>`));
    }

    function postFormPage(id) {
        if (!requireLogin()) return;
        const isEdit = id !== 'new';
        app.html(`<section class="card form-card"><h1>${isEdit?'編輯公告':'建立公告'}</h1><p id="formMessage" class="error hidden"></p><form id="postForm"><div class="form-grid"><label>公告標題<input name="title" required></label><label>遊戲名稱<select name="gameName" required><option value="撲克牌">撲克牌</option><option value="問答遊戲">問答遊戲</option><option value="圖靈解密">圖靈解密</option></select></label><label>活動類型<input name="activityType" required></label><label>最大人數<input name="maxPlayers" type="number" min="2" value="5" required></label><label>開始時間<input name="startTime" type="datetime-local" required></label><label>結束時間<input name="endTime" type="datetime-local"></label><label>語音需求<select name="voiceRequired"><option value="true">需要</option><option value="false">不需要</option></select></label><label>段位條件<input name="rankRequirement"></label><label class="wide">標籤<input name="tags" placeholder="新手友善,語音"></label><label class="wide">詳細說明<textarea name="description" required></textarea></label></div><div class="actions"><a class="secondary" href="#posts">取消</a><button class="primary">${isEdit?'更新公告':'發佈公告'}</button></div></form></section>`);
        if (isEdit) api.post(id).done(function(post){const f=$('#postForm')[0];Object.keys(post).forEach(key=>{if(f.elements[key])$(f.elements[key]).val(typeof post[key]==='boolean'?String(post[key]):post[key]);});if(post.startTime)f.startTime.value=post.startTime.slice(0,16);if(post.endTime)f.endTime.value=post.endTime.slice(0,16);});
        $('#postForm').on('submit', function(event){event.preventDefault();const f=this;const data={title:f.title.value.trim(),gameName:f.gameName.value,activityType:f.activityType.value.trim(),startTime:f.startTime.value,endTime:f.endTime.value||null,maxPlayers:Number(f.maxPlayers.value),voiceRequired:f.voiceRequired.value==='true',rankRequirement:f.rankRequirement.value.trim(),description:f.description.value.trim(),tags:f.tags.value.trim(),status:'RECRUITING',captainId:currentUser().id};const action=isEdit?api.updatePost(id,data):api.createPost(data);action.done(post=>location.hash=`#post/${post.id}`).fail(error=>$('#formMessage').removeClass('hidden').text(error.message));});
    }

    function listPage(title, loader, renderer) {
        if (!requireLogin()) return;
        app.html(`<section><h1 class="page-title">${title}</h1><div id="manageList" class="card manager"><p class="loading">載入中...</p></div></section>`);
        loader().done(data=>$('#manageList').html(data?.length?data.map(renderer).join(''):'<p class="table-empty">目前沒有資料</p>')).fail(error=>$('#manageList').html(`<p class="error">${escapeHtml(error.message)}</p>`));
    }

    function applicationsPage(){const user=currentUser();listPage('我的申請',()=>api.myApplications(user.id),x=>`<div class="manage-row"><div><b>${escapeHtml(x.post?.title)}</b><p>${escapeHtml(x.post?.gameName)}・${dateText(x.createdAt)}</p></div><span class="status ${String(x.status).toLowerCase()}">${statusText(x.status)}</span></div>`);}
    function favoritesPage(){const user=currentUser();listPage('我的收藏',()=>api.favorites(user.id),x=>postCard(x.post));}
    function notificationsPage(){const user=currentUser();listPage('我的通知',()=>api.notifications(user.id),x=>`<div class="notice"><b>🔔 ${escapeHtml(x.title)}</b><p>${escapeHtml(x.message)}</p><small>${dateText(x.createdAt)}</small></div>`);}

    function captainPage(){if(!requireLogin())return;const user=currentUser();app.html('<section><div class="title-row"><h1>隊長管理</h1><a class="primary" href="#form/new">＋建立公告</a></div><div id="requests" class="card manager"><h2>申請審核</h2><p class="loading">載入中...</p></div><div id="myPosts" class="card manager"><h2>我的公告</h2><p class="loading">載入中...</p></div></section>');api.captainRequests(user.id).done(data=>$('#requests').html(`<h2>申請審核</h2>${data.length?data.map(r=>`<div class="manage-row"><div><b>${escapeHtml(r.applicant?.nickname)}</b><p>申請加入「${escapeHtml(r.post?.title)}」</p></div><div>${r.status==='PENDING'?`<button class="ok review" data-id="${r.id}" data-status="APPROVED">同意</button> <button class="danger review" data-id="${r.id}" data-status="REJECTED">拒絕</button>`:`<span class="status ${String(r.status).toLowerCase()}">${statusText(r.status)}</span>`}</div></div>`).join(''):'<p class="table-empty">目前沒有申請</p>'}`));api.captainPosts(user.id).done(data=>$('#myPosts').html(`<h2>我的公告</h2>${data.length?data.map(p=>`<div class="manage-row"><div><b>${escapeHtml(p.title)}</b><p>${p.currentPlayers}/${p.maxPlayers}・${statusText(p.status)}</p></div><div><a class="secondary" href="#form/${p.id}">編輯</a> <button class="ok confirm-team" data-id="${p.id}">確認組隊</button> <button class="danger delete-post" data-id="${p.id}">刪除</button></div></div>`).join(''):'<p class="table-empty">目前沒有公告</p>'}`));}

    function roomPage(roomId){if(!requireLogin())return;app.html('<p class="loading">載入遊戲房中...</p>');request('GET',`/game-rooms/${roomId}`).done(function(room){const isLeader=currentUser()?.id===room.captainId;app.html(`<section class="card manager"><div class="title-row"><div><h1>遊戲房 #${room.roomId}</h1><p>${escapeHtml(room.title)}・${escapeHtml(room.gameName)}</p></div><span class="status">${escapeHtml(room.status)}</span></div><h2>房間成員</h2>${room.members.map(m=>`<div class="manage-row"><div><b>${escapeHtml(m.nickname)}</b><p>${escapeHtml(m.account)}・Lv.${m.level||1}</p></div><span class="chip">${m.role}</span></div>`).join('')}<div class="actions">${isLeader&&room.status==='WAITING'?'<button id="startGameButton" class="primary">開始遊戲</button>':''}<a class="secondary" href="#captain">返回隊長管理</a></div></section>`);$('#startGameButton').on('click',function(){api.startGame(room.roomId,currentUser().id).done(()=>roomPage(room.roomId)).fail(e=>notify(e.message,true));});}).fail(e=>app.html(`<p class="error">${escapeHtml(e.message)}</p>`));}

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
        else if(parts[0]==='room'&&parts[1])roomPage(parts[1]);
        else notFoundPage();
        $('#mainNav').removeClass('open');
    }

    $(document).on('click','#logoutButton',function(){saveUser(null);location.hash='#home';notify('已登出');});
    $(document).on('click','.protected-link',function(event){if(!currentUser()){event.preventDefault();requireLogin();}});
    $(document).on('click','.review',function(){api.review($(this).data('id'),$(this).data('status')).done(captainPage).fail(e=>notify(e.message,true));});
    $(document).on('click','.confirm-team',function(){const postId=$(this).data('id');api.confirmTeam(postId,currentUser().id).done(room=>{notify('已建立遊戲房');location.hash=`#room/${room.roomId}`;}).fail(e=>notify(e.message,true));});
    $(document).on('click','.delete-post',function(){if(confirm('確定刪除公告？'))api.deletePost($(this).data('id')).done(captainPage).fail(e=>notify(e.message,true));});
    $('#menuButton').on('click',()=>$('#mainNav').toggleClass('open'));
    $('#quickBattle').on('click',function(){api.posts('','RECRUITING').done(function(posts){if(posts?.length){const post=posts[Math.floor(Math.random()*posts.length)];location.hash=`#post/${post.id}`;}else notify('目前沒有招募中的公告',true);}).fail(e=>notify(e.message,true));});
    $(window).on('hashchange',route);
    renderMember();
    route();
})(jQuery);
