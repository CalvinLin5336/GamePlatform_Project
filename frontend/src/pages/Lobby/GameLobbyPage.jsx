import React, { useState } from 'react';

const API_BASE = `http://${window.location.hostname}:8080`;

export default function GameLobbyPage() {
    // 狀態管理：控制彈窗顯示與否、以及目前選中的遊戲
    const [showModal, setShowModal] = useState(false);
    const [selectedGame, setSelectedGame] = useState(null);

    // 點擊遊戲方格時觸發：記錄遊戲名稱並打開彈窗
    const handleGameClick = (gameName) => {
        setSelectedGame(gameName);
        setShowModal(true);
    };

    // 點擊彈窗內「開始建立」時觸發：呼叫 Spring Boot API
    const handleCreateRoom = async () => {
        // 💡 嘗試從瀏覽器記憶體拿出玩家名字，如果沒登入就預設叫 "神秘玩家"
        const currentPlayer = localStorage.getItem("playerName") || "神秘玩家";

        try {
            // ⚠️ 請確認你的 Spring Boot 伺服器是跑在 8080 port
            const response = await fetch(`${API_BASE}/api/lobby/create-room`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ 
                    gameType: selectedGame 
                })
            });
            
            const data = await response.json();

            if (data.success) {
                alert('🎉 成功拿到後端資料！假房間 ID 是: ' + data.roomId);
                
                // 💡 未來這裡可以寫跳轉邏輯，把房間 ID 帶進去
                // window.location.href = `../poker_client.html?room=${data.roomId}`;
                
                // 成功後關閉彈窗
                setShowModal(false); 
            }
        } catch (error) {
            console.error('API 呼叫失敗:', error);
            alert('伺服器連線失敗，請檢查 Spring Boot 是否有啟動，或是 CORS 跨域設定是否正確！');
        }
    };

    return (
        // 外層容器：確保填滿可用空間並設定背景色
        <div className="p-8 text-white h-full w-full bg-slate-950 overflow-y-auto">
            <h1 className="text-3xl font-bold mb-8 bg-gradient-to-r from-indigo-400 to-cyan-400 bg-clip-text text-transparent">
                🎮 選擇你的遊戲
            </h1>

            {/* 遊戲選擇方格區 (Grid Layout) */}
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                
                {/* 遊戲方格：田忌撲克 */}
                <div onClick={() => handleGameClick('田忌撲克')} 
                     className="bg-slate-800 hover:bg-slate-700 border border-slate-700 hover:border-indigo-500 rounded-xl p-6 cursor-pointer transition transform hover:-translate-y-1 shadow-lg shadow-black/20">
                    <div className="text-5xl mb-4">🃏</div>
                    <h3 className="font-bold text-xl text-slate-100">田忌撲克</h3>
                    <p className="text-slate-400 text-sm mt-2">經典卡牌心理戰與策略對決</p>
                </div>

                {/* 遊戲方格：開發中佔位圖 */}
                <div className="bg-slate-800/40 border border-slate-700/50 rounded-xl p-6 opacity-60 cursor-not-allowed">
                    <div className="text-5xl mb-4 grayscale">🔒</div>
                    <h3 className="font-bold text-xl text-slate-400">更多遊戲</h3>
                    <p className="text-slate-500 text-sm mt-2">開發中，敬請期待...</p>
                </div>
            </div>

            {/* 遊戲設定彈跳視窗 (Modal) */}
            {showModal && (
                <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 transition-opacity">
                    <div className="bg-slate-900 border border-slate-700 rounded-2xl p-6 w-96 shadow-2xl animate-[fadeIn_0.2s_ease-out]">
                        
                        <h2 className="text-xl font-bold mb-5 text-slate-100 flex items-center gap-2">
                            <span>⚙️</span> 設定「{selectedGame}」
                        </h2>
                        
                        {/* 選項設定區塊 */}
                        <div className="mb-6">
                            <label className="block text-sm text-slate-400 mb-2">房間模式</label>
                            <select className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2.5 text-white focus:outline-none focus:border-indigo-500 transition">
                                <option>標準對戰</option>
                                <option>電腦對戰</option>
                            </select>
                        </div>

                        {/* 按鈕區塊 */}
                        <div className="flex justify-end gap-3 mt-8 pt-4 border-t border-slate-800">
                            <button onClick={() => setShowModal(false)} 
                                    className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm transition">
                                取消
                            </button>
                            <button onClick={handleCreateRoom} 
                                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium shadow-lg shadow-indigo-600/20 transition active:scale-95">
                                開始建立
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
