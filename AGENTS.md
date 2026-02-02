# AGENTS.md – Entwicklungsregeln für CCC (CreatorControlClient)

## Purpose
This document defines binding rules for:
- human contributors
- AI agents (Codex, Claude, GPT)

Goal: fast, reproducible progress with minimal risk.

---

## Core Principles
- Small, verifiable changes (no big bang)
- Always keep `main` buildable (CI-ready mindset)
- Every change must be explainable and testable
- Offline-first and open-source only

---

## Workflow / Collaboration
### Source of truth
- `main` is the truth.
- Work is done in small increments aligned to ROADMAP/Milestones.

### Local workflow (human)
- Local machine does **only**:
  - `git pull`
  - run tests/build
  - optional install to device
- Avoid manual local refactors. Prefer changes through Codex/PRs.

### AI agent workflow (Codex)
- Make minimal diffs.
- Include commands used and their output (build/test).
- One concern per commit.
- Prefer fixing build/test issues before adding features.

---

## Scope Guard (v0.1)
AI agents MUST NOT:
- introduce new feature areas outside current milestone
- integrate cloud services
- use proprietary SDKs
- add tracking/analytics
- add complex UI frameworks unless explicitly required

Allowed:
- bugfixes
- refactoring with clear benefit
- performance improvements
- documentation improvements
- build stability (Gradle, deps, packaging)

---

## Quality Rules
- No dead code
- No “TODO later” piles
- Clear error handling (fail loud, log actionable)
- Keep dependencies minimal

---

## Testing Rules (Android)
Before merging/pushing:
- `./gradlew :automotive:assembleDebug` must pass
- If a device is connected: `./gradlew :automotive:installDebug` (optional but nice)
- Prefer logs filtered to app PID when debugging

---

## Networking Rules (Android)
- Support:
  - LAN-IP mode (device -> PC via WiFi)
  - USB reverse mode (`adb reverse`) for development
- Base URL must be explicit and end with trailing `/` (Retrofit requirement).
- No hard-coded secrets.

---

## Commit Conventions
- Use Conventional Commits-ish:
  - `fix(android): ...`
  - `feat(server): ...`
  - `docs: ...`
- Commit must match the diff scope (no mixed topics).

---

## Release Notes
- From v0.4.0: Release notes are generated from PR titles.
- Therefore PR titles must be concise and user-facing.

---

## Language
- Code & comments: English
- Docs & concepts: English or German
