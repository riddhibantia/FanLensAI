from pydantic import BaseModel, ConfigDict


class RulesetResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    name: str
    version: str
