# AGENTS.md – Entwicklungsregeln für CCC

## Ziel
Dieses Dokument definiert verbindliche Regeln für:
- menschliche Contributors
- KI-Agenten (Codex, Claude, GPT)

## Grundprinzipien
- Kleine, überprüfbare Änderungen
- Kein „Big Bang“
- Jede Entscheidung muss erklärbar sein

## Arbeitsweise
- Jede Änderung erfolgt über klar benannte Issues
- Commits sind klein und thematisch sauber
- Keine Features außerhalb des aktuellen Milestones

## v0.1 Scope Guard
KI-Agenten dürfen NICHT:
- neue Feature-Bereiche einführen
- Cloud-Services integrieren
- proprietäre SDKs verwenden

Erlaubt sind:
- Refactorings
- Performance-Optimierungen
- Bugfixes
- Dokumentationsverbesserungen

## Code-Qualität
- Kein ungenutzter Code
- Keine „TODO später“-Leichen
- Verständliche Kommentare

## Sprache
- Code & Kommentare: Englisch
- Doku & Konzepte: Englisch oder Deutsch
