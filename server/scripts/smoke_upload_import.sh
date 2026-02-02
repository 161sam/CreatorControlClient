#!/bin/sh
set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
SERVER_DIR=$(dirname "$SCRIPT_DIR")
FIXTURE="$SCRIPT_DIR/fixtures/sample.stl"

cd "$SERVER_DIR"

if [ ! -d .venv ]; then
  python3 -m venv .venv
fi

. .venv/bin/activate
pip install -r requirements.txt

export CCC_TOKEN="test-token"

uvicorn freecad_remote.app:app --host 127.0.0.1 --port 4828 &
SERVER_PID=$!

cleanup() {
  kill "$SERVER_PID" 2>/dev/null || true
  wait "$SERVER_PID" 2>/dev/null || true
}

trap cleanup EXIT

BASE_URL="http://127.0.0.1:4828"

i=0
while [ "$i" -lt 20 ]; do
  if [ "$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/healthz")" = "401" ]; then
    break
  fi
  i=$((i + 1))
  sleep 0.2
done

upload_response=$(curl -s -H "Authorization: Bearer ${CCC_TOKEN}" \
  -w "\n%{http_code}" \
  -F "file=@${FIXTURE};type=application/sla" \
  "$BASE_URL/api/v1/files/upload")
upload_body=$(printf "%s" "$upload_response" | head -n 1)
status_code=$(printf "%s" "$upload_response" | tail -n 1)

if [ "$status_code" != "200" ]; then
  echo "Expected 200 upload, got ${status_code}"
  echo "$upload_body"
  exit 1
fi

file_id=$(python - <<PY
import json
import sys
body = json.loads('''$upload_body''')
file_id = body.get("file_id") or body.get("id")
if not file_id:
    print("missing_file_id", file=sys.stderr)
    sys.exit(1)
print(file_id)
PY
)

import_payload=$(printf '{"command":"import_file","args":{"file_id":"%s"}}' "$file_id")
import_response=$(curl -s -H "Authorization: Bearer ${CCC_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\n%{http_code}" \
  -d "$import_payload" \
  "$BASE_URL/api/v1/commands/exec")
import_body=$(printf "%s" "$import_response" | head -n 1)
import_status=$(printf "%s" "$import_response" | tail -n 1)

if [ "$import_status" != "200" ]; then
  echo "Expected 200 import, got ${import_status}"
  echo "$import_body"
  exit 1
fi

python - <<PY
import json
body = json.loads('''$import_body''')
result = body.get("result") or {}
status = result.get("status")
if not status:
    raise SystemExit("missing status in import response")
PY

echo "smoke test passed"
