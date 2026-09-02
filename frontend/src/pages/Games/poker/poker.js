var backendHost=location.hostname+":8080";
var api="http://"+backendHost+"/api/games/poker";
var pokerSocketPath="/ws/games/poker";
var roomId="";
var token="";
var game=null;
var socket=null;
var pollTimer=null;
var refreshing=false;
var draftChoices={};
var selectedOrder=[];
var sortByPoint=false;
var saving=false;
var saveAgain=false;
var confirmAfterSave=false;
var lastRound=0;
var platformRoom=true;
var platformModeId=null;
var platformJwt="";

function element(id) {
    return document.getElementById(id);
}

$(document).ready(function() {
    var roomParameter=getParameter("roomId");
    var modeParameter=getParameter("modeCode");
    modeParameter=modeParameter.toUpperCase();
    platformModeId=Number(getParameter("modeId"));
    platformJwt=localStorage.getItem("token") || "";
    element("roomInput").value=roomParameter || "room-1";
    element("computerButton").onclick=function() { join("COMPUTER", newComputerRoomId()); };
    element("playerButton").onclick=function() { join("PLAYER", element("roomInput").value.trim()); };
    element("sortButton").onclick=changeSort;
    element("clearButton").onclick=clearCurrentRound;
    element("restartButton").onclick=restart;
    element("autoButton").onclick=autoSelect;
    element("confirmButton").onclick=confirmRound;
    element("leaveButton").onclick=leave;

    if(modeParameter==="PLAYER" && roomParameter) join("PLAYER", roomParameter);
    if(modeParameter==="COMPUTER" && roomParameter) join("COMPUTER", roomParameter);
});

function getParameter(name) {
    var query=location.search;
    if(query.length>0) query=query.substring(1);
    var items=query.split("&");
    for(var i=0;i<items.length;i++) {
        var pair=items[i].split("=");
        if(decodeURIComponent(pair[0])===name) {
            if(pair.length<2) return "";
            return decodeURIComponent(pair[1].replace(/\+/g, " "));
        }
    }
    return "";
}

function newComputerRoomId() {
    return "computer-"+Date.now()+"-"+Math.floor(Math.random()*1000000);
}

function request(method, path, data, success, complete) {
    var options={
        method:method,
        url:api+path,
        contentType:"application/json",
        dataType:"json",
        success:success,
        error:function(response) {
            var message="伺服器連線失敗";
            if(response.status===404) {
                message="找不到 Poker 後端，請確認目前執行的是 gamePoker 的 GamePokerApplication";
            }
            else if(response.responseJSON && response.responseJSON.message) {
                message=response.responseJSON.message;
            }
            saving=false;
            saveAgain=false;
            confirmAfterSave=false;
            showMessage(message);
            render();
        }
    };
    if(complete) options.complete=complete;
    options.headers={};
    if(platformJwt) options.headers["Authorization"]="Bearer "+platformJwt;
    if(token) options.headers["X-Player-Token"]=token;
    if(data!==null) options.data=JSON.stringify(data);
    return $.ajax(options);
}

function join(mode, id) {
    if(!id) {
        showMessage("請輸入房間編號", true);
        return;
    }
    var body={roomId:id};
    if(platformRoom) {
        if(!platformModeId) {
            showMessage("缺少遊戲模式資料", true);
            return;
        }
        if(!platformJwt) {
            showMessage("請先登入平台再進入遊戲", true);
            return;
        }
        body.modeId=platformModeId;
    }
    request("POST", "/join", body, function(joined) {
        roomId=joined.roomId;
        token=joined.token;
        game=joined.game;
        syncDraft();
        document.body.classList.add("playing");
        element("lobby").classList.add("hidden");
        element("game").classList.remove("hidden");
        connectSocket();
        render();
        clearInterval(pollTimer);
        pollTimer=setInterval(refresh, 800);
    });
}

