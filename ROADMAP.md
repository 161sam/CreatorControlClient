
## 🧭 Roadmap (v0.1)

### Milestones

| Milestone    | Ziel                             | Status  |
| ------------ | -------------------------------- | ------- |
| `v0.1-alpha` | Technisches Fundament steht      | geplant |
| `v0.1-beta`  | End-to-End Workflow funktioniert | geplant |
| `v0.1`       | Stabiler MVP-Release             | geplant |

---

## 🧱 Milestone: `v0.1-alpha`

**Ziel:** Architektur + Infrastruktur lauffähig

### Issues

---

### 🟢 Issue #1: Repository Bootstrap

**Type:** chore
**Milestone:** v0.1-alpha

**Beschreibung:**
Initiales Repository-Setup für CCC.

**Tasks:**

* Repo-Struktur anlegen
* `README.md`, `PROJECT.md`, `AGENTS.md`
* Lizenz hinzufügen
* `.gitignore` für Android + Rust

**Acceptance Criteria:**

* Repo ist clonebar
* Docs sind vollständig
* Keine Build-Artefakte im Repo

---

### 🟢 Issue #2: Android App Skeleton

**Type:** feat
**Milestone:** v0.1-alpha

**Beschreibung:**
Minimal lauffähige Android-App als Grundlage.

**Tasks:**

* Android Projekt (Kotlin)
* Jetpack Compose Setup
* Minimaler Screen + Navigation
* Build läuft lokal

**Acceptance Criteria:**

* App startet auf Emulator / Device
* Kein Business-Code enthalten

---

### 🟢 Issue #3: 3D Viewer Integration (Fork)

**Type:** feat
**Milestone:** v0.1-alpha

**Beschreibung:**
Integration eines OSS 3D-Model-Viewers als Modul.

**Tasks:**

* OSS Viewer forken oder einbinden
* STL Preview anzeigen
* Beispielmodell laden

**Acceptance Criteria:**

* STL kann lokal angezeigt werden
* Viewer ist vom restlichen UI getrennt

---

### 🟢 Issue #4: Remote Stream – Tech Spike

**Type:** spike
**Milestone:** v0.1-alpha

**Beschreibung:**
Evaluierung & Proof-of-Concept für Low-Latency Remote Streaming (Rust).

**Tasks:**

* RustDesk / WebRTC Analyse
* Minimalen Stream anzeigen (read-only)
* Latenz messen (lokal)

**Acceptance Criteria:**

* Stream läuft stabil
* Kein VNC/RDP im Einsatz
* Erkenntnisse dokumentiert (`docs/remote.md`)

---

### 🟢 Issue #5: FreeCAD Remote API – Konzept

**Type:** design
**Milestone:** v0.1-alpha

**Beschreibung:**
Technisches Design der FreeCAD Remote API.

**Tasks:**

* Kommunikationsmodell festlegen
* Security-Modell (Token / Scope)
* Command Whitelist definieren

**Acceptance Criteria:**

* API-Spec als Markdown vorhanden
* Keine Implementierung nötig

---

## 🧱 Milestone: `v0.1-beta`

**Ziel:** End-to-End funktioniert

---

### 🟡 Issue #6: FreeCAD Python Remote Server

**Type:** feat
**Milestone:** v0.1-beta

**Beschreibung:**
Minimaler FreeCAD Server zur Remote-Steuerung.

**Tasks:**

* Python TCP/HTTP Server
* Befehle empfangen
* FreeCAD Commands ausführen

**Acceptance Criteria:**

* FreeCAD reagiert auf Remote Commands
* Nur Whitelist erlaubt
* Server separat startbar

---

### 🟡 Issue #7: Remote Input (Touch → Desktop)

**Type:** feat
**Milestone:** v0.1-beta

**Beschreibung:**
Touch-Eingaben auf Desktop übertragen.

**Tasks:**

* Touch → Mausbewegung
* Klick / Drag
* On-Screen Keyboard

**Acceptance Criteria:**

* FreeCAD vollständig bedienbar
* Keine UI-Freezes

---

### 🟡 Issue #8: Stream + Input Integration

**Type:** feat
**Milestone:** v0.1-beta

**Beschreibung:**
Kombination aus Stream + Eingabe.

**Tasks:**

* Stream anzeigen
* Input einspeisen
* Session Handling

**Acceptance Criteria:**

* Nutzer kann FreeCAD vollständig remote bedienen
* Verbindung stabil ≥ 10 Minuten

---

### 🟡 Issue #9: Modell → FreeCAD Übergabe

**Type:** feat
**Milestone:** v0.1-beta

**Beschreibung:**
STL vom Viewer an FreeCAD übergeben.

**Tasks:**

* Datei Download
* Transfer an Server
* Automatisches Öffnen in FreeCAD

**Acceptance Criteria:**

* Ein Klick → Modell erscheint in FreeCAD
* Kein manuelles Kopieren nötig

---

## 🧱 Milestone: `v0.1`

**Ziel:** Release-fähiger MVP

---

### 🔵 Issue #10: UX Cleanup & Minimal Polish

**Type:** chore
**Milestone:** v0.1

**Beschreibung:**
UX vereinheitlichen, Bugs entfernen.

**Tasks:**

* Ladeindikatoren
* Fehlerhandling
* Basis-Icons / Labels

**Acceptance Criteria:**

* App fühlt sich konsistent an
* Keine kritischen Bugs

---

### 🔵 Issue #11: Security Hardening

**Type:** feat
**Milestone:** v0.1

**Beschreibung:**
Minimal notwendige Sicherheitsmaßnahmen.

**Tasks:**

* Token Auth
* Verbindung absichern
* Keine offenen Ports ohne Schutz

**Acceptance Criteria:**

* Kein unauth Zugriff möglich
* Docs aktualisiert

---

### 🔵 Issue #12: Documentation & Release Prep

**Type:** docs
**Milestone:** v0.1

**Beschreibung:**
Projekt release-fähig machen.

**Tasks:**

* Setup Anleitung
* Architektur-Doku
* Release Notes

**Acceptance Criteria:**

* Neuer Nutzer kann CCC lokal testen
* v0.1 Release-Tag möglich

---

## 🧠 Wichtig: Scope Guard (nochmal explizit)

**Nicht erlaubt in v0.1 Issues:**

* Blender
* Bambu Studio
* Printing
* RAG / Wiki
* Cloud Sync

Diese bekommen **eigene Milestones ab v0.2**.

