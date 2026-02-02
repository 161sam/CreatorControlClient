# Creator Control Client (CCC)

CCC is a **local-first control layer** for creative desktop tools. It lets an Android device
talk to a self-hosted server that exposes explicit command APIs (starting with FreeCAD).
No cloud, no screen scraping—just inspectable, user-owned control. 

**Features**
- Android Automotive client with command browser and diagnostics
- FastAPI control server with token auth
- File upload/import and export/download workflows
- Explicit health/info/version endpoints for verification

**Creator Control Client (CCC)** is an open-source, privacy-first system for
**remote control of creative desktop tools from mobile devices** – starting with **FreeCAD**.

CCC is **not a cloud service**.  
CCC is **not a remote desktop clone**.  
CCC is a **creator-native control layer**.

---

## What is CCC?

CCC consists of two main components:

1. **Android Client**
   - Native Android app (Kotlin)
   - Designed for creators, makers, engineers
   - Mobile-first UI (not a shrunken desktop)

2. **Self-hosted Control Server**
   - Python + FastAPI
   - Headless control of desktop tools (FreeCAD first)
   - Explicit command API instead of screen streaming

The system allows you to **inspect, upload and control CAD projects remotely**
without VNC, cloud lock-in or vendor dependencies.

---

## Current Status (v0.1)

🚧 **Active development – technical foundation phase**

### Implemented
- ✅ FreeCAD Remote Control Server (FastAPI)
- ✅ Token-based authentication
- ✅ Health & info endpoints
- ✅ Headless FreeCAD execution (`freecadcmd`)
- ✅ Android app skeleton
- ✅ Android ↔ Server connectivity (Retrofit + Moshi)
- ✅ End-to-end health check from Android device
- ✅ File upload and import pipeline

### In Progress
- ⏳ Command whitelist execution
- ⏳ Basic Android UI for server state & actions

### Planned (later milestones)
- ⏳ Native 3D preview (STL / OBJ)
- ⏳ Remote render / stream experiments
- ⏳ Multi-tool support (beyond FreeCAD)
- ⏳ Rust-based low-latency streaming (research)

---

## Architecture (High Level)

```text
[ Android App ]
      |
      |  HTTPS (JSON, multipart)
      v
[ CCC Control Server ]
      |
      |  Headless CLI
      v
[ FreeCAD / Desktop Tools ]
````

**Key principle:**
We control **intent**, not pixels.

---

## Why CCC?

Most existing solutions fall into one of two categories:

* 🖥️ Remote desktop tools (VNC, RDP)
* ☁️ Cloud-based CAD platforms

CCC deliberately chooses a third path:

* No cloud dependency
* No vendor lock-in
* No screen scraping
* Explicit, inspectable control APIs
* Creator-owned infrastructure

---

## Repository Structure

```text
CreatorControlClient/
├── android/        # Android client (Kotlin)
├── server/         # FastAPI FreeCAD control server
├── docs/           # API & design documentation
├── scripts/        # Dev & automation scripts
├── data/           # Runtime data (ignored by git)
├── PROJECT.md
├── ROADMAP.md
├── AGENTS.md
└── README.md
```

---

## Quickstart (5 minutes)

1. **Start the server**

   ```bash
   cd server
   CCC_TOKEN=dev-token-change-me ./run_dev.sh
   ```

2. **Enable USB reverse (Android ↔ PC)**

   ```bash
   adb reverse tcp:4828 tcp:4828
   ```

3. **Install and run the Android app**

   ```bash
   cd android
   ./gradlew :automotive:assembleDebug
   ```

4. **Verify in the UI**
   - Healthz / Info / Version show green status
   - Run a command (e.g. `open_new_doc`)
   - Upload → Import
   - Export → Download

---

## Common Issues

- **401 Unauthorized**: token missing or invalid. Set `CCC_TOKEN` for the server and update
  `CCC_TOKEN` in the Android build config fields.
- **`adb reverse` not set**: ensure `adb reverse tcp:4828 tcp:4828` is active for USB mode.
- **Port already in use**: stop the other process or change the port in server settings.
- **FreeCAD not found**: install `freecadcmd` or set `freecad_cmd` in `server/.env`.

---

## Development Setup

### Server

```bash
cd server
export CCC_TOKEN="dev-token-change-me"
./run_dev.sh
```

`CCC_TOKEN` is required and must be set before starting the server. For
backward compatibility you can also provide `api_token=...` in `server/.env`.
Optionally set `CCC_GIT_SHA` to expose the current build revision in `/api/v1/version`
and `/api/v1/info`.

Server runs on:

```
http://127.0.0.1:4828
```

Health check (requires bearer token):

```bash
# Missing token -> 401
curl -i http://127.0.0.1:4828/api/v1/healthz

# Valid token -> 200
curl -i -H "Authorization: Bearer ${CCC_TOKEN}" \
  http://127.0.0.1:4828/api/v1/healthz
```

Version and info (requires bearer token):

```bash
curl -i -H "Authorization: Bearer ${CCC_TOKEN}" \
  http://127.0.0.1:4828/api/v1/version

curl -i -H "Authorization: Bearer ${CCC_TOKEN}" \
  http://127.0.0.1:4828/api/v1/info
```

Capabilities and commands (requires bearer token):

```bash
curl -H "Authorization: Bearer ${CCC_TOKEN}" \
  http://127.0.0.1:4828/api/v1/capabilities

curl -H "Authorization: Bearer ${CCC_TOKEN}" \
  http://127.0.0.1:4828/api/v1/commands

