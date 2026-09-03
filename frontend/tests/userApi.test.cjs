const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync(require('node:path').join(__dirname, '../src/pages/User/api/userApi.js'), 'utf8');

function jwt(id = 7, account = 'player', exp = Date.now() / 1000 + 3600) {
    return 'header.' + Buffer.from(JSON.stringify({userId:id, sub:account, exp})).toString('base64url') + '.signature';
}

function setup(href = 'http://127.0.0.1:5500/src/pages/Board/index.html#form/new', scriptSrc) {
    const values = new Map(), calls = [], events = [];
    const localStorage = {getItem:key => values.get(key) ?? null, setItem:(key,value) => values.set(key,String(value)), removeItem:key => values.delete(key)};
    const url = new URL(href);
    const location = {hostname:url.hostname, origin:url.origin, href:url.href, pathname:url.pathname, assign:value => {location.assigned = value;}};
    const $ = {
        extend:(...args) => Object.assign(...args.filter(arg => typeof arg !== 'boolean')),
        Deferred:() => ({reject(error) { this.error = error; return this; }, promise() {return Promise.reject(this.error);}}),
        ajax:options => {
            let resolve, reject;
            const promise = new Promise((yes,no) => {resolve=yes;reject=no;});
            promise.fail = callback => {promise.catch(callback); return promise;};
            calls.push({options, resolve, reject});
            return promise;
        }
    };
    const window = {location, document:{currentScript:scriptSrc ? {src:scriptSrc} : null}, atob:value => Buffer.from(value,'base64').toString('binary'), dispatchEvent:event => events.push(event.type)};
    vm.runInNewContext(source, {window, jQuery:$, localStorage, URL, CustomEvent:class {constructor(type){this.type=type;}}});
    return {api:window.UserApi, values, calls, events, location};
}

function login(s, token = jwt()) {s.api.saveLoginSession({token, userId:7, account:'player', username:'隊長', role:'PLAYER', status:'Active'});}

test('session exposes platform identity and clears stale Board login on account change', () => {
    const s=setup();
    s.values.set('sgpUser','old user'); s.values.set('boardMember','old member');
    login(s);
    assert.equal(s.api.isLoggedIn(),true);
    assert.equal(s.api.getCurrentUser().userId,7);
    assert.equal(s.api.getCurrentUser().username,'隊長');
    assert.equal(s.values.has('sgpUser'),false);
    assert.equal(s.values.has('boardMember'),false);
    assert.equal(s.events.at(-1),'user-session-changed');
});

test('expired, malformed, disabled or mismatched local session is not considered logged in', () => {
    const s=setup();
    for (const token of ['invalid', jwt(7,'player',1), jwt(8), jwt(7,'someone-else')]) {
        login(s,token); assert.equal(s.api.isLoggedIn(),false);
    }
    login(s); s.values.set('status','Disabled'); assert.equal(s.api.getLoginSession(),null);
});

test('authenticated requests carry JWT; admin forbidden does not sign out; unauthorized clears only session keys', async () => {
    const s=setup(); login(s); s.values.set('unrelated','keep');
    const forbidden=s.api.getUsers();
    assert.equal(s.calls[0].options.url,'http://127.0.0.1:8080/api/user/admin/users');
    assert.equal(s.calls[0].options.headers.Authorization,'Bearer '+s.api.getToken());
    s.calls[0].reject({status:403}); await assert.rejects(forbidden);
    assert.equal(s.api.isLoggedIn(),true);
    const expired=s.api.getUsers(); s.calls[1].reject({status:401}); await assert.rejects(expired);
    assert.equal(s.api.getToken(),''); assert.equal(s.values.get('unrelated'),'keep');
});

test('login never attaches an old token or clears an unrelated session on wrong password', async () => {
    const s=setup(); login(s);
    const attempt=s.api.login('player','wrong');
    assert.equal(s.calls[0].options.headers.Authorization,undefined);
    s.calls[0].reject({status:401}); await assert.rejects(attempt);
    assert.equal(s.api.isLoggedIn(),true);
});

