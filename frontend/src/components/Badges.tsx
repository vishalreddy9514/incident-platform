import type { IncidentPriority, IncidentSeverity, IncidentStatus } from "../api/types";

const STATUS_LABEL: Record<IncidentStatus, string> = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  ESCALATED: "Escalated",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
};

const STATUS_VAR: Record<IncidentStatus, string> = {
  OPEN: "open",
  IN_PROGRESS: "in-progress",
  ESCALATED: "escalated",
  RESOLVED: "resolved",
  CLOSED: "closed",
};

export function StatusBadge({ status }: { status: IncidentStatus }) {
  const key = STATUS_VAR[status];
  return (
    <span
      className="badge"
      style={{
        color: `var(--status-${key})`,
        background: `var(--status-${key}-soft)`,
      }}
    >
      {STATUS_LABEL[status]}
    </span>
  );
}

const PRIORITY_COLOR: Record<IncidentPriority, string> = {
  LOW: "var(--color-ink-muted)",
  MEDIUM: "var(--status-open)",
  HIGH: "var(--status-in-progress)",
  CRITICAL: "var(--status-escalated)",
};

const PRIORITY_LABEL: Record<IncidentPriority, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  CRITICAL: "Critical",
};

export function PriorityBadge({ priority }: { priority: IncidentPriority | IncidentSeverity }) {
  return (
    <span style={{ color: PRIORITY_COLOR[priority], fontWeight: 600, fontSize: 12.5 }}>
      {PRIORITY_LABEL[priority]}
    </span>
  );
}
