from app.main import app
from app.providers.errors import AnalysisProviderError
from app.routers.analyse import get_provider

PAYLOAD = {
    "title": "VPN keeps disconnecting",
    "description": (
        "My VPN connection drops every few minutes and I can't reach internal services. "
        "This is blocking my work."
    ),
}


def test_analyse_requires_internal_token(client):
    response = client.post("/internal/v1/analyse", json=PAYLOAD)

    assert response.status_code == 401


def test_analyse_rejects_wrong_token(client):
    response = client.post(
        "/internal/v1/analyse", json=PAYLOAD, headers={"X-Internal-Token": "wrong"}
    )

    assert response.status_code == 401


def test_analyse_rejects_empty_title(client, auth_headers):
    response = client.post(
        "/internal/v1/analyse",
        json={"title": "", "description": "d"},
        headers=auth_headers,
    )

    assert response.status_code == 422


def test_analyse_rejects_missing_description(client, auth_headers):
    response = client.post("/internal/v1/analyse", json={"title": "t"}, headers=auth_headers)

    assert response.status_code == 422


def test_analyse_returns_shaped_response(client, auth_headers):
    response = client.post("/internal/v1/analyse", json=PAYLOAD, headers=auth_headers)

    assert response.status_code == 200
    body = response.json()
    assert set(body.keys()) == {
        "suggestedCategory",
        "predictedPriority",
        "summary",
        "keywords",
        "suggestedSteps",
        "modelUsed",
    }
    assert body["suggestedCategory"] == "Network"
    assert body["predictedPriority"] in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
    assert isinstance(body["keywords"], list) and body["keywords"]
    assert isinstance(body["suggestedSteps"], list) and body["suggestedSteps"]
    assert body["modelUsed"] == "mock-heuristic-v1"


def test_analyse_accepts_optional_category_hint(client, auth_headers):
    payload = {**PAYLOAD, "category": "Network"}

    response = client.post("/internal/v1/analyse", json=payload, headers=auth_headers)

    assert response.status_code == 200


def test_analyse_maps_a_provider_failure_to_502(client, auth_headers):
    class FailingProvider:
        async def analyse(self, request):
            raise AnalysisProviderError("upstream is down")

    app.dependency_overrides[get_provider] = FailingProvider
    try:
        response = client.post("/internal/v1/analyse", json=PAYLOAD, headers=auth_headers)
    finally:
        del app.dependency_overrides[get_provider]

    assert response.status_code == 502
