import os

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    api_token: str = Field(..., validation_alias="CCC_TOKEN")
    bind_host: str = "127.0.0.1"
    bind_port: int = 4828
    storage_dir: str = "../data/uploads"
    export_dir: str = "../data/exports"
    freecad_cmd: str = "freecadcmd"

if not os.getenv("CCC_TOKEN"):
    raise RuntimeError("CCC_TOKEN is required. Set CCC_TOKEN before starting the server.")

settings = Settings()
