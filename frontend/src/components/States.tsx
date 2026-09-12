import type { ReactNode } from "react";
import { ApiError } from "../api/ApiError";

export function LoadingState({ label = "Loading…" }: { label?: string }) {
  return <p style={{ color: "var(--color-ink-faint)", fontSize: 13.5 }}>{label}</p>;
}

export function ErrorState({ error }: { error: unknown }) {
  const message =
    error instanceof ApiError ? error.message : "Something went wrong. Please try again.";
  return <div className="error-banner">{message}</div>;
}

export function EmptyState({ children }: { children: ReactNode }) {
  return (
    <div
      style={{
        padding: "40px 20px",
        textAlign: "center",
        color: "var(--color-ink-faint)",
        fontSize: 13.5,
      }}
    >
      {children}
    </div>
  );
}
