def test_health_ok_and_unauthenticated(client):
    response = client.get("/internal/v1/health")

    assert response.status_code == 200
    body = response.json()
    assert body == {"status": "ok", "provider": "mock"}
