#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -d .venv ]]; then
  python3 -m venv .venv
fi

source .venv/bin/activate
pip install -r requirements.txt

# load env (optional)
if [[ -f .env ]]; then
  export $(grep -v '^#' .env | xargs) || true
fi

PORT="${BIND_PORT:-4828}"
if command -v lsof >/dev/null 2>&1; then
  if lsof -iTCP:"${PORT}" -sTCP:LISTEN -n -P >/dev/null 2>&1; then
    echo "Port ${PORT} is already in use:"
    lsof -iTCP:"${PORT}" -sTCP:LISTEN -n -P || true
    EXISTING_PID="$(lsof -t -iTCP:"${PORT}" -sTCP:LISTEN 2>/dev/null | head -n1 || true)"
    if [[ -n "${EXISTING_PID}" ]]; then
      echo "To stop it: kill -9 ${EXISTING_PID}"
    fi
  fi
elif command -v ss >/dev/null 2>&1; then
  if ss -ltnp "sport = :${PORT}" >/dev/null 2>&1; then
    echo "Port ${PORT} is already in use:"
    ss -ltnp "sport = :${PORT}" || true
  fi
fi

uvicorn freecad_remote.app:app --host "${BIND_HOST:-127.0.0.1}" --port "${PORT}" --reload
