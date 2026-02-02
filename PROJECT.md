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
