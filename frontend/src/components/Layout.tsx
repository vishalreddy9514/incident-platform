import type { ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const navLinkStyle = ({ isActive }: { isActive: boolean }) => ({
  display: "block",
  padding: "8px 12px",
  borderRadius: 6,
  fontSize: 13.5,
  fontWeight: 500,
  color: isActive ? "var(--color-accent)" : "var(--color-ink-muted)",
  background: isActive ? "var(--color-accent-soft)" : "transparent",
});

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();

  return (
    <div style={{ display: "flex", minHeight: "100vh" }}>
      <aside
        style={{
          width: 220,
          flexShrink: 0,
          borderRight: "1px solid var(--color-border)",
          background: "var(--color-surface)",
          display: "flex",
          flexDirection: "column",
          padding: "20px 14px",
        }}
      >
        <div style={{ padding: "0 8px", marginBottom: 28 }}>
          <div style={{ fontSize: 15, fontWeight: 700, letterSpacing: "-0.01em" }}>
            Incident Platform
          </div>
        </div>

        <nav style={{ display: "flex", flexDirection: "column", gap: 2, flex: 1 }}>
          <NavLink to="/" end style={navLinkStyle}>
            Dashboard
          </NavLink>
          <NavLink to="/incidents" style={navLinkStyle}>
            Incidents
          </NavLink>
          <NavLink to="/incidents/new" style={navLinkStyle}>
            New incident
          </NavLink>
          {user?.role === "ADMIN" && (
            <NavLink to="/admin" style={navLinkStyle}>
              Admin
            </NavLink>
          )}
        </nav>

        <div style={{ borderTop: "1px solid var(--color-border)", paddingTop: 14, marginTop: 14 }}>
          <div style={{ padding: "0 8px", marginBottom: 10 }}>
            <div style={{ fontSize: 13.5, fontWeight: 600 }}>{user?.displayName}</div>
            <div style={{ fontSize: 12, color: "var(--color-ink-faint)" }}>{user?.role}</div>
          </div>
          <button
            className="btn-secondary btn"
            style={{ width: "100%", justifyContent: "center" }}
            onClick={logout}
          >
            Log out
          </button>
        </div>
      </aside>

      <main style={{ flex: 1, padding: "28px 36px", maxWidth: 1100 }}>{children}</main>
    </div>
  );
}
