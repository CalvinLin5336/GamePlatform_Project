/* global $ */
const API_BASE = '/api/game'; 
let currentAccount = "訪客";
let diffChar = 'B'; 

let activePuzzle = null;
let currentPuzzleId = null; 
let currentRound = 1;
let testsThisRound = 0;
let totalTests = 0;

let gameHistoryRounds = [];
let cardLabelsCache = [];

$(document).ready(function() {
    currentAccount = localStorage.getItem("account") || localStorage.getItem("username") || "訪客";
    $('#playerName').text(currentAccount);

    const urlParams = new URLSearchParams(location.search);
    const difficultyParam = urlParams.get('difficulty') || urlParams.get('mode') || '4'; 
    
    if (difficultyParam === '4') diffChar = 'A';
    else if (difficultyParam === '6') diffChar = 'C';
    else diffChar = 'B'; 

    initNotepad();

    $('#btnNextRound').on('click', handleNextRound);
    $('#btnSubmitDecode').on('click', handleSubmitDecode);
    $('#btnLeave').on('click', function() {
        if(confirm("確定要放棄這局並離開嗎？")) {
            window.location.href = 'jquery_lobby.html';
        }
    });

    startGame(diffChar);
});

function startGame(difficulty) {
    logAction(`正在為您準備 [${difficulty}] 難度的謎題...`, "text-cyan-400");
    
    $.ajax({
        url: `${API_BASE}/new?difficulty=${difficulty}`,
        type: 'GET',
        success: function(puzzle) {
            activePuzzle = puzzle;
            currentPuzzleId = puzzle.puzzleId; 
            
            const labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            cardLabelsCache = puzzle.verifiers.map((card, index) => ({
                cardId: card.cardId,
                label: labels.charAt(index % 26)
            }));

            renderVerifierCards(puzzle.verifiers);
            renderMatrixTable();
            $('#gameBoard').removeClass('hidden');
            logAction(`謎題載入成功！共有 ${puzzle.verifiers.length} 張驗證卡。`, "text-emerald-400");
        },
        error: function(xhr) {
            alert("載入遊戲失敗，請檢查後端是否啟動！");
            console.error(xhr);
        }
    });
}

function renderVerifierCards(verifiers) {
    const $container = $('#verifierCardsContainer');
    $container.empty();
    
    $container.removeClass('grid-cols-2 grid-cols-3');
    // 4 張卡顯示 2 欄，5 張以上卡片顯示 3 欄
    if (verifiers.length <= 4) {
        $container.addClass('grid-cols-2');
    } else {
        $container.addClass('grid-cols-3');
    }
    
    const labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    verifiers.forEach((card, index) => {
        const label = labels.charAt(index % 26);
        const imagePath = `/assets/Games/turing/cards/TM_GameCards_CNT-${card.cardId}.png`;
        
        const cardHtml = `
            <div id="card-${card.cardId}" class="verifier-card bg-slate-900 rounded-xl p-2 shadow-lg border-2 border-slate-700 flex flex-col justify-between transition relative w-full">
                <div class="flex justify-between items-center mb-1 px-1">
                    <span class="text-[11px] text-slate-400 font-bold">卡片 #${card.cardId}</span>
                    <span class="bg-slate-800 text-emerald-400 px-2 py-0.5 rounded-full font-bold text-[11px] border border-emerald-500/50 shadow">
                        機台 ${label}
                    </span>
                </div>
                <div class="card-img-wrapper relative cursor-crosshair rounded-lg overflow-hidden border border-slate-600 bg-white w-full max-h-[280px] flex items-center justify-center">
                    <img src="${imagePath}" alt="驗證卡 ${card.cardId}" class="w-full h-auto object-contain block select-none pointer-events-none max-h-[280px]">
                </div>
                <div class="mt-1.5">
                    <button onclick="verifyCard(${card.cardId}, '${label}')" class="w-full py-1 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded shadow transition text-[11px] flex items-center justify-center gap-1">
                        <span>🔍 驗證此卡</span>
                    </button>
                </div>
            </div>
        `;
        $container.append(cardHtml);
    });

    $('.card-img-wrapper').off('click').on('click', function(e) {
        if ($(e.target).hasClass('card-cross-mark')) {
            $(e.target).remove();
            return;
        }
        const $wrapper = $(this);
        const rect = this.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        const $cross = $('<div></div>')
            .addClass('card-cross-mark')
            .text('✕')
            .css({ left: `${x}px`, top: `${y}px` });
        $wrapper.append($cross);
    });
}

