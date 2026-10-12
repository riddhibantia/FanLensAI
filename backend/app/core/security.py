import hashlib
import secrets

_TOKEN_BYTES = 32
_INVITE_CODE_BYTES = 8


def hash_secret(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def generate_device_token() -> tuple[str, str]:
    """Returns (raw token for the client, hash to store)."""
    raw = secrets.token_urlsafe(_TOKEN_BYTES)
    return raw, hash_secret(raw)


def generate_invite_code() -> tuple[str, str]:
    """Returns (raw code to show once, hash to store)."""
    raw = secrets.token_urlsafe(_INVITE_CODE_BYTES)
    return raw, hash_secret(raw)
