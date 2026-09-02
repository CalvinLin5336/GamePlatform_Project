// API 一律連到目前提供前端頁面的主機；本機與區網測試都不必改 IP。
const API_BASE = "http://" + location.hostname + ":8080/api/quiz";


// 建立 Quill 富文本編輯器
const quillTitle = new Quill('#editor-title',{theme:'snow'});
const quillExplanation = new Quill('#editor-explanation',{theme:'snow'});
let optionEditors=[];

// 全域變數
let examQuestions=[];
let currentExamIndex = 0;
let userExamAnswers =[]; //[{ questionId, selectedOptionIds }]
let examTimer = null;
let secondsLeft= 15;
let currentUsername = "";

//儲存使用者 Token
let platformJwt= "";

//=================
//身分驗證與統一請求
//=================

/*
* 檢查登入狀態，若未登入則阻擋操作
*/
function requireAuth(){
	if(!platformJwt){
		alert("請先登入平台再進行操作");
		return false;
	}
	return true;
}

//解析 JWT Payload
function parseJwt(token){
	try{
		const base64Url = token.split('.')[1];
		const base64 = base64Url.replace(/-/g,'+').replace(/_/g,'/');
		const jsonPayLoad = decodeURIComponent(
			atob(base64)
				.split('')
				.map(c=> '%' + ('00'+ c.charCodeAt(0).toString(16)).slice(-2))
				.join('')
			);
			return JSON.parse(jsonPayLoad);

	}catch(e){
		return null;
	}
}

/*
* 封裝feth: 自動在 Headers 帶入 Authorization Bearer Token
* 並統一攔截 401/403 授權失敗
*/
async function authFetch(url,options={}){
	options.headers = options.headers || {};

	if(platformJwt){
		options.headers["Authorization"]= `Bearer ${platformJwt}`;
	}
	const res = await fetch(url,options);

	//攔截Token 過期或未授權
	if(res.status===401 || res.status ===403){
		alert("登入憑證已失效或無權限，請重新登入!");
		localStorage.removeItem("token");
		platformJwt="";
		return null;
	}
	return res;
}

// 頁面初始化
document.addEventListener("DOMContentLoaded", ()=>{
	//從 localStorage讀取 Token
	const urlParams = new URLSearchParams(location.search);
	platformJwt = urlParams.get('token') || localStorage.getItem("token")||"";

	const nameInput = document.getElementById('player-name');

	//若有 Token，解析出使用者名稱或帳號填入
	if(platformJwt && nameInput){
		const payload = parseJwt(platformJwt);
		if(payload){
			//依後端 JWT生成時塞入的claim鍵名調整(例如: sub, username, account, nickname)
			const loggedUser = payload.username || payload.account || payload.sub;
			if(loggedUser){
				nameInput.value = loggedUser;
				//避免竄改
				//nameInput.readOnly = true;
			}
		}
	}

	addAdminOptionRow();
	addAdminOptionRow();
	loadAdminTable();
	loadLeaderboard();

	// 檢查網址參數是否需要自動參考(列如:index.html?user=Player1&auto=true)	
	const autoStart = urlParams.get('auto');
	const userParam = urlParams.get('user');
	if(autoStart === 'true'){
		const nameInput = document.getElementById('player-name');
		if(nameInput){
			nameInput.value = userParam || ("Player_" + Math.floor(Math.random()* 1000));
			startQuiz();
		}	
	}
});

// 分頁切換
function switchTab(tabName,event){
	//進入 admin頁面時強制檢查是否有登入
	if(tabName==='admin'&& !requireAuth()){
		return;
	}

	document.querySelectorAll('.tab-content').forEach(el=> el.classList.remove('active'));
	document.querySelectorAll('.nav-tabs button').forEach(el=> el.classList.remove('active'));
	document.getElementById(`${tabName}-tab`).classList.add('active');
	if(event) event.target.classList.add('active');
	
	if(tabName === 'reader') loadReaderData();
	if(tabName === 'admin') loadAdminTable();
	if(tabName === 'leaderboard') loadLeaderboard();
}

//---1. 遊戲邏輯 ---

