import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "../api/client";
import type { CategoryResponse } from "../api/types";

export function useCategories() {
  return useQuery({
    queryKey: ["categories"],
    queryFn: () => apiFetch<CategoryResponse[]>("/categories"),
  });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { name: string; description: string }) =>
      apiFetch<CategoryResponse>("/categories", { method: "POST", body: input }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["categories"] }),
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      ...input
    }: {
      id: number;
      name: string;
      description: string;
      isActive: boolean;
    }) => apiFetch<CategoryResponse>(`/categories/${id}`, { method: "PUT", body: input }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["categories"] }),
  });
}
