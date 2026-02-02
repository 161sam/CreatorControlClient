# Creator Control Client (CCC)

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

### In Progress
- ⏳ File upload → FreeCAD import pipeline
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

## Development Setup

### Server

```bash
cd server
./run_dev.sh
```

Server runs on:

```
http://127.0.0.1:4828
```

### Android (local build)

```bash
cd android
./gradlew :automotive:assembleDebug
```

### Android Server Config (build-time)

The Android client reads its server configuration from `BuildConfig` via `AppConfig`.
To switch between USB reverse and LAN, update the build config fields in
`android/automotive/build.gradle.kts`:

```kotlin
buildConfigField("String", "CCC_BASE_URL", "\"http://127.0.0.1:4828/\"")
buildConfigField("String", "CCC_MODE", "\"usb\"")
```

**USB reverse (recommended):**

* `CCC_BASE_URL`: `http://127.0.0.1:4828/`
* `CCC_MODE`: `usb`

**LAN mode (device → PC over WiFi):**

* `CCC_BASE_URL`: `http://<YOUR_PC_IP>:4828/`
* `CCC_MODE`: `lan`

Note: the base URL must include the trailing `/` (Retrofit requirement).

### Android ↔ Server (USB, recommended)

```bash
adb reverse tcp:4828 tcp:4828
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
