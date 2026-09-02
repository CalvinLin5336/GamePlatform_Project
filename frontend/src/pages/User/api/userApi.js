(function (window, $) {
    'use strict';

    const API_BASE = 'http://localhost:8080';

    function getToken() {
        return localStorage.getItem('token') || '';
    }

    function request(options) {
        const settings = $.extend(true, {}, options);
        settings.url = API_BASE + settings.url;
        settings.headers = $.extend({}, settings.headers || {});

        const token = getToken();
        if (token) {
            settings.headers.Authorization = 'Bearer ' + token;
        }

        return $.ajax(settings);
    }

    function saveLoginSession(response) {
        localStorage.setItem('token', response.token || '');
        localStorage.setItem('userId', response.userId != null ? String(response.userId) : '');
        localStorage.setItem('account', response.account || '');
        localStorage.setItem('username', response.username || '');
        localStorage.setItem('role', response.role || '');
        localStorage.setItem('status', response.status || '');
    }

    function clearLoginSession() {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('account');
        localStorage.removeItem('username');
        localStorage.removeItem('role');
        localStorage.removeItem('status');
    }

    function login(account, password) {
        return request({
            url: '/api/user/auth/login',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ account: account, password: password })
        });
    }

    function register(data) {
        return request({
            url: '/api/user/auth/register',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data)
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
        getOperationLogs: getOperationLogs
    };
})(window, jQuery);