test('checkLogin refreshes identity from server and Board session preserves its independent member ID', async () => {
    const s=setup(); login(s);
    const check=s.api.checkLogin();
    assert.match(s.calls[0].options.url,/\/auth\/me$/);
    s.calls[0].resolve({id:7, account:'player', username:'伺服器暱稱', role:'PLAYER', status:'Active'});
    assert.equal((await check).username,'伺服器暱稱');
    const board=s.api.getBoardSession();
    s.calls[1].resolve({id:51,platformUserId:7,account:'player'});
    assert.equal((await board).id,51);
    assert.equal(s.api.getCurrentUser().userId,7);
    assert.equal(JSON.parse(s.values.get('boardMember')).id,51);
});

test('disabled current user is signed out; temporary service errors preserve login', async () => {
    const s=setup(); login(s);
    const offline=s.api.checkLogin(); s.calls[0].reject({status:500}); await assert.rejects(offline);
    assert.equal(s.api.isLoggedIn(),true);
    const disabled=s.api.checkLogin(); s.calls[1].reject({status:403}); await assert.rejects(disabled);
    assert.equal(s.api.isLoggedIn(),false);
});

test('late response from a previous token cannot clear or replace a new login', async () => {
    const s=setup(); login(s);
    const old=s.api.checkLogin();
    const newer=jwt(7,'player',Date.now()/1000+7200); login(s,newer);
    s.calls[0].reject({status:401}); await assert.rejects(old);
    assert.equal(s.api.getToken(),newer);
});

test('login return target keeps Board route and rejects external redirects', () => {
    const s=setup(); s.api.redirectToLogin();
    const target=new URL(s.location.assigned);
    assert.equal(target.pathname,'/src/pages/User/Login/login.html');
    assert.equal(target.searchParams.get('returnTo'),s.location.href);
    const valid=setup(s.location.assigned);
    assert.equal(valid.api.getLoginReturnUrl(),s.location.href);
    const external=setup('http://127.0.0.1:5500/src/pages/User/Login/login.html?returnTo=https://example.com');
    assert.equal(external.api.getLoginReturnUrl(),'http://127.0.0.1:5500/src/pages/Lobby/jquery_lobby.html');
});

test('login URL follows the actual User API script under different Live Server roots', () => {
    for (const pages of ['/src/pages/', '/frontend/src/pages/', '/GamePlatform_Project/frontend/src/pages/', '/']) {
        const origin = 'http://10.10.2.151:5500';
        const board = origin + pages + 'Board/index.html#form/new';
        const script = origin + pages + 'User/api/userApi.js';
        const s = setup(board, script);
        s.api.redirectToLogin(board);
        const loginUrl = new URL(s.location.assigned);
        assert.equal(loginUrl.pathname, pages + 'User/Login/login.html');
        assert.equal(loginUrl.searchParams.get('returnTo'), board);
        assert.equal(setup(loginUrl.href, script).api.getLoginReturnUrl(), board);
        assert.equal(setup(origin + pages + 'User/Login/login.html', script).api.getLoginReturnUrl(), origin + pages + 'Lobby/jquery_lobby.html');
    }
});

test('script location remains authoritative for nested pages and self-login redirects are rejected', () => {
    const script = 'http://localhost:5500/frontend/src/pages/User/api/userApi.js';
    const s = setup('http://localhost:5500/custom/wrapper.html', script);
    s.api.redirectToLogin(undefined, 'register');
    const url = new URL(s.location.assigned);
    assert.equal(url.pathname, '/frontend/src/pages/User/Login/login.html');
    assert.equal(url.searchParams.get('mode'), 'register');
    const self = setup('http://localhost:5500/frontend/src/pages/User/Login/login.html?returnTo=/frontend/src/pages/user/login/login.html', script);
    assert.equal(self.api.getLoginReturnUrl(), 'http://localhost:5500/frontend/src/pages/Lobby/jquery_lobby.html');
});
