import { useState } from "react";
import { useNavigate } from "react-router-dom";

const API_BASE = "http://localhost:8080";

export default function ResetPasswordPage() {
  const navigate = useNavigate();

  const [identifier, setIdentifier] = useState("");
  const [newPw, setNewPw] = useState("");
  const [newPw2, setNewPw2] = useState("");
  const [showPw, setShowPw] = useState(false);

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");
  const [ok, setOk] = useState("");

  async function resetOn(url) {
    return fetch(API_BASE + url, {
      method: "PUT", 
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        identifier: identifier.trim(),
        newPassword: newPw,
      }),
    });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setErr("");
    setOk("");

    const id = identifier.trim();
    if (!id) return setErr("Completează username/email.");
    if (!newPw.trim()) return setErr("Completează parola nouă.");
    if (newPw.trim().length < 6) return setErr("Parola trebuie să aibă minim 6 caractere.");
    if (newPw !== newPw2) return setErr("Parolele nu coincid.");

    setBusy(true);
    try {
      // patient
      let res = await resetOn("/api/patient/reset-password");

      // if patient does not exist -> doctor
      if (res.status === 404) {
        res = await resetOn("/api/doctor/reset-password");
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare la resetare (${res.status}).`);
        return;
      }

      const msg = await res.text().catch(() => "");
      setOk(msg || "Parola a fost actualizată. Te poți autentifica acum.");
      setTimeout(() => navigate("/login"), 800);
    } catch {
      setErr("Backend indisponibil / eroare rețea.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.topBar}>
        <button onClick={() => navigate("/login")} style={styles.backBtn}>
          ← Înapoi la login
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Resetare parolă</h1>
        <p style={styles.heroText}>Introdu username/email și setează o parolă nouă.</p>
      </section>

      <div style={styles.cardWrapper}>
        <form onSubmit={handleSubmit} style={styles.card}>
          <h2 style={styles.cardTitle}>Schimbă parola</h2>

          <label style={styles.label}>Username / Email</label>
          <input
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            style={styles.input}
            placeholder="ex: ion.popescu / ion@email.com"
          />

          <label style={styles.label}>Parolă nouă</label>
          <div style={{ position: "relative" }}>
            <input
              type={showPw ? "text" : "password"}
              value={newPw}
              onChange={(e) => setNewPw(e.target.value)}
              style={{ ...styles.input, paddingRight: 50 }}
              placeholder="Minim 6 caractere"
            />
            <button
              type="button"
              onClick={() => setShowPw(!showPw)}
              style={styles.eyeBtn}
            >
              {showPw ? "🙈" : "👁️"}
            </button>
          </div>

          <label style={styles.label}>Confirmă parola</label>
          <input
            type={showPw ? "text" : "password"}
            value={newPw2}
            onChange={(e) => setNewPw2(e.target.value)}
            style={styles.input}
          />

          {err && <div style={styles.error}>{err}</div>}
          {ok && <div style={styles.ok}>{ok}</div>}

          <button type="submit" disabled={busy} style={styles.submit}>
            {busy ? "Se salvează..." : "Actualizează parola"}
          </button>
        </form>
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
  heroTitle: { fontSize: 54, fontWeight: 600, margin: 0, lineHeight: 1.15 },
  heroText: { fontSize: 20, fontWeight: 400, marginTop: 10, lineHeight: 1.5 },
  cardWrapper: { display: "flex", justifyContent: "center", marginTop: -40 },
  card: {
    background: "white",
    padding: 30,
    borderRadius: 18,
    width: 520,
    maxWidth: "95vw",
    boxShadow: "0 10px 25px rgba(0,0,0,0.15)",
    display: "flex",
    flexDirection: "column",
    gap: 10,
  },
  cardTitle: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 28 },
  label: { marginTop: 10, fontWeight: 500, color: "#0a4d80" },
  input: {
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
  error: {
    background: "#ffe8e8",
    padding: 10,
    borderRadius: 12,
    color: "#7a1b1b",
    border: "1px solid #ffb3b3",
    marginTop: 6,
  },
  ok: {
    background: "rgba(233, 255, 240, 0.95)",
    padding: 10,
    borderRadius: 12,
    color: "#0b5a2a",
    border: "1px solid rgba(159, 230, 183, 0.95)",
    marginTop: 6,
  },
  submit: {
    marginTop: 10,
    backgroundColor: "#0a4d80",
    color: "white",
    border: "none",
    padding: "12px 14px",
    borderRadius: 12,
    fontWeight: 600,
    cursor: "pointer",
  },
  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
