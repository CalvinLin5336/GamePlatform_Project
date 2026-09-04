(function (window) {
    'use strict';
    window.BoardRealtime = function (options) {
        let socket = null, reconnect = null, heartbeat = null, stopped = true, token = '', generation = 0, attempts = 0, lastPong = 0;
        function clearTimers() { clearTimeout(reconnect); clearInterval(heartbeat); }
        function open(version) {
            if (stopped || version !== generation) return;
            options.onStatus('連線中');
            const url = new URL('/ws/board', options.base || window.location.origin);
            url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
            try { socket = new WebSocket(url.href); } catch (_) { retry(version); return; }
            const active = socket;
            active.onopen = function () {
                if (stopped || version !== generation) { active.close(); return; }
                active.send(JSON.stringify(token ? {type:'AUTH', token} : {type:'SUBSCRIBE'}));
                lastPong = Date.now();
                heartbeat = setInterval(function () {
                    if (Date.now() - lastPong > 60000) { active.close(); return; }
                    if (active.readyState === WebSocket.OPEN) active.send(JSON.stringify({type:'PING'}));
                }, 25000);
            };
            active.onmessage = function (message) {
                if (stopped || version !== generation) return;
                let event;
                try { event = JSON.parse(message.data); } catch (_) { return; }
                if (event.type === 'PONG') { lastPong = Date.now(); return; }
                if (event.type === 'READY') { attempts = 0; options.onStatus('已連線'); options.onEvent({type:'RESYNC'}); return; }
                options.onEvent(event);
            };
            active.onerror = function () { active.close(); };
            active.onclose = function (event) {
                if (stopped || version !== generation) return;
                clearInterval(heartbeat);
                if (event.code === 1008 && token) options.onAuthError();
                retry(version);
            };
        }
        function retry(version) {
            if (stopped || version !== generation) return;
            options.onStatus('重新連線中');
            clearTimeout(reconnect);
            reconnect = setTimeout(() => open(version), Math.min(30000, 1000 * 2 ** Math.min(attempts++, 5)));
        }
        return {
            connect(value) { this.stop(); stopped = false; token = value || ''; open(generation); },
            stop() { stopped = true; generation++; clearTimers(); if (socket) socket.close(); socket = null; }
        };
    };
})(window);
