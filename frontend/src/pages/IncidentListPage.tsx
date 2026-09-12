import { useState, type CSSProperties } from "react";
import { Link } from "react-router-dom";
import { Layout } from "../components/Layout";
import { StatusBadge, PriorityBadge } from "../components/Badges";
import { LoadingState, ErrorState, EmptyState } from "../components/States";
import { useIncidents, type IncidentFilters } from "../hooks/useIncidents";
import { INCIDENT_STATUSES, INCIDENT_PRIORITIES } from "../api/types";
import type { IncidentPriority, IncidentStatus } from "../api/types";

export function IncidentListPage() {
  const [filters, setFilters] = useState<IncidentFilters>({ page: 0 });
  const { data, isPending, error } = useIncidents(filters);

  function updateFilter<K extends keyof IncidentFilters>(key: K, value: IncidentFilters[K]) {
    setFilters((prev) => ({ ...prev, [key]: value, page: 0 }));
  }

  return (
    <Layout>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          marginBottom: 20,
        }}
      >
        <h1>Incidents</h1>
        <Link to="/incidents/new" className="btn">
          + New incident
        </Link>
      </div>

      <div style={{ display: "flex", gap: 10, marginBottom: 20 }}>
        <select
          value={filters.status ?? ""}
          onChange={(e) =>
            updateFilter("status", (e.target.value || undefined) as IncidentStatus | undefined)
          }
          style={selectStyle}
        >
          <option value="">All statuses</option>
          {INCIDENT_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s.replace("_", " ")}
            </option>
          ))}
        </select>

        <select
          value={filters.priority ?? ""}
          onChange={(e) =>
            updateFilter("priority", (e.target.value || undefined) as IncidentPriority | undefined)
          }
          style={selectStyle}
        >
          <option value="">All priorities</option>
          {INCIDENT_PRIORITIES.map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>
      </div>

      {isPending && <LoadingState label="Loading incidents…" />}
      {error && <ErrorState error={error} />}

      {data && data.content.length === 0 && (
        <div className="card">
          <EmptyState>
            No incidents match these filters yet. <Link to="/incidents/new">Create one</Link> to get
            started.
          </EmptyState>
        </div>
      )}

      {data && data.content.length > 0 && (
        <div className="card" style={{ overflow: "hidden" }}>
          <table>
            <thead>
              <tr>
                <th>Title</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Category</th>
                <th>Assignee</th>
                <th>Reported by</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((incident) => (
                <tr key={incident.id}>
                  <td>
                    <Link to={`/incidents/${incident.id}`} style={{ fontWeight: 500 }}>
                      {incident.title}
                    </Link>
                  </td>
                  <td>
                    <StatusBadge status={incident.status} />
                  </td>
                  <td>
                    <PriorityBadge priority={incident.priority} />
                  </td>
                  <td>{incident.categoryName}</td>
                  <td>{incident.assignedToDisplayName ?? "—"}</td>
                  <td>{incident.createdByDisplayName}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div style={{ display: "flex", gap: 8, marginTop: 16, alignItems: "center" }}>
          <button
            className="btn-secondary btn"
            disabled={data.page === 0}
            onClick={() => setFilters((prev) => ({ ...prev, page: (prev.page ?? 0) - 1 }))}
          >
            Previous
          </button>
          <span style={{ fontSize: 13, color: "var(--color-ink-muted)" }}>
            Page {data.page + 1} of {data.totalPages}
          </span>
          <button
            className="btn-secondary btn"
            disabled={data.page + 1 >= data.totalPages}
            onClick={() => setFilters((prev) => ({ ...prev, page: (prev.page ?? 0) + 1 }))}
          >
            Next
          </button>
        </div>
      )}
    </Layout>
  );
}

const selectStyle: CSSProperties = {
  padding: "8px 10px",
  border: "1px solid var(--color-border-strong)",
  borderRadius: 6,
  fontSize: 13.5,
  background: "var(--color-surface)",
  color: "var(--color-ink)",
};