function verifyCard(cardId, label) {
    if (testsThisRound >= 3) {
        alert("本輪已測試 3 次，請結束本輪！");
        return;
    }

    const proposal = getCurrentProposal();
    const requestData = { proposal: proposal, cardId: cardId, secret: activePuzzle.secretCode };
    const cardEl = $(`#card-${cardId}`);
    
    $.ajax({
        url: `${API_BASE}/verify`,
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestData),
        success: function(isValid) {
            testsThisRound++;
            totalTests++;
            updateStatsUI();

            const proposalStr = `${proposal.blue}-${proposal.yellow}-${proposal.purple}`;
            cardEl.removeClass('verify-pass verify-fail');
            if (isValid) {
                cardEl.addClass('verify-pass');
                logAction(`[${proposalStr}] 測 ${label} ➔ <span class="text-emerald-400 font-bold">✔ 符合</span>`);
            } else {
                cardEl.addClass('verify-fail');
                logAction(`[${proposalStr}] 測 ${label} ➔ <span class="text-red-400 font-bold">✘ 不符合</span>`);
            }
            setTimeout(() => cardEl.removeClass('verify-pass verify-fail'), 1500);

            recordMatrixResult(proposal, label, isValid);
        },
        error: function() {
            alert("驗證請求失敗！");
        }
    });
}

function recordMatrixResult(proposal, cardLabel, isValid) {
    let existingRound = gameHistoryRounds.find(r => r.round === currentRound);

    if (!existingRound) {
        existingRound = { round: currentRound, proposal: proposal, results: {} };
        gameHistoryRounds.push(existingRound);
    } else {
        existingRound.proposal = proposal;
    }
    
    existingRound.results[cardLabel] = isValid;
    renderMatrixTable();
}

function renderMatrixTable() {
    const $headerTr = $('#matrixHeaderTr');
    
    // 將前三欄的寬度改為 w-6 確保在 table-fixed 下有足夠的顯示空間
    let headersHtml = `
        <th class="p-0.5 border-r border-slate-300 text-blue-600 font-bold w-6">▲</th>
        <th class="p-0.5 border-r border-slate-300 text-yellow-600 font-bold w-6">■</th>
        <th class="p-0.5 border-r border-slate-400 text-purple-600 font-bold w-6">●</th>
    `;
    cardLabelsCache.forEach(c => {
        headersHtml += `<th class="p-0.5 border-r border-slate-200 font-bold text-slate-700">${c.label}</th>`;
    });
    $headerTr.html(headersHtml);

    const $tbody = $('#turingMatrixBody');
    $tbody.empty();

    gameHistoryRounds.forEach((round, index) => {
        let rowHtml = `<tr class="border-b border-slate-200">
            <td class="p-1 border-r border-slate-200 font-bold text-blue-600 bg-slate-100">${round.proposal.blue}</td>
            <td class="p-1 border-r border-slate-200 font-bold text-yellow-600 bg-slate-100">${round.proposal.yellow}</td>
            <td class="p-1 border-r border-slate-400 font-bold text-purple-600 bg-slate-100">${round.proposal.purple}</td>`;
        
        cardLabelsCache.forEach(c => {
            let status = round.results[c.label];
            let mark = '';
            let bgClass = '';
            if (status === true) {
                mark = '✔';
                bgClass += ' bg-emerald-100 text-emerald-600 font-bold';
            } else if (status === false) {
                mark = '✘';
                bgClass += ' bg-red-100 text-red-500 font-bold';
            }
            rowHtml += `<td class="p-1 border-r border-slate-200 ${bgClass}">${mark}</td>`;
        });
        rowHtml += `</tr>`;
        $tbody.append(rowHtml);
    });

    let emptyRowsNeeded = Math.max(0, 5 - gameHistoryRounds.length);
    for (let i = 0; i < emptyRowsNeeded; i++) {
        let emptyRow = `<tr class="border-b border-slate-200 h-6">
            <td class="p-1 border-r border-slate-200 text-slate-300">-</td>
            <td class="p-1 border-r border-slate-200 text-slate-300">-</td>
            <td class="p-1 border-r border-slate-400 text-slate-300 text-[10px]">-</td>`;
        cardLabelsCache.forEach(() => {
            emptyRow += `<td class="p-1 border-r border-slate-200"></td>`;
        });
        emptyRow += `</tr>`;
        $tbody.append(emptyRow);
    }
}

