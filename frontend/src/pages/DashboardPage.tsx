import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useDashboardMetrics } from "../hooks/useUsers";
import { Layout } from "../components/Layout";
import { LoadingState, ErrorState } from "../components/States";
import type { IncidentStatus } from "../api/types";

const SCOPE_LABEL: Record<string, string> = {
  OWN: "Your incidents",
  ASSIGNED: "Assigned to you",
  SYSTEM_WIDE: "All incidents",
};

const STATUS_ORDER: IncidentStatus[] = ["OPEN", "IN_PROGRESS", "ESCALATED", "RESOLVED", "CLOSED"];

const STATUS_LABEL: Record<IncidentStatus, string> = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  ESCALATED: "Escalated",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
};

export function DashboardPage() {
  const { user } = useAuth();
  const { data, isPending, error } = useDashboardMetrics();

  return (
    <Layout>
      <h1>Welcome back, {user?.displayName?.split(" ")[0]}</h1>
      <p style={{ color: "var(--color-ink-muted)", marginBottom: 28 }}>
        {data ? SCOPE_LABEL[data.scope] : "Loading your incident overview…"}
      </p>

      {isPending && <LoadingState />}
      {error && <ErrorState error={error} />}

      {data && (
        <>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))",
              gap: 12,
              marginBottom: 32,
            }}
          >
            <div className="card" style={{ padding: "18px 20px" }}>
              <div style={{ fontSize: 12, color: "var(--color-ink-muted)", marginBottom: 6 }}>
                Total
              </div>
              <div className="mono" style={{ fontSize: 26, fontWeight: 700 }}>
                {data.totalIncidents}
              </div>
            </div>
            {STATUS_ORDER.map((status) => (
              <div key={status} className="card" style={{ padding: "18px 20px" }}>
                <div style={{ fontSize: 12, color: "var(--color-ink-muted)", marginBottom: 6 }}>
                  {STATUS_LABEL[status]}
                </div>
                <div
                  className="mono"
                  style={{
                    fontSize: 26,
                    fontWeight: 700,
                    color: `var(--status-${status.toLowerCase().replace("_", "-")})`,
                  }}
                >
                  {data.countsByStatus[status] ?? 0}
                </div>
              </div>
            ))}
          </div>

          <Link to="/incidents" className="btn">
            View incidents
          </Link>
        </>
      )}
    </Layout>
  );
}
