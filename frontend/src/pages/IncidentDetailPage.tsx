import { useState } from "react";
import { useParams } from "react-router-dom";
import { Layout } from "../components/Layout";
import { StatusBadge, PriorityBadge } from "../components/Badges";
import { LoadingState, ErrorState } from "../components/States";
import { useAuth } from "../auth/AuthContext";
import {
  useAddComment,
  useAssignIncident,
  useEscalateIncident,
  useIncident,
  useIncidentComments,
  useIncidentHistory,
  useUpdateIncident,
} from "../hooks/useIncidents";
import { useUsers } from "../hooks/useUsers";
import { ALLOWED_STATUS_TRANSITIONS } from "../api/types";
import type { CommentResponse, HistoryEntryResponse, IncidentStatus } from "../api/types";
import { ApiError } from "../api/ApiError";

const CAN_MANAGE_ROLES = new Set(["ENGINEER", "ADMIN"]);

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

export function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const incidentId = id ? Number(id) : undefined;
  const { user } = useAuth();

  const { data: incident, isPending, error } = useIncident(incidentId);
  const { data: comments } = useIncidentComments(incidentId);
  const { data: history } = useIncidentHistory(incidentId);

  if (isPending) {
    return (
      <Layout>
        <LoadingState label="Loading incident…" />
      </Layout>
    );
  }

  if (error || !incident) {
    return (
      <Layout>
        <ErrorState error={error} />
      </Layout>
    );
  }

  const canManage = user ? CAN_MANAGE_ROLES.has(user.role) : false;

  return (
    <Layout>
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}>
          <span className="mono" style={{ fontSize: 13, color: "var(--color-ink-faint)" }}>
            #{incident.id}
          </span>
          <StatusBadge status={incident.status} />
          <PriorityBadge priority={incident.priority} />
        </div>
        <h1>{incident.title}</h1>
      </div>

      <div
        style={{ display: "grid", gridTemplateColumns: "1.6fr 1fr", gap: 24, alignItems: "start" }}
      >
        <div>
          <div className="card" style={{ padding: 24, marginBottom: 20 }}>
            <h3>Description</h3>
            <p style={{ whiteSpace: "pre-wrap", color: "var(--color-ink)" }}>
              {incident.description}
            </p>

            <dl style={infoGridStyle}>
              <InfoItem label="Category" value={incident.categoryName} />
              <InfoItem label="Severity" value={incident.severity} />
              <InfoItem label="Reported by" value={incident.createdByDisplayName} />
              <InfoItem
                label="Assigned to"
                value={incident.assignedToDisplayName ?? "Unassigned"}
              />
              <InfoItem label="Created" value={formatDate(incident.createdAt)} />
              <InfoItem label="Updated" value={formatDate(incident.updatedAt)} />
            </dl>
          </div>

          <CommentsSection incidentId={incident.id} comments={comments ?? []} />
        </div>

        <div>
          {canManage && (
            <ManageIncidentPanel incidentId={incident.id} currentStatus={incident.status} />
          )}
          <HistoryPanel entries={history ?? []} />
        </div>
      </div>
    </Layout>
  );
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt style={{ fontSize: 11.5, color: "var(--color-ink-muted)", marginBottom: 2 }}>{label}</dt>
      <dd style={{ margin: 0, fontSize: 13.5 }}>{value}</dd>
    </div>
  );
}

const infoGridStyle = {
  display: "grid",
  gridTemplateColumns: "repeat(3, 1fr)",
  gap: "14px 20px",
  marginTop: 20,
  paddingTop: 16,
  borderTop: "1px solid var(--color-border)",
} as const;

