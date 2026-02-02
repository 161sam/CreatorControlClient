# CCC FreeCAD Remote API (v0.1)

Base:
- Base URL: `http://<host>:4828/api/v1`
- Auth: `Authorization: Bearer <token>`
- Content-Type: `application/json`

## Endpoints

### Health
- `GET /healthz` → `{ ok: true }`
- `GET /readyz` → 200 if runner ready else 503

### Info
- `GET /info` (auth) → service, version, capabilities

### Commands (whitelist)
- `POST /commands/exec` (auth)

Request:

{ "command": "fit_all", "args": {}, "request_id": "..." }


Response:

{ "ok": true, "request_id": "...", "result": { "message": "..." } }

Files

    POST /files/upload (auth, multipart file=...)
    Response:

{ "ok": true, "file_id": "file_...", "name": "...", "size": 123, "sha256": "..." }

    POST /files/import (auth)
    Request:

{ "file_id": "file_...", "format": "stl", "into_document": null, "options": {} }

    GET /files/download/{export_id} (auth) → file stream

Security (v0.1)

    Single token in .env for LAN/self-host

    No arbitrary Python execution via API

    Only whitelisted commands

Notes

v0.1 uses a headless runner (freecadcmd) for import/export and minimal ops.
GUI session integration is planned for v0.2 via a FreeCAD add-on.