function initNotepad() {
    const $tbody = $('#notepadBody');
    $tbody.empty();
    
    for (let i = 1; i <= 5; i++) {
        let rowHtml = `<tr class="border-b border-slate-200">
            <td class="p-1 border-r border-slate-200 note-cell note-empty font-bold text-slate-800" data-val="0" data-num="${i}">${i}</td>
            <td class="p-1 border-r border-slate-200 note-cell note-empty font-bold text-slate-800" data-val="0" data-num="${i}">${i}</td>
            <td class="p-1 note-cell note-empty font-bold text-slate-800" data-val="0" data-num="${i}">${i}</td>
        </tr>`;
        $tbody.append(rowHtml);
    }

    $('.note-cell').off('click').on('click', function() {
        let val = parseInt($(this).attr('data-val'));
        let num = $(this).attr('data-num');
        val = (val + 1) % 3; 
        
        $(this).attr('data-val', val);
        $(this).removeClass('note-empty note-o note-x');
        
        if (val === 1) {
            $(this).addClass('note-o').text('O');
        } else if (val === 2) {
            $(this).addClass('note-x').text('X');
        } else {
            $(this).addClass('note-empty').text(num);
        }
    });
}

function handleSubmitDecode() {
    if (!confirm("確定要提交這組密碼作為最終答案嗎？")) return;

    const proposal = getCurrentProposal();
    const requestData = {
        puzzleId: currentPuzzleId, 
        currentRound: currentRound,
        totalTests: totalTests,
        b: proposal.blue,
        y: proposal.yellow,
        p: proposal.purple,
        diffChar: diffChar,
        playerName: currentAccount
    };

    $.ajax({
        url: `${API_BASE}/submit`,
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestData),
        success: function(result) {
            showResultModal(result);
        },
        error: function(xhr) {
            alert("結算失敗！");
            console.error(xhr.responseText);
        }
    });
}

function handleNextRound() {
    currentRound++;
    testsThisRound = 0;
    updateStatsUI();
    logAction(`進入第 ${currentRound} 輪。`, "text-amber-400 font-bold");
}

function updateStatsUI() {
    $('#currentRound').text(currentRound);
    $('#testsThisRound').text(`${testsThisRound} / 3`);
}

function getCurrentProposal() {
    return {
        blue: parseInt($('#blueInput').val()),
        yellow: parseInt($('#yellowInput').val()),
        purple: parseInt($('#purpleInput').val())
    };
}

function logAction(msg, cssClass = "text-slate-300") {
    const $log = $('#actionLog');
    $log.prepend(`<div class="${cssClass} pb-0.5 border-b border-slate-700/50">▶ ${msg}</div>`);
}

function showResultModal(result) {
    $('#resultModal').removeClass('hidden');
    if (result.won) {
        $('#resultTitle').text("🎉 破譯成功！").addClass("text-emerald-400");
    } else {
        $('#resultTitle').text("💥 破譯失敗！").addClass("text-red-500");
    }
    $('#resultMsg').text(result.message);
    if (result.rewardTokens > 0) {
        $('#rewardBox').removeClass('hidden').html(`💰 獲得代幣：+${result.rewardTokens}`);
    }
}