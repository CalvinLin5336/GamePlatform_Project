# 組隊公告欄：HTML／JavaScript／jQuery 版

這個前端不使用 React、Vite、JSX 或 npm，可搭配現有 Spring Boot REST API。

## 啟動方式

1. 先在 Eclipse 啟動 Spring Boot，確認後端位於 `http://localhost:8080`。
2. 使用 VS Code 開啟本 `frontend` 資料夾。
3. 在 `index.html` 按右鍵，選擇 `Open with Live Server`。
4. 瀏覽器通常會開啟 `http://127.0.0.1:5500` 或 `http://localhost:5500`。

不可直接雙擊使用 `file://` 開啟，請使用 Live Server。

## API 位址

`assets/app.js` 會依目前前端主機自動連至：

```javascript
const API_BASE = `http://${window.location.hostname || 'localhost'}:8080/api`;
```

本機與同一 Wi-Fi 測試都不需要手動更換 IP。後端仍需允許前端來源的 CORS。

## 測試帳號

- `player02 / password`
- `teamleader / password`

## 功能

- 遊戲大廳與三種遊戲主題
- 登入、註冊與登出
- 公告列表、搜尋及狀態篩選
- 公告詳細、新增、編輯與刪除
- 申請加入、隊長審核
- 留言新增及刪除
- 收藏、通知與即刻開戰

## 團隊整合提醒

若團隊已經有共用登入 API，請將 `assets/app.js` 中 `/auth/login`、`/auth/register` 及會員欄位名稱改成團隊實際規格，不要建立第二套登入 Controller。
