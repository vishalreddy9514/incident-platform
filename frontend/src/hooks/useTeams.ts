import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "../api/client";
import type { TeamResponse } from "../api/types";

export function useTeams() {
  return useQuery({
    queryKey: ["teams"],
    queryFn: () => apiFetch<TeamResponse[]>("/teams"),
  });
}

export function useCreateTeam() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { name: string; description: string }) =>
      apiFetch<TeamResponse>("/teams", { method: "POST", body: input }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["teams"] }),
  });
}
