from typing import Any
from .runner import FreeCADRunner

# v0.1 whitelist
_ALLOWED = {
    "open_new_doc",
    "recompute",
    "fit_all",
    "save_as",
}

def list_commands() -> list[str]:
    return sorted(_ALLOWED)

def exec_command(runner: FreeCADRunner, command: str, args: dict[str, Any]) -> dict[str, Any]:
    if command not in _ALLOWED:
        raise ValueError("command_not_allowed")

    # v0.1: minimal placeholders; most interaction is via remote stream.
    if command == "open_new_doc":
        return {"message": "ok (v0.1 placeholder)"}
    if command == "recompute":
        return {"message": "ok (v0.1 placeholder)"}
    if command == "fit_all":
        return {"message": "ok (v0.1 placeholder)"}
    if command == "save_as":
        # args: {"path": "..."} - validated in runner/export layer later
        return {"message": "ok (v0.1 placeholder)"}

    return {"message": "ok"}
