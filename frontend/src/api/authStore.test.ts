import { afterEach, describe, expect, it, vi } from "vitest";
import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  getUser,
  hasStoredRefreshToken,
  setSession,
  subscribe,
} from "./authStore";

const sampleUser = {
  id: 1,
  email: "user@example.com",
  displayName: "Test User",
  role: "USER" as const,
  teamId: null,
  isActive: true,
};

afterEach(() => {
  clearSession();
  localStorage.clear();
});

describe("authStore", () => {
  it("has no session by default", () => {
    expect(getAccessToken()).toBeNull();
    expect(getUser()).toBeNull();
    expect(hasStoredRefreshToken()).toBe(false);
  });

  it("setSession stores the access token in memory and the rest in localStorage", () => {
    setSession("access-token", "refresh-token", sampleUser);

    expect(getAccessToken()).toBe("access-token");
    expect(getRefreshToken()).toBe("refresh-token");
    expect(getUser()).toEqual(sampleUser);
    expect(hasStoredRefreshToken()).toBe(true);
    expect(localStorage.getItem("incident-platform.refreshToken")).toBe("refresh-token");
  });

  it("clearSession removes everything", () => {
    setSession("access-token", "refresh-token", sampleUser);

    clearSession();

    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
    expect(getUser()).toBeNull();
    expect(hasStoredRefreshToken()).toBe(false);
  });

  it("notifies subscribers on setSession and clearSession", () => {
    const listener = vi.fn();
    const unsubscribe = subscribe(listener);

    setSession("access-token", "refresh-token", sampleUser);
    clearSession();

    expect(listener).toHaveBeenCalledTimes(2);
    unsubscribe();
  });

  it("unsubscribe stops further notifications", () => {
    const listener = vi.fn();
    const unsubscribe = subscribe(listener);
    unsubscribe();

    setSession("access-token", "refresh-token", sampleUser);

    expect(listener).not.toHaveBeenCalled();
  });

  it("rehydrates the cached user from localStorage on module load", async () => {
    localStorage.setItem("incident-platform.user", JSON.stringify(sampleUser));

    // The module caches `user` in a top-level variable read once at import time, so seeing
    // localStorage's value requires a fresh module instance rather than the one already
    // imported above.
    vi.resetModules();
    const freshStore = await import("./authStore");

    expect(freshStore.getUser()).toEqual(sampleUser);
  });
});
