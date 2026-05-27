import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

export default function AdminDashboardPage() {
  const navigate = useNavigate();

  const [role, setRole] = useState("");
  const [userId, setUserId] = useState(null);

  const roleLabel = useMemo(() => (role ? role.toUpperCase() : "-"), [role]);

  useEffect(() => {
    const roleRaw = localStorage.getItem("role");
    const userRaw = localStorage.getItem("user");

    if (!roleRaw || !userRaw) {
      navigate("/login", { replace: true, state: { from: "/admin" } });
      return;
    }

    let parsed = null;
    try {
      parsed = JSON.parse(userRaw);
    } catch {
      parsed = null;
    }

    const r = (roleRaw || "").toLowerCase();
    const id = parsed?.id ?? null;

    if (!r || !id) {
      navigate("/login", { replace: true, state: { from: "/admin" } });
      return;
    }

    // guard admin
    if (r !== "admin") {
      navigate("/account", { replace: true });
      return;
    }

    setRole(r);
    setUserId(id);
  }, [navigate]);

  function go(to) {
    navigate(to);
  }

  return (
    <div style={styles.page}>
      {/* TOP BAR */}
      <div style={styles.topBar}>
        <button onClick={() => navigate("/account")} style={styles.backBtn}>
          ← Înapoi la cont
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      {/* HERO */}
      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Administrare</h1>
        <p style={styles.heroText}>
          Gestionează pacienți, doctori, admini, programări și specializări.
        </p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <div style={{ minWidth: 260 }}>
              <h2 style={styles.name}>Panou administrare</h2>
              <div style={styles.sub}>
                Rol: <b>{roleLabel}</b>
                {userId ? <> • ID: <b>{userId}</b></> : null}
              </div>
            </div>
          </div>

          <div style={styles.grid}>
            <button onClick={() => go("/admin/patients")} style={styles.tileBtn}>
              Gestionează pacienți
              <div style={styles.tileHint}>Listă, detalii, editare, ștergere, programări pacient</div>
            </button>

            <button onClick={() => go("/admin/doctors")} style={styles.tileBtn}>
              Gestionează doctori
              <div style={styles.tileHint}>Listă, creare cont, editare, ștergere, programări doctor</div>
            </button>

            <button onClick={() => go("/admin/admins")} style={styles.tileBtn}>
              Gestionează admini
              <div style={styles.tileHint}>Listă, creare admin, editare, ștergere</div>
            </button>

            <button onClick={() => go("/admin/appointments")} style={styles.tileBtn}>
              Gestionează programări
              <div style={styles.tileHint}>Listă completă, schimbare status, ștergere</div>
            </button>

            <button onClick={() => go("/admin/specializations")} style={styles.tileBtn}>
              Gestionează specializări
              <div style={styles.tileHint}>Listă, adăugare, editare, ștergere, doctori pe specializare</div>
            </button>
          </div>
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
  name: {
    margin: 0,
    color: "#0a4d80",
    fontWeight: 600,
    fontSize: 28,
    lineHeight: 1.2,
  },
  sub: { marginTop: 6, color: "#556", fontSize: 14 },

  grid: {
    marginTop: 16,
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: 12,
  },

  tileBtn: {
    textAlign: "left",
    backgroundColor: "white",
    color: "#0a4d80",
    border: "1px solid #0a4d80",
    padding: "16px 16px",
    borderRadius: 16,
    fontWeight: 600,
    fontSize: 18,
    cursor: "pointer",
    minHeight: 88,
  },
  tileHint: {
    marginTop: 8,
    color: "#556",
    fontWeight: 400,
    fontSize: 13,
    lineHeight: 1.4,
  },

  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
