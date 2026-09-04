$(function () {
    const $dashboardPage = $('#dashboardPage');
    const $usersPage = $('#usersPage');
    const $logsPage = $('#logsPage');
    const $pageTitle = $('#pageTitle');

    function getErrorMessage(xhr, fallback) {
        if (xhr && xhr.responseJSON) {
            return xhr.responseJSON.detail || xhr.responseJSON.message || xhr.responseJSON.error || fallback;
        }
        return fallback;
    }

    function requireAdmin() {
        const token = UserApi.getToken();
        const role = (localStorage.getItem('role') || '').toUpperCase();
        if (!token || role !== 'ADMIN') {
            $dashboardPage.find('.info-card').html(
                '<div><span class="section-kicker">ACCESS DENIED</span><h3>需要 ADMIN 權限</h3><p>請先使用管理員帳號登入，再進入使用者管理。</p></div>'
            );
            $('#refreshDashboardBtn').hide();
            return false;
        }
        return true;
    }

    function loadDashboard() {
        if (!requireAdmin()) return;

        UserApi.getDashboard()
            .done(function (data) {
                $('#totalUsers').text(data.totalUsers ?? 0);
                $('#activeUsers').text(data.activeUsers ?? 0);
                $('#disabledUsers').text(data.disabledUsers ?? 0);
                $('#adminUsers').text(data.adminUsers ?? 0);
                $('#todayOperations').text(data.todayOperations ?? 0);
            })
            .fail(function (xhr) {
                $('#todayOperations').text('—');
                if (xhr.status === 401 || xhr.status === 403) {
                    $('#dashboardPage .info-card p').text(getErrorMessage(xhr, '目前沒有權限讀取 Dashboard。'));
                }
            });
    }

    function showPage(page) {
        $dashboardPage.removeClass('active-page');
        $usersPage.removeClass('active-page');
        $logsPage.removeClass('active-page');
        $('.nav-item').removeClass('active');

        if (page === 'dashboard') {
            $dashboardPage.addClass('active-page');
            $('.nav-item[data-page="dashboard"]').addClass('active');
            $pageTitle.text('Dashboard');
            loadDashboard();
            return;
        }

        if (page === 'logs') {
            $logsPage.addClass('active-page');
            $('.nav-item[data-page="logs"]').addClass('active');
            $pageTitle.text('Operation Logs');
            if (window.OperationLogs && typeof window.OperationLogs.load === 'function') {
                window.OperationLogs.load();
            }
            return;
        }

        $usersPage.addClass('active-page');
        $('.nav-item[data-page="users"]').addClass('active');
        $pageTitle.text('Users');
        if (window.UserManagement && typeof window.UserManagement.load === 'function') {
            window.UserManagement.load();
        }
    }

    function initNavigation() {
        $('.nav-item').on('click', function (event) {
            event.preventDefault();
            const page = $(this).data('page');
            const url = new URL(window.location.href);
            url.searchParams.set('tab', page);
            history.replaceState({}, '', url);
            showPage(page);
        });

        const requestedPage = new URLSearchParams(window.location.search).get('tab') || 'users';
        showPage(['dashboard', 'users', 'logs'].includes(requestedPage) ? requestedPage : 'users');
    }

    $('#operatorName').text(localStorage.getItem('username') || 'Admin');
    $('#operatorAccount').text(localStorage.getItem('account') || 'ADMIN');

    $('#refreshDashboardBtn').on('click', loadDashboard);

    $('#backLobbyBtn').on('click', function (event) {
        event.preventDefault();
        window.location.href = '/pages/Lobby/jquery_lobby.html';
    });

    $('#logoutBtn').on('click', function () {
        UserApi.clearLoginSession();
        window.location.href = '/pages/Lobby/jquery_lobby.html';
    });

    initNavigation();
});
