import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";

const API_BASE = "http://localhost:8080";
const SPEC_ENDPOINT = "/api/specializations"; 

export default function AdminDoctorEditPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams();

  const [profile, setProfile] = useState(location.state?.profile ?? null);
  const [specs, setSpecs] = useState([]);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState("");

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    specializationId: "",
    password: "", 
  });

  useEffect(() => {
    (async () => {
      try {
        const res = await fetch(`${API_BASE}${SPEC_ENDPOINT}`);
        const data = await res.json().catch(() => []);
        setSpecs(Array.isArray(data) ? data : []);
      } catch {
        setSpecs([]);
      }
    })();
  }, []);

  async function load() {
    setLoading(true);
    setErr("");
    try {
      const res = await fetch(`${API_BASE}/api/admin/doctors/${id}`);
      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare backend: ${res.status}`);
        setProfile(null);
        return;
      }
      const data = await res.json();
      setProfile(data);
      setForm({
        firstName: data?.firstName ?? "",
        lastName: data?.lastName ?? "",
        username: data?.username ?? "",
        email: data?.email ?? "",
        specializationId: data?.specialization?.id ? String(data.specialization.id) : "",
        password: "",
      });
    } catch {
      setErr("Backend indisponibil / CORS / eroare rețea.");
      setProfile(null);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [id]);

  const displayName = useMemo(() => {
    const full = `${form.firstName || ""} ${form.lastName || ""}`.trim();
    return full || profile?.username || `Doctor #${id}`;
  }, [form.firstName, form.lastName, profile, id]);

  function onChange(e) {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  }

  async function handleSave(e) {
    e.preventDefault();
    setErr("");

    if (!form.firstName.trim() || !form.lastName.trim() || !form.username.trim()) {
      setErr("Prenumele, numele și username sunt obligatorii.");
      return;
    }

    if (!form.specializationId) {
      setErr("Alege o specializare.");
      return;
    }

    const payload = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      username: form.username.trim(),
      email: form.email.trim() || null,
      specialization: { id: Number(form.specializationId) },
    };

    // if admin resets password => reset
    if (form.password.trim()) payload.password = form.password;

    setSaving(true);
    try {
      const res = await fetch(`${API_BASE}/api/admin/doctors/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (res.status === 409) {
        const txt = await res.text().catch(() => "");
        setErr(txt || "Username sau email deja folosit.");
        return;
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare la salvare (${res.status})`);
        return;
      }

      navigate(`/admin/doctors/${id}`, { replace: true });
    } catch {
      setErr("Backend indisponibil / eroare rețea la salvare.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.topBar}>
        <button onClick={() => navigate(`/admin/doctors/${id}`)} style={styles.backBtn}>
          ← Detalii doctor
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Editează doctor</h1>
        <p style={styles.heroText}>Actualizează datele contului.</p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <div style={{ minWidth: 240 }}>
              <h2 style={styles.name}>{loading ? "Se încarcă..." : displayName}</h2>
              <div style={styles.sub}>
                Tip: <b>DOCTOR</b>
              </div>
            </div>

            <div style={styles.actionsRow}>
              <button
                onClick={() => navigate(`/admin/doctors/${id}`)}
                style={styles.secondaryBtn}
                disabled={saving}
              >
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
                <Field label="Username" name="username" value={form.username} onChange={onChange} />

                <Field label="Prenume" name="firstName" value={form.firstName} onChange={onChange} />
                <Field label="Nume" name="lastName" value={form.lastName} onChange={onChange} />

                <Field label="Email" name="email" value={form.email} onChange={onChange} placeholder="email@exemplu.com" />

                <div style={styles.field}>
                  <div style={styles.fieldLabel}>Specializare</div>
                  <select
                    name="specializationId"
                    value={form.specializationId}
                    onChange={onChange}
                    style={styles.select}
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
  smallNote: { marginTop: 8, fontSize: 12, color: "#667", lineHeight: 1.35 },

  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
