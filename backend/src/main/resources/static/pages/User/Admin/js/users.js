$(function () {
    let allUsers = [];
    let editingId = null;

    const $modal = $('#userModal');
    const $form = $('#userForm');
    const $message = $('#modalMessage');

    function getErrorMessage(xhr, fallback) {
        if (xhr && xhr.responseJSON) {
            return xhr.responseJSON.detail || xhr.responseJSON.message || xhr.responseJSON.error || fallback;
        }
        return fallback;
    }

    function setModalMessage(text, type) {
        $message.removeClass('error success').text(text || '');
        if (type) $message.addClass(type);
    }

    function escapeHtml(value) {
        return $('<div>').text(value == null ? '' : String(value)).html();
    }

    function renderUsers() {
        const search = $('#userSearchInput').val().trim().toLowerCase();
        const role = $('#roleFilter').val();
        const status = $('#statusFilter').val();

        const filtered = allUsers.filter(function (user) {
            const matchesSearch = !search
                || String(user.account || '').toLowerCase().includes(search)
                || String(user.username || '').toLowerCase().includes(search);
            const matchesRole = role === 'ALL' || user.role === role;
            const matchesStatus = status === 'ALL' || user.status === status;
            return matchesSearch && matchesRole && matchesStatus;
        });

        $('#userCountText').text(filtered.length + ' Users');

        if (!filtered.length) {
            $('#usersTableBody').html('<tr><td colspan="7" class="empty-cell">找不到符合條件的使用者。</td></tr>');
            return;
        }

        const rows = filtered.map(function (user) {
            const roleClass = String(user.role).toLowerCase();
            const statusClass = String(user.status).toLowerCase();
            return `
                <tr>
                    <td>#${escapeHtml(user.id)}</td>
                    <td><strong>${escapeHtml(user.account)}</strong></td>
                    <td>${escapeHtml(user.username)}</td>
                    <td><span class="badge ${roleClass}">${escapeHtml(user.role)}</span></td>
                    <td><span class="badge ${statusClass}">${escapeHtml(user.status)}</span></td>
                    <td>${escapeHtml(user.lastLogin || '—')}</td>
                    <td>
                        <div class="action-group">
                            <button class="action-btn view-user" type="button" data-id="${escapeHtml(user.id)}">查看</button>
                            <button class="action-btn edit-user" type="button" data-id="${escapeHtml(user.id)}">編輯</button>
                            <button class="action-btn danger delete-user" type="button" data-id="${escapeHtml(user.id)}" data-account="${escapeHtml(user.account)}">刪除</button>
                        </div>
                    </td>
                </tr>`;
        }).join('');

        $('#usersTableBody').html(rows);
    }

    function loadUsers() {
        if ((localStorage.getItem('role') || '').toUpperCase() !== 'ADMIN') return;

        $('#userTableMessage').text('載入中…');
        UserApi.getUsers()
            .done(function (data) {
                allUsers = Array.isArray(data) ? data : [];
                renderUsers();
                $('#userTableMessage').text('已更新');
                setTimeout(function () { $('#userTableMessage').text(''); }, 1500);
            })
            .fail(function (xhr) {
                allUsers = [];
                renderUsers();
                $('#userTableMessage').text(getErrorMessage(xhr, '無法取得使用者列表。'));
            });
    }

    function resetForm() {
        editingId = null;
        $form[0].reset();
        $('#editingUserId').val('');
        $('#modalTitle').text('新增使用者');
        $('#passwordHint').text('新增時必填，至少 8 個字元');
        $('#userPassword').attr('required', true);
        setModalMessage('', null);
    }

    function openModal(mode, user) {
        resetForm();

        if (mode === 'edit' && user) {
            editingId = user.id;
            $('#editingUserId').val(user.id);
            $('#modalTitle').text('編輯使用者');
            $('#passwordHint').text('留白代表不修改密碼');
            $('#userPassword').removeAttr('required');
            $('#userAccount').val(user.account || '');
            $('#userUsername').val(user.username || '');
            $('#userRole').val(user.role || 'PLAYER');
            $('#userStatus').val(user.status || 'Active');
            $('#userAvatar').val(user.avatar || '');
            $('#userDescription').val(user.description || '');
        }

        $modal.prop('hidden', false);
        $('#userAccount').trigger('focus');
    }

    function closeModal() {
        $modal.prop('hidden', true);
        resetForm();
    }

    function findUser(id) {
        return allUsers.find(function (user) { return String(user.id) === String(id); });
    }

    function loadOneUser(id, onSuccess) {
        UserApi.getUser(id)
            .done(onSuccess)
            .fail(function (xhr) {
                setModalMessage(getErrorMessage(xhr, '讀取使用者資料失敗。'), 'error');
            });
    }

    $('#createUserBtn').on('click', function () {
        openModal('create');
    });

    $('#closeModalBtn, #cancelModalBtn').on('click', closeModal);
    $modal.on('click', function (event) {
        if (event.target === this) closeModal();
    });

    $('#userSearchInput, #roleFilter, #statusFilter').on('input change', renderUsers);
    $('#refreshUsersBtn').on('click', loadUsers);

    $('#usersTableBody').on('click', '.view-user', function () {
        const id = $(this).data('id');
        loadOneUser(id, function (user) {
            const details = [
                'Account: ' + (user.account || '—'),
                'Username: ' + (user.username || '—'),
                'Role: ' + (user.role || '—'),
                'Status: ' + (user.status || '—'),
                'Last Login: ' + (user.lastLogin || '—'),
                'Description: ' + (user.description || '—')
            ].join('\n');
            window.alert(details);
        });
    });

    $('#usersTableBody').on('click', '.edit-user', function () {
        const id = $(this).data('id');
        const localUser = findUser(id);
        openModal('edit', localUser || { id: id });
        if (!localUser) {
            loadOneUser(id, function (user) {
                openModal('edit', user);
            });
        }
    });

    $('#usersTableBody').on('click', '.delete-user', function () {
        const id = $(this).data('id');
        const account = $(this).data('account');
        if (!window.confirm('確定要刪除使用者「' + account + '」嗎？')) return;

        UserApi.deleteUser(id)
            .done(function () {
                loadUsers();
            })
            .fail(function (xhr) {
                window.alert(getErrorMessage(xhr, '刪除失敗。'));
            });
    });

    $form.on('submit', function (event) {
        event.preventDefault();
        setModalMessage('', null);

        const account = $('#userAccount').val().trim();
        const username = $('#userUsername').val().trim();
        const password = $('#userPassword').val();

        if (!account || !username) {
            setModalMessage('Account 與 Username 為必填。', 'error');
            return;
        }

        if (!editingId && password.length < 8) {
            setModalMessage('新增使用者時，Password 至少需要 8 個字元。', 'error');
            return;
        }

        if (editingId && password && password.length < 8) {
            setModalMessage('若要修改 Password，至少需要 8 個字元。', 'error');
            return;
        }

        const data = {
            account: account,
            password: password || null,
            username: username,
            avatar: $('#userAvatar').val().trim() || null,
            description: $('#userDescription').val().trim() || null,
            role: $('#userRole').val(),
            status: $('#userStatus').val()
        };

        const $saveButton = $('#saveUserBtn');
        $saveButton.prop('disabled', true).text(editingId ? '更新中…' : '建立中…');

        const request = editingId ? UserApi.updateUser(editingId, data) : UserApi.createUser(data);
        request
            .done(function () {
                setModalMessage(editingId ? '更新成功。' : '建立成功。', 'success');
                setTimeout(function () {
                    closeModal();
                    loadUsers();
                }, 350);
            })
            .fail(function (xhr) {
                setModalMessage(getErrorMessage(xhr, '儲存失敗。'), 'error');
            })
            .always(function () {
                $saveButton.prop('disabled', false).text('儲存');
            });
    });

    window.UserManagement = { load: loadUsers };

    loadUsers();
});
