
# 🎯 CCC v0.1 – MVP vs. Nice-to-have

> **Leitfrage:**
> *Ist das Feature zwingend nötig, damit ein Nutzer sagen kann:*
> **„Ich kann mobil ein 3D-Modell finden und es in FreeCAD remote bearbeiten.“**

Wenn **nein** → nicht MVP.

---

## 🧱 Kernfunktionen

| Bereich      | Feature                                                       | MVP (v0.1) | Nice-to-have (später) | Begründung                           |
| ------------ | ------------------------------------------------------------- | ---------- | --------------------- | ------------------------------------ |
| Modell-Suche | Suche auf **1 Plattform** (z. B. Thingiverse oder Printables) | ✅          | 🔄 Multi-Plattform    | Eine Plattform reicht zum Validieren |
| Modell-Suche | Plattform-Filter                                              | ❌          | ✅                     | Komfort, kein Kern                   |
| 3D-Preview   | STL / OBJ Preview                                             | ✅          | –                     | Absolut notwendig                    |
| 3D-Preview   | GLTF / DAE                                                    | ❌          | ✅                     | Später                               |
| 3D-Preview   | Texturen / Farben                                             | ❌          | ✅                     | Nice                                 |
| Favoriten    | Lokale Favoriten                                              | ❌          | ✅                     | MVP braucht kein Persistenz-UX       |
| Downloads    | STL Download                                                  | ✅          | –                     | Übergabe an FreeCAD                  |
| Offline      | Offline-Cache                                                 | ❌          | ✅                     | Später sinnvoll                      |

---

## 🖥️ Remote / Streaming

| Bereich       | Feature                                   | MVP | Nice-to-have | Begründung              |
| ------------- | ----------------------------------------- | --- | ------------ | ----------------------- |
| Remote Stream | Low-Latency Desktop Stream                | ✅   | –            | Herzstück               |
| Protokoll     | Rust-basierter Stream (RustDesk / WebRTC) | ✅   | –            | Kernentscheidung        |
| VNC / RDP     | Unterstützung                             | ❌   | ❌            | Explizit ausgeschlossen |
| Auflösung     | Feste Auflösung                           | ✅   | –            | Reduziert Komplexität   |
| Auflösung     | Dynamisch / HiDPI                         | ❌   | ✅            | Später                  |
| Multi-Session | Mehrere Verbindungen                      | ❌   | ✅            | Nicht nötig             |
| Auth          | Shared Secret / Token                     | ✅   | –            | Minimal, aber notwendig |
| Encryption    | Transport-Verschlüsselung                 | ✅   | –            | Pflicht                 |

---

## 🧠 CAD-Steuerung (FreeCAD)

| Bereich | Feature                           | MVP | Nice-to-have | Begründung               |
| ------- | --------------------------------- | --- | ------------ | ------------------------ |
| CAD     | **FreeCAD Support**               | ✅   | –            | Fokus                    |
| CAD     | Blender Support                   | ❌   | ✅            | Nächste Phase            |
| CAD     | Bambu Studio                      | ❌   | ❌            | Nicht CAD, extra Projekt |
| API     | Python Command Execution          | ✅   | –            | Einfache & stabile Basis |
| API     | Command Whitelist                 | ✅   | –            | Sicherheit               |
| API     | Live Model Sync                   | ❌   | ✅            | Komplex                  |
| API     | Scene Graph Zugriff               | ❌   | ✅            | Später                   |
| UI      | Preset CAD Buttons (Extrude etc.) | ❌   | ✅            | Stream reicht fürs MVP   |

---

## 📱 Android App / UX

| Bereich      | Feature                     | MVP | Nice-to-have | Begründung         |
| ------------ | --------------------------- | --- | ------------ | ------------------ |
| UI           | Minimal UI (Tabs / Buttons) | ✅   | –            | Fokus auf Funktion |
| UI           | Jetpack Compose             | ✅   | –            | Modern, wartbar    |
| UI           | Gesten-Optimierung          | ❌   | ✅            | Komfort            |
| Input        | Touch → Maus                | ✅   | –            | Pflicht            |
| Input        | Pen / Pressure              | ❌   | ✅            | Spezialfall        |
| Input        | On-Screen Keyboard          | ✅   | –            | Minimal nötig      |
| Multi-Window | Tablet Layout               | ❌   | ✅            | Später             |

---

## 🖨️ Printing / Pipeline

| Bereich  | Feature          | MVP | Nice-to-have | Begründung   |
| -------- | ---------------- | --- | ------------ | ------------ |
| Printing | OctoPrint        | ❌   | ✅            | Nach CAD     |
| Printing | Klipper          | ❌   | ✅            | Nach CAD     |
| Slicing  | In-App Slicing   | ❌   | ❌            | Out of scope |
| G-Code   | Upload / Monitor | ❌   | ✅            | Phase 3      |

---

## 🔒 Infrastruktur / OSS

| Bereich    | Feature            | MVP | Nice-to-have | Begründung   |
| ---------- | ------------------ | --- | ------------ | ------------ |
| Hosting    | Self-hosted        | ✅   | –            | Kernwert     |
| Cloud      | Proprietäre Server | ❌   | ❌            | Nie          |
| Telemetrie | Tracking           | ❌   | ❌            | Nie          |
| Lizenz     | Apache / MIT       | ✅   | –            | OSS          |
| Plugins    | Plugin-System      | ❌   | ✅            | Später       |
| API Docs   | Minimal Docs       | ✅   | –            | Contributors |
| CI         | Full CI/CD         | ❌   | ✅            | Später       |

---

## 🧠 Essenz in einem Satz

> **CCC v0.1 ist erfolgreich, wenn:**
> *Ein Nutzer auf dem Handy ein STL findet,
> es ansehen kann und es live in FreeCAD auf einem anderen Rechner bearbeitet.*

Alles andere ist **bewusst verschoben** – nicht vergessen.

