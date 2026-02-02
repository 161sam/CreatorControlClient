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

uvicorn freecad_remote.app:app --host "${BIND_HOST:-127.0.0.1}" --port "${BIND_PORT:-4828}" --reload
