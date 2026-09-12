import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { StatusBadge, PriorityBadge } from "./Badges";

describe("StatusBadge", () => {
  it("renders a human-readable label for each status", () => {
    render(<StatusBadge status="IN_PROGRESS" />);
    expect(screen.getByText("In progress")).toBeInTheDocument();
  });

  it("never renders raw enum values with underscores", () => {
    render(<StatusBadge status="ESCALATED" />);
    expect(screen.queryByText(/_/)).not.toBeInTheDocument();
  });
});

describe("PriorityBadge", () => {
  it("renders the priority label", () => {
    render(<PriorityBadge priority="CRITICAL" />);
    expect(screen.getByText("Critical")).toBeInTheDocument();
  });
});
