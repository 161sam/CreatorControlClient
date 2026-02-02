import os
import uuid
import hashlib
from typing import Any
from fastapi import UploadFile

def _ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)

async def save_upload(file: UploadFile, storage_dir: str) -> dict:
    _ensure_dir(storage_dir)

    file_id = "file_" + uuid.uuid4().hex[:16]
    safe_name = (file.filename or "upload.bin").replace("/", "_").replace("\\", "_")
    out_path = os.path.join(storage_dir, file_id)
    content_type = file.content_type or "application/octet-stream"

    sha256 = hashlib.sha256()
    size = 0

    with open(out_path, "wb") as f:
        while True:
            chunk = await file.read(1024 * 1024)
            if not chunk:
                break
            f.write(chunk)
            sha256.update(chunk)
            size += len(chunk)

    return {
        "file_id": file_id,
        "filename": safe_name,
        "path": out_path,
        "size": size,
        "sha256": sha256.hexdigest(),
        "mime": content_type,
    }

def get_file_path(storage_dir: str, file_id: str) -> str | None:
    path = os.path.join(storage_dir, file_id)
    return path if os.path.isfile(path) else None

def create_export_path(export_dir: str, filename: str) -> str:
    _ensure_dir(export_dir)
    export_id = "exp_" + uuid.uuid4().hex[:16] + "_" + filename.replace("/", "_").replace("\\", "_")
    return os.path.join(export_dir, export_id)

_EXPORTS: dict[str, dict[str, Any]] = {}

def sanitize_filename(filename: str) -> str:
    base = os.path.basename(filename)
    cleaned = base.replace("/", "_").replace("\\", "_").strip()
    return cleaned or "export.bin"

def build_export_path(export_dir: str, export_id: str, filename: str) -> str:
    _ensure_dir(export_dir)
    safe_name = sanitize_filename(filename)
    return os.path.join(export_dir, f"{export_id}_{safe_name}")

def register_export(export_id: str, payload: dict[str, Any]) -> None:
    _EXPORTS[export_id] = payload

def get_export(export_id: str) -> dict[str, Any] | None:
    return _EXPORTS.get(export_id)

def is_path_within_dir(base_dir: str, path: str) -> bool:
    base = os.path.realpath(base_dir)
    target = os.path.realpath(path)
    return target == base or target.startswith(base + os.sep)
