(function (window) {
    'use strict';
    function localInput(date) {
        const pad = n => String(n).padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }
    function nextMinute(now = new Date()) { return localInput(new Date(Math.floor(now.getTime()/60000)*60000+60000)); }
    function timeError(start, end, original, now = new Date()) {
        if (!start || Number.isNaN(new Date(start).getTime())) return '請選擇開始時間';
        if (start !== original && new Date(start).getTime() < now.getTime()) return '開始時間不能選擇過去的時間';
        if (end && (Number.isNaN(new Date(end).getTime()) || new Date(end) < new Date(start))) return '結束時間不能早於開始時間';
        return '';
    }
    function pagination(data, target) {
        if (!data.totalElements) return '';
        const page = data.page, total = data.totalPages;
        return `<nav class="pagination" aria-label="分頁"><button class="secondary page-button" data-target="${target}" data-page="${page-1}" ${page===0?'disabled':''}>上一頁</button><span>第 ${page+1}／${total} 頁 · 共 ${data.totalElements} 筆（每頁 10 筆）</span><button class="secondary page-button" data-target="${target}" data-page="${page+1}" ${page+1>=total?'disabled':''}>下一頁</button></nav>`;
    }
    window.BoardUi = {nextMinute, timeError, pagination};
})(window);
