#!/bin/sh
set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
SERVER_DIR=$(dirname "$SCRIPT_DIR")

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

open_payload='{"command":"open_new_doc","args":{}}'
curl -s -H "Authorization: Bearer ${CCC_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "$open_payload" \
  "$BASE_URL/api/v1/commands/exec" >/dev/null

export_payload='{"command":"export_current_doc","args":{"format":"stl"}}'
export_response=$(curl -s -H "Authorization: Bearer ${CCC_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\n%{http_code}" \
  -d "$export_payload" \
  "$BASE_URL/api/v1/commands/exec")
export_body=$(printf "%s" "$export_response" | head -n 1)
export_status=$(printf "%s" "$export_response" | tail -n 1)

if [ "$export_status" != "200" ]; then
  echo "Expected 200 export, got ${export_status}"
  echo "$export_body"
  exit 1
fi

download_url=$(python - <<PY
import json
import sys
body = json.loads('''$export_body''')
result = body.get("result") or {}
download_url = result.get("download_url")
if not download_url:
    print("missing_download_url", file=sys.stderr)
    sys.exit(1)
print(download_url)
PY
)

output_path="/tmp/ccc_export.stl"
curl -s -H "Authorization: Bearer ${CCC_TOKEN}" \
  "$BASE_URL$download_url" \
  -o "$output_path"

if [ ! -s "$output_path" ]; then
  echo "Export download failed or empty: $output_path"
  exit 1
fi

echo "smoke test passed"
