import { useState } from "react";
import { useNavigate } from "react-router-dom";

const API_BASE = "http://localhost:8080";

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export default function SignUpPage() {
  const navigate = useNavigate();

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [password2, setPassword2] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [phone, setPhone] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSignUp(e) {
    e.preventDefault();
    setError("");

    if (!firstName.trim() || !lastName.trim() || !email.trim() || !username.trim() || !password.trim()) {
      setError("Completează prenume, nume, email, username și parola.");
      return;
    }

    if (email.trim() && !isValidEmail(email.trim())) {
      setError("Email invalid. Format corect: exemplu@email.com");
      return;
    }

    if (password.length < 6) {
      setError("Parola trebuie să aibă minim 6 caractere.");
      return;
    }

    if (password !== password2) {
      setError("Parolele nu coincid.");
      return;
    }

    if (!phone.trim()) {
      setError("Completează numărul de telefon.");
      return;
    }

    if (!dateOfBirth.trim()) {
      setError("Completează data nașterii.");
      return;
    }

    setLoading(true);
    try {
      // imposing pacient register only
      const res = await fetch(API_BASE + "/api/patient/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          firstName,
          lastName,
          username,
          email: email.trim() ? email.trim() : null,
          password,
          phone,
          dateOfBirth,
        }),
      });

      if (res.status === 409) {
        setError("Username sau email deja folosit.");
        return;
      }

      if (!res.ok) {
        setError("Eroare server la înregistrare.");
        return;
      }

      let data = null;
      try {
        data = await res.json();
      } catch {
        data = null;
      }

      // if token provided, automatic login
      if (data?.token) {
        localStorage.setItem("token", data.token);
        localStorage.setItem("role", "patient");
        localStorage.setItem(
          "user",
          JSON.stringify({
            firstName: data.firstName || firstName,
            lastName: data.lastName || lastName,
            username: data.username || username,
          })
        );
        navigate("/", { replace: true });
        return;
      }

      // otherwise, redirect to login page
      navigate("/login", { replace: true, state: { from: "/" } });
    } catch {
      setError("Backend indisponibil.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      {/* TOP BAR */}
      <div style={styles.topBar}>
        <button onClick={() => navigate("/")} style={styles.backBtn}>
          ← Înapoi acasă
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      {/* HERO */}
      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Creare cont</h1>
        <p style={styles.heroText}>Înregistrează-te pentru programări online</p>
      </section>

      {/* SIGNUP CARD */}
      <div style={styles.cardWrapper}>
        <form onSubmit={handleSignUp} style={styles.card}>
          <h2 style={styles.cardTitle}>Cont nou</h2>

          <label style={styles.label}>Prenume</label>
          <input value={firstName} onChange={(e) => setFirstName(e.target.value)} style={styles.input} />

          <label style={styles.label}>Nume</label>
          <input value={lastName} onChange={(e) => setLastName(e.target.value)} style={styles.input} />

          <label style={styles.label}>Username</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} style={styles.input} />

          <label style={styles.label}>Email (opțional)</label>
          <input value={email} onChange={(e) => setEmail(e.target.value)} style={styles.input} />

          <label style={styles.label}>Telefon</label>
          <input value={phone} onChange={(e) => setPhone(e.target.value)} style={styles.input} />

          <label style={styles.label}>Data nașterii</label>
          <input
            type="date"
            value={dateOfBirth}
            onChange={(e) => setDateOfBirth(e.target.value)}
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
            >
              {showPw ? "🙈" : "👁️"}
            </button>
          </div>

          <label style={styles.label}>Confirmă parola</label>
          <input
            type={showPw ? "text" : "password"}
            value={password2}
            onChange={(e) => setPassword2(e.target.value)}
            style={styles.input}
          />

          {error && <div style={styles.error}>{error}</div>}

          <button type="submit" disabled={loading} style={styles.submit}>
            {loading ? "Se creează contul..." : "Creează cont"}
          </button>

          <div style={styles.linkRow}>
            Ai deja cont?{" "}
            <span style={styles.link} onClick={() => navigate("/login")}>
              Autentifică-te
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
    width: 560,
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
