$(function () {
    const $message = $('#message');

    function setMessage(text, type) {
        $message.removeClass('success error').text(text || '');
        if (type) $message.addClass(type);
    }

    function getErrorMessage(xhr, fallback) {
        if (xhr && xhr.responseJSON) {
            if (xhr.responseJSON.detail) return xhr.responseJSON.detail;
            if (xhr.responseJSON.message) return xhr.responseJSON.message;
            if (xhr.responseJSON.error) return xhr.responseJSON.error;
        }
        return fallback;
    }

    function loadProfile() {
        UserApi.getPlayerProfile()
            .done(function (user) {
                $('#account').val(user.account || '');
                $('#username').val(user.username || '');
                currentAvatar = user.avatar || '';
                setAvatar(user.avatar);
                $('#description').val(user.description || '');
                $('#lastLogin').text(user.lastLogin || '尚未登入');
                $('#status').text(user.status || '');
                setAvatar(user.avatar);
            })
            .fail(function (xhr) {
                setMessage(getErrorMessage(xhr, '無法取得會員資料。'), 'error');
                if (xhr.status === 401 || xhr.status === 403) {
                    UserApi.redirectToLogin(window.location.href);
                }
            });
    }

    function setAvatar(path) {
        const value = (path || '').trim();
        $('#avatarPreview').attr('src', value || 'avatar/user.png').on('error', function () {
            this.onerror = null;
            this.src = 'avatar/user.png';
        });
    }

    let currentAvatar = '';

    $('#avatarFile').on('change', function () {
        const file = this.files && this.files[0];
        if (!file) return;
        if (!['image/png', 'image/jpeg'].includes(file.type)) {
            setMessage('Avatar 只允許 PNG 或 JPG/JPEG。', 'error');
            this.value = '';
            return;
        }
        if (file.size > 2 * 1024 * 1024) {
            setMessage('Avatar 檔案大小不可超過 2 MB。', 'error');
            this.value = '';
            return;
        }
        const reader = new FileReader();
        reader.onload = function () {
            currentAvatar = reader.result;
            setAvatar(currentAvatar);
            setMessage('', null);
        };
        reader.onerror = function () {
            setMessage('Avatar 讀取失敗。', 'error');
            $('#avatarFile').val('');
        };
        reader.readAsDataURL(file);
    });

    $('#profileForm').on('submit', function (event) {
        event.preventDefault();
        setMessage('', null);

        const account = $('#account').val().trim();
        const username = $('#username').val().trim();

        if (!account || !username) {
            setMessage('Account 與 Username 為必填。', 'error');
            return;
        }

        UserApi.updatePlayerProfile({
            account: account,
            username: username,
            avatar: currentAvatar || null,
            description: $('#description').val().trim() || null
        })
        .done(function (response) {
            // Account 修改後，後端會回傳新的 JWT；直接更新目前登入 session。
            UserApi.saveLoginSession(response);
            setMessage('資料更新成功。', 'success');
            $('#status').text(response.status || 'Active');
            loadProfile();
        })
        .fail(function (xhr) {
            setMessage(getErrorMessage(xhr, '更新失敗，請確認資料。'), 'error');
        });
    });

    $('#deleteBtn').on('click', function () {
        if (!window.confirm('確定要停用自己的帳號嗎？停用後將無法登入。')) return;

        UserApi.deletePlayerAccount()
            .done(function () {
                UserApi.clearLoginSession();
                window.location.assign(UserApi.getLoginReturnUrl());
            })
            .fail(function (xhr) {
                setMessage(getErrorMessage(xhr, '帳號停用失敗。'), 'error');
            });
    });

    $('#logoutBtn').on('click', function () {
        UserApi.clearLoginSession();
        UserApi.redirectToLogin(window.location.href);
    });

    loadProfile();
});