async function startQuiz(){
	//開始作答前驗證使用者是否登入
	if(!requireAuth())return;
	
	currentUsername = document.getElementById('player-name').value.trim();
	if(!currentUsername) return alert('請輸入暱稱!');
	
	try{
		const res = await fetch(`${API_BASE}/questions/exam`);
		examQuestions = await res.json();
		
		if(examQuestions.length===0){
			return alert('目前資料庫沒有題目，請先至管理頁面新增題目!');
		} 
		
		userExamAnswers = [];
		currentExamIndex = 0;		
	
		
		document.getElementById('player-setup').style.display='none';
		document.getElementById('report-card').style.display='none';
		document.getElementById('quiz-area').style.display ='block';
		document.getElementById('total-count').innerText = examQuestions.length;
		
		renderQuestion();	  				
	}catch(err){
		alert('無法連線至後端服務');
		console.error(err);	  			
	}
}

function renderQuestion(){	  		
	clearInterval(examTimer);
	
	if(currentExamIndex >= examQuestions.length){
		submitWholeQuiz();
		return;
	}
	
	const q = examQuestions[currentExamIndex];
	document.getElementById('current-index').innerText = currentExamIndex + 1;
	document.getElementById('question-title-display').innerHTML= q.title;
	
	const optionsBox = document.getElementById('options-display');
	optionsBox.innerHTML = '';
	
	// 渲染複選框 Options
	q.options.forEach(opt=>{
		const label = document.createElement('label');
		label.className = 'option-label';
		label.innerHTML = `
			<input type="checkbox" name="exam-option" value="${opt.id}" style="margin-right: 10px;">
			<span>${opt.optionText}</span>
			`;
			optionsBox.appendChild(label);
	});
	
	// 動態倒數計時
	secondsLeft = q.timeLimitSeconds || 15;
	document.getElementById('timer-count').innerText = secondsLeft;
	examTimer = setInterval(() => {
		secondsLeft--;
		document.getElementById('timer-count').innerText = secondsLeft;
		if(secondsLeft <= 0){			
			clearInterval(examTimer);
			nextQuestion(); //時間到自動進入下一題
		}
	},1000);
}

function nextQuestion(){
	clearInterval(examTimer);
	const q = examQuestions[currentExamIndex];
	const selectedCbs = document.querySelectorAll('input[name="exam-option"]:checked');
	const selectedOptionIds = Array.from(selectedCbs)
			.map(cd=> parseInt(cd.value, 10))
			.filter(val => !isNaN(val));
	
	userExamAnswers.push({
		questionId: q.id,
		selectedOptionIds: selectedOptionIds
	});
	
	currentExamIndex++;
	renderQuestion();
}

async function submitWholeQuiz(){
	try{
		const payload = {
			username: currentUsername,
			answers: userExamAnswers
		};
		
		const res = await fetch(`${API_BASE}/questions/submit`,{
			method:'POST',
			headers:{'Content-Type':'application/json'},
			body: JSON.stringify(payload)
		});
		
		const report = await res.json();
		displayReportCard(report);	
	}catch(err){
		alert("提交答案失敗!");
	}
}

function displayReportCard(report){
	document.getElementById('quiz-area').style.display ='none';
	document.getElementById('report-card').style.display = 'block';
	
	document.getElementById('final-score').innerHTML= report.score;
	document.getElementById('report-total').innerHTML= report.totalQuestions;
	document.getElementById('report-correct').innerText = report.correctCount;
	
	const detailsBox = document.getElementById('report-details-container');
	detailsBox.innerHTML='';
	
	report.details.forEach((detail, idx) =>{
		const q = detail.question;
		const card = document.createElement('div');
		card.className = `card ${detail.isCorrect ? 'correct-answer' : 'wrong-answer'}`;
	
		let optionsHtml ='';
		q.options.forEach(opt=>{
			const isSelected = detail.userSelectedOptionIds.includes(opt.id);
			const isCorrect = opt.isCorrect;
			let badge = '';
			if (isCorrect) badge += ' <b style="color:green;">[正確答案]</b>';
			if (isSelected) badge += ' <b style="color:blue;">[你的選擇]</b>';
			
			optionsHtml += `<div style="margin: 5px 0;">．${opt.optionText} ${badge}</div>`;
		});
		
		card.innerHTML = `
			<h4>第 ${idx + 1} 題:${detail.isCorrect ? '✅ 答對' : '❌ 答錯'}</h4>
			<div><b>題目:</b><div class="card-box">${q.title}</div></div>
			<div style="margin-top:10px;"><b>選擇列表:</b>${optionsHtml}</div>
			<div class="explanation-box">
				<b>💡 詳細解答說明:</b>
				<div>${q.explanation || '無說明'}</div>
			</div>
		`;
		detailsBox.appendChild(card);
	});	
}

