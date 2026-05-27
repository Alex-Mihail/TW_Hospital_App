import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";

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

export default function AdminPatientEditPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams();

  const stored = getStoredSession();
  const role = stored.role;

  const [profile, setProfile] = useState(location.state?.profile ?? null);

  const [loading, setLoading] = useState(!profile);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState("");

  const [form, setForm] = useState({
    firstName: profile?.firstName ?? "",
    lastName: profile?.lastName ?? "",
    email: profile?.email ?? "",
    phone: profile?.phone ?? "",
    dateOfBirth: profile?.dateOfBirth ?? "",
    password: "", // reset password (optional)
  });

  const displayName = useMemo(() => {
    const full = `${form.firstName || ""} ${form.lastName || ""}`.trim();
    return full || profile?.username || `Pacient #${id}`;
  }, [form.firstName, form.lastName, profile, id]);

  useEffect(() => {
    // guard admin
    if (!role || role !== "admin") {
      navigate("/login", { replace: true, state: { from: `/admin/patients/${id}/edit` } });
      return;
    }

    if (profile) return;

    (async () => {
      setLoading(true);
      setErr("");
      try {
        const res = await fetch(`${API_BASE}/api/admin/patients/${id}`, {
          headers: authHeaders(),
        });

        if (res.status === 401) {
          localStorage.removeItem("token");
          localStorage.removeItem("role");
          localStorage.removeItem("user");
          navigate("/login", { replace: true, state: { from: `/admin/patients/${id}/edit` } });
          return;
        }

        if (res.status === 404) {
          setErr("Pacientul nu a fost găsit (404).");
          return;
        }

        if (!res.ok) {
          const txt = await res.text().catch(() => "");
          setErr(txt || `Eroare backend: ${res.status}`);
          return;
        }

        const data = await res.json();
        setProfile(data);

        setForm({
          firstName: data?.firstName ?? "",
          lastName: data?.lastName ?? "",
          email: data?.email ?? "",
          phone: data?.phone ?? "",
          dateOfBirth: data?.dateOfBirth ?? "",
          password: "",
        });
      } catch (e) {
        console.log(e);
        setErr("Backend indisponibil / CORS / eroare rețea.");
      } finally {
        setLoading(false);
      }
    })();
  }, [navigate, role, id, profile]);

  function onChange(e) {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  }

  function handleCancel() {
    navigate(`/admin/patients/${id}`, { replace: true });
  }

  async function handleSave(e) {
    e.preventDefault();
    setErr("");

    if (!form.firstName.trim() || !form.lastName.trim()) {
      setErr("Prenumele și numele sunt obligatorii.");
      return;
    }

    if (!profile) {
      setErr("Profil invalid (nu este încărcat).");
      return;
    }

    // send entire existing profile (safe overwrite) + changes 
    const payload = {
      ...profile,
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      email: form.email.trim() || null,
      phone: form.phone.trim() || null,
      dateOfBirth: form.dateOfBirth.trim() || null,
    };

    // if admin fills in password => reset
    if (form.password.trim()) payload.password = form.password;

    setSaving(true);
    try {
      const res = await fetch(`${API_BASE}/api/admin/patients/${id}`, {
        method: "PUT",
        headers: authHeaders(true),
        body: JSON.stringify(payload),
      });

      if (res.status === 409) {
        const txt = await res.text().catch(() => "");
        setErr(txt || "Email deja folosit / conflict.");
        return;
      }

      if (res.status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("user");
        navigate("/login", { replace: true, state: { from: `/admin/patients/${id}/edit` } });
        return;
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare la salvare (${res.status})`);
        return;
      }

      navigate(`/admin/patients/${id}`, { replace: true });
    } catch (e2) {
      console.log(e2);
      setErr("Backend indisponibil / eroare rețea la salvare.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.topBar}>
        <button onClick={handleCancel} style={styles.backBtn}>
          ← Detalii pacient
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Editează pacient</h1>
        <p style={styles.heroText}>Actualizează datele contului.</p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <div style={{ minWidth: 240 }}>
              <h2 style={styles.name}>{loading ? "Se încarcă..." : displayName}</h2>
              <div style={styles.sub}>
                Tip: <b>PATIENT</b>
              </div>
            </div>

            <div style={styles.actionsRow}>
              <button onClick={handleCancel} style={styles.secondaryBtn} disabled={saving}>
                Anulează
              </button>
              <button
                type="submit"
                form="edit-form"
                style={{
                  ...styles.primaryBtn,
                  opacity: saving ? 0.75 : 1,
                  cursor: saving ? "not-allowed" : "pointer",
                }}
                disabled={saving}
              >
                {saving ? "Se salvează..." : "Salvează"}
              </button>
            </div>
          </div>

          {err && <div style={styles.error}>{err}</div>}
          {loading && !err && <div style={styles.infoNote}>Se încarcă datele...</div>}

          {!loading && !err && (
            <form id="edit-form" onSubmit={handleSave} style={{ marginTop: 16 }}>
              <div style={styles.formGrid}>
                <Field label="ID" value={profile?.id ?? "-"} readOnly />
                <Field label="Username" value={profile?.username ?? "-"} readOnly />

                <Field label="Prenume" name="firstName" value={form.firstName} onChange={onChange} />
                <Field label="Nume" name="lastName" value={form.lastName} onChange={onChange} />

                <Field
                  label="Email"
                  name="email"
                  value={form.email}
                  onChange={onChange}
                  placeholder="email@exemplu.com"
                />
                <Field
                  label="Telefon"
                  name="phone"
                  value={form.phone}
                  onChange={onChange}
                  placeholder="07xx xxx xxx"
                />
                <Field
                  label="Data nașterii"
                  name="dateOfBirth"
                  value={form.dateOfBirth}
                  onChange={onChange}
                  placeholder="YYYY-MM-DD"
                />

                {/* Reset password bar */}
                <div style={styles.field}>
                  <div style={styles.fieldLabel}>Reset parolă (opțional)</div>
                  <input
                    name="password"
                    value={form.password}
                    onChange={onChange}
                    placeholder="Lasă gol dacă nu schimbi"
                    style={styles.input}
                  />
                  <div style={styles.smallNote}>
                    Dacă completezi aici, parola se va actualiza.
                  </div>
                </div>
              </div>
            </form>
          )}
        </div>
      </div>

      <footer style={styles.footer}>© 2026 Spitalul Central TW</footer>
    </div>
  );
}

function Field({ label, name, value, onChange, placeholder, readOnly }) {
  return (
    <div style={styles.field}>
      <div style={styles.fieldLabel}>{label}</div>
      <input
        name={name}
        value={value ?? ""}
        onChange={onChange}
        placeholder={placeholder}
        readOnly={!!readOnly}
        style={{
          ...styles.input,
          backgroundColor: readOnly ? "#f5f9fc" : "#fbfdff",
          cursor: readOnly ? "not-allowed" : "text",
        }}
      />
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

  name: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 28, lineHeight: 1.2 },
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

  formGrid: { marginTop: 2, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 },
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
  smallNote: { marginTop: 8, fontSize: 12, color: "#667", lineHeight: 1.35 },

  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
