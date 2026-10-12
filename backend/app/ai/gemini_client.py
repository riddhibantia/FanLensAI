import httpx


class LLMError(Exception):
    """Any failure from the LLM call: network, quota, malformed response."""


class GeminiClient:
    API_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"

    def __init__(self, api_key: str, model: str = "gemini-2.0-flash", timeout: float = 10.0):
        if not api_key:
            raise ValueError("api key required")
        self.api_key = api_key
        self.model = model
        self.timeout = timeout

    def explain(self, prompt: str) -> str:
        try:
            response = httpx.post(
                self.API_URL.format(model=self.model),
                params={"key": self.api_key},
                json={
                    "contents": [{"parts": [{"text": prompt}]}],
                    "generationConfig": {"temperature": 0.2, "maxOutputTokens": 200},
                },
                timeout=self.timeout,
            )
            response.raise_for_status()
        except httpx.HTTPError as exc:
            raise LLMError(f"request failed: {exc}") from exc
        try:
            return response.json()["candidates"][0]["content"]["parts"][0]["text"]
        except (KeyError, IndexError, TypeError) as exc:
            raise LLMError("malformed response") from exc


def get_llm_client(api_key: str | None) -> GeminiClient | None:
    """No key (dev/demo) means template-only insights — a normal state, not an error."""
    return GeminiClient(api_key) if api_key else None
