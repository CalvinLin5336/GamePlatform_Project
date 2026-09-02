# User Frontend Module

依照目前 Game Platform 的 User module 後端 API 建立，使用 HTML、CSS、JavaScript 與 jQuery，不使用 React / Vue。

## 頁面

- Login / Register：`/src/pages/User/Login/index.html`
- Admin：`/src/pages/User/Admin/index.html`

Admin 頁面提供：

- Dashboard
- Users 完整 CRUD
- Operation Logs

## 登入流程

1. 從 Lobby 的登入按鈕進入 Login 頁面（本次沒有修改 Lobby）。
2. 登入成功後將 token 與基本使用者資訊存入 `localStorage`。
3. 登入成功導回：`/src/pages/Lobby/jquery_lobby.html`
4. Admin 後台需要 `role = ADMIN`。

## API

使用 `api/userApi.js` 對接：

- `POST /api/user/auth/login`
- `POST /api/user/auth/register`
- `GET /api/user/admin/dashboard`
- `GET /api/user/admin/users`
- `GET /api/user/admin/users/{id}`
- `POST /api/user/admin/users`
- `PUT /api/user/admin/users/{id}`
- `DELETE /api/user/admin/users/{id}`
- `GET /api/user/admin/operation-logs`

預設後端位址：`http://localhost:8080`。