function ManageIncidentPanel({
  incidentId,
  currentStatus,
}: {
  incidentId: number;
  currentStatus: IncidentStatus;
}) {
  const updateIncident = useUpdateIncident(incidentId);
  const assignIncident = useAssignIncident(incidentId);
  const escalateIncident = useEscalateIncident(incidentId);
  // size=100: a pragmatic stand-in for a dedicated "assignable users" query. The backend's
  // GET /users supports a single-role filter, but assignment accepts ENGINEER *or* ADMIN, so a
  // wide page plus the client-side filter below covers both without a second backend change.
  const { data: usersPage } = useUsers(0, 100);

  const [assigneeId, setAssigneeId] = useState("");
  const [actionError, setActionError] = useState<string | null>(null);

  const nextStatuses = ALLOWED_STATUS_TRANSITIONS[currentStatus];
  const assignableUsers = (usersPage?.content ?? []).filter(
    (u) => u.role === "ENGINEER" || u.role === "ADMIN",
  );

  async function handleStatusChange(status: IncidentStatus) {
    setActionError(null);
    try {
      await updateIncident.mutateAsync({ status });
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't update status.");
    }
  }

  async function handleAssign() {
    if (!assigneeId) return;
    setActionError(null);
    try {
      await assignIncident.mutateAsync(Number(assigneeId));
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't assign incident.");
    }
  }

  async function handleEscalate() {
    setActionError(null);
    try {
      await escalateIncident.mutateAsync(undefined);
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't escalate incident.");
    }
  }

  return (
    <div className="card" style={{ padding: 20, marginBottom: 20 }}>
      <h3>Manage</h3>
      {actionError && <div className="error-banner">{actionError}</div>}

      <div className="field">
        <label htmlFor="status">Change status</label>
        <select
          id="status"
          value=""
          disabled={nextStatuses.length === 0 || updateIncident.isPending}
          onChange={(e) => e.target.value && handleStatusChange(e.target.value as IncidentStatus)}
        >
          <option value="">
            {nextStatuses.length === 0 ? "No further transitions" : "Select a new status…"}
          </option>
          {nextStatuses.map((status) => (
            <option key={status} value={status}>
              {status.replace("_", " ")}
            </option>
          ))}
        </select>
      </div>

      <div className="field">
        <label htmlFor="assignee">Assign to</label>
        <select id="assignee" value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)}>
          <option value="">Choose an engineer…</option>
          {assignableUsers.map((u) => (
            <option key={u.id} value={u.id}>
              {u.displayName}
            </option>
          ))}
        </select>
        <button
          className="btn-secondary btn"
          disabled={!assigneeId || assignIncident.isPending}
          onClick={handleAssign}
          style={{ marginTop: 8 }}
        >
          {assignIncident.isPending ? "Assigning…" : "Assign"}
        </button>
      </div>

      <button
        className="btn-secondary btn"
        disabled={currentStatus === "ESCALATED" || escalateIncident.isPending}
        onClick={handleEscalate}
        style={{ width: "100%", justifyContent: "center" }}
      >
        {escalateIncident.isPending ? "Escalating…" : "Escalate"}
      </button>
    </div>
  );
}

function CommentsSection({
  incidentId,
  comments,
}: {
  incidentId: number;
  comments: CommentResponse[];
}) {
  const [body, setBody] = useState("");
  const addComment = useAddComment(incidentId);

  async function handleSubmit() {
    if (!body.trim()) return;
    await addComment.mutateAsync(body);
    setBody("");
  }

  return (
    <div className="card" style={{ padding: 24 }}>
      <h3>Comments</h3>

      {comments.length === 0 && (
        <p style={{ color: "var(--color-ink-faint)", fontSize: 13.5 }}>No comments yet.</p>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 14, marginBottom: 18 }}>
        {comments.map((comment) => (
          <div
            key={comment.id}
            style={{ borderTop: "1px solid var(--color-border)", paddingTop: 12 }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
              <span style={{ fontWeight: 600, fontSize: 13 }}>{comment.authorDisplayName}</span>
              <span style={{ fontSize: 12, color: "var(--color-ink-faint)" }}>
                {formatDate(comment.createdAt)}
              </span>
            </div>
            <p style={{ margin: 0, fontSize: 13.5, whiteSpace: "pre-wrap" }}>{comment.body}</p>
          </div>
        ))}
      </div>

      <div className="field" style={{ marginBottom: 8 }}>
        <textarea
          rows={3}
          placeholder="Add a comment…"
          value={body}
          onChange={(e) => setBody(e.target.value)}
        />
      </div>
      <button
        className="btn"
        disabled={!body.trim() || addComment.isPending}
        onClick={handleSubmit}
      >
        {addComment.isPending ? "Posting…" : "Post comment"}
      </button>
    </div>
  );
}

function HistoryPanel({ entries }: { entries: HistoryEntryResponse[] }) {
  return (
    <div className="card" style={{ padding: 20 }}>
      <h3>Timeline</h3>
      {entries.length === 0 && (
        <p style={{ color: "var(--color-ink-faint)", fontSize: 13 }}>No history yet.</p>
      )}
      <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
        {entries.map((entry) => (
          <div key={entry.id} style={{ fontSize: 12.5 }}>
            <div style={{ color: "var(--color-ink-faint)", marginBottom: 2 }}>
              {formatDate(entry.changedAt)} · {entry.actorDisplayName}
            </div>
            <div>
              <strong>{entry.fieldChanged}</strong> {entry.oldValue ? `${entry.oldValue} → ` : ""}
              {entry.newValue ?? "—"}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
