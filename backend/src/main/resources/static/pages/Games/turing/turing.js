/* global $ */
const API_BASE = '/api/game'; 
let currentAccount = "訪客";
let currentRoomId = null; 
let diffChar = 'B'; 

let activePuzzle = null;
let currentPuzzleId = null; 
let currentRound = 1;
let testsThisRound = 0;
let totalTests = 0;

let gameHistoryRounds = [];
let cardLabelsCache = [];
let isProposalLocked = false; 

// 🌟 多人同步變數
let roomPlayersCount = 1; 
let roomSocket = null;
let loggedReadyPlayers = new Set(); 
let submittedPlayers = new Set(); 
let hasSubmitted = false;         
let roomPlayerNames = {}; 

// 🌟 1. 新增：開放給外層 chatclient 呼叫的發送訊息 API
window.sendRoomChatMessage = function(text) {
    if (roomSocket && roomSocket.readyState === WebSocket.OPEN) {
        const currentUsername = localStorage.getItem("username") || localStorage.getItem("account") || "訪客";
        roomSocket.send(JSON.stringify({
            type: "ROOM_CHAT",
            roomId: currentRoomId,
            userName: currentUsername,
            message: text
        }));
    }
};

$(document).ready(function() {
    const token = localStorage.getItem("token"); 

    if (!token) {
        alert("請先登入會員！");
        window.location.href = '../../User/login.html'; 
        return;
    }

    try {
        const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
        currentAccount = payload.sub || payload.account || "未知玩家"; 
        
        const displayUsername = localStorage.getItem("username") || payload.name || payload.username || currentAccount;
        $('#playerName').text(displayUsername); 
    } catch (e) {
        alert("登入狀態異常，請重新登入！");
        window.location.href = '../../User/login.html';
        return;
    }

    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + token);
        }
    });

    const urlParams = new URLSearchParams(location.search);
    currentRoomId = urlParams.get('room') || ""; 
    
    if (currentRoomId) {
        $('#roomIdDisplay').text(currentRoomId);
        
        // 🌟 2. 修正：只呼叫 showTab() 喚醒聊天介面，不要呼叫 startGameChannel 建立重複連線
        if (window.parent && window.parent.RoomChatManager && typeof window.parent.RoomChatManager.showTab === 'function') {
            window.parent.RoomChatManager.showTab();
        }
        
        $.ajax({
            url: `/api/lobby/room/${currentRoomId}`,
            type: 'GET',
            success: function(res) {
                if (res.success && res.room) {
                    if (res.room.players) roomPlayersCount = res.room.players.length;
                    roomPlayerNames = res.room.playerNames || {}; 
                }
                connectWebSocket(); 
            }
        });
    } else {
        connectWebSocket(); 
    }

    initNotepad();
    $('#guessCard').draggable();
    $('#resultCard').draggable();

    $('#btnNextRound').on('click', function() {
        if ($(this).prop('disabled')) return; 

        $(this).prop('disabled', true).text('等待其他玩家...')
               .removeClass('bg-slate-800 hover:bg-slate-700').addClass('bg-slate-600 cursor-not-allowed');
        $('#blueInput, #yellowInput, #purpleInput').prop('disabled', true).addClass('opacity-60 cursor-not-allowed');
        isProposalLocked = true;
        
        $.ajax({
            url: `${API_BASE}/ready`,
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ roomId: currentRoomId, round: currentRound, playerName: currentAccount, totalPlayers: roomPlayersCount })
        });
    });
    
    $('#btnSubmitDecode').on('click', function() {
        const current = getCurrentProposal();
        $('#finalBlue').val(current.blue);
        $('#finalYellow').val(current.yellow);
        $('#finalPurple').val(current.purple);
        $('#finalGuessModal').removeClass('hidden');
    });

    $('#confirmFinalSubmit').on('click', function() {
        if (!confirm("確定要用這組密碼作為最終答案送出嗎？")) return;

        $('#finalGuessModal').addClass('hidden');

        const proposal = { blue: parseInt($('#finalBlue').val()), yellow: parseInt($('#finalYellow').val()), purple: parseInt($('#finalPurple').val()) };
        const requestData = {
            roomId: currentRoomId,     
            playerName: currentAccount,
            currentRound: currentRound,
            totalTests: totalTests,
            b: proposal.blue, y: proposal.yellow, p: proposal.purple,
            totalPlayers: roomPlayersCount
        };

        $.ajax({
            url: `${API_BASE}/submit`,
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function(result) {
                if(result.status === "WAITING") {
                    hasSubmitted = true; 
                    logAction("✅ 答案已提交！等待其他玩家完成...", "text-fuchsia-400 font-bold text-sm");
                    $('#btnNextRound, #btnSubmitDecode').prop('disabled', true).addClass('opacity-50');
                    $('.verifier-card button').prop('disabled', true).addClass('opacity-50');
                }
            }
        });
    });

    $('#btnLeave').on('click', function() {
        if(confirm("確定要放棄這局並離開嗎？")) {
            $(this).prop('disabled', true).text('離開中...');
            $.ajax({
                url: `/api/game/clear-cache/${currentRoomId}`,
                type: 'DELETE',
                complete: function() {
                    $.ajax({
                        url: `/api/lobby/room/${currentRoomId}/abandon`, 
                        type: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({ playerAccount: currentAccount }),
                        complete: function() { window.location.href = '../../Lobby/jquery_lobby.html'; }
                    });
                }
            });
        }
    });

    startGame();
});

