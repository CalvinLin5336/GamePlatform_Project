(function (window, $) {
    'use strict';

    // API 一律連到目前提供前端頁面的主機，方便本機與區網共用。
    const API_BASE = 'http://' + window.location.hostname + ':8080';
    const sessionKeys = ['token', 'userId', 'account', 'username', 'role', 'status'];
    // Live Server 可從 frontend、專案根目錄或 pages 啟動，頁面網址不可寫死在網站根目錄。
    const pagesPath = window.location.pathname.match(/^(.*\/)(?:Board|Lobby|Chat|User)(?:\/|$)/i)?.[1] || '/src/pages/';
    const apiScriptUrl = window.document?.currentScript?.src
        || new URL('User/api/userApi.js', new URL(pagesPath, window.location.origin)).href;
    const userModuleUrl = new URL('../', apiScriptUrl);

    function getToken() {
        return localStorage.getItem('token') || '';
    }

    function request(options) {
        const settings = $.extend(true, {}, options);
        settings.url = API_BASE + settings.url;
        settings.headers = $.extend({}, settings.headers || {});

        const token = settings.auth === false ? '' : getToken();
        delete settings.auth;
        if (token) {
            settings.headers.Authorization = 'Bearer ' + token;
        }

        return $.ajax(settings).fail(function (xhr) {
            if (xhr.status === 401 && token && getToken() === token) clearLoginSession();
        });
    }

    // 前端狀態只用於畫面；checkLogin() 仍會交由後端驗證簽章與帳號狀態。
    function getLoginSession() {
        const token = getToken();
        const userId = Number(localStorage.getItem('userId'));
        const account = localStorage.getItem('account') || '';
        try {
            const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
            const bytes = window.atob(part.padEnd(Math.ceil(part.length / 4) * 4, '='));
            const claims = JSON.parse(decodeURIComponent(Array.from(bytes, c => '%' + c.charCodeAt(0).toString(16).padStart(2, '0')).join('')));
            if (!claims.exp || claims.exp * 1000 <= Date.now() || claims.sub !== account || Number(claims.userId) !== userId) return null;
        } catch (_) { return null; }
        if (!Number.isSafeInteger(userId) || userId <= 0 || !account || (localStorage.getItem('status') || '').toLowerCase() !== 'active') return null;
        return {userId, account, username:localStorage.getItem('username') || account,
            role:localStorage.getItem('role') || '', status:localStorage.getItem('status') || ''};
    }

    function isLoggedIn() { return getLoginSession() !== null; }

    function sessionChanged() { window.dispatchEvent(new CustomEvent('user-session-changed')); }

    function checkLogin() {
        const token = getToken();
        if (!token) return $.Deferred().reject({status:401, responseJSON:{message:'請先登入會員'}}).promise();
        return request({url:'/api/user/auth/me', method:'GET'}).then(function (user) {
            if (getToken() !== token) return $.Deferred().reject({status:409, sessionChanged:true}).promise();
            saveLoginSession({token, userId:user.id, account:user.account, username:user.username, role:user.role, status:user.status});
            return getLoginSession();
        }, function (xhr) {
            if (xhr.status === 403 && getToken() === token) clearLoginSession();
            return $.Deferred().reject(xhr).promise();
        });
    }

    function getBoardSession() {
        const token = getToken();
        return request({url:'/board/auth/session', method:'POST'}).then(function (member) {
            if (!token || getToken() !== token) return $.Deferred().reject({status:409, sessionChanged:true}).promise();
            localStorage.setItem('boardMember', JSON.stringify(member));
            return member;
        });
    }

    function redirectToLogin(returnTo, mode) {
        const url = new URL('Login/login.html', userModuleUrl);
        url.searchParams.set('returnTo', returnTo || window.location.href);
        if (mode === 'register') url.searchParams.set('mode', 'register');
        window.location.assign(url.href);
    }

    function getLoginReturnUrl() {
        const fallback = new URL('../Lobby/jquery_lobby.html', userModuleUrl);
        const loginPath = new URL('Login/login.html', userModuleUrl).pathname.toLowerCase();
        try {
            const value = new URL(window.location.href).searchParams.get('returnTo');
            const target = value ? new URL(value, window.location.origin) : fallback;
            if (target.origin === window.location.origin && target.pathname.toLowerCase() !== loginPath) return target.href;
        } catch (_) { /* 非本站網址使用預設大廳。 */ }
        return fallback.href;
    }

    function saveLoginSession(response) {
        if (getToken() !== (response.token || '') || localStorage.getItem('userId') !== String(response.userId)) {
            localStorage.removeItem('boardMember');
        }
        localStorage.removeItem('sgpUser');
        localStorage.setItem('token', response.token || '');
        localStorage.setItem('userId', response.userId != null ? String(response.userId) : '');
        localStorage.setItem('account', response.account || '');
        localStorage.setItem('username', response.username || '');
        localStorage.setItem('role', response.role || '');
        localStorage.setItem('status', response.status || '');
        sessionChanged();
    }

    function clearLoginSession() {
        sessionKeys.concat(['boardMember', 'sgpUser']).forEach(key => localStorage.removeItem(key));
        sessionChanged();
    }

    function login(account, password) {
        return request({
            url: '/api/user/auth/login',
            auth: false,
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ account: account, password: password })
        });
    }

    function register(data) {
        return request({
            url: '/api/user/auth/register',
            auth: false,
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data)
        });
    }

    function getPlayerProfile() {
        return request({
            url: '/api/user/player/me',
            method: 'GET'
        });
    }

    function updatePlayerProfile(data) {
        return request({
            url: '/api/user/player/me',
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(data)
        });
    }

    function deletePlayerAccount() {
        return request({
            url: '/api/user/player/me',
            method: 'DELETE'
        });
    }

    function getDashboard() {
        return request({
            url: '/api/user/admin/dashboard',
            method: 'GET'
        });
    }

    function getUsers() {
        return request({
            url: '/api/user/admin/users',
            method: 'GET'
        });
    }

    function getUser(id) {
        return request({
            url: '/api/user/admin/users/' + encodeURIComponent(id),
            method: 'GET'
        });
    }

    function createUser(data) {
        return request({
            url: '/api/user/admin/users',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data)
        });
    }

    function updateUser(id, data) {
        return request({
            url: '/api/user/admin/users/' + encodeURIComponent(id),
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(data)
        });
    }

    function deleteUser(id) {
        return request({
            url: '/api/user/admin/users/' + encodeURIComponent(id),
            method: 'DELETE'
        });
    }

    function getOperationLogs() {
        return request({
            url: '/api/user/admin/operation-logs',
            method: 'GET'
        });
    }

    window.UserApi = {
        API_BASE: API_BASE,
        getToken: getToken,
        request: request,
        getLoginSession: getLoginSession,
        getCurrentUser: getLoginSession,
        isLoggedIn: isLoggedIn,
        checkLogin: checkLogin,
        getBoardSession: getBoardSession,
        redirectToLogin: redirectToLogin,
        getLoginReturnUrl: getLoginReturnUrl,
        saveLoginSession: saveLoginSession,
        clearLoginSession: clearLoginSession,
        login: login,
        register: register,
        getDashboard: getDashboard,
        getUsers: getUsers,
        getUser: getUser,
        createUser: createUser,
        updateUser: updateUser,
        deleteUser: deleteUser,
        getOperationLogs: getOperationLogs,
        getPlayerProfile: getPlayerProfile,
        updatePlayerProfile: updatePlayerProfile,
        deletePlayerAccount: deletePlayerAccount
    };
})(window, jQuery);
