import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "../api/client";
import type {
  CommentResponse,
  HistoryEntryResponse,
  IncidentDetailResponse,
  IncidentPriority,
  IncidentSeverity,
  IncidentStatus,
  IncidentSummaryResponse,
  PageResponse,
} from "../api/types";

export interface IncidentFilters {
  status?: IncidentStatus;
  priority?: IncidentPriority;
  categoryId?: number;
  assignedToId?: number;
  page?: number;
  size?: number;
}

export function useIncidents(filters: IncidentFilters) {
  return useQuery({
    queryKey: ["incidents", filters],
    queryFn: () =>
      apiFetch<PageResponse<IncidentSummaryResponse>>("/incidents", {
        query: {
          status: filters.status,
          priority: filters.priority,
          categoryId: filters.categoryId,
          assignedToId: filters.assignedToId,
          page: filters.page ?? 0,
          size: filters.size ?? 20,
        },
      }),
  });
}

export function useIncident(id: number | undefined) {
  return useQuery({
    queryKey: ["incident", id],
    queryFn: () => apiFetch<IncidentDetailResponse>(`/incidents/${id}`),
    enabled: id !== undefined,
  });
}

export function useIncidentComments(id: number | undefined) {
  return useQuery({
    queryKey: ["incident", id, "comments"],
    queryFn: () => apiFetch<CommentResponse[]>(`/incidents/${id}/comments`),
    enabled: id !== undefined,
  });
}

export function useIncidentHistory(id: number | undefined) {
  return useQuery({
    queryKey: ["incident", id, "history"],
    queryFn: () => apiFetch<HistoryEntryResponse[]>(`/incidents/${id}/history`),
    enabled: id !== undefined,
  });
}

export interface CreateIncidentInput {
  title: string;
  description: string;
  categoryId: number;
}

export function useCreateIncident() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateIncidentInput) =>
      apiFetch<IncidentDetailResponse>("/incidents", { method: "POST", body: input }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["incidents"] }),
  });
}

export interface UpdateIncidentInput {
  title?: string;
  description?: string;
  categoryId?: number;
  status?: IncidentStatus;
  priority?: IncidentPriority;
  severity?: IncidentSeverity;
}

export function useUpdateIncident(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: UpdateIncidentInput) =>
      apiFetch<IncidentDetailResponse>(`/incidents/${id}`, { method: "PUT", body: input }),
    onSuccess: (updated) => {
      queryClient.setQueryData(["incident", id], updated);
      queryClient.invalidateQueries({ queryKey: ["incident", id, "history"] });
      queryClient.invalidateQueries({ queryKey: ["incidents"] });
    },
  });
}

export function useAssignIncident(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (assignedToUserId: number) =>
      apiFetch<IncidentDetailResponse>(`/incidents/${id}/assign`, {
        method: "POST",
        body: { assignedToUserId },
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData(["incident", id], updated);
      queryClient.invalidateQueries({ queryKey: ["incident", id, "history"] });
      queryClient.invalidateQueries({ queryKey: ["incidents"] });
    },
  });
}

export function useEscalateIncident(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reason: string | undefined) =>
      apiFetch<IncidentDetailResponse>(`/incidents/${id}/escalate`, {
        method: "POST",
        body: { reason },
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData(["incident", id], updated);
      queryClient.invalidateQueries({ queryKey: ["incident", id, "history"] });
      queryClient.invalidateQueries({ queryKey: ["incidents"] });
    },
  });
}

export function useAddComment(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: string) =>
      apiFetch<CommentResponse>(`/incidents/${id}/comments`, { method: "POST", body: { body } }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["incident", id, "comments"] }),
  });
}
