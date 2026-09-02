# 組隊公告欄：Game／Lobby 串接

## 啟動

1. 重新啟動 `backend` 的 Spring Boot（Java 17 以上），預設後端為 `http://localhost:8080`。
2. 使用 Live Server 開啟本資料夾的 `index.html`；不要直接使用 `file://`。
3. 使用 `teamleader / password` 或 `player02 / password` 測試。

Board 是獨立 HTML／jQuery 頁面，不需要 React 或 npm build。後端保留
`spring.jpa.hibernate.ddl-auto=update`，啟動時會補上 Board 新增的欄位。

## 操作流程

- 隊長建立隊伍：從遊戲下拉選單選擇遊戲，再選擇資料庫提供的模式。
- 表單的「遊玩人數（含隊長）」可在模式的 `minPlayers`～`maxPlayers` 範圍內選擇；電腦人數由模式設定。
- 段位條件為選填，留空代表不限段位，後端儲存為 `null`。
- `對戰電腦` 若最大真人為 1，建立公告時立即滿員並建立 Lobby 房間。
- `玩家對戰` 先以隊長 1 人招募。其他玩家可用「遊戲＋模式＋狀態＋關鍵字」搜尋，並填寫加入留言。
- 隊長在「我的通知」或「隊長管理」同意／拒絕申請；達選定人數時，自動建立房間，並把隊長與全部核准隊員加入。
- 滿員後由隊長按「開始遊戲」。開始前可「踢除隊員」，隊伍恢復招募並同步移除房間成員；補招後沿用原房間。
- 隊長從通知或管理頁，隊員從「我的申請」，或雙方從公告詳情按「進入房間」。已開始的隊伍會顯示「進入遊戲」。
- 等待頁沿用 `../Lobby/waiting_room.html?room=房號&boardPostId=公告ID`。Board 會同步 Lobby 使用的 `localStorage.account`。
- Board 等待頁的開始／踢人會同步處理隊伍與房間；返回公告不會退出隊伍。已開始的房間會自動導向遊戲。
- 審核完成後，隊員重新開啟「我的申請」即可看到房間入口；目前未加入即時推播更新 Board 畫面。

### 日後新增遊戲的人數設定

在既有遊戲管理功能設定模式的 `minPlayers`、`maxPlayers` 與 `computerPlayers`。
例如最少 2、最多 4、電腦 0，建立隊伍就會出現 2／3／4 人選項，選 3 人便在 3 人核准到齊時建房。
最少與最多相同時只顯示固定人數。田忌撲克維持原設定：玩家對戰 2 人，對戰電腦 1 位真人＋1 位電腦。

## API

`assets/app.js` 依前端 hostname 連到後端的 8080 埠。

| 用途 | API |
|---|---|
| 遊戲及其啟用模式 | `GET /api/game-management/games` |
| 公告篩選 | `GET /board/team-posts?gameId=1&modeId=2&status=RECRUITING&keyword=...` |
| 建立公告 | `POST /board/team-posts` |
| 更新公告 | `PUT /board/team-posts/{id}` |
| 申請加入 | `POST /board/team-posts/{id}/join` |
| 審核 | `PUT /board/applications/{id}/APPROVED?captainId=1`（或 `REJECTED`） |
| 取得可進入的房號 | `GET /board/team-posts/{id}/room?memberId=1` |
| 隊長踢除已核准隊員 | `POST /board/team-posts/{id}/kick?captainId=1`，JSON：`{"account":"player02"}` |
| 隊長開始遊戲 | `POST /board/team-posts/{id}/start?captainId=1` |
| 查詢房間／個人遊戲入口 | `GET /board/team-posts/{id}/game?memberId=1` |
| Board 會員加入田忌撲克牌局 | `POST /board/team-posts/{id}/game/join?memberId=1` |

建立／更新公告範例：

```json
{
  "title": "今晚一起打牌",
  "gameId": 1,
  "modeId": 2,
  "playerCount": 2,
  "captainId": 1,
  "activityType": "新手友善",
  "startTime": "2026-09-03T20:00",
  "endTime": null,
  "voiceRequired": true,
  "description": "歡迎加入",
  "rankRequirement": null,
  "tags": "新手友善"
}
```

