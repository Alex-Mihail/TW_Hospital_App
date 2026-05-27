import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

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

  const userId = user?.id ?? null;
  const role = (roleRaw || "").toLowerCase();
  return { role, userId };
}

function patientGetEndpoint(id) {
  return `/api/patient/${id}`;
}
function doctorGetEndpoint(id) {
  // doctor account endpoint
  return `/api/doctor/${id}/account`;
}
function adminGetEndpoint(id) {
  return `/api/admin/${id}`;
}

function patientPutEndpoint(id) {
  return `/api/patient/${id}`;
}
function doctorPutEndpoint(id) {
  return `/api/doctor/${id}/account`;
}
function adminPutEndpoint(id) {
  return `/api/admin/${id}/account`;
}

export default function EditAccountPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const navState = location.state ?? {};

  const stored = getStoredSession();

  const role = ((navState.role ?? stored.role) || "").toLowerCase();
  const userId = navState.userId ?? stored.userId;

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState("");

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    dateOfBirth: "",
  });

  const roleLabel = useMemo(() => (role ? role.toUpperCase() : "-"), [role]);

  const showPatientExtras = role === "patient";

  useEffect(() => {
    if (!role || !userId) {
      navigate("/login", { replace: true, state: { from: "/account/edit" } });
      return;
    }

    const endpoint =
      role === "patient"
        ? patientGetEndpoint(userId)
        : role === "doctor"
        ? doctorGetEndpoint(userId)
        : role === "admin"
        ? adminGetEndpoint(userId)
        : "";

    if (!endpoint) {
      setErr(`Rol necunoscut: "${role}"`);
      setLoading(false);
      return;
    }

    (async () => {
      setLoading(true);
      setErr("");

      try {
        const token = localStorage.getItem("token");
        const headers = token ? { Authorization: `Bearer ${token}` } : {};

        const res = await fetch(API_BASE + endpoint, { headers });

        if (res.status === 401) {
          localStorage.removeItem("token");
          localStorage.removeItem("role");
          localStorage.removeItem("user");
          navigate("/login", { replace: true, state: { from: "/account/edit" } });
          return;
        }

        if (res.status === 404) {
          setErr("Profilul nu a fost găsit (404).");
          setLoading(false);
          return;
        }

        if (!res.ok) {
          const txt = await res.text().catch(() => "");
          setErr(txt ? `Eroare backend: ${txt}` : `Eroare backend: ${res.status}`);
          setLoading(false);
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
        });
      } catch (e) {
        console.log(e);
        setErr("Backend indisponibil / CORS / eroare rețea.");
      } finally {
        setLoading(false);
      }
    })();
  }, [navigate, role, userId]);

  const displayName = useMemo(() => {
    const full = `${form.firstName || ""} ${form.lastName || ""}`.trim();
    return full || profile?.username || "Cont";
  }, [form.firstName, form.lastName, profile]);

  function onChange(e) {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  }

  function handleCancel() {
    navigate("/account", { replace: true });
  }

  async function handleSave(e) {
    e.preventDefault();
    setErr("");

    if (!form.firstName.trim() || !form.lastName.trim()) {
      setErr("Prenumele și numele sunt obligatorii.");
      return;
    }

    const endpoint =
      role === "patient"
        ? patientPutEndpoint(userId)
        : role === "doctor"
        ? doctorPutEndpoint(userId)
        : role === "admin"
        ? adminPutEndpoint(userId)
        : "";

    if (!endpoint) {
      setErr(`Rol necunoscut: "${role}"`);
      return;
    }

    const payload =
      role === "patient"
        ? {
            firstName: form.firstName.trim(),
            lastName: form.lastName.trim(),
            email: form.email.trim() || null,
            phone: form.phone.trim() || null,
            dateOfBirth: form.dateOfBirth.trim() || null,
          }
        : role === "doctor" || role === "admin"
        ? {
            firstName: form.firstName.trim(),
            lastName: form.lastName.trim(),
            email: form.email.trim() || null,
          }
        : null;

    if (!payload) {
      setErr("Nu pot construi payload pentru acest rol.");
      return;
    }

    setSaving(true);
    try {
      const token = localStorage.getItem("token");
      const headers = {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      };

      console.log("PUT ->", API_BASE + endpoint, payload);

      const res = await fetch(API_BASE + endpoint, {
        method: "PUT",
        headers,
        body: JSON.stringify(payload),
      });

      if (res.status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("user");
        navigate("/login", { replace: true, state: { from: "/account/edit" } });
        return;
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt ? `Eroare la salvare: ${txt}` : `Eroare la salvare (${res.status})`);
        return;
      }

      // update localStorage 
      try {
        const u = JSON.parse(localStorage.getItem("user") || "null");
        if (u) {
          localStorage.setItem(
            "user",
            JSON.stringify({
              ...u,
              firstName: payload.firstName,
              lastName: payload.lastName,
              email: payload.email ?? u.email,
            })
          );
        }
      } catch {}

      navigate("/account", { replace: true });
    } catch (e) {
      console.log(e);
      setErr("Backend indisponibil / eroare rețea la salvare.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.topBar}>
        <button onClick={handleCancel} style={styles.backBtn}>
          ← Înapoi la cont
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Editează cont</h1>
        <p style={styles.heroText}>Actualizează datele contului tău.</p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <div style={{ minWidth: 240 }}>
              <h2 style={styles.name}>{loading ? "Se încarcă..." : displayName}</h2>
              <div style={styles.sub}>
                Rol: <b>{roleLabel}</b>
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
                <Field label="Username" value={profile?.username ?? "-"} readOnly />

                <Field
                  label="Prenume"
                  name="firstName"
                  value={form.firstName}
                  onChange={onChange}
                  placeholder="Prenume"
                />
                <Field
                  label="Nume"
                  name="lastName"
                  value={form.lastName}
                  onChange={onChange}
                  placeholder="Nume"
                />

                <Field
                  label="Email"
                  name="email"
                  value={form.email}
                  onChange={onChange}
                  placeholder="email@exemplu.com"
                />

                {showPatientExtras && (
                  <>
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
                  </>
                )}
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

  formGrid: {
    marginTop: 2,
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: 12,
  },
  field: {
    border: "1px solid #e6eef6",
    borderRadius: 12,
    padding: 12,
    background: "#fbfdff",
  },
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

  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
