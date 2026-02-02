# Creator Control Client (CCC)

## Projektstatus
Version: v0.1 (MVP)
Status: active development
Lizenz: Apache-2.0 (vorgeschlagen)

## Vision
Creator Control Client (CCC) ist eine **Open-Source Android Client App** für 3D-Creator,
die **Modell-Browsing, 3D-Preview und echte Remote-Steuerung von CAD-Software**
in einer einzigen, privacy-freundlichen Anwendung vereint.

CCC ist **offline-first**, **self-hosted-first** und modular aufgebaut.
Es richtet sich an Maker, Engineers, Artists und Researcher.

## Kernidee
CCC verbindet drei bislang getrennte Welten:
1. 3D-Modell-Plattformen (Discovery & Preview)
2. Desktop-CAD-Software (FreeCAD, Blender)
3. Mobile Steuerung mit niedriger Latenz

## Fokus v0.1 (MVP)
Der Fokus von v0.1 liegt **ausschließlich** auf:

- 3D-Model Discovery + Preview (OSS)
- Echter Remote-Zugriff auf **FreeCAD**
- Niedrige Latenz (kein klassisches VNC/RDP)
- Vollständig Open Source
- Lokale / selbst gehostete Infrastruktur

## Explizit NICHT Teil von v0.1
- ❌ Bambu Studio Steuerung
- ❌ Slicing / Printing
- ❌ Wiki / RAG / Knowledge Base
- ❌ Cloud-Accounts oder Sync
- ❌ Mesh / Multi-Node Support

Diese Punkte sind **bewusst verschoben**, nicht verworfen.

## Ziel von v0.1
> „Ich kann auf dem Smartphone ein 3D-Modell finden,  
> es ansehen und **direkt in FreeCAD auf einem Remote-Rechner bearbeiten**.“

Wenn dieses Ziel erreicht ist, ist v0.1 erfolgreich.

## Architektur (High Level)

Android App
- UI: Kotlin / Jetpack Compose
- 3D Preview: OpenGL (Fork eines OSS Viewers)
- Storage: lokal (Room DB)

Remote Layer
- Low-Latency Stream: Rust-basierter Client
- Input Handling: Touch → Mouse / Keyboard

CAD Control
- FreeCAD Python API Server (remote)
- Command Execution (safe subset)

## Open-Source Prinzipien
- No tracking
- No telemetry
- No proprietary SDKs
- Fork-friendly
- Dokumentierte APIs

## Zielgruppe
- Makers & 3D-Printing Enthusiasts
- Engineers & Designers
- OSS-Communities
- Privacy-first Nutzer


---

# Creator Control Client (CCC)

## Project Status

Version: v0.1 (MVP)
Status: active development
License: Apache-2.0 (proposed)

## Vision

Creator Control Client (CCC) is an **open-source Android client app** for 3D creators that combines **model browsing, 3D preview, and true remote control of CAD software** in a single, privacy-friendly application.

CCC is **offline-first**, **self-hosted-first**, and built with a modular architecture.
It is aimed at makers, engineers, artists, and researchers.

## Core Idea

CCC connects three previously separate worlds:

1. 3D model platforms (discovery & preview)
2. Desktop CAD software (FreeCAD, Blender)
3. Mobile control with low latency

## Focus of v0.1 (MVP)

The focus of v0.1 is **exclusively** on:

* 3D model discovery + preview (OSS)
* True remote access to **FreeCAD**
* Low latency (no classic VNC/RDP)
* Fully open source
* Local / self-hosted infrastructure

## Explicitly NOT Part of v0.1

* ❌ Bambu Studio control
* ❌ Slicing / printing
* ❌ Wiki / RAG / knowledge base
* ❌ Cloud accounts or sync
* ❌ Mesh / multi-node support

These items are **intentionally postponed**, not discarded.

## Goal of v0.1

> “I can find a 3D model on my smartphone,
> view it, and **edit it directly in FreeCAD on a remote machine**.”

If this goal is achieved, v0.1 is considered successful.

## Architecture (High Level)

**Android App**

* UI: Kotlin / Jetpack Compose
* 3D Preview: OpenGL (fork of an OSS viewer)
* Storage: local (Room DB)

**Remote Layer**

* Low-latency stream: Rust-based client
* Input handling: touch → mouse / keyboard

**CAD Control**

* FreeCAD Python API server (remote)
* Command execution (safe subset)

## Open-Source Principles

* No tracking
* No telemetry
* No proprietary SDKs
* Fork-friendly
* Documented APIs

## Target Audience

* Makers & 3D printing enthusiasts
* Engineers & designers
* OSS communities
* Privacy-first users
