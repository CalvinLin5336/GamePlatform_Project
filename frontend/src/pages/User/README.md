# User Frontend Module

依照目前 Game Platform 的 User module 後端 API 建立，使用 HTML、CSS、JavaScript 與 jQuery，不使用 React / Vue。

## 頁面

- Login / Register：`/src/pages/User/Login/login.html`
- Admin：`/src/pages/User/Admin/admin.html`

Admin 頁面提供：

- Dashboard
- Users 完整 CRUD
- Operation Logs

## 登入流程

1. Lobby 或 Board 共用 Login 頁面與 `UserApi`。
2. 登入成功後將 token 與基本使用者資訊存入 `localStorage`。
3. 登入成功返回 `returnTo` 指定的本站頁面；未指定時導回 `/src/pages/Lobby/jquery_lobby.html`。外部網址不會被採用。
4. Admin 後台需要 `role = ADMIN`。
5. Board 開啟時呼叫 `/api/user/auth/me` 驗證 JWT 簽章、期限與會員是否啟用，再呼叫 `/board/auth/session` 取得 Board 會員 ID。
6. 登出會清除共用登入資料及 Board 快取；Board 的其他分頁同步更新。登入過期時，需登入的操作會返回登入頁。
7. 大廳建立／加入房間與等待頁會先驗證平台會員，不再代入 `test_account_01`／`test_account_02`。

## 資料庫與初始化

正式資料庫使用 `backend/gameplatform.db`（SQLite）；後端測試使用獨立暫存 SQLite，已移除未使用的 H2 依賴。
啟動時保留 User 資料表、角色與啟用狀態的必要初始化，不再自動建立固定密碼的 `admin` 測試帳號，
也不會刪除或覆寫現有會員。既有管理員仍可登入；一般會員由平台註冊或管理後台建立。
全新資料庫的第一位管理員需由維護者另外配置，不會提供預設管理員密碼。

### 其他前端頁面取得登入狀態

先載入 jQuery，再載入 `api/userApi.js`：

```javascript
const loggedIn = UserApi.isLoggedIn(); // 畫面顯示用的本機狀態
const user = UserApi.getCurrentUser(); // {userId, account, username, role, status} 或 null

UserApi.checkLogin().done(function (user) {
    $('#memberName').text(user.username); // 後端確認後的會員資料
}).fail(function (xhr) {
    if (xhr.status === 401 || xhr.status === 403) UserApi.redirectToLogin();
});
```

`UserApi.request(options)` 自動附上 Bearer token；收到 401 時清除過期登入。
一般管理 API 的 403 不會登出會員，避免把「沒有管理權限」誤認為「未登入」。
登入與註冊不會附上先前帳號的 token。前端 localStorage 的角色與登入提示不能代替後端授權。

## API

使用 `api/userApi.js` 對接：

- `POST /api/user/auth/login`
- `POST /api/user/auth/register`
- `GET /api/user/auth/me`
- `POST /board/auth/session`
- `GET /api/user/admin/dashboard`
- `GET /api/user/admin/users`
- `GET /api/user/admin/users/{id}`
- `POST /api/user/admin/users`
- `PUT /api/user/admin/users/{id}`
- `DELETE /api/user/admin/users/{id}`
- `GET /api/user/admin/operation-logs`

後端位址使用目前前端 hostname 的 8080 埠，因此同一份前端可直接用於本機或區網測試。
各頁面必須使用同一個前端 origin（協定、主機、埠），才能共用 localStorage 登入。

登入頁與預設大廳網址會依實際載入的 `User/api/userApi.js` 位置解析；Live Server 從 `frontend`、專案根目錄或 `src/pages` 啟動都可使用，不再固定導向網站根目錄的 `/src/pages/...`。由建立公告入口登入後，會返回建立隊伍表單。

## 驗證

`node --test frontend/tests/userApi.test.cjs`：登入狀態、過期 token、401／403、舊請求與跨頁返回網址。
後端 `BoardSessionIntegrationTests` 使用獨立暫存 SQLite 驗證會員狀態、兩模組 ID 對應及同名帳號防誤連結。