function connectSocket() {
    var protocol=location.protocol==="https:" ? "wss" : "ws";
    // var url=protocol+"://"+location.host+"/ws/poker?roomId="+encodeURIComponent(roomId)+"&token="+encodeURIComponent(token);
    var url=protocol+"://"+backendHost+
        pokerSocketPath+"?roomId="+encodeURIComponent(roomId)+
        "&token="+encodeURIComponent(token);
    socket=new WebSocket(url);
    socket.onopen=function() { element("connectionBadge").textContent="已連線"; };
    socket.onclose=function() {
        if(token) {
            element("connectionBadge").textContent="連線已中斷，請重新載入";
            clearInterval(pollTimer);
            pollTimer=null;
        }
    };
}

function refresh() {
    if(!token || refreshing || saving) return;
    refreshing=true;
    request("GET", "/rooms/"+encodeURIComponent(roomId), null, function(latest) {
        var roundChanged=game && latest.currentRound!==game.currentRound;
        var stateChanged=gameStateSignature(game)!==gameStateSignature(latest);
        game=latest;
        if(roundChanged || lastRound===0) syncDraft();
        if(stateChanged) render();
        else renderCountdown();
    }, function() {
        refreshing=false;
    });
}

function gameStateSignature(value) {
    return JSON.stringify(value, function(key, item) {
        if(key==="roundResultRemainingMillis") return 0;
        return item;
    });
}

function syncDraft() {
    draftChoices={};
    selectedOrder=[];
    if(game && game.hand) {
        for(var i=0;i<game.hand.length;i++) {
            var card=game.hand[i];
            draftChoices[card.id]=card.slot;
            if(card.slot===game.currentRound) selectedOrder.push(card.id);
        }
    }
    lastRound=game ? game.currentRound : 0;
}

function render() {
    if(!game) return;
    element("roomLabel").textContent=game.mode==="COMPUTER" ? "電腦對戰" : "房間 "+game.roomId;
    element("player1Name").textContent=game.player1Name || "玩家一";
    element("player2Name").textContent=game.player2Name || "玩家二";
    element("player1PreviewName").textContent=game.player1Name || "玩家一";
    element("player2PreviewName").textContent=game.player2Name || "玩家二";
    element("seatLabel").textContent="你是 "+(game.seat===1 ? game.player1Name : game.player2Name);
    element("roundTitle").textContent=isResultPause() ? "第 "+game.currentRound+" 輪結果" : "第 "+game.currentRound+" 輪";
    element("roundHint").textContent="請選擇 "+cardsNeeded()+" 張牌";
    seatStatus("player1Status", game.player1Connected, game.player1Confirmed);
    seatStatus("player2Status", game.player2Connected, game.player2Confirmed);
    element("player1Seat").classList.toggle("confirmed", game.player1Confirmed);
    element("player2Seat").classList.toggle("confirmed", game.player2Confirmed);

    if(isResultPause()) {
        element("gameMessage").textContent="結果已公布";
        renderCountdown();
    }
    else if(game.status==="WAITING") {
        element("gameMessage").textContent="等待對手";
        element("roundHint").textContent="另一位玩家加入後自動開始";
    }
    else if(game.status==="PLAYING" && ownConfirmed()) {
        element("gameMessage").textContent="已鎖定";
        element("roundHint").textContent="等待對手確認";
    }
    else if(game.status==="PLAYING") {
        element("gameMessage").textContent="選牌中";
        element("roundHint").textContent="選擇 "+cardsNeeded()+" 張牌";
    }
    else {
        element("gameMessage").textContent="本局結束";
        element("roundHint").textContent="查看最終結果";
    }

    renderPreviews();
    renderHand();
    renderResults();

    var canEdit=game.status==="PLAYING" && !ownConfirmed() && !isResultPause();
    element("sortButton").disabled=!canEdit;
    element("clearButton").disabled=!canEdit;
    element("autoButton").disabled=!canEdit || game.currentRound!==1;
    element("confirmButton").disabled=!canEdit || selectedCount()!==cardsNeeded();
    element("confirmButton").classList.toggle("hidden", game.status==="FINISHED");
    element("handInstruction").textContent=canEdit ? "點擊手牌放入本輪出牌區" : "本輪手牌已鎖定";
}

function renderCountdown() {
    if(!game || game.status!=="ROUND_RESULT") return;
    var seconds=Math.max(1, Math.ceil(game.roundResultRemainingMillis/1000));
    element("roundHint").textContent=seconds+" 秒後開放第 "+(game.currentRound+1)+" 輪";
}

