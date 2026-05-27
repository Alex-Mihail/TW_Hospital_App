import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

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

function authHeaders() {
  const token = localStorage.getItem("token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export default function AdminPatientsPage() {
  const navigate = useNavigate();

  const stored = getStoredSession();
  const role = stored.role;

  const [patients, setPatients] = useState([]);
  const [search, setSearch] = useState("");

  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    // guard admin
    if (!role || role !== "admin") {
      navigate("/login", { replace: true, state: { from: "/admin/patients" } });
      return;
    }

    (async () => {
      setLoading(true);
      setErr("");

      try {
        const res = await fetch(`${API_BASE}/api/admin/patients`, {
          headers: authHeaders(),
        });

        if (res.status === 401) {
          localStorage.removeItem("token");
          localStorage.removeItem("role");
          localStorage.removeItem("user");
          navigate("/login", { replace: true, state: { from: "/admin/patients" } });
          return;
        }

        if (!res.ok) {
          const txt = await res.text().catch(() => "");
          setErr(txt || `Eroare backend: ${res.status}`);
          setPatients([]);
          return;
        }

        const data = await res.json();
        setPatients(Array.isArray(data) ? data : []);
      } catch (e) {
        console.log(e);
        setErr("Backend indisponibil / CORS / eroare rețea.");
        setPatients([]);
      } finally {
        setLoading(false);
      }
    })();
  }, [navigate, role]);

  const filtered = useMemo(() => {
    const list = Array.isArray(patients) ? patients : [];
    const q = search.trim().toLowerCase();
    if (!q) return list;

    return list.filter((p) =>
      `${p.id} ${p.username || ""} ${p.firstName || ""} ${p.lastName || ""} ${p.email || ""}`
        .toLowerCase()
        .includes(q)
    );
  }, [patients, search]);

  return (
    <div style={styles.page}>
      {/* TOP BAR */}
      <div style={styles.topBar}>
        <button onClick={() => navigate("/admin")} style={styles.backBtn}>
          ← Admin
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      {/* HERO */}
      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Gestionare pacienți</h1>
        <p style={styles.heroText}>Lista completă cu pacienți din baza de date.</p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <div style={{ minWidth: 260 }}>
              <h2 style={styles.name}>Pacienți</h2>
              <div style={styles.sub}>
                Total: <b>{loading ? "-" : filtered.length}</b>
              </div>
            </div>

            <div style={styles.actionsRow}>
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Caută după nume, username, email..."
                style={styles.searchInput}
              />

              <button
                onClick={() => navigate("/admin/patients/register")}
                style={styles.primaryBtn}
              >
                Înregistrează pacient
              </button>
            </div>
          </div>

          {err && <div style={styles.error}>{err}</div>}

          {loading && !err && <div style={styles.infoNote}>Se încarcă...</div>}

          {!loading && !err && (
            <div style={styles.list}>
              {filtered.map((p) => (
                <div
                  key={p.id}
                  style={styles.row}
                  onClick={() => navigate(`/admin/patients/${p.id}`)}
                >
                  <div style={styles.rowTop}>
                    <div style={styles.rowTitle}>
                      #{p.id} — {(p.firstName || "")} {(p.lastName || "")}
                    </div>
                    <div style={styles.rowChip}>{p.username || "username"}</div>
                  </div>

                  <div style={styles.rowSub}>
                    {p.email ? `Email: ${p.email}` : "Fără email"}{" "}
                    {p.phone ? `• Tel: ${p.phone}` : ""}
                  </div>
                </div>
              ))}

              {filtered.length === 0 && (
                <div style={styles.infoNote}>
                  Nu există pacienți (sau nu se potrivesc filtrului).
                </div>
              )}
            </div>
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
  heroTitle: { fontSize: 52, fontWeight: 600, margin: 0, lineHeight: 1.15 },
  heroText: { fontSize: 20, fontWeight: 400, marginTop: 10, lineHeight: 1.5 },

  cardWrapper: {
    display: "flex",
    justifyContent: "center",
    marginTop: -40,
    padding: "0 20px",
  },
  card: {
    background: "white",
    padding: 28,
    borderRadius: 18,
    width: 920,
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
  actionsRow: {
    display: "flex",
    gap: 10,
    alignItems: "center",
    flexWrap: "wrap",
    justifyContent: "flex-end",
  },

  name: {
    margin: 0,
    color: "#0a4d80",
    fontWeight: 600,
    fontSize: 28,
    lineHeight: 1.2,
  },
  sub: { marginTop: 6, color: "#556", fontSize: 14 },

  searchInput: {
    padding: 10,
    borderRadius: 12,
    border: "1px solid #d7e3ee",
    backgroundColor: "#fbfdff",
    fontSize: 14,
    width: 320,
    outline: "none",
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

  list: {
    marginTop: 16,
    display: "grid",
    gap: 12,
  },

  row: {
    border: "1px solid #e6eef6",
    borderRadius: 14,
    padding: 14,
    background: "#fbfdff",
    cursor: "pointer",
    transition: "transform 0.05s ease-in-out",
  },

  rowTop: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    gap: 12,
    flexWrap: "wrap",
  },

  rowTitle: {
    fontWeight: 600,
    fontSize: 18,
    color: "#0a4d80",
  },

  rowChip: {
    padding: "6px 10px",
    borderRadius: 999,
    border: "1px solid #0a4d80",
    color: "#0a4d80",
    fontWeight: 600,
    fontSize: 14,
    background: "white",
  },

  rowSub: {
    marginTop: 8,
    color: "#556",
    fontSize: 13,
    lineHeight: 1.4,
  },

  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
