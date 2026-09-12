import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { apiFetch } from "../api/client";
import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  getUser,
  hasStoredRefreshToken,
  setSession,
  subscribe,
} from "../api/authStore";
import type { AuthResponse, UserResponse } from "../api/types";

interface AuthContextValue {
  user: UserResponse | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(getUser());

  useEffect(() => subscribe(() => setUser(getUser())), []);

  async function login(email: string, password: string) {
    const auth = await apiFetch<AuthResponse>("/auth/login", {
      method: "POST",
      body: { email, password },
    });
    setSession(auth.accessToken, auth.refreshToken, auth.user);
  }

  async function register(email: string, password: string, displayName: string) {
    const auth = await apiFetch<AuthResponse>("/auth/register", {
      method: "POST",
      body: { email, password, displayName },
    });
    setSession(auth.accessToken, auth.refreshToken, auth.user);
  }

  function logout() {
    // Best-effort: revoke the refresh token server-side, but don't block clearing local state on
    // it — a failed logout call shouldn't leave the user stuck "logged in" in the UI.
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      apiFetch("/auth/logout", { method: "POST", body: { refreshToken } }).catch(() => {});
    }
    clearSession();
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: user !== null, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

/** Used on app load to decide whether to show a loading state while a stored refresh token is
 * exchanged for a fresh access token, vs. immediately showing the logged-out state. */
export function hasStoredSession(): boolean {
  return hasStoredRefreshToken() || getAccessToken() !== null;
}