// ==========================================
// 🌟 建立 WebSocket 與處理伺服器推播
// ==========================================
function connectWebSocket() {
    const wsProtocol = location.protocol === 'https:' ? 'wss' : 'ws';
    roomSocket = new WebSocket(`${wsProtocol}://${location.host}/ws/room/${currentRoomId}?player=${encodeURIComponent(currentAccount)}`);
    
    roomSocket.onmessage = function(event) {
        const msg = JSON.parse(event.data);
        
        // 🌟 3. 關鍵修復：攔截伺服器廣播的聊天訊息，轉發給外層的聊天室 UI
        if (msg.type === 'ROOM_CHAT' || (msg.userName && msg.message)) {
            if (window.parent && window.parent.RoomChatManager) {
                window.parent.RoomChatManager.appendMessage(msg.userName, msg.message, msg.userName === '系統');
            }
            return; // 處理完聊天就終止，不往下執行當作遊戲指令
        }
        
        if (msg.type === 'ROUND_READY' && msg.round === currentRound) {
            if (!submittedPlayers.has(msg.playerName) && !loggedReadyPlayers.has(msg.playerName)) {
                loggedReadyPlayers.add(msg.playerName);
                const activeCount = roomPlayersCount - submittedPlayers.size;
                const displayName = roomPlayerNames[msg.playerName] || msg.playerName; 
                logAction(`等待中... 玩家 [${displayName}] 已結束本輪！( ${loggedReadyPlayers.size} / ${activeCount} )`, "text-indigo-300");
            }
            checkRoundAdvance();
        } 
        else if (msg.type === 'PLAYER_SUBMITTED') {
            const displayName = roomPlayerNames[msg.playerName] || msg.playerName; 
            logAction(`🔔 玩家 [${displayName}] 已經提交最終答案！`, "text-amber-400 font-bold");
            submittedPlayers.add(msg.playerName);
            loggedReadyPlayers.delete(msg.playerName); 
            checkRoundAdvance();
        }
        else if (msg.type === 'GAME_OVER') {
            $('#resultTitle').text(msg.title).addClass(msg.title.includes("🎉") ? "text-emerald-400" : "text-red-500");
            
            let finalMsg = msg.message;
            Object.keys(roomPlayerNames).forEach(acc => {
                finalMsg = finalMsg.replace(acc, roomPlayerNames[acc]);
            });
            $('#resultMsg').text(finalMsg);
            
            $('#resultModal').removeClass('hidden');
        }
    };
}

function checkRoundAdvance() {
    if (hasSubmitted) return; 

    const activeCount = roomPlayersCount - submittedPlayers.size;
    
    if (activeCount > 0 && loggedReadyPlayers.size >= activeCount) {
        executeNextRound();
    }
}

function executeNextRound() {
    currentRound++;
    testsThisRound = 0;
    isProposalLocked = false; 
    loggedReadyPlayers.clear(); 

    $('#btnNextRound').prop('disabled', false).text('結束本輪')
           .removeClass('bg-slate-600 cursor-not-allowed').addClass('bg-slate-800 hover:bg-slate-700');
    $('#blueInput, #yellowInput, #purpleInput').prop('disabled', false).removeClass('opacity-60 cursor-not-allowed');
    
    updateStatsUI();
    logAction(`還在場上的玩家已準備完畢，進入第 ${currentRound} 輪！`, "text-emerald-400 font-bold text-sm");
}

