from fastapi import Header, HTTPException

from .settings import settings


def require_token(authorization: str | None = Header(default=None)):
    if authorization is None:
        raise HTTPException(status_code=401, detail="missing token")
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="invalid token")
    token = authorization.removeprefix("Bearer ").strip()
    if not token or token != settings.api_token:
        raise HTTPException(status_code=401, detail="invalid token")
