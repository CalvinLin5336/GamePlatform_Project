const test = require('node:test'), assert = require('node:assert/strict'), fs = require('node:fs'), vm = require('node:vm'), path = require('node:path');
const base=path.join(__dirname,'../src/pages/Board/assets');
const utils={window:{}};vm.runInNewContext(fs.readFileSync(path.join(base,'board-utils.js'),'utf8'),utils);
const ui=utils.window.BoardUi;
test('new start dates reject the past and end dates reject invalid ranges',()=>{
    const now=new Date(2026,8,3,10,30,45);
    assert.equal(ui.nextMinute(now),'2026-09-03T10:31');
    assert.match(ui.timeError('2026-09-03T10:30','',null,now),/過去/);
    assert.equal(ui.timeError('2026-09-03T10:31','2026-09-03T11:00',null,now),'');
    assert.match(ui.timeError('2026-09-03T10:31','2026-09-03T10:00',null,now),/結束時間/);
    assert.match(ui.timeError('','',null,now),/開始時間/);
});
test('historical starts can remain unchanged when editing the description',()=>{
    const original='2026-09-01T10:00:13';
    assert.equal(ui.timeError(original,'',original,new Date(2026,8,3)),'');
    assert.match(ui.timeError('2026-09-02T10:00','',original,new Date(2026,8,3)),/過去/);
});
test('minimum time rolls over at midnight and pagination handles boundary pages',()=>{
    assert.equal(ui.nextMinute(new Date(2026,8,3,23,59,59)),'2026-09-04T00:00');
    assert.equal(ui.pagination({totalElements:0},'posts'),'');
    const first=ui.pagination({totalElements:21,page:0,totalPages:3},'posts');
    const last=ui.pagination({totalElements:21,page:2,totalPages:3},'posts');
    assert.match(first,/disabled>上一頁/);assert.doesNotMatch(first,/disabled>下一頁/);
    assert.match(last,/disabled>下一頁/);assert.match(last,/第 3／3 頁/);
});
function realtime(){
    const timers=new Map(), sockets=[],events=[],statuses=[];let id=0;
    class Socket {
        static OPEN=1;
        constructor(url){this.url=url;this.readyState=0;this.sent=[];sockets.push(this);}
        send(data){this.sent.push(JSON.parse(data));}
        close(){this.readyState=3;this.onclose?.({code:1000});}
        open(){this.readyState=1;this.onopen();}
        message(value){this.onmessage({data:JSON.stringify(value)});}
    }
    const window={};
    vm.runInNewContext(fs.readFileSync(path.join(base,'realtime.js'),'utf8'),{window,WebSocket:Socket,URL,Date,setTimeout:(fn,ms)=>{timers.set(++id,{fn,ms});return id;},clearTimeout:key=>timers.delete(key),setInterval:(fn,ms)=>{timers.set(++id,{fn,ms});return id;},clearInterval:key=>timers.delete(key)});
    const client=new window.BoardRealtime({base:'https://platform.example',onEvent:e=>events.push(e),onStatus:s=>statuses.push(s),onAuthError:()=>events.push({type:'AUTH_ERROR'})});
    return {client,sockets,events,timers};
}
test('WebSocket sends JWT only in authentication frame and resynchronizes after READY',()=>{
    const s=realtime();s.client.connect('private-token');const socket=s.sockets[0];socket.open();
    assert.equal(socket.url,'wss://platform.example/ws/board');assert.equal(socket.sent[0].type,'AUTH');assert.equal(socket.sent[0].token,'private-token');
    socket.message({type:'READY'});assert.equal(s.events[0].type,'RESYNC');
    socket.message({type:'NOTIFICATION',noticeId:1});assert.equal(s.events[1].noticeId,1);
    s.client.stop();assert.equal(s.timers.size,0);
});
test('guests subscribe without JWT and reconnection uses backoff',()=>{
    const s=realtime();s.client.connect('');s.sockets[0].open();assert.equal(s.sockets[0].sent[0].type,'SUBSCRIBE');
    s.sockets[0].close();const retry=[...s.timers.values()][0];assert.equal(retry.ms,1000);retry.fn();assert.equal(s.sockets.length,2);
    s.client.stop();
});
test('switching accounts cancels old sockets and ignores stale messages',()=>{
    const s=realtime();s.client.connect('old');const old=s.sockets[0];old.open();s.client.connect('new');
    old.message({type:'NOTIFICATION',noticeId:999});assert.equal(s.events.length,0);
    s.sockets[1].open();assert.equal(s.sockets[1].sent[0].token,'new');
    s.sockets[1].onmessage({data:'broken json'});assert.equal(s.events.length,0);s.client.stop();
});
