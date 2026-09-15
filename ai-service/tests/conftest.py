import pytest
from fastapi.testclient import TestClient

from app.config import Settings, get_settings
from app.main import app

TEST_TOKEN = "test-internal-token"


def _test_settings() -> Settings:
    return Settings(ai_service_internal_token=TEST_TOKEN, llm_provider="mock")


@pytest.fixture
def client():
    app.dependency_overrides[get_settings] = _test_settings
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()


@pytest.fixture
def auth_headers() -> dict[str, str]:
    return {"X-Internal-Token": TEST_TOKEN}