function isResultPause() {
    return game && game.status==="ROUND_RESULT";
}

function renderPreviews() {
    renderOnePreview(1);
    renderOnePreview(2);
}

function renderOnePreview(playerNumber) {
    var isMe=game.seat===playerNumber;
    var result=currentRoundResult();
    var cards=[];
    var type="--";
    var countText="待開牌";
    var canRemove=false;

    if(result) {
        cards=playerNumber===1 ? result.player1Cards : result.player2Cards;
        type=playerNumber===1 ? result.player1Type : result.player2Type;
        countText=cards.length+" / "+cardsNeededForRound(result.round);
    }
    else if(isMe) {
        cards=currentSelectedCards();
        var preview=game.preview[game.currentRound];
        type=preview && preview.type ? preview.type : "張數不足";
        countText=cards.length+" / "+cardsNeeded();
        canRemove=game.status==="PLAYING" && !ownConfirmed() && !isResultPause();
    }
    else {
        var confirmed=playerNumber===1 ? game.player1Confirmed : game.player2Confirmed;
        countText=confirmed ? "已選好" : "待開牌";
    }

    element("player"+playerNumber+"Count").textContent=countText;
    element("player"+playerNumber+"Type").textContent=type;
    var panel=element("player"+playerNumber+"Preview");
    panel.innerHTML="";

    if(cards.length===0) {
        var placeholder=document.createElement("span");
        placeholder.className="preview-placeholder";
        placeholder.textContent=countText;
        panel.appendChild(placeholder);
        return;
    }

    for(var i=0;i<cards.length;i++) {
        var card=cards[i];
        var button=document.createElement("button");
        button.className="mini-card"+(isRed(card) ? " red" : "")+(canRemove ? " selectable" : "");
        var previewImage=document.createElement("img");
        previewImage.src=cardImagePath(card);
        previewImage.alt=card.name || card;
        button.appendChild(previewImage);
        if(canRemove) {
            button.dataset.id=card.id;
            button.onclick=function() { removeCard(Number(this.dataset.id)); };
        }
        else button.disabled=true;
        panel.appendChild(button);
    }
}

function renderHand() {
    var cards=orderedCards(game.hand);
    var hand=element("hand");
    hand.innerHTML="";
    var canEdit=game.status==="PLAYING" && !ownConfirmed() && !isResultPause();

    for(var i=0;i<cards.length;i++) {
        var card=cards[i];
        var slot=draftChoices[card.id];
        var button=document.createElement("button");
        button.className="playing-card slot-"+slot+(isRed(card) ? " red" : "");
        var handImage=document.createElement("img");
        handImage.src=cardImagePath(card);
        handImage.alt=card.name;
        button.appendChild(handImage);
        button.dataset.id=card.id;

        var canSelect=canEdit && slot===0;
        if(canSelect) button.onclick=function() { selectCard(Number(this.dataset.id)); };
        else {
            button.disabled=true;
            if(slot!==0) button.classList.add("locked");
        }
        hand.appendChild(button);
    }
}

function currentSelectedCards() {
    var cards=[];
    for(var i=0;i<game.hand.length;i++) {
        var card=game.hand[i];
        if(draftChoices[card.id]===game.currentRound) cards.push(card);
    }
    cards.sort(function(a, b) { return selectedOrder.indexOf(a.id)-selectedOrder.indexOf(b.id); });
    return cards;
}

function currentRoundResult() {
    if(!game.results) return null;
    var shownRound=game.currentRound;
    for(var i=0;i<game.results.length;i++) {
        if(game.results[i].round===shownRound) return game.results[i];
    }
    return null;
}

function selectCard(cardId) {
    if(selectedCount()>=cardsNeeded()) {
        var first=selectedOrder.shift();
        if(first!==undefined) draftChoices[first]=0;
    }
    draftChoices[cardId]=game.currentRound;
    selectedOrder.push(cardId);
    queueSave();
    render();
}

function removeCard(cardId) {
    draftChoices[cardId]=0;
    selectedOrder=selectedOrder.filter(function(id) { return id!==cardId; });
    queueSave();
    render();
}

