from typing import Any

from .runner import FreeCADRunner

_COMMANDS: dict[str, dict[str, Any]] = {
    "open_new_doc": {
        "name": "open_new_doc",
        "description": "Create a new document in the current session.",
        "args_schema": {"type": "object", "properties": {}, "additionalProperties": False},
        "returns": "message",
        "tags": ["session", "document"],
    },
    "recompute": {
        "name": "recompute",
        "description": "Recompute the active document.",
        "args_schema": {"type": "object", "properties": {}, "additionalProperties": False},
        "returns": "message",
        "tags": ["document"],
    },
    "fit_all": {
        "name": "fit_all",
        "description": "Fit all objects in the viewport.",
        "args_schema": {"type": "object", "properties": {}, "additionalProperties": False},
        "returns": "message",
        "tags": ["view"],
    },
    "save_as": {
        "name": "save_as",
        "description": "Save the active document to a path.",
        "args_schema": {
            "type": "object",
            "properties": {"path": {"type": "string"}},
            "required": ["path"],
            "additionalProperties": False,
        },
        "returns": "message",
        "tags": ["file", "document"],
    },
}


def list_commands() -> list[str]:
    return sorted(_COMMANDS.keys())


def get_commands_metadata() -> list[dict[str, Any]]:
    return [meta for _, meta in sorted(_COMMANDS.items())]


def get_command_metadata(name: str) -> dict[str, Any] | None:
    return _COMMANDS.get(name)


def exec_command(runner: FreeCADRunner, command: str, args: dict[str, Any]) -> dict[str, Any]:
    if command not in _COMMANDS:
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
