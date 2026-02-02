#!/usr/bin/env bash
set -euo pipefail

# --- Detect repo (owner/name) from current directory ---
REPO="$(gh repo view --json nameWithOwner -q .nameWithOwner)"
echo "Repo: $REPO"

api() { gh api -H "Accept: application/vnd.github+json" "$@"; }

ensure_milestone() {
  local title="$1"
  local desc="$2"

  # Try find existing milestone number by title (open milestones only)
  local num
  num="$(api "/repos/$REPO/milestones?state=open" --jq ".[] | select(.title==\"$title\") | .number" || true)"

  if [[ -n "${num:-}" ]]; then
    echo "Milestone exists: $title (#$num)"
    return 0
  fi

  echo "Creating milestone: $title"
  api --method POST "/repos/$REPO/milestones" \
    -f title="$title" \
    -f description="$desc" >/dev/null

  num="$(api "/repos/$REPO/milestones?state=open" --jq ".[] | select(.title==\"$title\") | .number")"
  echo "Created milestone: $title (#$num)"
}

create_issue() {
  local title="$1"
  local body="$2"
  local labels="$3"
  local milestone="$4"

  echo "Creating issue: $title"
  gh issue create \
    --title "$title" \
    --body "$body" \
    --label "$labels" \
    --milestone "$milestone" >/dev/null
}

# --- Ensure milestones (core roadmap) ---
ensure_milestone "v0.1-alpha" "Technical foundation: app skeleton, viewer, remote stream spike, FreeCAD API design"
ensure_milestone "v0.1-beta"  "End-to-end workflow: model → remote FreeCAD → live editing"
ensure_milestone "v0.1"       "Stabilized MVP release of Creator Control Client"

# --- Create v0.1-alpha (Server) issues ---
MS="v0.1-alpha"

issue_exists() {
  local title="$1"
  gh issue list --search "in:title \"$title\"" --json title -q ".[].title" | grep -Fxq "$title"
}

create_issue_safe() {
  local title="$1"
  local labels="$2"
  local milestone="$3"
  local body_file="$4"

  if issue_exists "$title"; then
    echo "Skip (already exists): $title"
    return 0
  fi

  echo "Creating issue: $title"
  gh issue create \
    --title "$title" \
    --label "$labels" \
    --milestone "$milestone" \
    --body-file "$body_file" >/dev/null
}

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

cat > "$tmpdir/body_bootstrap.md" <<'EOF'
Create minimal FastAPI service `ccc-freecad-remote` with:

- GET `/api/v1/healthz`
- GET `/api/v1/readyz` (checks `freecadcmd --version`)
- GET `/api/v1/info` (auth protected) incl. capabilities

## Acceptance Criteria
- /healthz returns 200 `{ok:true}`
- /readyz returns 200 when runner OK else 503
- /info returns service+capabilities (with auth)
EOF

cat > "$tmpdir/body_auth.md" <<'EOF'
Implement Bearer token auth via `Authorization: Bearer <token>`.

## Acceptance Criteria
- Missing header → 401
- Wrong token → 403
- Correct token → /info returns 200
EOF

cat > "$tmpdir/body_upload.md" <<'EOF'
Implement `POST /api/v1/files/upload` (multipart form).

Response includes `file_id`, `size`, `sha256`.

## Acceptance Criteria
- Upload works for STL
- File stored on disk using file_id
- Response returns sha256 + size
EOF

cat > "$tmpdir/body_import.md" <<'EOF'
Implement `POST /api/v1/files/import` that runs `freecadcmd scripts/import_model.py`.

## Acceptance Criteria
- Returns 200 `{ok:true, import_id, imported_objects}` on success
- Unsupported format returns clear error
- Errors surfaced as HTTP 500 with message
EOF

cat > "$tmpdir/body_cmds.md" <<'EOF'
Implement `POST /api/v1/commands/exec` with strict whitelist.

v0.1 can be placeholders, but must reject unknown commands.

## Acceptance Criteria
- Unknown command returns 400 `command_not_allowed`
- Allowed commands return `{ok:true}` response
EOF

cat > "$tmpdir/body_docs.md" <<'EOF'
Add/verify documentation:

- `docs/freecad-remote-api.md`
- `server/.env.example`

## Acceptance Criteria
- Spec lists endpoints, auth, request/response shapes
- env example matches server settings
EOF

cat > "$tmpdir/body_chore.md" <<'EOF'
Ensure dev ergonomics:

- `server/run_dev.sh` works
- `.gitignore` ignores runtime and venv
- optional: `server/tests/test_smoke.py`

## Acceptance Criteria
- `./server/run_dev.sh` starts service with reload
- No runtime data committed
- Smoke tests can run (optional)
EOF

create_issue_safe "server: FastAPI bootstrap (healthz/readyz/info)" "feat,scope-v0.1,infra" "$MS" "$tmpdir/body_bootstrap.md"
create_issue_safe "server: token auth (Bearer) for protected endpoints" "security,scope-v0.1" "$MS" "$tmpdir/body_auth.md"
create_issue_safe "server: file upload endpoint (multipart) + sha256" "feat,scope-v0.1" "$MS" "$tmpdir/body_upload.md"
create_issue_safe "server: headless import pipeline via freecadcmd script" "feat,freecad,scope-v0.1" "$MS" "$tmpdir/body_import.md"
create_issue_safe "server: commands whitelist endpoint (exec)" "design,feat,scope-v0.1" "$MS" "$tmpdir/body_cmds.md"
create_issue_safe "docs: FreeCAD Remote API spec + env example" "docs,scope-v0.1" "$MS" "$tmpdir/body_docs.md"
create_issue_safe "chore: dev run script + ignores + smoke tests" "chore,infra,scope-v0.1" "$MS" "$tmpdir/body_chore.md"

echo "Done ✅  Ensured v0.1-alpha server issues."