function resetQuiz(){
	document.getElementById('player-setup').style.display='block';
	document.getElementById('report-card').style.display = 'none';
}

// ---2.完整閱讀區---

async function loadReaderData(){
	try{
		const res= await fetch(`${API_BASE}/questions`);
		const questions = await res.json();
		const container = document.getElementById('reader-container');
		container.innerHTML ='';
		
		questions.forEach((q,idx)=>{
			const card = document.createElement('div');
			card.className='card';
			
			let optsHtml = '';
			q.options.forEach(opt=>{
				optsHtml += `
					<div style="padding:6px; margin: 4px 0; 
					   background:${opt.isCorrect ? '#e6fffa' : '#f8f9fa'};
					   border-radius:4px;">
					   ${opt.isCorrect ? '✔ <b>[正確選項]</b> ' :						
					     '❌ '}${opt.optionText}
					</div>
				`;
			});
			
			card.innerHTML=`
				<h3>第 ${idx + 1} 題 (ID: ${q.id})</h3>
				<div><b>題目:</b><div class="card-box">${q.title}</div></div>
				<div style="margin: 10px 0;"><b>選項:</b>${optsHtml}</div>
				<div class="explanation-box"><b>💡 解答說明:</b>
				${q.explanation || '尚無說明'}</div>			
			`;
			container.appendChild(card);
		});
	}catch(err){
		console.error(err);
	}
}

// --- 3.後端管理維護 (CRUD + 富文本)---

function addAdminOptionRow(text= '', isCorrect=false){
	const wrapper = document.getElementById('admin-options-wrapper');
	const div = document.createElement('div');
	div.style.cssText = "display: flex; gap: 10px; align-items: center;margin-bottom:10px;";
	
	const cb=document.createElement('input');
	cb.type = 'checkbox';
	cb.checked = isCorrect;
	cb.style.cssText = "width: 20px; height: 20px;";
	
	const editorDiv = document.createElement('div');
	editorDiv.className = 'option-editor-container';
	
	const btnDel = document.createElement('button');
	btnDel.className = 'btn btn-danger btn-sm';
	btnDel.innerText = '刪除';
	btnDel.onclick = () =>{
		wrapper.removeChild(div);
	};
	
	div.appendChild(cb);
	div.appendChild(editorDiv);
	div.appendChild(btnDel);
	wrapper.appendChild(div);
	
	// 初始化富文本編輯器
	const qEditor = new Quill(editorDiv, { 
		theme: 'snow',
		modules: {toolbar: false}
	});
	if(text) qEditor.root.innerHTML = text;
	
	optionEditors.push({cb, qEditor, div});
}

async function saveQuestion(){
	const id = document.getElementById('edit-question-id').value;	
	const title = quillTitle.root.innerHTML;
	const explanation = quillExplanation.root.innerHTML;
	const timeLimitSeconds = 
		parseInt(document.getElementById('admin-time-limit').value);
		
	const options = [];
	const rows = document.querySelectorAll('#admin-options-wrapper > div');
	let hasCorrect = false;
	
	rows.forEach(row =>{
		const cb = row.querySelector('input[type="checkbox"]')
		const editorEl = 
			row.querySelector('.option-editor-container .ql-editor');
		const optionText = editorEl.innerHTML;
		
		if(editorEl.innerText.trim() !== ''){
			if(cb.checked) hasCorrect = true;
			options.push({
				optionText: optionText,
				isCorrect: cb.checked
			});
		}
	});
	
	if(options.length <2) return alert('請至少新增兩個有效選項');
	if(!hasCorrect) return alert('請至少勾選一個正確答案');

 	const payload = {title, explanation, timeLimitSeconds, options};
	const method = id ? 'PUT' : 'POST';
	const url = id ? `${API_BASE}/questions/${id}`: `${API_BASE}/questions`;
	
	try{
		const res = await fetch(url,{
			method: method,
			headers: {'Content-Type':'application/json'},
			body: JSON.stringify(payload)
		});
		
		if(res.ok){
			alert(id ? "修改成功!" : "新增成功!");
			resetAdminForm();
			loadAdminTable();
		}
	} catch (err){
		alert("儲存失敗");
	}
}

