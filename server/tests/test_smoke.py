import os
import requests

BASE = os.environ.get("CCC_BASE", "http://127.0.0.1:4828/api/v1")
TOKEN = os.environ.get("CCC_TOKEN", "dev-token-change-me")

def test_healthz():
    r = requests.get(f"{BASE}/healthz", timeout=2)
    assert r.status_code == 200
    assert r.json().get("ok") is True

def test_info_requires_auth():
    r = requests.get(f"{BASE}/info", timeout=2)
    assert r.status_code in (401, 403)

def test_info_ok_with_auth():
    r = requests.get(f"{BASE}/info", headers={"Authorization": f"Bearer {TOKEN}"}, timeout=2)
    assert r.status_code == 200
    assert r.json().get("service") == "ccc-freecad-remote"
