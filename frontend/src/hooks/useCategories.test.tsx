import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import type { ReactNode } from "react";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { useCategories, useCreateCategory } from "./useCategories";

const BASE_URL = import.meta.env.VITE_API_BASE_URL;

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("useCategories", () => {
  it("fetches and returns the category list", async () => {
    server.use(
      http.get(`${BASE_URL}/categories`, () =>
        HttpResponse.json([{ id: 1, name: "Hardware", description: null, isActive: true }]),
      ),
    );

    const { result } = renderHook(() => useCategories(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toEqual([
      { id: 1, name: "Hardware", description: null, isActive: true },
    ]);
  });

  it("surfaces an error state when the request fails", async () => {
    server.use(
      http.get(`${BASE_URL}/categories`, () =>
        HttpResponse.json(
          { error: { code: "INTERNAL_ERROR", message: "boom", details: null } },
          { status: 500 },
        ),
      ),
    );

    const { result } = renderHook(() => useCategories(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

describe("useCreateCategory", () => {
  it("posts the new category and returns the created response", async () => {
    server.use(
      http.post(`${BASE_URL}/categories`, async ({ request }) => {
        const body = (await request.json()) as { name: string; description: string };
        return HttpResponse.json(
          { id: 2, name: body.name, description: body.description, isActive: true },
          { status: 201 },
        );
      }),
    );

    const { result } = renderHook(() => useCreateCategory(), { wrapper });

    result.current.mutate({ name: "Network", description: "Connectivity issues" });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({
      id: 2,
      name: "Network",
      description: "Connectivity issues",
      isActive: true,
    });
  });
});
