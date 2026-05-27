import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

const API_BASE = "http://localhost:8080";
const SPEC_ENDPOINT = "/api/specializations";

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export default function AdminDoctorRegisterPage() {
  const navigate = useNavigate();

  const [specs, setSpecs] = useState([]);
  const [loadingSpecs, setLoadingSpecs] = useState(true);

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [specializationId, setSpecializationId] = useState("");
  const [password, setPassword] = useState("");
  const [password2, setPassword2] = useState("");
  const [showPw, setShowPw] = useState(false);

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  useEffect(() => {
    (async () => {
      setLoadingSpecs(true);
      try {
        const res = await fetch(`${API_BASE}${SPEC_ENDPOINT}`);
        const data = await res.json().catch(() => []);
        setSpecs(Array.isArray(data) ? data : []);
      } catch {
        setSpecs([]);
      } finally {
        setLoadingSpecs(false);
      }
    })();
  }, []);

  const specLabel = useMemo(() => {
    const s = specs.find((x) => String(x.id) === String(specializationId));
    return s?.name || "-";
  }, [specs, specializationId]);

  async function handleSubmit(e) {
    e.preventDefault();
    setErr("");

    if (!firstName.trim() || !lastName.trim() || !username.trim() || !password.trim()) {
      setErr("Completează prenume, nume, username și parola.");
      return;
    }

    if (email.trim() && !isValidEmail(email.trim())) {
      setErr("Email invalid. Format corect: exemplu@email.com");
      return;
    }

    if (!specializationId) {
      setErr("Alege o specializare.");
      return;
    }

    if (password.length < 6) {
      setErr("Parola trebuie să aibă minim 6 caractere.");
      return;
    }

    if (password !== password2) {
      setErr("Parolele nu coincid.");
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/admin/doctors/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          username: username.trim(),
          email: email.trim() || null,
          password,
          specializationId: Number(specializationId),
        }),
      });

      if (res.status === 409) {
        const txt = await res.text().catch(() => "");
        setErr(txt || "Username sau email deja folosit.");
        return;
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare la înregistrare (${res.status}).`);
        return;
      }

      const created = await res.json().catch(() => null);
      if (!created?.id) {
        navigate("/admin/doctors", { replace: true });
        return;
      }

      navigate(`/admin/doctors/${created.id}`, { replace: true });
    } catch {
      setErr("Backend indisponibil / CORS / eroare rețea.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.topBar}>
        <button onClick={() => navigate("/admin/doctors")} style={styles.backBtn}>
          ← Doctori
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Înregistrare doctor</h1>
        <p style={styles.heroText}>Creează cont nou pentru medic.</p>
      </section>

      <div style={styles.cardWrapper}>
        <form onSubmit={handleSubmit} style={styles.card}>
          <div style={styles.headerRow}>
            <div>
              <h2 style={styles.cardTitle}>Doctor nou</h2>
              <div style={styles.sub}>
                {loadingSpecs ? "Se încarcă specializările..." : `Specializare: ${specLabel}`}
              </div>
            </div>

            <div style={styles.actionsRow}>
              <button type="button" onClick={() => navigate("/admin/doctors")} style={styles.secondaryBtn} disabled={loading}>
                Anulează
              </button>
              <button type="submit" style={styles.primaryBtn} disabled={loading}>
                {loading ? "Se creează..." : "Creează"}
              </button>
            </div>
          </div>

          {err && <div style={styles.error}>{err}</div>}

          <div style={styles.formGrid}>
            <Field label="Prenume" value={firstName} onChange={setFirstName} />
            <Field label="Nume" value={lastName} onChange={setLastName} />

            <Field label="Username" value={username} onChange={setUsername} />
            <Field label="Email (opțional)" value={email} onChange={setEmail} />

            <div style={styles.field}>
              <div style={styles.fieldLabel}>Specializare</div>
              <select
                value={specializationId}
                onChange={(e) => setSpecializationId(e.target.value)}
                style={styles.select}
                disabled={loadingSpecs}
              >
                <option value="">Alege specializare</option>
                {specs.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>

            <div style={styles.field}>
              <div style={styles.fieldLabel}>Parolă</div>
              <div style={{ position: "relative" }}>
                <input
                  type={showPw ? "text" : "password"}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  style={{ ...styles.input, paddingRight: 50 }}
                />
                <button type="button" onClick={() => setShowPw(!showPw)} style={styles.eyeBtn}>
                  {showPw ? "🙈" : "👁️"}
                </button>
              </div>
            </div>

            <div style={styles.field}>
              <div style={styles.fieldLabel}>Confirmă parola</div>
              <input
                type={showPw ? "text" : "password"}
                value={password2}
                onChange={(e) => setPassword2(e.target.value)}
                style={styles.input}
              />
            </div>
          </div>
        </form>
      </div>

      <footer style={styles.footer}>© 2026 Spitalul Central TW</footer>
    </div>
  );
}

function Field({ label, value, onChange }) {
  return (
    <div style={styles.field}>
      <div style={styles.fieldLabel}>{label}</div>
      <input value={value} onChange={(e) => onChange(e.target.value)} style={styles.input} />
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
    width: 920,
    maxWidth: "100%",
    boxShadow: "0 10px 25px rgba(0,0,0,0.15)",
  },
  headerRow: { display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "center" },
  actionsRow: { display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap", justifyContent: "flex-end" },

  cardTitle: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 26 },
  sub: { marginTop: 6, color: "#556", fontSize: 14 },

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

  error: {
    marginTop: 14,
    background: "#ffe8e8",
    padding: 10,
    borderRadius: 12,
    color: "#7a1b1b",
    border: "1px solid #ffb3b3",
  },

  formGrid: { marginTop: 16, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 },
  field: { border: "1px solid #e6eef6", borderRadius: 12, padding: 12, background: "#fbfdff" },
  fieldLabel: { color: "#0a4d80", fontWeight: 700, fontSize: 13 },
  input: {
    marginTop: 8,
    width: "100%",
    padding: 12,
    borderRadius: 12,
    border: "1px solid #d7e3ee",
    backgroundColor: "#fbfdff",
    fontSize: 15,
    lineHeight: 1.4,
    outline: "none",
    boxSizing: "border-box",
  },
  select: {
    marginTop: 8,
    width: "100%",
    padding: 12,
    borderRadius: 12,
    border: "1px solid #d7e3ee",
    backgroundColor: "#fbfdff",
    fontSize: 15,
    outline: "none",
    boxSizing: "border-box",
  },
  eyeBtn: {
    position: "absolute",
    right: 10,
    top: "50%",
    transform: "translateY(-50%)",
    border: "1px solid #d7e3ee",
    background: "#fbfdff",
    borderRadius: 10,
    padding: "6px 10px",
    cursor: "pointer",
  },

  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
