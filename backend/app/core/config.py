from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="FANLENS_", env_file=".env", extra="ignore")

    app_name: str = "FanLens AI"
    environment: str = "demo"  # demo | production
    database_url: str = "sqlite:///./dev.db"
    log_level: str = "INFO"
    ai_api_key: str = ""  # server only; never sent to clients
    invite_ttl_hours: int = 72
    retention_days: int = 90  # purge derived rows (insights/snapshots) older than this


@lru_cache
def get_settings() -> Settings:
    return Settings()
