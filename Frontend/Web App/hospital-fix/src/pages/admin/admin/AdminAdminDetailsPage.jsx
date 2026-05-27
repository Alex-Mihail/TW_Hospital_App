import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

const API_BASE = "http://localhost:8080";

function getStoredSession() {
  const roleRaw = localStorage.getItem("role");
  const userRaw = localStorage.getItem("user");
  if (!roleRaw || !userRaw) return { role: "", userId: null };

  let user = null;
  try {
    user = JSON.parse(userRaw);
  } catch {
    user = null;
  }

  return { role: (roleRaw || "").toLowerCase(), userId: user?.id ?? null };
}

function authHeaders(json = false) {
  const token = localStorage.getItem("token");
  const h = {};
  if (json) h["Content-Type"] = "application/json";
  if (token) h["Authorization"] = `Bearer ${token}`;
  return h;
}

export default function AdminAdminDetailsPage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const stored = getStoredSession();
  const role = stored.role;

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [deleteBusy, setDeleteBusy] = useState(false);

  async function load() {
    setLoading(true);
    setErr("");
    setProfile(null);

    try {
      const res = await fetch(`${API_BASE}/api/admin/${id}`, { headers: authHeaders() });

      if (res.status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("user");
        navigate("/login", { replace: true, state: { from: `/admin/admins/${id}` } });
        return;
      }

      if (res.status === 404) {
        setErr("Adminul nu a fost găsit (404).");
        setLoading(false);
        return;
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare backend: ${res.status}`);
        setLoading(false);
        return;
      }

      const data = await res.json();
      setProfile(data);
    } catch {
      setErr("Backend indisponibil / CORS / eroare rețea.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (role !== "admin") {
      navigate("/login", { replace: true, state: { from: `/admin/admins/${id}` } });
      return;
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [role, navigate, id]);

  const displayName = useMemo(() => {
    if (!profile) return "";
    const full = `${profile.firstName || ""} ${profile.lastName || ""}`.trim();
    return full || profile.username || `Admin #${id}`;
  }, [profile, id]);

  async function handleDelete() {
    const ok = window.confirm("Ești sigur că vrei să ștergi acest admin?");
    if (!ok) return;

    setDeleteBusy(true);
    setErr("");

    try {
      const res = await fetch(`${API_BASE}/api/admin/${id}`, {
        method: "DELETE",
        headers: authHeaders(),
      });

      if (res.status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("user");
        navigate("/login", { replace: true, state: { from: `/admin/admins/${id}` } });
        return;
      }

      if (res.status === 404) {
        setErr("Adminul nu a fost găsit (404).");
        return;
      }

      if (!(res.status === 204 || res.ok)) {
        const txt = await res.text().catch(() => "");
        setErr(`Eroare la ștergere: ${res.status} ${txt}`);
        return;
      }

      navigate("/admin/admins", { replace: true });
    } catch {
      setErr("Backend indisponibil / eroare rețea la ștergere.");
    } finally {
      setDeleteBusy(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.topBar}>
        <button onClick={() => navigate("/admin/admins")} style={styles.backBtn}>
          ← Admini
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Detalii admin</h1>
        <p style={styles.heroText}>Vizualizează și gestionează contul admin.</p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <div style={{ minWidth: 240 }}>
              <h2 style={styles.name}>{loading ? "Se încarcă..." : displayName}</h2>
              <div style={styles.sub}>
                Tip: <b>ADMIN</b>
              </div>
            </div>

            <div style={styles.actionsRow}>
              <button onClick={load} style={styles.refreshBtn} disabled={loading}>
                Reîncarcă
              </button>

              <button
                onClick={() => navigate(`/admin/admins/${id}/edit`, { state: { profile } })}
                disabled={!profile}
                style={styles.editBtn}
              >
                Editează
              </button>

              <button
                onClick={handleDelete}
                disabled={deleteBusy}
                style={{
                  ...styles.deleteBtn,
                  opacity: deleteBusy ? 0.7 : 1,
                  cursor: deleteBusy ? "not-allowed" : "pointer",
                }}
              >
                {deleteBusy ? "Se șterge..." : "Șterge admin"}
              </button>
            </div>
          </div>

          {err && <div style={styles.error}>{err}</div>}

          {!loading && !err && !profile && (
            <div style={styles.infoNote}>Nu există date de profil încărcate.</div>
          )}

          {!loading && profile && (
            <div style={styles.grid}>
              <Info label="ID" value={profile.id} />
              <Info label="Username" value={profile.username} />
              <Info label="Prenume" value={profile.firstName} />
              <Info label="Nume" value={profile.lastName} />
              {"email" in profile && <Info label="Email" value={profile.email} />}
            </div>
          )}
        </div>
      </div>

      <footer style={styles.footer}>© 2026 Spitalul Central TW</footer>
    </div>
  );
}

function Info({ label, value }) {
  return (
    <div style={styles.infoBox}>
      <div style={styles.infoLabel}>{label}</div>
      <div style={styles.infoValue}>{value ?? "-"}</div>
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
  heroTitle: { fontSize: 52, fontWeight: 600, margin: 0, lineHeight: 1.15 },
  heroText: { fontSize: 20, fontWeight: 400, marginTop: 10, lineHeight: 1.5 },

  cardWrapper: { display: "flex", justifyContent: "center", marginTop: -40, padding: "0 20px" },
  card: {
    background: "white",
    padding: 28,
    borderRadius: 18,
    width: 1100,
    maxWidth: "100%",
    boxShadow: "0 10px 25px rgba(0,0,0,0.15)",
  },

  headerRow: { display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "center" },
  actionsRow: { display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap", justifyContent: "flex-end" },

  name: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 28, lineHeight: 1.2 },
  sub: { marginTop: 6, color: "#556", fontSize: 14 },

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
  editBtn: {
    backgroundColor: "white",
    color: "#0a4d80",
    border: "1px solid #0a4d80",
    padding: "10px 14px",
    borderRadius: 12,
    fontWeight: 700,
    cursor: "pointer",
    height: 42,
  },
  deleteBtn: {
    backgroundColor: "#b42318",
    color: "white",
    border: "none",
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
  infoNote: {
    marginTop: 16,
    padding: 12,
    borderRadius: 12,
    background: "#f5f9fc",
    border: "1px solid #e6eef6",
    color: "#445",
    fontSize: 14,
    lineHeight: 1.45,
  },

  grid: { marginTop: 16, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 },
  infoBox: { border: "1px solid #e6eef6", borderRadius: 12, padding: 12, background: "#fbfdff" },
  infoLabel: { color: "#0a4d80", fontWeight: 700, fontSize: 13 },
  infoValue: { marginTop: 6, color: "#334", fontSize: 15 },

  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
