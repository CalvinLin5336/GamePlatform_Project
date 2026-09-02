$(function () {
    function escapeHtml(value) {
        return $('<div>').text(value == null ? '' : String(value)).html();
    }

    function getErrorMessage(xhr, fallback) {
        if (xhr && xhr.responseJSON) {
            return xhr.responseJSON.detail || xhr.responseJSON.message || xhr.responseJSON.error || fallback;
        }
        return fallback;
    }

    function actionClass(action) {
        const normalized = String(action || '').toUpperCase();
        if (normalized === 'DELETE') return 'danger';
        return '';
    }

    function loadLogs() {
        if ((localStorage.getItem('role') || '').toUpperCase() !== 'ADMIN') return;

        $('#logsTableBody').html('<tr><td colspan="6" class="empty-cell">載入中…</td></tr>');

        UserApi.getOperationLogs()
            .done(function (logs) {
                const items = Array.isArray(logs) ? logs : [];
                $('#logCountText').text(items.length + ' Operation Logs');

                if (!items.length) {
                    $('#logsTableBody').html('<tr><td colspan="6" class="empty-cell">目前沒有操作紀錄。</td></tr>');
                    return;
                }

                const rows = items.map(function (log) {
                    return `
                        <tr>
                            <td>${escapeHtml(log.createdAt || '—')}</td>
                            <td><strong>${escapeHtml(log.account || '—')}</strong></td>
                            <td><span class="badge ${actionClass(log.action)}">${escapeHtml(log.action || '—')}</span></td>
                            <td>${log.targetId == null ? '—' : '#' + escapeHtml(log.targetId)}</td>
                            <td>${escapeHtml(log.role || '—')}</td>
                            <td>${escapeHtml(log.description || '—')}</td>
                        </tr>`;
                }).join('');

                $('#logsTableBody').html(rows);
            })
            .fail(function (xhr) {
                $('#logsTableBody').html('<tr><td colspan="6" class="empty-cell">' + escapeHtml(getErrorMessage(xhr, '無法取得操作紀錄。')) + '</td></tr>');
            });
    }

    $('#refreshLogsBtn').on('click', loadLogs);
    window.OperationLogs = { load: loadLogs };
});