function clearCurrentRound() {
    for(var id in draftChoices) {
        if(draftChoices[id]===game.currentRound) draftChoices[id]=0;
    }
    selectedOrder=[];
    queueSave();
    showMessage("本輪選牌已清除");
    render();
}

function changeSort() {
    sortByPoint=!sortByPoint;
    showMessage(sortByPoint ? "依點數整理手牌" : "依花色整理手牌");
    render();
}

function orderedCards(cards) {
    var copy=cards.slice();
    if(sortByPoint) {
        copy.sort(function(a, b) { return a.rank-b.rank || a.id-b.id; });
    }
    else copy.sort(function(a, b) { return a.id-b.id; });
    return copy;
}

function autoSelect() {
    request("POST", "/rooms/"+encodeURIComponent(roomId)+"/auto-select", null, function(result) {
        game=result;
        syncDraft();
        showMessage("自動選牌完成");
        render();
    });
}

function queueSave() {
    if(saving) {
        saveAgain=true;
        return;
    }
    saving=true;
    var snapshot={};
    for(var id in draftChoices) snapshot[id]=draftChoices[id];
    request("PUT", "/rooms/"+encodeURIComponent(roomId)+"/selection",
            {choices:snapshot}, function(result) {
        game=result;
        saving=false;
        render();
        if(saveAgain) {
            saveAgain=false;
            queueSave();
        }
        else if(confirmAfterSave) {
            confirmAfterSave=false;
            sendConfirm();
        }
    });
}

function confirmRound() {
    if(selectedCount()!==cardsNeeded()) {
        showMessage("第 "+game.currentRound+" 輪需要選 "+cardsNeeded()+" 張牌");
        return;
    }
    element("confirmButton").disabled=true;
    confirmAfterSave=true;
    if(!saving) queueSave();
}

function sendConfirm() {
    request("POST", "/rooms/"+encodeURIComponent(roomId)+"/confirm", null, function(result) {
        var paused=result.status==="ROUND_RESULT";
        game=result;
        if(paused || game.status==="FINISHED") showMessage("雙方已確認，本輪開牌");
        else showMessage("本輪已確認，等待對手");
        if(paused) syncDraft();
        render();
    });
}

function restart() {
    request("POST", "/rooms/"+encodeURIComponent(roomId)+"/restart", null, function(result) {
        game=result;
        syncDraft();
        showMessage("遊戲已重新開始");
        render();
    });
}

