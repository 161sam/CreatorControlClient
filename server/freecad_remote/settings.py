from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    api_token: str | None = Field(
        default=None,
        validation_alias=AliasChoices("CCC_TOKEN", "api_token"),
    )
    bind_host: str = "127.0.0.1"
    bind_port: int = 4828
    storage_dir: str = "../data/uploads"
    export_dir: str = "../data/exports"
    freecad_cmd: str = "freecadcmd"

    def model_post_init(self, __context):
        if self.api_token is None or not self.api_token.strip():
            raise RuntimeError(
                "CCC_TOKEN is required. Set environment variable CCC_TOKEN or add "
                "api_token to server/.env."
            )

settings = Settings()
