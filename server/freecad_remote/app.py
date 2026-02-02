from datetime import datetime, timezone

from fastapi import FastAPI, Depends, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel

from .security import require_token
from .settings import settings
from .runner import FreeCADRunner
from .commands import exec_command, list_commands, get_commands_metadata, get_command_metadata
from .files import save_upload, get_file_path
from .version import get_version_payload

app = FastAPI(title="ccc-freecad-remote", version="0.1.0")
runner = FreeCADRunner()
API_VERSION = "v1"
SERVICE_NAME = "ccc-freecad-remote"
IMPORT_FORMATS = ["fcstd", "step", "stl", "iges"]
EXPORT_FORMATS = ["fcstd", "step", "stl", "iges", "obj"]
CAPABILITY_LIMITS = {
    "max_upload_mb": 200,
    "max_export_mb": 200,
    "max_job_seconds": 300,
}

class CommandReq(BaseModel):
    command: str
    args: dict = {}
    request_id: str | None = None

class ImportReq(BaseModel):
    file_id: str
    format: str
    into_document: str | None = None
    options: dict = {}

@app.get("/api/v1/healthz", dependencies=[Depends(require_token)])
def healthz():
    return {"ok": True}

@app.get("/api/v1/version", dependencies=[Depends(require_token)])
def version():
    return get_version_payload()

@app.get("/api/v1/readyz")
def readyz():
    if not runner.ping():
        raise HTTPException(status_code=503, detail="runner not ready (freecadcmd missing?)")
    return {"ok": True}

@app.get("/api/v1/info", dependencies=[Depends(require_token)])
def info():
    return {
        "bind_host": settings.bind_host,
        "bind_port": settings.bind_port,
        "storage_dir": settings.storage_dir,
        "export_dir": settings.export_dir,
        "freecad_cmd": settings.freecad_cmd,
        "auth": {"required": True, "scheme": "bearer"},
        "time_utc": datetime.now(timezone.utc).isoformat(),
        "version": get_version_payload(),
    }

@app.get("/api/v1/capabilities", dependencies=[Depends(require_token)])
def capabilities():
    return {
        "service": SERVICE_NAME,
        "api_version": API_VERSION,
        "time_utc": datetime.now(timezone.utc).isoformat(),
        "auth": {"required": True, "scheme": "bearer"},
        "version": get_version_payload(),
        "freecad": runner.get_freecad_info(),
        "session": runner.get_session_info(),
        "capabilities": {
            "import_formats": IMPORT_FORMATS,
            "export_formats": EXPORT_FORMATS,
            "commands": list_commands(),
            "limits": CAPABILITY_LIMITS,
            "features": {
                "model_browser": True,
                "remote_exec": True,
                "batch_jobs": False,
            },
        },
    }

@app.get("/api/v1/commands", dependencies=[Depends(require_token)])
def commands_list():
    return {"commands": get_commands_metadata()}

@app.get("/api/v1/commands/{name}", dependencies=[Depends(require_token)])
def commands_get(name: str):
    command = get_command_metadata(name)
    if not command:
        raise HTTPException(status_code=404, detail="command_not_found")
    return command

@app.post("/api/v1/commands/exec", dependencies=[Depends(require_token)])
def commands_exec(req: CommandReq):
    try:
        res = exec_command(runner, req.command, req.args)
        return {"ok": True, "request_id": req.request_id, "result": res}
    except ValueError:
        raise HTTPException(status_code=400, detail="command_not_allowed")

@app.post("/api/v1/files/upload", dependencies=[Depends(require_token)])
async def files_upload(file: UploadFile = File(...)):
    meta = await save_upload(file, settings.storage_dir)
    return {"ok": True, **meta}

@app.post("/api/v1/files/import", dependencies=[Depends(require_token)])
def files_import(req: ImportReq):
    path = get_file_path(settings.storage_dir, req.file_id)
    if not path:
        raise HTTPException(status_code=404, detail="file not found")
    try:
        result = runner.import_file(path, req.format, req.into_document, req.options)
        return {"ok": True, **result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"import failed: {e}")

@app.get("/api/v1/files/download/{export_id}", dependencies=[Depends(require_token)])
def files_download(export_id: str):
    path = f"{settings.export_dir}/{export_id}"
    if not path or not path.startswith(settings.export_dir):
        raise HTTPException(status_code=400, detail="invalid export id")
    if not __import__("os").path.isfile(path):
        raise HTTPException(status_code=404, detail="export not found")
    return FileResponse(path, filename=export_id)