function renderResults() {
    var panel=element("resultPanel");
    var player1Panel=element("player1PreviewPanel");
    var player2Panel=element("player2PreviewPanel");
    panel.classList.remove("win", "lose", "draw");
    player1Panel.classList.remove("winner", "loser");
    player2Panel.classList.remove("winner", "loser");
    panel.classList.remove("hidden");
    var completed=game.results ? game.results.length : 0;
    var winCount=0;
    var loseCount=0;
    for(var countIndex=0;countIndex<completed;countIndex++) {
        if(game.results[countIndex].winner===game.seat) winCount++;
        else loseCount++;
    }

    if(completed===0) {
        element("resultCaption").textContent="三輪結果";
        element("resultSymbol").textContent="…";
        element("winnerTitle").textContent="尚未公布結果";
    }
    else if(isResultPause() || game.status==="FINISHED") {
        var latest=game.results[completed-1];
        var winner=game.status==="FINISHED" ? game.winner : latest.winner;
        element("resultCaption").textContent=game.status==="FINISHED" ? "整局總結果" : "本輪結果";

        var ownWin=winner===game.seat;
        panel.classList.add(ownWin ? "win" : "lose");
        element("resultSymbol").textContent=ownWin ? "✓" : "×";
        if(game.status==="FINISHED") element("winnerTitle").textContent=(ownWin ? "整局獲勝" : "整局落敗")+"｜"+winCount+"勝 "+loseCount+"敗";
        else element("winnerTitle").textContent="第 "+latest.round+" 輪"+(ownWin ? "獲勝" : "落敗");
        element("player"+winner+"PreviewPanel").classList.add("winner");
        element("player"+(winner===1 ? 2 : 1)+"PreviewPanel").classList.add("loser");
    }
    else {
        element("resultCaption").textContent="目前戰績";
        element("resultSymbol").textContent=completed+"/3";
        element("winnerTitle").textContent=winCount+"勝 "+loseCount+"敗";
    }

    var html="";
    for(var roundNumber=1;roundNumber<=3;roundNumber++) {
        var result=null;
        for(var resultIndex=0;resultIndex<completed;resultIndex++) {
            if(game.results[resultIndex].round===roundNumber) result=game.results[resultIndex];
        }

        if(result===null) {
            html+="<div class='result-row waiting'>";
            html+="<div class='round-number'><b>第 "+roundNumber+" 輪</b><strong>等待</strong></div>";
            html+="<div class='round-hand empty-hand'><small>你的牌</small><span>尚未公布</span></div>";
            html+="<div class='versus'>VS</div>";
            html+="<div class='round-hand empty-hand'><small>對手的牌</small><span>尚未公布</span></div>";
            html+="</div>";
        }
        else {
            var ownCards=game.seat===1 ? result.player1Cards : result.player2Cards;
            var opponentCards=game.seat===1 ? result.player2Cards : result.player1Cards;
            var ownType=game.seat===1 ? result.player1Type : result.player2Type;
            var opponentType=game.seat===1 ? result.player2Type : result.player1Type;
            var roundState=result.winner===game.seat ? "win" : "lose";
            var roundText=result.winner===game.seat ? "你贏" : "你輸";

            html+="<div class='result-row "+roundState+"'>";
            html+="<div class='round-number'><b>第 "+roundNumber+" 輪</b><strong>"+roundText+"</strong></div>";
            html+="<div class='round-hand own-hand'><small>你的牌</small><b>"+ownType+"</b><span class='result-cards'>"+resultCardImages(ownCards)+"</span></div>";
            html+="<div class='versus'>VS</div>";
            html+="<div class='round-hand opponent-hand'><small>對手的牌</small><b>"+opponentType+"</b><span class='result-cards'>"+resultCardImages(opponentCards)+"</span></div>";
            html+="</div>";
        }
    }
    element("roundResults").innerHTML=html;
}

function resultCardImages(cards) {
    var html="";
    for(var i=0;i<cards.length;i++) html+="<img src='"+cardImagePath(cards[i])+"' alt='"+cards[i]+"'>";
    return html;
}

function seatStatus(id, connected, confirmed) {
    if(!connected) element(id).textContent="離線／空缺";
    else if(confirmed) element(id).textContent="本輪已確認";
    else element(id).textContent="在線";
}

function ownConfirmed() {
    return game.seat===1 ? game.player1Confirmed : game.player2Confirmed;
}
function cardsNeeded() { return game.currentRound===1 ? 3 : 5; }
function cardsNeededForRound(round) { return round===1 ? 3 : 5; }
function selectedCount() { return currentSelectedCards().length; }
function isRed(card) {
    var suit=card.suit || String(card).charAt(0);
    return suit==="♡" || suit==="♢" || suit==="♥" || suit==="♦";
}

function cardImagePath(card) {
    var cardName=card.name ? card.name : String(card);
    var suit=card.suit ? card.suit : cardName.charAt(0);
    var suitCode="s";
    if(suit==="♡" || suit==="♥") suitCode="h";
    else if(suit==="♢" || suit==="♦") suitCode="d";
    else if(suit==="♣") suitCode="c";

    var rank=card.rank;
    if(rank===undefined) rank=cardName.substring(1);
    var point=String(rank).toLowerCase();
    if(rank===1 || point==="a") point="a";
    else if(rank===11 || point==="j") point="j";
    else if(rank===12 || point==="q") point="q";
    else if(rank===13 || point==="k") point="k";
    return "../../../assets/Games/poker/cards/card_a_"+suitCode+point+".png";
}

function leave() {
    if(!token) {
        location.replace("poker_client.html");
        return;
    }
    $.ajax({
        method:"DELETE",
        url:api+"/rooms/"+encodeURIComponent(roomId)+"/leave",
        headers:{"X-Player-Token":token}
    });
    token="";
    refreshing=false;
    clearInterval(pollTimer);
    if(socket) socket.close();
    setTimeout(function() { location.replace("poker_client.html"); }, 30);
}

function showMessage(message, lobby) {
    element(lobby ? "lobbyMessage" : "actionMessage").textContent=message;
}
