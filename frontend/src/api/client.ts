import { ApiError } from "./ApiError";
import { clearSession, getAccessToken, getRefreshToken, setSession } from "./authStore";
import type { AuthResponse, ErrorResponse } from "./types";

const BASE_URL = import.meta.env.VITE_API_BASE_URL;

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined | null>;
}

let refreshInFlight: Promise<string | null> | null = null;

/** Exchanges the stored refresh token for a new access/refresh pair. Deduplicated via
 * `refreshInFlight` so concurrent 401s (e.g. several queries firing at once on page load) trigger
 * only one refresh call, not one per request. */
async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;

  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const response = await fetch(`${BASE_URL}/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
        if (!response.ok) return null;
        const auth = (await response.json()) as AuthResponse;
        setSession(auth.accessToken, auth.refreshToken, auth.user);
        return auth.accessToken;
      } catch {
        return null;
      } finally {
        refreshInFlight = null;
      }
    })();
  }
  return refreshInFlight;
}

function buildUrl(path: string, query?: RequestOptions["query"]): string {
  const url = new URL(`${BASE_URL}${path}`);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.toString();
}

async function parseErrorBody(response: Response): Promise<ErrorResponse["error"]> {
  try {
    const body = (await response.json()) as ErrorResponse;
    return body.error;
  } catch {
    return {
      code: "UNKNOWN_ERROR",
      message: response.statusText || "Something went wrong",
      details: null,
    };
  }
}

/**
 * The one function every query/mutation hook goes through. Public endpoints (auth) work the same
 * as protected ones — they simply have no token to attach yet.
 */
export async function apiFetch<T>(
  path: string,
  options: RequestOptions = {},
  isRetry = false,
): Promise<T> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  const token = getAccessToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const response = await fetch(buildUrl(path, options.query), {
    method: options.method ?? "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 401 && !isRetry && path !== "/auth/refresh") {
    const newToken = await refreshAccessToken();
    if (newToken) {
      return apiFetch<T>(path, options, true);
    }
    clearSession();
    throw new ApiError(401, "UNAUTHORIZED", "Your session has expired. Please log in again.");
  }

  if (!response.ok) {
    const error = await parseErrorBody(response);
    throw new ApiError(response.status, error.code, error.message, error.details);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
