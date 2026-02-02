import os
import uuid
from datetime import datetime, timezone
from typing import Any

from .runner import FreeCADRunner
from .files import build_export_path, get_file_path, register_export, sanitize_filename
from .settings import settings

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
    "import_file": {
        "name": "import_file",
        "description": "Import an uploaded file into the current session.",
        "args_schema": {
            "type": "object",
            "properties": {
                "file_id": {"type": "string"},
                "path": {"type": "string"},
                "format": {"type": "string"},
                "into_document": {"type": "string"},
                "options": {"type": "object"},
            },
            "required": ["file_id"],
            "additionalProperties": False,
        },
        "returns": "import_result",
        "tags": ["file", "import"],
    },
    "export_current_doc": {
        "name": "export_current_doc",
        "description": "Export current document to STL/STEP and return download url.",
        "args_schema": {
            "type": "object",
            "properties": {
                "format": {"type": "string", "enum": ["stl", "step"]},
                "filename": {"type": "string"},
            },
            "required": ["format"],
            "additionalProperties": False,
        },
        "returns": "export_response",
        "tags": ["export", "file"],
    },
}

def _infer_format(path: str) -> str | None:
    ext = os.path.splitext(path)[1].lower().lstrip(".")
    if ext == "stp":
        return "step"
    return ext or None


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
    if command == "import_file":
        file_id = args.get("file_id")
        path = args.get("path")
        if file_id:
            path = get_file_path(settings.storage_dir, file_id)
            if not path:
                return {"status": "error", "error": "file_not_found", "file_id": file_id}
        elif not path:
            return {"status": "error", "error": "path_or_file_id_required"}

        fmt = args.get("format") or _infer_format(path)
        if not fmt:
            return {"status": "error", "error": "format_required", "path": path}

        into_document = args.get("into_document")
        options = args.get("options") if isinstance(args.get("options"), dict) else {}
        try:
            result = runner.import_file(path, fmt, into_document, options)
            return {
                "status": "ok",
                "result": result,
                "file_id": file_id,
                "path": path,
                "format": fmt,
            }
        except Exception as exc:
            return {
                "status": "error",
                "error": "import_failed",
                "detail": str(exc),
                "path": path,
                "format": fmt,
            }
    if command == "export_current_doc":
        fmt = (args.get("format") or "").lower()
        if fmt not in {"stl", "step"}:
            return {"status": "error", "error": "format_required", "format": fmt}
        export_id = uuid.uuid4().hex
        filename = sanitize_filename(args.get("filename") or f"export_{export_id}.{fmt}")
        export_path = build_export_path(settings.export_dir, export_id, filename)
        try:
            runner.export_current_doc(export_path, fmt)
            size = os.path.getsize(export_path) if os.path.isfile(export_path) else 0
            created_utc = datetime.now(timezone.utc).isoformat()
            download_url = f"/api/v1/exports/{export_id}/download"
            payload = {
                "ok": True,
                "export_id": export_id,
                "format": fmt,
                "filename": filename,
                "path": export_path,
                "download_url": download_url,
                "size": size,
                "created_utc": created_utc,
            }
            register_export(export_id, payload)
            return payload
        except Exception as exc:
            return {
                "status": "error",
                "error": "export_failed",
                "detail": str(exc),
                "format": fmt,
                "path": export_path,
            }

    return {"message": "ok"}
