import { useState } from "react";
import { Layout } from "../components/Layout";
import { LoadingState, ErrorState } from "../components/States";
import { useAuth } from "../auth/AuthContext";
import { useUsers, useUpdateUserRole } from "../hooks/useUsers";
import { useCategories, useCreateCategory, useUpdateCategory } from "../hooks/useCategories";
import { useCreateTeam, useTeams } from "../hooks/useTeams";
import type { Role } from "../api/types";
import { ApiError } from "../api/ApiError";

type Tab = "users" | "categories" | "teams";

export function AdminPage() {
  const [tab, setTab] = useState<Tab>("users");

  return (
    <Layout>
      <h1>Admin</h1>
      <p style={{ color: "var(--color-ink-muted)", marginBottom: 24 }}>
        Manage user roles, categories, and teams.
      </p>

      <div
        style={{
          display: "flex",
          gap: 4,
          marginBottom: 24,
          borderBottom: "1px solid var(--color-border)",
        }}
      >
        {(["users", "categories", "teams"] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            style={{
              padding: "10px 16px",
              background: "none",
              border: "none",
              borderBottom: tab === t ? "2px solid var(--color-accent)" : "2px solid transparent",
              color: tab === t ? "var(--color-accent)" : "var(--color-ink-muted)",
              fontWeight: 500,
              fontSize: 13.5,
              textTransform: "capitalize",
            }}
          >
            {t}
          </button>
        ))}
      </div>

      {tab === "users" && <UsersTab />}
      {tab === "categories" && <CategoriesTab />}
      {tab === "teams" && <TeamsTab />}
    </Layout>
  );
}

function UsersTab() {
  const { user: currentUser } = useAuth();
  const { data, isPending, error } = useUsers();
  const updateRole = useUpdateUserRole();
  const [roleError, setRoleError] = useState<string | null>(null);

  async function handleRoleChange(userId: number, role: Role) {
    setRoleError(null);
    try {
      await updateRole.mutateAsync({ id: userId, role });
    } catch (err) {
      setRoleError(err instanceof ApiError ? err.message : "Couldn't update role.");
    }
  }

  if (isPending) return <LoadingState />;
  if (error) return <ErrorState error={error} />;

  return (
    <div className="card">
      {roleError && (
        <div className="error-banner" style={{ margin: 16 }}>
          {roleError}
        </div>
      )}
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {data?.content.map((u) => (
            <tr key={u.id}>
              <td>{u.displayName}</td>
              <td>{u.email}</td>
              <td>
                <select
                  value={u.role}
                  disabled={u.id === currentUser?.id || updateRole.isPending}
                  onChange={(e) => handleRoleChange(u.id, e.target.value as Role)}
                  style={{
                    padding: "4px 8px",
                    borderRadius: 6,
                    border: "1px solid var(--color-border-strong)",
                  }}
                >
                  <option value="USER">USER</option>
                  <option value="ENGINEER">ENGINEER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
                {u.id === currentUser?.id && (
                  <span style={{ fontSize: 11, color: "var(--color-ink-faint)", marginLeft: 8 }}>
                    (you)
                  </span>
                )}
              </td>
              <td>{u.isActive ? "Active" : "Deactivated"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CategoriesTab() {
  const { data, isPending, error } = useCategories();
  const createCategory = useCreateCategory();
  const updateCategory = useUpdateCategory();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  async function handleCreate() {
    setFormError(null);
    try {
      await createCategory.mutateAsync({ name, description });
      setName("");
      setDescription("");
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't create category.");
    }
  }

  async function toggleActive(
    id: number,
    currentName: string,
    currentDescription: string,
    isActive: boolean,
  ) {
    await updateCategory.mutateAsync({
      id,
      name: currentName,
      description: currentDescription,
      isActive: !isActive,
    });
  }

  return (
    <div>
      <div className="card" style={{ padding: 20, marginBottom: 20 }}>
        <h3>Add a category</h3>
        {formError && <div className="error-banner">{formError}</div>}
        <div style={{ display: "flex", gap: 10, alignItems: "flex-end" }}>
          <div className="field" style={{ flex: 1, marginBottom: 0 }}>
            <label htmlFor="cat-name">Name</label>
            <input id="cat-name" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="field" style={{ flex: 2, marginBottom: 0 }}>
            <label htmlFor="cat-desc">Description</label>
            <input
              id="cat-desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <button
            className="btn"
            disabled={!name || createCategory.isPending}
            onClick={handleCreate}
          >
            Add
          </button>
        </div>
      </div>

      {isPending && <LoadingState />}
      {error && <ErrorState error={error} />}

      {data && (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {data.map((c) => (
                <tr key={c.id}>
                  <td>{c.name}</td>
                  <td>{c.description}</td>
                  <td>{c.isActive ? "Active" : "Deactivated"}</td>
                  <td>
                    <button
                      className="btn-secondary btn"
                      style={{ padding: "4px 10px", fontSize: 12.5 }}
                      onClick={() => toggleActive(c.id, c.name, c.description ?? "", c.isActive)}
                    >
                      {c.isActive ? "Deactivate" : "Reactivate"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function TeamsTab() {
  const { data, isPending, error } = useTeams();
  const createTeam = useCreateTeam();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  async function handleCreate() {
    setFormError(null);
    try {
      await createTeam.mutateAsync({ name, description });
      setName("");
      setDescription("");
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't create team.");
    }
  }

  return (
    <div>
      <div className="card" style={{ padding: 20, marginBottom: 20 }}>
        <h3>Add a team</h3>
        {formError && <div className="error-banner">{formError}</div>}
        <div style={{ display: "flex", gap: 10, alignItems: "flex-end" }}>
          <div className="field" style={{ flex: 1, marginBottom: 0 }}>
            <label htmlFor="team-name">Name</label>
            <input id="team-name" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="field" style={{ flex: 2, marginBottom: 0 }}>
            <label htmlFor="team-desc">Description</label>
            <input
              id="team-desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <button className="btn" disabled={!name || createTeam.isPending} onClick={handleCreate}>
            Add
          </button>
        </div>
      </div>

      {isPending && <LoadingState />}
      {error && <ErrorState error={error} />}

      {data && (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              {data.map((t) => (
                <tr key={t.id}>
                  <td>{t.name}</td>
                  <td>{t.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
