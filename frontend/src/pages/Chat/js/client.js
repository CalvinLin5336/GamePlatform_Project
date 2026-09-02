window.onload = function () {
    //獲取DOM元件
    var loginBtn = document.getElementById("loginBtn");
    var userNameInput = document.getElementById("userNameInput");
    var infoWindow = document.getElementById("infoWindow");
    var userinput = document.getElementById("userinput");
    var chatRoomForm = document.getElementById("chatRoomForm");
    var messageDisplay = document.getElementById("messageDisplay");
 
    var webSocket;
    var isConnectSuccess = false;
 
    //設置登入鈕的動作，沒有登出，登入才可發言
    loginBtn.addEventListener("click", function () {
        //檢查有無輸入名稱
        if (userNameInput.value && userNameInput.value !== "") {
            setWebSocket();  //設置WebSocket連接
        } else {
            infoWindow.innerHTML = "請輸入名稱";
        }
 
    });
    //Submit Form時送出訊息
    chatRoomForm.addEventListener("submit", function () {
		// 關鍵：阻止表單預設的重新整理/跳轉行為
		    event.preventDefault(); 
		    
		    sendMessage();
		    
		    // 清空輸入框（發送後順便清空比較方便）
		    document.getElementById("userinput").value = "";
		    
		    return false;
    });
    //使用webSocket擁有的function, send(), 送出訊息
    function sendMessage() {
        //檢查WebSocket連接狀態
        if (webSocket && isConnectSuccess) {
            var messageInfo = {
                userName: userNameInput.value,
                message: userinput.value
            }
            webSocket.send(JSON.stringify(messageInfo));
        } else {
            infoWindow.innerHTML = "未登入";
        }
    }
 
    //設置WebSocket
    function setWebSocket() {
        //開始WebSocket連線
        var wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
        webSocket = new WebSocket(wsProtocol + '://' + window.location.hostname + ':8080/ws/chat');
        //以下開始偵測WebSocket的各種事件
         
        //onerror , 連線錯誤時觸發  
        webSocket.onerror = function (event) {
            loginBtn.disabled = false;
            userNameInput.disabled = false;
            infoWindow.innerHTML = "登入失敗";
        };
 
        //onopen , 連線成功時觸發
        webSocket.onopen = function (event) {
            isConnectSuccess = true;
            loginBtn.disabled = true;
            userNameInput.disabled = true;
            infoWindow.innerHTML = "登入成功";
             
            //送一個登入聊天室的訊息
            var firstLoginInfo = {
                userName : "系統",
                message : userNameInput.value + " 登入了聊天室"
            };
            webSocket.send(JSON.stringify(firstLoginInfo));
        };
 

		// onmessage , 接收到來自Server的訊息時觸發
		webSocket.onmessage = function (event) {
		    var messageObject = JSON.parse(event.data);
		    
		    // 建立訊息卡片容器
		    var messageDiv = document.createElement("div");
		    messageDiv.className = "flex flex-col mb-2 animate-fade-in";
		    
		    // 判斷是否為系統訊息
		    if (messageObject.userName === "系統") {
		        messageDiv.className += " items-center my-1";
		        messageDiv.innerHTML = `<span class="text-xs bg-slate-700/60 text-slate-400 px-3 py-1 rounded-full">${messageObject.message}</span>`;
		    } else {
		        // 一般使用者訊息
		        messageDiv.innerHTML = `
		            <span class="text-xs text-slate-400 mb-1 px-1">${messageObject.userName}</span>
		            <div class="bg-slate-700/80 border border-slate-600/50 px-3.5 py-2 rounded-2xl max-w-[80%] text-slate-200 shadow-sm">
		                ${messageObject.message}
		            </div>
		        `;
		    }
		    
		    messageDisplay.appendChild(messageDiv);
		    // 自動滾動到最新訊息
		    messageDisplay.scrollTop = messageDisplay.scrollHeight;
		};

    }
};



 
