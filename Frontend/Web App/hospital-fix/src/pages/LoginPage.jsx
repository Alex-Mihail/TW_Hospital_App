import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

const API_BASE = "http://localhost:8080";

export default function LoginPage() {
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = location.state?.from || "/";

  async function handleLogin(e) {
    e.preventDefault();
    setError("");

    const id = identifier.trim();
    const pw = password.trim();

    if (!id || !pw) {
      setError("Completează username/email și parola.");
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(API_BASE + "/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ identifier: id, password: pw }),
      });

      if (res.status === 401) {
        setError("Date de autentificare invalide.");
        return;
      }
      if (!res.ok) {
        setError("Eroare server.");
        return;
      }

      const data = await res.json();

      localStorage.removeItem("token");
      localStorage.removeItem("role");
      localStorage.removeItem("user");

      localStorage.setItem("token", data.token || "");
      localStorage.setItem("role", (data.role || "").toLowerCase());
      localStorage.setItem(
        "user",
        JSON.stringify({
          firstName: data.firstName || "",
          lastName: data.lastName || "",
          username: data.username || id,
          id: data.id,
        })
      );

      navigate(redirectTo, { replace: true });
    } catch {
      setError("Backend indisponibil.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.topBar}>
        <button onClick={() => navigate("/")} style={styles.backBtn}>
          ← Înapoi acasă
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Autentificare</h1>
        <p style={styles.heroText}>Accesează programările și contul tău</p>
      </section>

      <div style={styles.cardWrapper}>
        <form onSubmit={handleLogin} style={styles.card}>
          <h2 style={styles.cardTitle}>Bine ai revenit</h2>

          <label style={styles.label}>Username / Email</label>
          <input
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            style={styles.input}
          />

          <label style={styles.label}>Parolă</label>
          <div style={{ position: "relative" }}>
            <input
              type={showPw ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={{ ...styles.input, paddingRight: 50 }}
            />
            <button
              type="button"
              onClick={() => setShowPw(!showPw)}
              style={styles.eyeBtn}
              aria-label={showPw ? "Ascunde parola" : "Arată parola"}
            >
              {showPw ? "🙈" : "👁️"}
            </button>
          </div>

          {/* Forgot password */}
          <div style={styles.forgotRow}>
            <span
              style={styles.forgotLink}
              onClick={() => navigate("/reset-password")}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === "Enter") navigate("/reset-password");
              }}
            >
              Am uitat parola
            </span>
          </div>

          {error && <div style={styles.error}>{error}</div>}

          <button type="submit" disabled={loading} style={styles.submit}>
            {loading ? "Se autentifică..." : "Autentificare"}
          </button>

          <div style={styles.linkRow}>
            Nu ai cont?{" "}
            <span style={styles.link} onClick={() => navigate("/signup")}>
              Creează cont
            </span>
          </div>
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
  forgotRow: { marginTop: 6, display: "flex", justifyContent: "flex-end" },
  forgotLink: {
    color: "#0a4d80",
    fontWeight: 700,
    cursor: "pointer",
    fontSize: 13,
    textDecoration: "underline",
    opacity: 0.95,
  },
  error: {
    background: "#ffe8e8",
    padding: 10,
    borderRadius: 12,
    color: "#7a1b1b",
    border: "1px solid #ffb3b3",
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
  linkRow: { marginTop: 10, fontSize: 14, color: "#556" },
  link: { color: "#0a4d80", fontWeight: 600, cursor: "pointer" },
  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};