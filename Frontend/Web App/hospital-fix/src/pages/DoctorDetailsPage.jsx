import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";

import "./Consultation.css"; 
import BookingModal from "../components/BookingModal"; 

const API_BASE = "http://localhost:8080";

export default function DoctorDetailsPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const location = useLocation();

  const doctorFromState = location.state?.doctor ?? null;

  const [profile, setProfile] = useState(doctorFromState);
  const [loading, setLoading] = useState(!doctorFromState);
  const [err, setErr] = useState("");

  const [modalOpen, setModalOpen] = useState(false);

  useEffect(() => {
    if (doctorFromState) return;
    if (!id) {
      setErr("ID doctor invalid.");
      setLoading(false);
      return;
    }

    (async () => {
      setLoading(true);
      setErr("");
      setProfile(null);

      try {
        const res = await fetch(`${API_BASE}/api/doctor/${id}`);

        if (res.status === 404) {
          setErr("Doctorul nu a fost găsit (404).");
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
  }, [id, doctorFromState]);

  const displayName = useMemo(() => {
    if (!profile) return "";
    const full = `${profile.firstName || ""} ${profile.lastName || ""}`.trim();
    return full || profile.username || "Doctor";
  }, [profile]);

  const specName =
    profile?.specialization?.name ||
    profile?.specialization?.title ||
    profile?.specialization?.id ||
    profile?.specialization ||
    "-";

  return (
    <div style={styles.page}>
      {/* TOP BAR */}
      <div style={styles.topBar}>
        <button onClick={() => navigate(-1)} style={styles.backBtn}>
          ← Înapoi
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      {/* HERO */}
      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Detalii medic</h1>
        <p style={styles.heroText}>
          Vezi informațiile medicului și programează o consultație.
        </p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <div style={{ minWidth: 240 }}>
              <h2 style={styles.name}>
                {loading ? "Se încarcă..." : displayName || "Doctor"}
              </h2>
              <div style={styles.sub}>
                Specializare: <b>{specName}</b>
              </div>
            </div>

            {/* ACTION BUTTONS */}
            <div style={styles.actionsRow}>
              <button
                type="button"
                onClick={() => setModalOpen(true)}
                disabled={!profile || loading}
                style={{
                  ...styles.primaryBtn,
                  opacity: !profile || loading ? 0.7 : 1,
                  cursor: !profile || loading ? "not-allowed" : "pointer",
                }}
              >
                Programează-te
              </button>

              <button
                type="button"
                onClick={() => navigate("/consultation")}
                style={styles.secondaryBtn}
              >
                Înapoi la consultații
              </button>
            </div>
          </div>

          {err && <div style={styles.error}>{err}</div>}

          {!loading && !err && !profile && (
            <div style={styles.infoNote}>Nu există date pentru acest medic.</div>
          )}

          {!loading && profile && (
            <>
              <div style={styles.grid}>
                <Info label="ID" value={profile.id} />
                <Info label="Username" value={profile.username} />
                <Info label="Prenume" value={profile.firstName} />
                <Info label="Nume" value={profile.lastName} />

                {"email" in profile && <Info label="Email" value={profile.email} />}
                {"phone" in profile && <Info label="Telefon" value={profile.phone} />}
                <Info label="Specializare" value={specName} />
                {"ratingAvg" in profile && <Info label="Rating" value={profile.ratingAvg} />}
              </div>

              <div style={styles.infoNote}>
                Apasă <b>Programează-te</b> pentru a vedea disponibilitatea și a selecta o oră.
              </div>
            </>
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

  primaryBtn: {
    backgroundColor: "#0aa862",
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
