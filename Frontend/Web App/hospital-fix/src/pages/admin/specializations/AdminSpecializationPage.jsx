import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

const API_BASE = "http://localhost:8080";

function authHeaders(json = false) {
  const token = localStorage.getItem("token");
  const h = {};
  if (json) h["Content-Type"] = "application/json";
  if (token) h["Authorization"] = `Bearer ${token}`;
  return h;
}

export default function AdminSpecializationsPage() {
  const navigate = useNavigate();

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [err, setErr] = useState("");

  // CREATE form
  const [newName, setNewName] = useState("");
  const [newDescription, setNewDescription] = useState("");

  async function load() {
    setLoading(true);
    setErr("");
    try {
      const res = await fetch(`${API_BASE}/api/admin/specializations`, {
        headers: authHeaders(),
      });

      if (res.status === 401) {
        localStorage.clear();
        navigate("/login", { replace: true, state: { from: "/admin/specializations" } });
        return;
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare backend (${res.status})`);
        return;
      }

      const data = await res.json();
      setItems(Array.isArray(data) ? data : []);
    } catch {
      setErr("Backend indisponibil.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const role = (localStorage.getItem("role") || "").toLowerCase();
    if (role !== "admin") {
      navigate("/login", { replace: true, state: { from: "/admin/specializations" } });
      return;
    }
    load();
    // eslint-disable-next-line
  }, []);

  async function createSpecialization() {
    if (!newName.trim()) {
      setErr("Numele specializării este obligatoriu.");
      return;
    }

    setErr("");
    try {
      const res = await fetch(`${API_BASE}/api/admin/specializations`, {
        method: "POST",
        headers: authHeaders(true),
        body: JSON.stringify({
          name: newName.trim(),
          description: newDescription.trim() || null, // description
        }),
      });

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || "Eroare la creare.");
        return;
      }

      setNewName("");
      setNewDescription("");
      load();
    } catch {
      setErr("Backend indisponibil.");
    }
  }

  async function updateSpecialization(id, payload) {
    setBusyId(id);
    setErr("");

    try {
      const res = await fetch(`${API_BASE}/api/admin/specializations/${id}`, {
        method: "PUT",
        headers: authHeaders(true),
        body: JSON.stringify(payload), // {name, description}
      });

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || "Eroare update.");
        return;
      }

      await load();
    } catch {
      setErr("Backend indisponibil.");
    } finally {
      setBusyId(null);
    }
  }

  async function deleteSpecialization(id) {
    const ok = window.confirm("Sigur vrei să ștergi această specializare?");
    if (!ok) return;

    setBusyId(id);
    setErr("");

    try {
      const res = await fetch(`${API_BASE}/api/admin/specializations/${id}`, {
        method: "DELETE",
        headers: authHeaders(),
      });

      if (!(res.status === 204 || res.ok)) {
        const txt = await res.text().catch(() => "");
        setErr(txt || "Eroare ștergere.");
        return;
      }

      setItems((xs) => xs.filter((x) => x.id !== id));
    } catch {
      setErr("Backend indisponibil.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.topBar}>
        <button onClick={() => navigate("/admin")} style={styles.backBtn}>
          ← Admin
        </button>
        <span>Administrare specializări</span>
      </div>

      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Specializări medicale</h1>
        <p style={styles.heroText}>Creează, editează sau șterge specializări.</p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <h2 style={styles.cardTitle}>
              {loading ? "Se încarcă..." : `${items.length} specializări`}
            </h2>
            <button onClick={load} style={styles.refreshBtn} disabled={loading}>
              Reîncarcă
            </button>
          </div>

          {err && <div style={styles.error}>{err}</div>}

          {/* CREATE */}
          <div style={styles.createBox}>
            <div style={styles.createRow}>
              <input
                placeholder="Nume specializare"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                style={styles.input}
              />
              <button onClick={createSpecialization} style={styles.primaryBtn}>
                Adaugă
              </button>
            </div>

            {/* DESCRIPTION BAR */}
            <textarea
              placeholder="Descriere (opțional) — ex: investigații, servicii, etc."
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              rows={3}
              style={styles.textarea}
            />
          </div>

          {!loading && items.length === 0 && (
            <div style={styles.infoNote}>Nu există specializări.</div>
          )}

          {!loading && items.length > 0 && (
            <table style={styles.table}>
              <thead>
                <tr>
                  <th style={{ ...styles.th, width: 80 }}>ID</th>
                  <th style={styles.th}>Nume</th>
                  <th style={styles.th}>Descriere</th>
                  <th style={{ ...styles.th, width: 150 }}>Acțiuni</th>
                </tr>
              </thead>
              <tbody>
                {items.map((s) => (
                  <tr key={s.id}>
                    <td style={styles.tdMono}>{s.id}</td>

                    {/* NAME editable */}
                    <td style={styles.td}>
                      <input
                        defaultValue={s.name || ""}
                        disabled={busyId === s.id}
                        onBlur={(e) => {
                          const name = e.target.value.trim();
                          if (!name) return;
                          if (name !== (s.name || "")) {
                            updateSpecialization(s.id, {
                              name,
                              description: s.description ?? null,
                            });
                          }
                        }}
                        style={styles.inlineInput}
                      />
                    </td>

                    {/* DESCRIPTION editable */}
                    <td style={styles.td}>
                      <textarea
                        defaultValue={s.description || ""}
                        disabled={busyId === s.id}
                        rows={2}
                        onBlur={(e) => {
                          const description = e.target.value.trim() || null;
                          const prev = (s.description || "").trim() || null;

                          if (description !== prev) {
                            updateSpecialization(s.id, {
                              name: s.name,
                              description,
                            });
                          }
                        }}
                        style={styles.inlineTextarea}
                      />
                    </td>

                    <td style={styles.td}>
                      <button
                        onClick={() => deleteSpecialization(s.id)}
                        disabled={busyId === s.id}
                        style={{
                          ...styles.deleteBtn,
                          opacity: busyId === s.id ? 0.7 : 1,
                          cursor: busyId === s.id ? "not-allowed" : "pointer",
                        }}
                      >
                        Șterge
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <footer style={styles.footer}>© 2026 Spitalul Central TW</footer>
    </div>
  );
}

const styles = {
  page: { fontFamily: "Arial", background: "#f5f9fc", minHeight: "100vh" },
  topBar: {
    background: "#063a62",
    color: "white",
    padding: 10,
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
  },
  backBtn: {
    background: "transparent",
    color: "white",
    border: "1px solid rgba(255,255,255,0.6)",
    borderRadius: 8,
    padding: "6px 12px",
    cursor: "pointer",
  },
  hero: {
    background:
      "linear-gradient(rgba(6,58,98,0.7), rgba(6,58,98,0.7)), url('/images/HomePage.jpg')",
    color: "white",
    textAlign: "center",
    padding: "60px 20px",
    backgroundSize: "cover",
    backgroundPosition: "center -380px",
  },
  heroTitle: { fontSize: 48, fontWeight: 600, margin: 0, lineHeight: 1.15 },
  heroText: { fontSize: 18, fontWeight: 400, marginTop: 10, lineHeight: 1.5 },

  cardWrapper: {
    display: "flex",
    justifyContent: "center",
    marginTop: -40,
    padding: "0 20px",
  },
  card: {
    background: "white",
    padding: 24,
    borderRadius: 18,
    width: 1100,
    maxWidth: "100%",
    boxShadow: "0 10px 25px rgba(0,0,0,0.15)",
  },

  headerRow: {
    display: "flex",
    justifyContent: "space-between",
    gap: 12,
    flexWrap: "wrap",
    alignItems: "center",
  },
  cardTitle: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 24 },

  refreshBtn: {
    backgroundColor: "rgba(10, 77, 128, 0.1)",
    color: "#0a4d80",
    border: "1px solid rgba(10, 77, 128, 0.18)",
    padding: "10px 14px",
    borderRadius: 12,
    fontWeight: 700,
    cursor: "pointer",
    height: 42,
  },

  error: {
    marginTop: 14,
    background: "#ffe8e8",
    padding: 10,
    borderRadius: 12,
    color: "#7a1b1b",
    border: "1px solid #ffb3b3",
  },

  createBox: {
    marginTop: 16,
    border: "1px solid #e6eef6",
    borderRadius: 14,
    padding: 12,
    background: "#fbfdff",
  },
  createRow: { display: "flex", gap: 10, alignItems: "center" },

  input: {
    flex: 1,
    padding: 12,
    borderRadius: 12,
    border: "1px solid #d7e3ee",
    backgroundColor: "#fbfdff",
    fontSize: 15,
    outline: "none",
    boxSizing: "border-box",
  },

  textarea: {
    marginTop: 10,
    width: "100%",
    padding: 12,
    borderRadius: 12,
    border: "1px solid #d7e3ee",
    backgroundColor: "#fbfdff",
    fontSize: 14,
    outline: "none",
    boxSizing: "border-box",
    resize: "vertical",
    lineHeight: 1.4,
  },

  primaryBtn: {
    backgroundColor: "#0a4d80",
    color: "white",
    border: "none",
    padding: "10px 14px",
    borderRadius: 12,
    fontWeight: 700,
    cursor: "pointer",
    height: 42,
  },

  table: { width: "100%", marginTop: 16, borderCollapse: "collapse" },
  th: {
    textAlign: "left",
    padding: 10,
    fontSize: 13,
    color: "#0a4d80",
    background: "#f3f8fd",
    borderBottom: "1px solid #e6eef6",
  },
  td: { padding: 10, borderBottom: "1px solid #eef4fb", verticalAlign: "top" },
  tdMono: {
    padding: 10,
    borderBottom: "1px solid #eef4fb",
    fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
    fontSize: 13,
  },

  inlineInput: {
    width: "100%",
    padding: 10,
    borderRadius: 10,
    border: "1px solid #d7e3ee",
    backgroundColor: "#fbfdff",
    fontSize: 14,
    outline: "none",
    boxSizing: "border-box",
  },

  inlineTextarea: {
    width: "100%",
    padding: 10,
    borderRadius: 10,
    border: "1px solid #d7e3ee",
    backgroundColor: "#fbfdff",
    fontSize: 14,
    outline: "none",
    boxSizing: "border-box",
    resize: "vertical",
    lineHeight: 1.4,
  },

  deleteBtn: {
    backgroundColor: "#b42318",
    color: "white",
    border: "none",
    padding: "8px 12px",
    borderRadius: 10,
    fontWeight: 700,
  },

  infoNote: {
    marginTop: 16,
    padding: 12,
    borderRadius: 12,
    background: "#f5f9fc",
    border: "1px solid #e6eef6",
    color: "#445",
    fontSize: 14,
  },

  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
