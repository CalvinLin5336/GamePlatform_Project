window.onload = function () {
    var userinput = document.getElementById("userinput");
    var chatRoomForm = document.getElementById("chatRoomForm");
    var submitBtn = chatRoomForm ? chatRoomForm.querySelector("button[type='submit']") : null;
    
    // UI 元件
    var tabLobby = document.getElementById("tabLobbyChat");
    var tabRoom = document.getElementById("tabRoomChat");
    var displayLobby = document.getElementById("messageDisplayLobby");
    var displayRoom = document.getElementById("messageDisplayRoom");

    var webSocket; // 專屬大廳的 WebSocket
    var roomWebSocket = null; // 遊戲開始後仍由外層頁面持有的房間 WebSocket
    var currentRoomId = null;
    var roomChannelFinished = false;
    var isConnectSuccess = false;
    var currentUserName = "訪客";
    var activeTab = "lobby"; 
    window.roomDisbandedFlag = false; // 紀錄房間是否已解散

    // 🌟 1. 切換分頁邏輯
// 🌟 1. 切換分頁邏輯 (使用 classList，避免覆蓋 hidden 屬性)
function switchTab(tabName) {
    activeTab = tabName;
    
    if (tabName === 'lobby') {
        // 大廳標籤：亮起
        if (tabLobby) {
            tabLobby.classList.add("bg-indigo-600", "text-white", "shadow-sm");
            tabLobby.classList.remove("bg-slate-800", "text-slate-400", "hover:bg-slate-700");
        }
        // 房間標籤：暗下 (不影響 hidden)
        if (tabRoom) {
            tabRoom.classList.add("bg-slate-800", "text-slate-400", "hover:bg-slate-700");
            tabRoom.classList.remove("bg-indigo-600", "text-white", "shadow-sm");
        }
        
        if (displayLobby) displayLobby.classList.remove("hidden");
        if (displayRoom) displayRoom.classList.add("hidden");
    } else {
        // 房間標籤：亮起 (不影響 hidden)
        if (tabRoom) {
            tabRoom.classList.add("bg-indigo-600", "text-white", "shadow-sm");
            tabRoom.classList.remove("bg-slate-800", "text-slate-400", "hover:bg-slate-700");
        }
        // 大廳標籤：暗下
        if (tabLobby) {
            tabLobby.classList.add("bg-slate-800", "text-slate-400", "hover:bg-slate-700");
            tabLobby.classList.remove("bg-indigo-600", "text-white", "shadow-sm");
        }
        
        if (displayRoom) displayRoom.classList.remove("hidden");
        if (displayLobby) displayLobby.classList.add("hidden");
        if (displayRoom) displayRoom.scrollTop = displayRoom.scrollHeight;
    }
    updateInputState();
}

    if (tabLobby) tabLobby.addEventListener("click", () => switchTab('lobby'));
    if (tabRoom) tabRoom.addEventListener("click", () => switchTab('room'));

    // 根據當前分頁與登入狀態更新輸入框
    function updateInputState() {
        var account = localStorage.getItem("account");
        if (!account) {
            userinput.disabled = true;
            userinput.placeholder = "請先登入以使用聊天室...";
            if (submitBtn) { submitBtn.disabled = true; submitBtn.classList.add("opacity-50", "cursor-not-allowed"); }
            return;
        }

        if (activeTab === 'lobby') {
            userinput.disabled = false;
            userinput.placeholder = "在大廳說點什麼...";
            if (submitBtn) { submitBtn.disabled = false; submitBtn.classList.remove("opacity-50", "cursor-not-allowed"); }
        } else if (activeTab === 'room') {
            if (window.roomDisbandedFlag) {
                userinput.disabled = true;
                userinput.placeholder = "房間已解散...";
                if (submitBtn) { submitBtn.disabled = true; submitBtn.classList.add("opacity-50", "cursor-not-allowed"); }
            } else {
                userinput.disabled = false;
                userinput.placeholder = "在房間說點什麼...";
                if (submitBtn) { submitBtn.disabled = false; submitBtn.classList.remove("opacity-50", "cursor-not-allowed"); }
            }
        }
    }

    // 🌟 2. 開放給內層 iframe (等待區) 呼叫的 API 控制器
    window.RoomChatManager = {
        showTab: function() {
            window.roomDisbandedFlag = false;
            if (tabRoom) tabRoom.classList.remove('hidden');
            displayRoom.innerHTML = ''; // 清空上一場的紀錄
            switchTab('room'); // 自動切過去
        },
        startGameChannel: function(roomId, playerAccount) {
            connectGameRoomChannel(roomId, playerAccount);
        },
        hideTab: function() {
            closeGameRoomChannel();
            if (tabRoom) tabRoom.classList.add('hidden');
            switchTab('lobby');
        },
        appendMessage: function(userName, messageText, isSystem) {
            if (!displayRoom) return;
            var messageDiv = document.createElement("div");
            messageDiv.className = "flex flex-col mb-2 animate-fade-in";
            
            if (isSystem || userName === "系統") {
                messageDiv.className += " items-center my-1";
                messageDiv.innerHTML = `<span class="text-xs bg-slate-700/60 text-slate-400 px-3 py-1 rounded-full">${escapeHtml(messageText)}</span>`;
            } else {
                messageDiv.innerHTML = `
                    <span class="text-xs text-slate-400 mb-1 px-1">${escapeHtml(userName)}</span>
                    <div class="bg-slate-700/80 border border-slate-600/50 px-3.5 py-2 rounded-2xl max-w-[80%] text-slate-200 shadow-sm break-words">
                        ${escapeHtml(messageText)}
                    </div>
                `;
            }
            displayRoom.appendChild(messageDiv);
            displayRoom.scrollTop = displayRoom.scrollHeight;
        },
        systemAlert: function(msg) {
            if (!displayRoom) return;
            var msgDiv = document.createElement("div");
            msgDiv.className = "text-center text-xs text-red-400 my-2 font-bold";
            msgDiv.innerText = msg;
            displayRoom.appendChild(msgDiv);
            displayRoom.scrollTop = displayRoom.scrollHeight;
            
            window.roomDisbandedFlag = true;
            updateInputState();
        }
    };

    // 遊戲開始後，房間聊天室改由外層 chatclient 持有 WebSocket。
    // 因為外層頁面不會隨遊戲 iframe 換頁而消失，所以聊天連線能跨越等待區與遊戲頁。
    function connectGameRoomChannel(roomId, playerAccount) {
        if (!roomId || !playerAccount) return;

        if (roomWebSocket && roomWebSocket.readyState === WebSocket.OPEN && currentRoomId === roomId) {
            return;
        }

        closeGameRoomChannel();

        currentRoomId = roomId;
        roomChannelFinished = false;
        window.roomDisbandedFlag = false;

        var wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
        roomWebSocket = new WebSocket(
            wsProtocol + '://' + window.location.hostname + ':8080/ws/room/'
            + encodeURIComponent(roomId) + '?player=' + encodeURIComponent(playerAccount)
        );

        roomWebSocket.onopen = function () {
            console.log('✅ 遊戲中的房間聊天 WebSocket 連線成功: ' + roomId);
        };

        roomWebSocket.onmessage = function (event) {
            var data;
            try {
                data = JSON.parse(event.data);
            } catch (e) {
                console.error('房間 WebSocket 訊息解析失敗', e);
                return;
            }

            if (data.type === 'ROOM_CHAT' || (data.userName && data.message)) {
                window.RoomChatManager.appendMessage(
                    data.userName || '系統',
                    data.message || '',
                    data.userName === '系統'
                );
                return;
            }

            if (data.type === 'ROOM_FINISHED') {
                roomChannelFinished = true;
                window.RoomChatManager.systemAlert(
                    data.message || '遊戲已結束，房間頻道已關閉'
                );

                if (roomWebSocket && roomWebSocket.readyState === WebSocket.OPEN) {
                    roomWebSocket.close();
                }
            }
        };

        roomWebSocket.onerror = function () {
            console.error('房間聊天 WebSocket 發生錯誤');
        };

        roomWebSocket.onclose = function () {
            if (!roomChannelFinished && currentRoomId) {
                console.log('❌ 房間聊天 WebSocket 已斷線: ' + currentRoomId);
            }
            roomWebSocket = null;
        };
    }

    function closeGameRoomChannel() {
        if (roomWebSocket) {
            roomWebSocket.onclose = null;
            if (roomWebSocket.readyState === WebSocket.OPEN || roomWebSocket.readyState === WebSocket.CONNECTING) {
                roomWebSocket.close();
            }
        }
        roomWebSocket = null;
        currentRoomId = null;
        roomChannelFinished = false;
    }

    // 🌟 3. 大廳聊天室的基礎連線邏輯
    function checkAndConnect() {
        var account = localStorage.getItem("account");
        currentUserName = localStorage.getItem("username") || account || "訪客";
        updateInputState();

        if (account) {
            if (!webSocket || webSocket.readyState === WebSocket.CLOSED) setWebSocket();
        } else {
            if (webSocket && webSocket.readyState === WebSocket.OPEN) webSocket.close();
        }
    }

    function checkKickNotification() {
        var kickMsg = localStorage.getItem("kickNotification");
        if (kickMsg && displayLobby) {
            var msgDiv = document.createElement("div");
            msgDiv.className = "flex justify-center my-4 animate-fade-in";
            msgDiv.innerHTML = `<span class="text-xs bg-red-900/60 text-red-200 px-3 py-1.5 rounded-full border border-red-700/50 shadow-lg">${escapeHtml(kickMsg)}</span>`;
            displayLobby.appendChild(msgDiv);
            displayLobby.scrollTop = displayLobby.scrollHeight;
            localStorage.removeItem("kickNotification");
        }
    }

    checkAndConnect();
    checkKickNotification();

    window.addEventListener('storage', function (event) {
        if (event.key === 'account' || event.key === 'username') checkAndConnect();
        else if (event.key === 'kickNotification') checkKickNotification();
    });
 
    // 🌟 4. 發送訊息 (智慧分發：若在房間分頁，就把文字丟給 iframe)
    if (chatRoomForm) {
        chatRoomForm.addEventListener("submit", function (event) {
            event.preventDefault(); 
            if (localStorage.getItem("account")) {
                var text = userinput.value.trim();
                if (!text) return false;

                if (activeTab === 'lobby') {
                    // 發送給大廳 WebSocket
                    var text = userinput.value.trim();
                    if (webSocket && isConnectSuccess) {
                        webSocket.send(JSON.stringify({
                            type: "LOBBY_CHAT",
                            roomId: "LOBBY",
                            userName: currentUserName,
                            message: text
                        }));
                    }
                } else if (activeTab === 'room') {
                    // 遊戲開始後優先使用外層持有的房間 WebSocket。
                    if (roomWebSocket && roomWebSocket.readyState === WebSocket.OPEN && currentRoomId) {
                        roomWebSocket.send(JSON.stringify({
                            type: "ROOM_CHAT",
                            roomId: currentRoomId,
                            userName: currentUserName,
                            message: text
                        }));
                    } else {
                        // 尚在等待區時維持原本流程，由 iframe 的 roomSocket 發送。
                        var iframe = document.getElementById("mainFrame");
                        if (iframe && iframe.contentWindow && typeof iframe.contentWindow.sendRoomChatMessage === 'function') {
                            iframe.contentWindow.sendRoomChatMessage(text);
                        }
                    }
                }
                userinput.value = "";
            }
            return false;
        });
    }

    // 連線大廳
    function setWebSocket() {
        var wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
        webSocket = new WebSocket(wsProtocol + '://' + window.location.hostname + ':8080/ws/chat');
         
        webSocket.onerror = function () {
            var errorDiv = document.createElement("div");
            errorDiv.className = "text-center text-xs text-red-400 my-2";
            errorDiv.innerText = "大廳連線失敗，請確認伺服器狀態。";
            if (displayLobby) displayLobby.appendChild(errorDiv);
        };
        webSocket.onopen = function () { isConnectSuccess = true; };
        
        webSocket.onmessage = function (event) {
            var data = JSON.parse(event.data);
            
            var messageDiv = document.createElement("div");
            messageDiv.className = "flex flex-col mb-2 animate-fade-in";
            
            if (data.userName === "系統") {
                messageDiv.className += " items-center my-1";
                messageDiv.innerHTML = `<span class="text-xs bg-slate-700/60 text-slate-400 px-3 py-1 rounded-full">${escapeHtml(data.message)}</span>`;
            } else {
                messageDiv.innerHTML = `
                    <span class="text-xs text-slate-400 mb-1 px-1">${escapeHtml(data.userName)}</span>
                    <div class="bg-slate-700/80 border border-slate-600/50 px-3.5 py-2 rounded-2xl max-w-[80%] text-slate-200 shadow-sm break-words">
                        ${escapeHtml(data.message)}
                    </div>
                `;
            }
            
            if (displayLobby) {
                displayLobby.appendChild(messageDiv);
                displayLobby.scrollTop = displayLobby.scrollHeight;
            }
        };
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
};