遊戲與模式 ID 必須以 API 實際回傳為準。後端使用 Game Management Service 查詢、驗證
模式所屬遊戲及啟用狀態，自行填入名稱、模式代碼、人數與公告狀態。
前端不提交 `gameName`、`modeName`、`maxPlayers` 或 `status` 作為設定。
`playerCount` 是隊長選擇的真人數，後端驗證範圍後保存為隊伍的 `maxPlayers`；省略時沿用模式上限。

## 後端分工與一致性

- `TeamPost` 保存 `gameId`、`gameName`、`modeId`、`modeCode`、`modeName`、
  `minPlayers`、`modeMaxPlayers`、`maxPlayers`、`computerPlayers`、`roomId`。
  `modeMaxPlayers` 保存模式上限，`maxPlayers` 保存本隊伍選定人數。
- `BoardRoomService` 呼叫既有 `LobbyController.createRoom()` 與 `joinRoom()`，分別沿用
  `/api/lobby/create-room` 與 `/api/lobby/join-room` 的輸入／回傳格式。
- 目前兩個模組共用 Spring 與 SQLite，因此由 Java 在同一交易內呼叫公開入口，
  不額外對本機送 HTTP。Board 不直接寫入 `rooms`，透過 Lobby 入口建立、加入、踢人及開始。
  若日後 Lobby 抽出 Service，可只更換此轉接層的呼叫對象。
- 建房、入房、踢人或開始失敗時，整筆公告／審核／人數／房間寫入回滾，可重新操作。
- 公告、申請與 Lobby 房間有版本檢查；開始與踢人的 WebSocket 事件在交易成功後才送出。
- 已保存房號不會再次建房，編輯公告也不會重設滿員狀態。踢除的申請改為 `CANCELLED`。
- SQLite 使用 `BoardSequenceGenerator` 在同一交易更新既有 `*_seq` 編號表，
  避免 Hibernate 預設分開取號造成鎖定。保留 Long ID 與原序列資料，編號可能跳號。
- 已有申請、隊員或房間時，不能更換遊戲／模式／選定人數。遊戲模式的人數設定若在招募期間改變，
  建房會提示重新建立隊伍，避免房間和公告人數不一致。

## 舊資料與整合範圍

舊公告沒有 `gameId`／`modeId` 時仍可瀏覽，但不會猜測遊戲或自動建立房間。
只有隊長一人且沒有申請的舊公告可編輯補選模式；已有隊員的舊公告請重新建立隊伍。
首次建立示範資料時，會從啟用遊戲模式產生真實可招募的公告，不再建立虛構隊員人數。

身份沿用 Board 現有會員與 `captainId`／`memberId` 介面。這些 ID 的歸屬檢查不等同完整登入驗證；
跨模組 JWT 與會員統一仍由既有登入整合負責。

Board 建立的房間固定使用公告中的遊戲、模式及人數。等待頁依目前 hostname 連線後端 HTTP／WebSocket 的 8080 埠。
田忌撲克入口由 Board 驗證已核准會員與房間名單後呼叫既有 `PokerGameService`，避免將 Board 會員 ID
誤當成 User 模組 ID；原本田忌撲克的遊戲規則與 User 入口不變。
未來遊戲可直接沿用可選人數的組隊與 Lobby 流程；實際遊戲頁仍需依該遊戲的會員／開局 API 接入。

## 驗證

```sh
cd backend
sh ./mvnw -Dtest=BoardRoomIntegrationTests test
```

測試使用獨立暫存 SQLite，涵蓋既有資料庫升級、單人立即建房、滿員審核與名單、
重複審核／超收防護、入房失敗回滾及重試、錯誤模式與隊長檢查、遊戲／模式篩選及過時版本更新。
另涵蓋三人隊伍與人數範圍、空白段位、通知關聯、踢人補招重用房間、隊長開始與交易回滾，
以及 Board 會員進入田忌撲克。此次執行全專案 22 項測試通過。
瀏覽器使用獨立暫存 SQLite 驗證了通知審核／踢人、2～4 人選單、段位留空建立三人隊伍，
以及通知頁開始對戰電腦後實際進入牌局並取得手牌。
若環境禁止 Mockito 動態附加 agent，可透過 Maven 的 `argLine` 指定已安裝的 `mockito-core` jar 為 `-javaagent`。