async function editQuestion(id){
	try{
		const res = await fetch(`${API_BASE}/questions/${id}`);
		const q = await res.json();
		
		document.getElementById('form-title').innerText = `✏ 編輯題目 (ID: ${q.id})`;
		document.getElementById('edit-question-id').value = q.id;
		quillTitle.root.innerHTML = q.title;
		quillExplanation.root.innerHTML = q.explanation || '';
		document.getElementById('admin-time-limit').value = q.timeLimitSeconds;
		
		// 清空舊選項
		document.getElementById('admin-options-wrapper').innerHTML = '';
		optionEditors = [];
		
		q.options.forEach(opt=>{
			addAdminOptionRow(opt.optionText, opt.isCorrect);
		});
		
		window.scrollTo({ top: 0, behavior: 'smooth'});
	}catch(err){
		alert("無法載入題目資訊");
	}
}

async function deleteQuestion(id){
	if(!confirm("確定要刪除這題嗎?"))return;
	try{
		await fetch(`${API_BASE}/questions/${id}`,{method: 'DELETE'});
		loadAdminTable();
	}catch(err){
		alert("刪除失敗");
	}
}

function resetAdminForm(){
	document.getElementById('form-title').innerText = "➕ 新增問答題";
	document.getElementById('edit-question-id').value = '';
	quillTitle.root.innerHTML = '';
	quillExplanation.root.innerHTML='';
	document.getElementById('admin-options-wrapper').innerHTML = '';
	optionEditors=[];
	addAdminOptionRow();
	addAdminOptionRow();
}


//非同步函式(async/await) 向後端API 請求排行榜資料、並動態顯示之
async function loadAdminTable(){
	try{
		//發送HTTP GET 請求，await 等待伺服器回應
		const res = await fetch(`${API_BASE}/questions`);
		//將回應的原始資料解析為JavaScript 物件或陣列
		const list = await res.json();		
		const tbody = document.getElementById('admin-table-body');
		tbody.innerHTML= '';
		
		//走訪陣列中的每一筆玩家資料
		//q:代表單一題目 ex:{id:1, title:"貓貓?"}		
		list.forEach(q => {
			const tr = document.createElement('tr');
			tr.innerHTML = `
				<td>${q.id}</td>
				<td>${q.title}</td>
				<td>${q.timeLimitSeconds} 秒</td>
				<td>${q.options ? q.options.length: 0} 個</td>
				<td>
					<button class="btn btn-warning btn-sm" 
						onclick="editQuestion(${q.id})">編輯</button>
					<button class="btn btn-danger btn-sm" 
						onclick="deleteQuestion(${q.id})">刪除</button>
				</td>
			`;
			tbody.appendChild(tr);
		});
	} catch(err){
		console.error(err);
	}
}

// ---- 4. 排行榜 ---

async function loadLeaderboard(){
	try{
		const res = await fetch(`${API_BASE}/players/leaderboard`);
		const data = await res.json();
		const tbody = document.getElementById('leaderboard-body');
		tbody.innerHTML = '';
		
		data.forEach((p,idx)=>{
			const tr = document.createElement('tr');
			tr.innerHTML = `
				<td>${idx + 1}</td>
				<td>${p.username}</td>
				<td>${p.highScore}</td>
			`;
			tbody.appendChild(tr);
		});
	}catch(err){
		console.error(err);
	}
}

