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
  if [ "$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/capabilities")" = "401" ]; then
    break
  fi
  i=$((i + 1))
  sleep 0.2
done

status_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/capabilities")
if [ "$status_code" != "401" ]; then
  echo "Expected 401 without token, got ${status_code}"
  exit 1
fi

response=$(curl -s -H "Authorization: Bearer ${CCC_TOKEN}" \
  -w "\n%{http_code}" \
  "$BASE_URL/api/v1/capabilities")
body=$(printf "%s" "$response" | head -n 1)
status_code=$(printf "%s" "$response" | tail -n 1)

if [ "$status_code" != "200" ]; then
  echo "Expected 200 with token, got ${status_code}"
  exit 1
fi

echo "$body" | grep -q '"capabilities"' || { echo "Missing capabilities in response"; exit 1; }
echo "$body" | grep -q '"version"' || { echo "Missing version in response"; exit 1; }
echo "$body" | grep -q '"auth"' || { echo "Missing auth in response"; exit 1; }
echo "$body" | grep -q '"freecad"' || { echo "Missing freecad in response"; exit 1; }
echo "$body" | grep -q '"session"' || { echo "Missing session in response"; exit 1; }

response=$(curl -s -H "Authorization: Bearer ${CCC_TOKEN}" \
  -w "\n%{http_code}" \
  "$BASE_URL/api/v1/commands")
body=$(printf "%s" "$response" | head -n 1)
status_code=$(printf "%s" "$response" | tail -n 1)

if [ "$status_code" != "200" ]; then
  echo "Expected 200 with token, got ${status_code}"
  exit 1
fi

echo "$body" | grep -q '"commands"' || { echo "Missing commands in response"; exit 1; }
echo "$body" | grep -q '"name"' || { echo "Missing command name in response"; exit 1; }

echo "smoke test passed"
