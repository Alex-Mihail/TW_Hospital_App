import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

const API_BASE = "http://localhost:8080";

export default function AccountPage() {
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [roleLabel, setRoleLabel] = useState("");
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const [role, setRole] = useState(""); // "patient" / "doctor" / "admin"
  const [userId, setUserId] = useState(null);

  const [deleteBusy, setDeleteBusy] = useState(false);

  useEffect(() => {
    const roleRaw = localStorage.getItem("role");
    const userRaw = localStorage.getItem("user");

    if (!roleRaw || !userRaw) {
      navigate("/login", { replace: true, state: { from: "/account" } });
      return;
    }

    let parsedUser = null;
    try {
      parsedUser = JSON.parse(userRaw);
    } catch {
      parsedUser = null;
    }

    const id = parsedUser?.id ?? null;
    const r = (roleRaw || "").toLowerCase();

    if (!id || !r) {
      navigate("/login", { replace: true, state: { from: "/account" } });
      return;
    }

    setUserId(id);
    setRole(r);
    setRoleLabel(r.toUpperCase());

    const endpoint =
      r === "patient"
        ? `/api/patient/${id}`
        : r === "doctor"
          ? `/api/doctor/${id}`
          : r === "admin"
            ? `/api/admin/${id}`
            : "";

    if (!endpoint) {
      setErr(`Rol necunoscut în localStorage: "${roleRaw}"`);
      setLoading(false);
      return;
    }

    (async () => {
      setLoading(true);
      setErr("");
      setProfile(null);

      try {
        const res = await fetch(API_BASE + endpoint);

        if (res.status === 401) {
          localStorage.removeItem("role");
          localStorage.removeItem("user");
          localStorage.removeItem("token");
          navigate("/login", { replace: true, state: { from: "/account" } });
          return;
        }

        if (res.status === 404) {
          setErr("Profilul nu a fost găsit în backend (404).");
          setLoading(false);
          return;
        }

        if (!res.ok) {
          setErr(`Eroare backend: ${res.status}`);
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
    })();
  }, [navigate]);

  const displayName = useMemo(() => {
    if (!profile) return "";
    const full = `${profile.firstName || ""} ${profile.lastName || ""}`.trim();
    return full || profile.username || "Cont";
  }, [profile]);

  function handleLogout() {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("user");
    navigate("/", { replace: true });
  }

  function handleEditAccount() {
    navigate("/account/edit", { state: { role, userId, profile } });
  }

  function handleAppointments() {
    navigate("/appointments");
  }

  async function handleDeleteAccount() {
    if (!userId) return;
    if (role !== "patient" && role !== "admin") return;

    const ok = window.confirm(
      "Ești sigur că vrei să ștergi acest cont? Această acțiune este ireversibilă."
    );
    if (!ok) return;

    setDeleteBusy(true);
    setErr("");

    const deleteEndpoint =
      role === "patient"
        ? `${API_BASE}/api/patient/${userId}`
        : `${API_BASE}/api/admin/${userId}`;

    console.log("DELETE ->", deleteEndpoint);

    try {
      const res = await fetch(deleteEndpoint, { method: "DELETE" });

      if (res.status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("user");
        navigate("/login", { replace: true, state: { from: "/account" } });
        return;
      }

      if (res.status === 404) {
        setErr("Contul nu a fost găsit (404).");
        return;
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(`Eroare la ștergere: ${res.status} ${txt}`);
        return;
      }

      localStorage.removeItem("token");
      localStorage.removeItem("role");
      localStorage.removeItem("user");
      navigate("/", { replace: true });
    } catch (e) {
      console.log(e);
      setErr("Backend indisponibil / eroare rețea la ștergere.");
    } finally {
      setDeleteBusy(false);
    }
  }

  const showEdit = role === "patient" || role === "doctor" || role === "admin";
  const showDelete = role === "patient" || role === "admin";
  const showAppointments = role === "patient" || role === "doctor"; 

  return (
    <div style={styles.page}>
      {/* TOP BAR */}
      <div style={styles.topBar}>
        <button onClick={() => navigate("/")} style={styles.backBtn}>
          ← Acasă
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      {/* HERO */}
      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Detalii cont</h1>
        <p style={styles.heroText}>Gestionează-ți contul simplu și rapid.</p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <div style={{ minWidth: 240 }}>
              <h2 style={styles.name}>
                {loading ? "Se încarcă..." : displayName || "Cont"}
              </h2>
              <div style={styles.sub}>
                Rol: <b>{roleLabel || "-"}</b>
              </div>
            </div>

            {/* ACTION BUTTONS */}
            <div style={styles.actionsRow}>
              {role === "admin" && (
                <button onClick={() => navigate("/admin")} style={styles.appointmentsBtn}>
                  Administrare conturi
                </button>
              )}

              {showAppointments && (
                <button onClick={handleAppointments} style={styles.appointmentsBtn}>
                  Programări
                </button>
              )}

              {showEdit && (
                <button onClick={handleEditAccount} style={styles.editBtn}>
                  Editează cont
                </button>
              )}

              {showDelete && (
                <button
                  onClick={handleDeleteAccount}
                  disabled={deleteBusy}
                  style={{
                    ...styles.deleteBtn,
                    opacity: deleteBusy ? 0.7 : 1,
                    cursor: deleteBusy ? "not-allowed" : "pointer",
                  }}
                >
                  {deleteBusy ? "Se șterge..." : "Șterge cont"}
                </button>
              )}

              <button onClick={handleLogout} style={styles.logoutBtn}>
                Logout
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
              {"phone" in profile && <Info label="Telefon" value={profile.phone} />}
              {"dateOfBirth" in profile && (
                <Info label="Data nașterii" value={profile.dateOfBirth} />
              )}

              {"specialization" in profile && (
                <Info
                  label="Specializare"
                  value={
                    profile.specialization?.name ||
                    profile.specialization?.title ||
                    profile.specialization?.id ||
                    "-"
                  }
                />
              )}
              {"ratingAvg" in profile && <Info label="Rating" value={profile.ratingAvg} />}
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
    width: 820,
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

  // NEW
  appointmentsBtn: {
    backgroundColor: "white",
    color: "#0a4d80",
    border: "1px solid #0a4d80",
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

  logoutBtn: {
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
  grid: {
    marginTop: 16,
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: 12,
  },
  infoBox: {
    border: "1px solid #e6eef6",
    borderRadius: 12,
    padding: 12,
    background: "#fbfdff",
  },
  infoLabel: { color: "#0a4d80", fontWeight: 700, fontSize: 13 },
  infoValue: { marginTop: 6, color: "#334", fontSize: 15 },
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
  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
