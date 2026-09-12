import type { UserResponse } from "./types";

/**
 * Holds auth state outside React so the plain `apiFetch` wrapper (used by every query/mutation,
 * not just components) can read/update it without needing to be a hook itself. `AuthContext`
 * subscribes to this store and re-renders on change — this module is the single source of truth,
 * not a second copy of it.
 *
 * Token storage tradeoff: the refresh token and a cached copy of the user are kept in
 * localStorage so a page reload doesn't force a re-login; the access token is kept in memory only
 * (lost on reload, silently re-issued via the refresh token). Storing the refresh token in
 * localStorage rather than an httpOnly cookie is a deliberate, documented tradeoff for this
 * project's scope — see docs/security.md's frontend section.
 */

const REFRESH_TOKEN_KEY = "incident-platform.refreshToken";
const USER_KEY = "incident-platform.user";

let accessToken: string | null = null;
let user: UserResponse | null = readCachedUser();

const listeners = new Set<() => void>();

function readCachedUser(): UserResponse | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as UserResponse) : null;
  } catch {
    return null;
  }
}

function notify() {
  listeners.forEach((listener) => listener());
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function getUser(): UserResponse | null {
  return user;
}

export function setSession(
  nextAccessToken: string,
  nextRefreshToken: string,
  nextUser: UserResponse,
) {
  accessToken = nextAccessToken;
  user = nextUser;
  localStorage.setItem(REFRESH_TOKEN_KEY, nextRefreshToken);
  localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
  notify();
}

export function clearSession() {
  accessToken = null;
  user = null;
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  notify();
}

export function hasStoredRefreshToken(): boolean {
  return localStorage.getItem(REFRESH_TOKEN_KEY) !== null;
}
