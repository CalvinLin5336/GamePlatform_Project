# GamePlatform Frontend

前端目前使用一般 HTML、CSS 與 JavaScript，各功能頁面位於 `src/pages`。

- 主畫面：`src/pages/Chat/chatclient.html`
- 遊戲大廳：`src/pages/Lobby/jquery_lobby.html`
- 房間等待區：`src/pages/Lobby/waiting_room.html`
- 遊戲頁面：`src/pages/Games`

開發時可使用 VS Code Live Server 開啟 `index.html`；專案不使用 React、Vite 或 npm 套件。

若日後將前端搬入 Spring Boot，請把頁面與素材放入 `src/main/resources/static`，並由 Spring Boot 直接提供靜態檔案。