function startGame() {
    logAction(`正在為您準備謎題...`, "text-cyan-400");
    $.ajax({
        url: `${API_BASE}/new?roomId=${currentRoomId}`,
        type: 'GET',
        success: function(response) {
            activePuzzle = response.puzzle;
            currentPuzzleId = activePuzzle.puzzleId; 
            const difficultyNum = response.difficulty || 5; 
            $('#difficultyDisplay').text(`${difficultyNum} 張卡`);
            if (difficultyNum === 4) diffChar = 'A'; else if (difficultyNum === 6) diffChar = 'C'; else diffChar = 'B';
            
            const labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            cardLabelsCache = activePuzzle.verifiers.map((card, index) => ({
                cardId: card.cardId, label: labels.charAt(index % 26)
            }));
            renderVerifierCards(activePuzzle.verifiers);
            renderMatrixTable();
            $('#gameBoard').removeClass('hidden');
            logAction(`謎題載入成功！共有 ${activePuzzle.verifiers.length} 張驗證卡。`, "text-emerald-400");
        }
    });
}

function renderVerifierCards(verifiers) {
    const $container = $('#verifierCardsContainer');
    $container.empty();
    $container.removeClass('grid-cols-2 grid-cols-3');
    if (verifiers.length <= 4) $container.addClass('grid-cols-2'); else $container.addClass('grid-cols-3');
    
    const labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    verifiers.forEach((card, index) => {
        const label = labels.charAt(index % 26);
        const imagePath = `/assets/Games/turing/cards/TM_GameCards_CNT-${card.cardId}.png`;
        const cardHtml = `
            <div id="card-${card.cardId}" class="verifier-card bg-slate-900 rounded-xl p-2 shadow-lg border-2 border-slate-700 flex flex-col justify-between transition relative w-full">
                <div class="flex justify-between items-center mb-1 px-1">
                    <span class="text-[11px] text-slate-400 font-bold">卡片 #${card.cardId}</span>
                    <span class="bg-slate-800 text-emerald-400 px-2 py-0.5 rounded-full font-bold text-[11px] border border-emerald-500/50 shadow">機台 ${label}</span>
                </div>
                <div class="card-img-wrapper relative cursor-crosshair rounded-lg overflow-hidden border border-slate-600 bg-white w-full max-h-[280px] flex items-center justify-center">
                    <img src="${imagePath}" class="w-full h-auto object-contain block select-none pointer-events-none max-h-[280px]">
                </div>
                <div class="mt-1.5">
                    <button onclick="verifyCard(${card.cardId}, '${label}')" class="w-full py-1 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded shadow transition text-[11px] flex items-center justify-center gap-1">
                        <span>🔍 驗證此卡</span>
                    </button>
                </div>
            </div>`;
        $container.append(cardHtml);
    });

    $('.card-img-wrapper').off('click').on('click', function(e) {
        if ($(e.target).hasClass('card-cross-mark')) { $(e.target).remove(); return; }
        const rect = this.getBoundingClientRect();
        const $cross = $('<div></div>').addClass('card-cross-mark').text('✕')
            .css({ left: `${e.clientX - rect.left}px`, top: `${e.clientY - rect.top}px` });
        $(this).append($cross);
    });
}

function verifyCard(cardId, label) {
    if (testsThisRound >= 3) { alert("本輪已測試 3 次，請結束本輪！"); return; }
    const proposal = getCurrentProposal();
    if (!isProposalLocked) {
        isProposalLocked = true;
        $('#blueInput, #yellowInput, #purpleInput').prop('disabled', true).addClass('opacity-60 cursor-not-allowed');
    }
    const cardEl = $(`#card-${cardId}`);
    
    $.ajax({
        url: `${API_BASE}/verify`, type: 'POST', contentType: 'application/json',
        data: JSON.stringify({ roomId: currentRoomId, proposal: proposal, cardId: cardId }),
        success: function(isValid) {
            testsThisRound++; totalTests++; updateStatsUI();
            const proposalStr = `${proposal.blue}-${proposal.yellow}-${proposal.purple}`;
            cardEl.removeClass('verify-pass verify-fail');
            if (isValid) {
                cardEl.addClass('verify-pass'); logAction(`[${proposalStr}] 測 ${label} ➔ <span class="text-emerald-400 font-bold">✔ 符合</span>`);
            } else {
                cardEl.addClass('verify-fail'); logAction(`[${proposalStr}] 測 ${label} ➔ <span class="text-red-400 font-bold">✘ 不符合</span>`);
            }
            setTimeout(() => cardEl.removeClass('verify-pass verify-fail'), 1500);
            recordMatrixResult(proposal, label, isValid);
        }
    });
}

