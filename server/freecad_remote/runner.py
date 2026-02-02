import subprocess
import os
from .settings import settings

class FreeCADRunner:
    def __init__(self):
        self.freecad_cmd = settings.freecad_cmd

    def ping(self) -> bool:
        try:
            subprocess.run([self.freecad_cmd, "--version"], check=True, capture_output=True, text=True)
            return True
        except Exception:
            return False

    def get_freecad_info(self) -> dict:
        try:
            r = subprocess.run([self.freecad_cmd, "--version"], check=True, capture_output=True, text=True)
            return {"version": (r.stdout or r.stderr).strip(), "platform": "linux", "headless": True}
        except Exception as e:
            return {"version": "unknown", "platform": "linux", "headless": True, "error": str(e)}

    def get_session_info(self) -> dict:
        return {"active_document": None, "documents_open": 0}

    def import_file(self, path: str, fmt: str, into_document: str | None, options: dict) -> dict:
        script = os.path.join(os.path.dirname(__file__), "scripts", "import_model.py")
        args = [self.freecad_cmd, script, "--input", path, "--format", fmt]
        subprocess.run(args, check=True, capture_output=True, text=True)
        return {"import_id": "imp_ok", "document": into_document or "headless", "imported_objects": 1}
