const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const base = 'http://127.0.0.1:8765';
const out = path.join(__dirname, 'screens');
fs.mkdirSync(out, { recursive: true });

const payload = Buffer.from(JSON.stringify({
  sub: 'thomas', userId: 1, username: 'Thomas', account: 'thomas',
  role: 'ADMIN', exp: Math.floor(Date.now() / 1000) + 86400
})).toString('base64url');
const token = `eyJhbGciOiJub25lIn0.${payload}.demo`;

const games = [
  { gameId: 1, gameCode: 'POKER', gameName: '田忌撲克', description: '第一輪選 3 張牌，第二、三輪各選 5 張牌的三輪撲克遊戲。', imagePath: '/src/assets/Games/poker/poker_game_icon.png', frontendPath: '/src/pages/Games/poker/poker_client.html', modes: [{ modeId: 1, modeCode: 'COMPUTER', modeName: '對戰電腦', minPlayers: 1, maxPlayers: 1 }, { modeId: 2, modeCode: 'PLAYER', modeName: '玩家對戰', minPlayers: 2, maxPlayers: 2 }] },
  { gameId: 2, gameCode: 'QUIZ', gameName: '限時問答挑戰', description: '20 題計時問答挑戰，涵蓋電腦科學與軟體開發。', imagePath: '/src/assets/Games/quiz/quiz_game_icon.png', frontendPath: '/src/pages/Games/quiz/quiz_client.html', modes: [{ modeId: 3, modeCode: 'SINGLE', modeName: '單人挑戰', minPlayers: 1, maxPlayers: 1 }] }
];

const posts = [
  { id: 101, title: '一起來玩撲克牌！', gameName: '田忌撲克', modeName: '玩家對戰', activityType: '新手友善', description: '歡迎喜歡策略遊戲的朋友一起來玩！', startTime: '2026-09-04T20:00', currentPlayers: 1, maxPlayers: 2, status: 'RECRUITING', captain: { id: 1, nickname: 'Thomas' } },
  { id: 102, title: '問答高手來挑戰！', gameName: '限時問答挑戰', modeName: '單人挑戰', activityType: '知識挑戰', description: '20 題限時挑戰，看看誰能登上排行榜。', startTime: '2026-09-05T19:30', currentPlayers: 1, maxPlayers: 1, status: 'FULL', captain: { id: 2, nickname: 'Amy' } },
  { id: 103, title: '週末休閒牌局', gameName: '田忌撲克', modeName: '玩家對戰', activityType: '休閒交流', description: '週末輕鬆玩，歡迎新手。', startTime: '2026-09-06T15:00', currentPlayers: 2, maxPlayers: 2, status: 'FULL', captain: { id: 3, nickname: 'Jacky' } }
];

async function routeApi(page) {
  await page.route('http://127.0.0.1:8080/**', async route => {
    const url = route.request().url();
    let body = { success: true };
    if (url.includes('/api/lobby/games-info')) body = { success: true, games };
    else if (url.includes('/api/lobby/room/54FA6780')) body = { success: true, room: { id: '54FA6780', gameId: 1, modeId: 2, hostAccount: 'thomas', minPlayers: 2, maxPlayers: 2, players: ['thomas', 'amy'], status: 'WAITING' } };
    else if (url.includes('/api/user/auth/me')) body = { id: 1, account: 'thomas', username: 'Thomas', role: 'ADMIN', status: 'Active' };
    else if (url.includes('/api/user/player/me')) body = { id: 1, account: 'thomas', username: 'Thomas', avatar: null, description: '喜歡策略與問答遊戲的玩家。', lastLogin: '2026-09-04 10:30', role: 'PLAYER', status: 'Active' };
    else if (url.includes('/api/user/admin/dashboard')) body = { totalUsers: 18, activeUsers: 16, disabledUsers: 2, adminUsers: 2, todayOperations: 27 };
    else if (url.includes('/api/user/admin/operation-logs')) body = [{ createdAt: '2026-09-04 10:20', account: 'admin', action: 'UPDATE', targetId: 8, role: 'ADMIN', description: '更新使用者資料' }];
    else if (url.includes('/api/user/admin/users')) body = [
      { id: 1, account: 'thomas', username: 'Thomas', role: 'ADMIN', status: 'Active', lastLogin: '2026-09-04 10:30' },
      { id: 2, account: 'amy', username: 'Amy', role: 'PLAYER', status: 'Active', lastLogin: '2026-09-04 10:18' },
      { id: 3, account: 'jacky', username: 'Jacky', role: 'PLAYER', status: 'Disabled', lastLogin: '2026-09-03 21:05' }
    ];
    else if (url.includes('/api/game-management/games')) body = games;
    else if (url.includes('/board/team-posts/page')) body = { content: posts, page: 0, totalPages: 1, totalElements: posts.length };
    else if (url.includes('/board/notifications/summary')) body = { unread: 3, unreadComments: 1, categories: { CAPTAIN: 2, APPLICANT: 1, WATCHING: 0 } };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
  });
}

async function capture(browser, name, url, options = {}) {
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: 1 });
  await routeApi(page);
  if (options.session) {
    await page.addInitScript(({ token }) => {
      localStorage.setItem('token', token); localStorage.setItem('userId', '1');
      localStorage.setItem('account', 'thomas'); localStorage.setItem('username', 'Thomas');
      localStorage.setItem('role', 'ADMIN'); localStorage.setItem('status', 'Active');
    }, { token });
  }
  await page.goto(base + url, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(options.wait || 1200);
  await page.screenshot({ path: path.join(out, `${name}.jpg`), type: 'jpeg', quality: 92, fullPage: false });
  await page.close();
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  await capture(browser, '01-login', '/src/pages/User/Login/login.html');
  await capture(browser, '02-platform', '/src/pages/Chat/chatclient.html', { wait: 1600 });
  await capture(browser, '03-lobby', '/src/pages/Lobby/jquery_lobby.html', { wait: 1600 });
  await capture(browser, '04-waiting', '/src/pages/Lobby/waiting_room.html?room=54FA6780', { session: true, wait: 1600 });
  await capture(browser, '05-board', '/src/pages/Board/index.html#posts', { wait: 1600 });
  await capture(browser, '06-poker', '/src/pages/Games/poker/poker_client.html', { wait: 1000 });
  await capture(browser, '07-quiz', '/src/pages/Games/quiz/quiz_client.html', { session: true, wait: 1200 });
  await capture(browser, '08-player', '/src/pages/User/Player/player.html', { session: true, wait: 1200 });
  await capture(browser, '09-admin', '/src/pages/User/Admin/admin.html?tab=users', { session: true, wait: 1200 });
  await browser.close();
  console.log(out);
})().catch(err => { console.error(err); process.exit(1); });
