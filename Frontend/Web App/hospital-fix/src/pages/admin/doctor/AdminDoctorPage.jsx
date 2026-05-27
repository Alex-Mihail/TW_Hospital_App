import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

const API_BASE = "http://localhost:8080";

export default function AdminDoctorsPage() {
  const navigate = useNavigate();

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [q, setQ] = useState("");

  async function load() {
    setLoading(true);
    setErr("");
    try {
      const res = await fetch(`${API_BASE}/api/admin/doctors`);
      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare backend: ${res.status}`);
        setItems([]);
        return;
      }
      const data = await res.json();
      setItems(Array.isArray(data) ? data : []);
    } catch {
      setErr("Backend indisponibil / CORS / eroare rețea.");
      setItems([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const filtered = useMemo(() => {
    const s = q.trim().toLowerCase();
    if (!s) return items;
    return items.filter((d) =>
      `${d.firstName || ""} ${d.lastName || ""} ${d.username || ""}`
        .toLowerCase()
        .includes(s)
    );
  }, [items, q]);

  return (
    <div style={styles.page}>
      <div style={styles.topBar}>
        <button onClick={() => navigate("/admin")} style={styles.backBtn}>
          ← Admin
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Gestionează doctori</h1>
        <p style={styles.heroText}>Listă doctori + creare / editare / ștergere.</p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <div>
              <h2 style={styles.cardTitle}>Listă doctori</h2>
              <div style={styles.sub}>
                {loading ? "Se încarcă..." : `${filtered.length} doctori`}
              </div>
            </div>

            <div style={styles.actionsRow}>
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Caută după nume / username..."
                style={styles.search}
              />
              <button onClick={load} style={styles.secondaryBtn} disabled={loading}>
                Reîncarcă
              </button>
              <button
                onClick={() => navigate("/admin/doctors/register")}
                style={styles.primaryBtn}
              >
                Înregistrează doctor
              </button>
            </div>
          </div>

          {err && <div style={styles.error}>{err}</div>}

          {!loading && !err && filtered.length === 0 && (
            <div style={styles.infoNote}>Nu există doctori.</div>
          )}

          {!loading && filtered.length > 0 && (
            <div style={styles.tableWrap}>
              <table style={styles.table}>
                <thead>
                  <tr>
                    <th style={styles.th}>ID</th>
                    <th style={styles.th}>Nume</th>
                    <th style={styles.th}>Username</th>
                    <th style={styles.th}>Email</th>
                    <th style={styles.th}>Specializare</th>
                    <th style={styles.th}>Acțiuni</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((d) => (
                    <tr key={d.id}>
                      <td style={styles.tdMono}>{d.id}</td>
                      <td style={styles.td}>
                        {`${d.firstName || ""} ${d.lastName || ""}`.trim() || "-"}
                      </td>
                      <td style={styles.td}>{d.username || "-"}</td>
                      <td style={styles.td}>{d.email || "-"}</td>
                      <td style={styles.td}>{d.specialization?.name || "-"}</td>
                      <td style={styles.td}>
                        <button
                          style={styles.linkBtn}
                          onClick={() => navigate(`/admin/doctors/${d.id}`)}
                        >
                          Detalii
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
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
  cardWrapper: { display: "flex", justifyContent: "center", marginTop: -40, padding: "0 20px" },
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
  actionsRow: { display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center" },
  cardTitle: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 24 },
  sub: { marginTop: 6, color: "#556", fontSize: 14 },

  search: {
    height: 42,
    padding: "0 12px",
    borderRadius: 12,
    border: "1px solid #d7e3ee",
    background: "#fbfdff",
    minWidth: 260,
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
  secondaryBtn: {
    backgroundColor: "white",
    color: "#0a4d80",
    border: "1px solid #0a4d80",
    padding: "10px 14px",
    borderRadius: 12,
    fontWeight: 700,
    cursor: "pointer",
    height: 42,
  },

  linkBtn: {
    backgroundColor: "rgba(10, 77, 128, 0.1)",
    color: "#0a4d80",
    border: "1px solid rgba(10, 77, 128, 0.18)",
    padding: "8px 10px",
    borderRadius: 10,
    fontWeight: 700,
    cursor: "pointer",
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
  tableWrap: { marginTop: 16, overflowX: "auto", border: "1px solid #e6eef6", borderRadius: 14 },
  table: { width: "100%", borderCollapse: "collapse", minWidth: 980 },
  th: {
    textAlign: "left",
    padding: 12,
    fontSize: 13,
    color: "#0a4d80",
    background: "#f3f8fd",
    borderBottom: "1px solid #e6eef6",
    position: "sticky",
    top: 0,
    zIndex: 1,
  },
  td: { padding: 12, borderBottom: "1px solid #eef4fb", verticalAlign: "top", fontSize: 14, color: "#223" },
  tdMono: {
    padding: 12,
    borderBottom: "1px solid #eef4fb",
    verticalAlign: "top",
    fontSize: 13,
    fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
    color: "#223",
  },
  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