function recordMatrixResult(proposal, cardLabel, isValid) {
    let existingRound = gameHistoryRounds.find(r => r.round === currentRound);
    if (!existingRound) {
        existingRound = { round: currentRound, proposal: proposal, results: {} };
        gameHistoryRounds.push(existingRound);
    } else { existingRound.proposal = proposal; }
    existingRound.results[cardLabel] = isValid;
    renderMatrixTable();
}

function renderMatrixTable() {
    const $headerTr = $('#matrixHeaderTr');
    let headersHtml = `<th class="p-0.5 border-r border-slate-300 text-blue-600 font-bold w-6">▲</th><th class="p-0.5 border-r border-slate-300 text-yellow-600 font-bold w-6">■</th><th class="p-0.5 border-r border-slate-400 text-purple-600 font-bold w-6">●</th>`;
    cardLabelsCache.forEach(c => { headersHtml += `<th class="p-0.5 border-r border-slate-200 font-bold text-slate-700">${c.label}</th>`; });
    $headerTr.html(headersHtml);

    const $tbody = $('#turingMatrixBody');
    $tbody.empty();
    gameHistoryRounds.forEach((round) => {
        let rowHtml = `<tr class="border-b border-slate-200"><td class="p-1 border-r border-slate-200 font-bold text-blue-600 bg-slate-100">${round.proposal.blue}</td><td class="p-1 border-r border-slate-200 font-bold text-yellow-600 bg-slate-100">${round.proposal.yellow}</td><td class="p-1 border-r border-slate-400 font-bold text-purple-600 bg-slate-100">${round.proposal.purple}</td>`;
        cardLabelsCache.forEach(c => {
            let status = round.results[c.label];
            let mark = '', bgClass = '';
            if (status === true) { mark = '✔'; bgClass += ' bg-emerald-100 text-emerald-600 font-bold'; } 
            else if (status === false) { mark = '✘'; bgClass += ' bg-red-100 text-red-500 font-bold'; }
            rowHtml += `<td class="p-1 border-r border-slate-200 ${bgClass}">${mark}</td>`;
        });
        rowHtml += `</tr>`;
        $tbody.append(rowHtml);
    });

    let emptyRowsNeeded = Math.max(0, 5 - gameHistoryRounds.length);
    for (let i = 0; i < emptyRowsNeeded; i++) {
        let emptyRow = `<tr class="border-b border-slate-200 h-6"><td class="p-1 border-r border-slate-200 text-slate-300">-</td><td class="p-1 border-r border-slate-200 text-slate-300">-</td><td class="p-1 border-r border-slate-400 text-slate-300 text-[10px]">-</td>`;
        cardLabelsCache.forEach(() => { emptyRow += `<td class="p-1 border-r border-slate-200"></td>`; });
        emptyRow += `</tr>`;
        $tbody.append(emptyRow);
    }
}

function initNotepad() {
    const $tbody = $('#notepadBody');
    $tbody.empty();
    for (let i = 1; i <= 5; i++) {
        $tbody.append(`<tr class="border-b border-slate-200"><td class="p-1 border-r border-slate-200 note-cell note-empty font-bold text-slate-800" data-val="0" data-num="${i}">${i}</td><td class="p-1 border-r border-slate-200 note-cell note-empty font-bold text-slate-800" data-val="0" data-num="${i}">${i}</td><td class="p-1 note-cell note-empty font-bold text-slate-800" data-val="0" data-num="${i}">${i}</td></tr>`);
    }
    $('.note-cell').off('click').on('click', function() {
        let val = parseInt($(this).attr('data-val'));
        let num = $(this).attr('data-num');
        val = (val + 1) % 3; 
        $(this).attr('data-val', val).removeClass('note-empty note-o note-x');
        if (val === 1) $(this).addClass('note-o').text('O');
        else if (val === 2) $(this).addClass('note-x').text('X');
        else $(this).addClass('note-empty').text(num);
    });
}

function updateStatsUI() { $('#currentRound').text(currentRound); $('#testsThisRound').text(`${testsThisRound} / 3`); }
function getCurrentProposal() { return { blue: parseInt($('#blueInput').val()), yellow: parseInt($('#yellowInput').val()), purple: parseInt($('#purpleInput').val()) }; }
function logAction(msg, cssClass = "text-slate-300") { $('#actionLog').prepend(`<div class="${cssClass} pb-0.5 border-b border-slate-700/50">▶ ${msg}</div>`); }

// 🌟 4. 新增：當離開遊戲（不論是按離開按鈕、還是結算後返回大廳），自動通知外層隱藏聊天室
window.addEventListener('pagehide', function() {
    if (window.parent && window.parent.RoomChatManager && typeof window.parent.RoomChatManager.hideTab === 'function') {
        window.parent.RoomChatManager.hideTab();
    }
});