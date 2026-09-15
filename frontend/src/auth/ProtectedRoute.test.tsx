import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { ProtectedRoute } from "./ProtectedRoute";
import * as AuthContext from "./AuthContext";

function renderProtectedRoute(allowedRoles?: ("USER" | "ENGINEER" | "ADMIN")[]) {
  return render(
    <MemoryRouter initialEntries={["/protected"]}>
      <Routes>
        <Route path="/login" element={<div>Login page</div>} />
        <Route path="/" element={<div>Home page</div>} />
        <Route
          path="/protected"
          element={
            <ProtectedRoute allowedRoles={allowedRoles}>
              <div>Protected content</div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ProtectedRoute", () => {
  it("redirects to /login when not authenticated", () => {
    vi.spyOn(AuthContext, "useAuth").mockReturnValue({
      user: null,
      isAuthenticated: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    });

    renderProtectedRoute();

    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("redirects to / when the user's role isn't in allowedRoles", () => {
    vi.spyOn(AuthContext, "useAuth").mockReturnValue({
      user: {
        id: 1,
        email: "user@example.com",
        displayName: "A User",
        role: "USER",
        teamId: null,
        isActive: true,
      },
      isAuthenticated: true,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    });

    renderProtectedRoute(["ADMIN"]);

    expect(screen.getByText("Home page")).toBeInTheDocument();
  });

  it("renders the protected content when authenticated and allowed", () => {
    vi.spyOn(AuthContext, "useAuth").mockReturnValue({
      user: {
        id: 1,
        email: "admin@example.com",
        displayName: "An Admin",
        role: "ADMIN",
        teamId: null,
        isActive: true,
      },
      isAuthenticated: true,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    });

    renderProtectedRoute(["ADMIN"]);

    expect(screen.getByText("Protected content")).toBeInTheDocument();
  });

  it("renders the protected content when authenticated and no roles are required", () => {
    vi.spyOn(AuthContext, "useAuth").mockReturnValue({
      user: {
        id: 1,
        email: "user@example.com",
        displayName: "A User",
        role: "USER",
        teamId: null,
        isActive: true,
      },
      isAuthenticated: true,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    });

    renderProtectedRoute();

    expect(screen.getByText("Protected content")).toBeInTheDocument();
  });
});
