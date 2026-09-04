import base64
import json
import os
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "frontend"
OUTPUT = Path(__file__).resolve().parent / "screens"
OUTPUT.mkdir(parents=True, exist_ok=True)

CAPTURE_PAGE = b"""<!doctype html><meta charset='utf-8'><title>Capture sink</title>
<style>body{font-family:sans-serif;padding:24px}textarea{width:90vw;height:60vh}button{padding:12px 24px}</style>
<h1>Capture sink</h1><textarea id='data' autofocus></textarea><br><button id='save'>Save</button><p id='status'></p>
<script>
document.getElementById('save').onclick = async () => {
  const name = new URLSearchParams(location.search).get('name');
  const data = document.getElementById('data').value;
  const r = await fetch('/__save_screenshot', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({name, data})
  });
  document.getElementById('status').textContent = await r.text();
};
</script>"""

class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def do_GET(self):
        if self.path.startswith("/__capture"):
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(CAPTURE_PAGE)))
            self.end_headers()
            self.wfile.write(CAPTURE_PAGE)
            return
        # The lobby currently redirects to /pages/Games/... while the checked-in
        # frontend lives under /src/pages/Games/.... Keep the capture environment
        # compatible without changing application code.
        if self.path.startswith("/pages/"):
            self.path = "/src" + self.path
        super().do_GET()

    def do_POST(self):
        if self.path != "/__save_screenshot":
            self.send_error(404)
            return
        size = int(self.headers.get("Content-Length", "0"))
        payload = json.loads(self.rfile.read(size))
        name = os.path.basename(payload["name"])
        if not name.lower().endswith((".png", ".jpg", ".jpeg")):
            name += ".jpg"
        target = OUTPUT / name
        target.write_bytes(base64.b64decode(payload["data"]))
        message = f"saved {target.name}".encode()
        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(message)))
        self.end_headers()
        self.wfile.write(message)

ThreadingHTTPServer(("127.0.0.1", 8765), Handler).serve_forever()
