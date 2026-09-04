import base64
import subprocess
import sys
from pathlib import Path

target = Path(sys.argv[1]).resolve()
target.parent.mkdir(parents=True, exist_ok=True)
encoded = subprocess.check_output(["pbpaste"], text=True).strip()
target.write_bytes(base64.b64decode(encoded))
print(target)
