"""Request/response contracts for the internal analysis endpoint.

Field names are declared snake_case (Python convention) but serialise as
camelCase on the wire via ``CamelModel``, matching the JSON shape the
Spring Boot backend's DTOs use everywhere else and the exact field names
in PHASE-1-requirements-and-architecture.md §9.2.
"""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

Priority = Literal["LOW", "MEDIUM", "HIGH", "CRITICAL"]


class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel, populate_by_name=True, protected_namespaces=()
    )


class AnalyseRequest(CamelModel):
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(min_length=1)
    category: str | None = None


class AnalyseResponse(CamelModel):
    suggested_category: str
    predicted_priority: Priority
    summary: str
    keywords: list[str]
    suggested_steps: list[str]
    model_used: str


class HealthResponse(CamelModel):
    status: Literal["ok"]
    provider: str
