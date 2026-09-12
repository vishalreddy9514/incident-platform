import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Layout } from "../components/Layout";
import { LoadingState, ErrorState } from "../components/States";
import { useCategories } from "../hooks/useCategories";
import { useCreateIncident } from "../hooks/useIncidents";
import { ApiError } from "../api/ApiError";

export function CreateIncidentPage() {
  const navigate = useNavigate();
  const {
    data: categories,
    isPending: categoriesLoading,
    error: categoriesError,
  } = useCategories();
  const createIncident = useCreateIncident();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState<string>("");
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      const created = await createIncident.mutateAsync({
        title,
        description,
        categoryId: Number(categoryId),
      });
      navigate(`/incidents/${created.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    }
  }

  return (
    <Layout>
      <h1>Report an incident</h1>
      <p style={{ color: "var(--color-ink-muted)", marginBottom: 24 }}>
        Describe the problem clearly — the more detail, the faster it can be triaged.
      </p>

      {categoriesError && <ErrorState error={categoriesError} />}

      <form onSubmit={handleSubmit} className="card" style={{ maxWidth: 560, padding: 28 }}>
        {error && <div className="error-banner">{error}</div>}

        <div className="field">
          <label htmlFor="title">Title</label>
          <input
            id="title"
            required
            maxLength={200}
            placeholder="e.g. Laptop won't boot"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="description">Description</label>
          <textarea
            id="description"
            required
            rows={5}
            placeholder="What happened, when it started, and anything you've already tried."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="category">Category</label>
          {categoriesLoading ? (
            <LoadingState label="Loading categories…" />
          ) : (
            <select
              id="category"
              required
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
            >
              <option value="" disabled>
                Choose a category
              </option>
              {categories?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          )}
        </div>

        <button className="btn" type="submit" disabled={createIncident.isPending}>
          {createIncident.isPending ? "Creating…" : "Create incident"}
        </button>
      </form>
    </Layout>
  );
}
