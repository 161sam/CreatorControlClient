import os
import uuid
import hashlib
from fastapi import UploadFile

def _ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)

async def save_upload(file: UploadFile, storage_dir: str) -> dict:
    _ensure_dir(storage_dir)

    file_id = "file_" + uuid.uuid4().hex[:16]
    safe_name = (file.filename or "upload.bin").replace("/", "_").replace("\\", "_")
    out_path = os.path.join(storage_dir, file_id)

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
        "name": safe_name,
        "size": size,
        "sha256": sha256.hexdigest(),
    }

def get_file_path(storage_dir: str, file_id: str) -> str | None:
    path = os.path.join(storage_dir, file_id)
    return path if os.path.isfile(path) else None

def create_export_path(export_dir: str, filename: str) -> str:
    _ensure_dir(export_dir)
    export_id = "exp_" + uuid.uuid4().hex[:16] + "_" + filename.replace("/", "_").replace("\\", "_")
    return os.path.join(export_dir, export_id)
