import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "../api/client";
import type { DashboardMetricsResponse, PageResponse, Role, UserResponse } from "../api/types";

export function useUsers(page = 0, size = 20) {
  return useQuery({
    queryKey: ["users", page, size],
    queryFn: () => apiFetch<PageResponse<UserResponse>>("/users", { query: { page, size } }),
  });
}

export function useUpdateUserRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, role }: { id: number; role: Role }) =>
      apiFetch<UserResponse>(`/users/${id}/role`, { method: "PATCH", body: { role } }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["users"] }),
  });
}

export function useDashboardMetrics() {
  return useQuery({
    queryKey: ["dashboard", "metrics"],
    queryFn: () => apiFetch<DashboardMetricsResponse>("/dashboard/metrics"),
  });
}
