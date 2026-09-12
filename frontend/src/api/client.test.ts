import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { apiFetch } from "./client";
import { ApiError } from "./ApiError";
import { clearSession, getAccessToken, setSession } from "./authStore";

const BASE_URL = import.meta.env.VITE_API_BASE_URL;

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => {
  server.resetHandlers();
  clearSession();
  localStorage.clear();
});
afterAll(() => server.close());

const sampleUser = {
  id: 1,
  email: "user@example.com",
  displayName: "Test User",
  role: "USER" as const,
  teamId: null,
  isActive: true,
};

describe("apiFetch", () => {
  it("attaches the Authorization header when an access token is present", async () => {
    setSession("valid-token", "refresh-token", sampleUser);
    let receivedAuthHeader: string | null = null;

    server.use(
      http.get(`${BASE_URL}/categories`, ({ request }) => {
        receivedAuthHeader = request.headers.get("Authorization");
        return HttpResponse.json([]);
      }),
    );

    await apiFetch("/categories");

    expect(receivedAuthHeader).toBe("Bearer valid-token");
  });

  it("throws a typed ApiError with the backend's error code on failure", async () => {
    server.use(
      http.get(`${BASE_URL}/incidents/999`, () =>
        HttpResponse.json(
          { error: { code: "NOT_FOUND", message: "Incident not found: 999", details: null } },
          { status: 404 },
        ),
      ),
    );

    await expect(apiFetch("/incidents/999")).rejects.toMatchObject({
      name: "ApiError",
      code: "NOT_FOUND",
      status: 404,
    });
  });

  it("automatically refreshes an expired access token and retries the original request once", async () => {
    setSession("expired-token", "refresh-token", sampleUser);
    let categoriesCallCount = 0;

    server.use(
      http.post(`${BASE_URL}/auth/refresh`, () =>
        HttpResponse.json({
          accessToken: "new-token",
          refreshToken: "new-refresh-token",
          tokenType: "Bearer",
          expiresInSeconds: 900,
          user: sampleUser,
        }),
      ),
      http.get(`${BASE_URL}/categories`, ({ request }) => {
        categoriesCallCount += 1;
        const auth = request.headers.get("Authorization");
        if (auth === "Bearer expired-token") {
          return HttpResponse.json(
            { error: { code: "UNAUTHORIZED", message: "Token expired", details: null } },
            { status: 401 },
          );
        }
        return HttpResponse.json([{ id: 1, name: "Hardware", description: null, isActive: true }]);
      }),
    );

    const result = await apiFetch<unknown[]>("/categories");

    expect(categoriesCallCount).toBe(2); // first attempt (401) + retry after refresh
    expect(result).toHaveLength(1);
    expect(getAccessToken()).toBe("new-token"); // the refreshed token is now stored
  });

  it("clears the session and throws when the refresh token is itself invalid", async () => {
    setSession("expired-token", "invalid-refresh-token", sampleUser);

    server.use(
      http.post(`${BASE_URL}/auth/refresh`, () =>
        HttpResponse.json(
          {
            error: {
              code: "INVALID_REFRESH_TOKEN",
              message: "Refresh token is invalid",
              details: null,
            },
          },
          { status: 401 },
        ),
      ),
      http.get(`${BASE_URL}/categories`, () =>
        HttpResponse.json(
          { error: { code: "UNAUTHORIZED", message: "Token expired", details: null } },
          { status: 401 },
        ),
      ),
    );

    await expect(apiFetch("/categories")).rejects.toBeInstanceOf(ApiError);
    expect(getAccessToken()).toBeNull();
  });
});
