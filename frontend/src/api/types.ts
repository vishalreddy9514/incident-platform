// Mirrors the backend DTOs (see docs/api.md) field-for-field. Kept as one file since the
// frontend has no independent domain model of its own yet — it's a thin client over this API.

export type Role = "USER" | "ENGINEER" | "ADMIN";

export type IncidentStatus = "OPEN" | "IN_PROGRESS" | "ESCALATED" | "RESOLVED" | "CLOSED";
export type IncidentPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type IncidentSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export const INCIDENT_STATUSES: IncidentStatus[] = [
  "OPEN",
  "IN_PROGRESS",
  "ESCALATED",
  "RESOLVED",
  "CLOSED",
];
export const INCIDENT_PRIORITIES: IncidentPriority[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
export const INCIDENT_SEVERITIES: IncidentSeverity[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

/** Valid next states per the backend's IncidentService.ALLOWED_TRANSITIONS (docs/api.md). Kept
 * in sync manually — see the note on IncidentDetailPage for why this isn't fetched from the API. */
export const ALLOWED_STATUS_TRANSITIONS: Record<IncidentStatus, IncidentStatus[]> = {
  OPEN: ["IN_PROGRESS", "ESCALATED", "CLOSED"],
  IN_PROGRESS: ["ESCALATED", "RESOLVED", "OPEN"],
  ESCALATED: ["IN_PROGRESS", "RESOLVED"],
  RESOLVED: ["CLOSED", "IN_PROGRESS"],
  CLOSED: [],
};

export interface UserResponse {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  teamId: number | null;
  isActive: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserResponse;
}

export interface CategoryResponse {
  id: number;
  name: string;
  description: string | null;
  isActive: boolean;
}

export interface TeamResponse {
  id: number;
  name: string;
  description: string | null;
}

export interface IncidentSummaryResponse {
  id: number;
  title: string;
  status: IncidentStatus;
  priority: IncidentPriority;
  severity: IncidentSeverity;
  categoryName: string;
  createdByDisplayName: string;
  assignedToDisplayName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface IncidentDetailResponse {
  id: number;
  title: string;
  description: string;
  status: IncidentStatus;
  priority: IncidentPriority;
  severity: IncidentSeverity;
  categoryId: number;
  categoryName: string;
  createdById: number;
  createdByDisplayName: string;
  assignedToId: number | null;
  assignedToDisplayName: string | null;
  teamId: number | null;
  teamName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CommentResponse {
  id: number;
  authorId: number;
  authorDisplayName: string;
  body: string;
  createdAt: string;
}

export interface HistoryEntryResponse {
  id: number;
  actorId: number;
  actorDisplayName: string;
  fieldChanged: string;
  oldValue: string | null;
  newValue: string | null;
  changedAt: string;
}

export interface DashboardMetricsResponse {
  scope: "OWN" | "ASSIGNED" | "SYSTEM_WIDE";
  totalIncidents: number;
  countsByStatus: Record<IncidentStatus, number>;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ErrorResponse {
  error: {
    code: string;
    message: string;
    details: unknown;
  };
}