curl -H "Authorization: Bearer ${CCC_TOKEN}" \
  http://127.0.0.1:4828/api/v1/commands/open_new_doc
```

These endpoints are used by the Android app as a command/model browser across CAD
backends. They expose available commands, formats, and session metadata without
coupling the client to a specific CAD runtime.

Quick verification (starts server + runs checks):

```bash
server/scripts/smoke_healthz.sh
server/scripts/smoke_info.sh
server/scripts/smoke_capabilities.sh
server/scripts/smoke_upload_import.sh
server/scripts/smoke_export.sh
```

### Android (local build)

```bash
cd android
./gradlew :automotive:assembleDebug
```

The automotive UI is now built with Jetpack Compose (Material 3) and a bottom navigation layout.

### Android Server Config (build-time)

The Android client reads its server configuration from `BuildConfig` via `AppConfig`.
To switch between USB reverse and LAN, update the build config fields in
`android/automotive/build.gradle.kts`:

```kotlin
buildConfigField("String", "CCC_BASE_URL", "\"http://127.0.0.1:4828/\"")
buildConfigField("String", "CCC_MODE", "\"usb\"")
buildConfigField("String", "CCC_TOKEN", "\"\"")
```

**USB reverse (recommended):**

* `CCC_BASE_URL`: `http://127.0.0.1:4828/`

* `CCC_MODE`: `usb`
* `CCC_TOKEN`: (optional, required for current `/api/v1/healthz`)

### Android Upload → Import flow

1. Start the server with a token.
2. Ensure `adb reverse tcp:4828 tcp:4828` is enabled (USB) or use LAN IP in `CCC_BASE_URL`.
3. Open **Browser → Files → Pick file → Upload → Import**.

Upload with curl:

```bash
curl -H "Authorization: Bearer ${CCC_TOKEN}" \
  -F "file=@./model.step" \
  http://127.0.0.1:4828/api/v1/files/upload
```

Import via command exec:

```bash
curl -H "Authorization: Bearer ${CCC_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"command":"import_file","args":{"file_id":"file_123"}}' \
  http://127.0.0.1:4828/api/v1/commands/exec
```

Export current document via command exec:

```bash
curl -H "Authorization: Bearer ${CCC_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"command":"export_current_doc","args":{"format":"stl"}}' \
  http://127.0.0.1:4828/api/v1/commands/exec
```

Download the export (use `download_url` from the response):

```bash
curl -H "Authorization: Bearer ${CCC_TOKEN}" \
  -o export.stl \
  http://127.0.0.1:4828/api/v1/exports/<export_id>/download
```

**LAN mode (device → PC over WiFi):**

* `CCC_BASE_URL`: `http://<YOUR_PC_IP>:4828/`
* `CCC_MODE`: `lan`
* `CCC_TOKEN`: (optional, required for current `/api/v1/healthz`)

Note: the base URL must include the trailing `/` (Retrofit requirement).

For local debug builds, set `CCC_TOKEN` in your local
`android/automotive/build.gradle.kts` (use a dev token only; do not commit
real tokens).

The Android start screen displays `/api/v1/healthz`, `/api/v1/version`, and
`/api/v1/info` results plus a "Copy diagnostics" button for bug reports. If
`CCC_TOKEN` is empty, the app will receive 401 responses for protected
endpoints and surface the error in the UI.

The Android browser screen shows `/api/v1/capabilities` and `/api/v1/commands`
metadata, with command detail fetched from `/api/v1/commands/{name}`, plus a
"Copy browser diagnostics" button to share the current payloads. Command detail
now supports **Run**, renders an arguments form from `args_schema`, and offers a
Raw JSON mode for fallback payloads.

### Android Export flow

1. Start the server with a token.
2. Ensure `adb reverse tcp:4828 tcp:4828` is enabled (USB) or use LAN IP in `CCC_BASE_URL`.
3. Open **Browser → Exports → Export STL/STEP → Download**.

Command exec example:

```bash
curl -H "Authorization: Bearer ${CCC_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"command":"open_new_doc","args":{}}' \
  http://127.0.0.1:4828/api/v1/commands/exec
```

### Android ↔ Server (USB, recommended)

```bash
adb reverse tcp:4828 tcp:4828
```

Use USB reverse so the Android device can reach `127.0.0.1:4828` on your dev machine.

### Test-Loop:

```bash
   1. server/run_dev.sh
   2. adb reverse tcp:4828 tcp:4828
   3. ./gradlew :automotive:installDebug
   4. monkey start
   5. adb logcat
```

---

## Contribution Rules

CCC is designed to be developed collaboratively with **human contributors and AI agents**.

Please read **AGENTS.md** before contributing.

Key rules:

* Small, focused changes
* No features outside the current milestone
* Keep `main` buildable at all times

---

## License

Planned: **Apache-2.0**

(Will be finalized before v1.0)

---

## Vision

CCC aims to become a **modular control foundation** for creative software:

* CAD
* 3D tools
* Media production
* Fabrication pipelines

All **offline-capable**, **self-hosted**, and **creator-controlled**.

---

If you are a:

* maker
* engineer
* artist
* or systems-minded creator

…and you want **real control** over your tools again:

**Welcome to CCC.**

---

## Repository Metadata (GitHub)

**Suggested description**
> Local-first Android + FastAPI control layer for FreeCAD (command APIs, no cloud).

**Suggested topics**
`android`, `android-automotive`, `fastapi`, `freecad`, `remote-control`, `cad`, `offline-first`
