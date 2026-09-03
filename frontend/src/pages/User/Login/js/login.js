$(function () {
    const returnUrl = UserApi.getLoginReturnUrl();
    $('.back-note strong').text(/\/Board\//i.test(new URL(returnUrl).pathname) ? '組隊公告' : 'Lobby');

    const $loginTab = $('#loginTab');
    const $registerTab = $('#registerTab');
    const $loginPanel = $('#loginPanel');
    const $registerPanel = $('#registerPanel');
    const $message = $('#authMessage');

    function setMessage(text, type) {
        $message.removeClass('error success').text(text || '');
        if (type) {
            $message.addClass(type);
        }
    }

    function switchMode(mode) {
        const isLogin = mode === 'login';
        $loginTab.toggleClass('active', isLogin);
        $registerTab.toggleClass('active', !isLogin);
        $loginPanel.toggleClass('active-panel', isLogin);
        $registerPanel.toggleClass('active-panel', !isLogin);
        setMessage('', null);
    }

    function getErrorMessage(xhr, fallback) {
        if (xhr && xhr.responseJSON) {
            if (xhr.responseJSON.detail) return xhr.responseJSON.detail;
            if (xhr.responseJSON.message) return xhr.responseJSON.message;
            if (xhr.responseJSON.error) return xhr.responseJSON.error;
        }
        return fallback;
    }

    function setButtonLoading($button, loadingText, isLoading) {
        $button.prop('disabled', isLoading);
        $button.find('span:first').text(isLoading ? loadingText : $button.data('default-text'));
    }

    $('#loginBtn').data('default-text', '登入');
    $('#registerBtn').data('default-text', '建立帳號');

    $loginTab.on('click', function () { switchMode('login'); });
    $registerTab.on('click', function () { switchMode('register'); });

    $('#loginForm').on('submit', function (event) {
        event.preventDefault();
        setMessage('', null);

        const account = $('#loginAccount').val().trim();
        const password = $('#loginPassword').val();

        if (!account || !password) {
            setMessage('請輸入 Account 與 Password。', 'error');
            return;
        }

        const $button = $('#loginBtn');
        setButtonLoading($button, '登入中...', true);

        UserApi.login(account, password)
            .done(function (response) {
                UserApi.saveLoginSession(response);
                setMessage('登入成功，正在返回頁面…', 'success');
                window.location.href = returnUrl;
            })
            .fail(function (xhr) {
                setMessage(getErrorMessage(xhr, '登入失敗，請確認帳號密碼或後端服務。'), 'error');
            })
            .always(function () {
                setButtonLoading($button, '登入中...', false);
            });
    });

    $('#registerForm').on('submit', function (event) {
        event.preventDefault();
        setMessage('', null);

        const account = $('#registerAccount').val().trim();
        const username = $('#registerUsername').val().trim();
        const password = $('#registerPassword').val();
        const avatar = $('#registerAvatar').val().trim();
        const description = $('#registerDescription').val().trim();

        if (!account || !username || !password) {
            setMessage('Account、Username 與 Password 為必填。', 'error');
            return;
        }

        if (password.length < 8) {
            setMessage('Password 至少需要 8 個字元。', 'error');
            return;
        }

        const $button = $('#registerBtn');
        setButtonLoading($button, '建立中...', true);

        UserApi.register({
            account: account,
            password: password,
            username: username,
            avatar: avatar || null,
            description: description || null
        })
            .done(function () {
                setMessage('註冊成功，請使用新帳號登入。', 'success');
                $('#loginAccount').val(account);
                $('#loginPassword').val('');
                $('#registerForm')[0].reset();
                switchMode('login');
                $('#loginAccount').val(account);
                $('#loginPassword').focus();
            })
            .fail(function (xhr) {
                setMessage(getErrorMessage(xhr, '註冊失敗，請確認資料或後端服務。'), 'error');
            })
            .always(function () {
                setButtonLoading($button, '建立中...', false);
            });
    });

    if (new URLSearchParams(window.location.search).get('mode') === 'register') switchMode('register');
});
