import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

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

function formatDateTime(dt) {
  if (!dt) return "-";
  const s = String(dt);
  const date = s.slice(0, 10);
  const time = s.slice(11, 16);
  return `${date} ${time}`;
}

function getDateField(a) {
  return a?.appointmentDatetime || a?.startAt || a?.appointmentDateTime || a?.appointment_datetime || null;
}

function StatusPill({ status }) {
  const st = (status || "").toUpperCase();

  const style =
    st === "ACCEPTED"
      ? { background: "rgba(10,168,98,0.12)", border: "1px solid rgba(10,168,98,0.35)", color: "#0b5a2a" }
      : st === "DENIED"
      ? { background: "rgba(180,35,24,0.12)", border: "1px solid rgba(180,35,24,0.35)", color: "#7a1b1b" }
      : st === "CANCELLED"
      ? { background: "rgba(10,77,128,0.10)", border: "1px solid rgba(10,77,128,0.25)", color: "#0a4d80" }
      : st === "FINISHED"
      ? { background: "rgba(15,23,42,0.10)", border: "1px solid rgba(15,23,42,0.18)", color: "#0f172a" }
      : { background: "rgba(245,158,11,0.14)", border: "1px solid rgba(245,158,11,0.35)", color: "#7a4b00" };

  return (
    <span
      style={{
        display: "inline-block",
        padding: "5px 10px",
        borderRadius: 999,
        fontSize: 12,
        fontWeight: 600,
        letterSpacing: 0.2,
        ...style,
      }}
    >
      {st || "-"}
    </span>
  );
}

const STATUSES = ["PENDING", "ACCEPTED", "DENIED", "CANCELLED", "FINISHED"];

export default function AdminPatientDetailsPage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const stored = getStoredSession();
  const role = stored.role;

  const [profile, setProfile] = useState(null);
  const [appts, setAppts] = useState([]);

  const [loading, setLoading] = useState(true);
  const [loadingAppts, setLoadingAppts] = useState(false);
  const [err, setErr] = useState("");

  const [deleteBusy, setDeleteBusy] = useState(false);
  const [busyApptId, setBusyApptId] = useState(null);

  async function loadPatientAndAppointments() {
    setLoading(true);
    setErr("");
    setProfile(null);

    try {
      // ---- PATIENT ----
      const res = await fetch(`${API_BASE}/api/admin/patients/${id}`, {
        headers: authHeaders(),
      });

      if (res.status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("user");
        navigate("/login", { replace: true, state: { from: `/admin/patients/${id}` } });
        return;
      }

      if (res.status === 404) {
        setErr("Pacientul nu a fost găsit în backend (404).");
        setLoading(false);
        return;
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare backend: ${res.status}`);
        setLoading(false);
        return;
      }

      const patient = await res.json();
      setProfile(patient);

      // ---- APPOINTMENTS ----
      setLoadingAppts(true);
      try {
        const aRes = await fetch(`${API_BASE}/api/admin/patients/${id}/appointments`, {
          headers: authHeaders(),
        });

        if (aRes.status === 401) {
          localStorage.removeItem("token");
          localStorage.removeItem("role");
          localStorage.removeItem("user");
          navigate("/login", { replace: true, state: { from: `/admin/patients/${id}` } });
          return;
        }

        if (!aRes.ok) {
          setAppts([]);
          return;
        }

        const aData = await aRes.json();
        setAppts(Array.isArray(aData) ? aData : []);
      } finally {
        setLoadingAppts(false);
      }
    } catch (e) {
      console.log(e);
      setErr("Backend indisponibil / CORS / eroare rețea.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    // guard admin
    if (!role || role !== "admin") {
      navigate("/login", { replace: true, state: { from: `/admin/patients/${id}` } });
      return;
    }

    loadPatientAndAppointments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [navigate, role, id]);

  const displayName = useMemo(() => {
    if (!profile) return "";
    const full = `${profile.firstName || ""} ${profile.lastName || ""}`.trim();
    return full || profile.username || `Pacient #${id}`;
  }, [profile, id]);

  function handleBack() {
    navigate("/admin/patients");
  }

  function handleEdit() {
    navigate(`/admin/patients/${id}/edit`, { state: { profile } });
  }

  async function handleDeletePatient() {
    if (!id) return;

    const ok = window.confirm(
      "Ești sigur că vrei să ștergi acest pacient? Se vor șterge și programările asociate."
    );
    if (!ok) return;

    setDeleteBusy(true);
    setErr("");

    try {
      const res = await fetch(`${API_BASE}/api/admin/patients/${id}`, {
        method: "DELETE",
        headers: authHeaders(),
      });

      if (res.status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("user");
        navigate("/login", { replace: true, state: { from: `/admin/patients/${id}` } });
        return;
      }

      if (res.status === 404) {
        setErr("Pacientul nu a fost găsit (404).");
        return;
      }

      if (!(res.status === 204 || res.ok)) {
        const txt = await res.text().catch(() => "");
        setErr(`Eroare la ștergere: ${res.status} ${txt}`);
        return;
      }

      navigate("/admin/patients", { replace: true });
    } catch (e) {
      console.log(e);
      setErr("Backend indisponibil / eroare rețea la ștergere.");
    } finally {
      setDeleteBusy(false);
    }
  }

  async function updateAppointmentStatus(apptId, status) {
    if (!apptId) return;

    setBusyApptId(apptId);
    setErr("");

    try {
      // update status
      const res = await fetch(`${API_BASE}/api/admin/appointments/${apptId}/status`, {
        method: "PUT",
        headers: authHeaders(true),
        body: JSON.stringify({ status }),
      });

      if (res.status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("user");
        navigate("/login", { replace: true, state: { from: `/admin/patients/${id}` } });
        return;
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare update status (${res.status})`);
        return;
      }

      // local refresh (or reload appts)
      const saved = await res.json().catch(() => null);
      if (saved?.id) setAppts((xs) => xs.map((x) => (x.id === saved.id ? saved : x)));
      else await loadPatientAndAppointments();
    } catch (e) {
      console.log(e);
      setErr("Backend indisponibil / eroare rețea.");
    } finally {
      setBusyApptId(null);
    }
  }

  async function deleteAppointment(apptId) {
    const ok = window.confirm("Sigur vrei să ștergi această programare?");
    if (!ok) return;

    setBusyApptId(apptId);
    setErr("");

    try {
      const res = await fetch(`${API_BASE}/api/admin/appointments/${apptId}`, {
        method: "DELETE",
        headers: authHeaders(),
      });

      if (res.status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("user");
        navigate("/login", { replace: true, state: { from: `/admin/patients/${id}` } });
        return;
      }

      if (!(res.status === 204 || res.ok)) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare ștergere (${res.status})`);
        return;
      }

      setAppts((xs) => xs.filter((x) => x.id !== apptId));
    } catch (e) {
      console.log(e);
      setErr("Backend indisponibil / eroare rețea.");
    } finally {
      setBusyApptId(null);
    }
  }

  return (
    <div style={styles.page}>
      {/* TOP BAR */}
      <div style={styles.topBar}>
        <button onClick={handleBack} style={styles.backBtn}>
          ← Pacienți
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      {/* HERO */}
      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>Detalii pacient</h1>
        <p style={styles.heroText}>Vizualizează contul și gestionează programările.</p>
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
              <button onClick={loadPatientAndAppointments} style={styles.refreshBtn} disabled={loading}>
                Reîncarcă
              </button>

              <button onClick={handleEdit} disabled={!profile} style={styles.editBtn}>
                Editează
              </button>

              <button
                onClick={handleDeletePatient}
                disabled={deleteBusy}
                style={{
                  ...styles.deleteBtn,
                  opacity: deleteBusy ? 0.7 : 1,
                  cursor: deleteBusy ? "not-allowed" : "pointer",
                }}
              >
                {deleteBusy ? "Se șterge..." : "Șterge pacient"}
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
              {"dateOfBirth" in profile && <Info label="Data nașterii" value={profile.dateOfBirth} />}
            </div>
          )}

          {/* APPOINTMENTS */}
          <div style={{ marginTop: 18 }}>
            <div style={styles.sectionTitleRow}>
              <h3 style={styles.sectionTitle}>Programări pacient</h3>
              <div style={styles.sub}>{loadingAppts ? "Se încarcă..." : `${appts.length} programări`}</div>
            </div>

            {loadingAppts && <div style={styles.infoNote}>Se încarcă programările...</div>}

            {!loadingAppts && appts.length === 0 && <div style={styles.infoNote}>Pacientul nu are programări.</div>}

            {!loadingAppts && appts.length > 0 && (
              <div style={styles.tableWrap}>
                <table style={styles.table}>
                  <thead>
                    <tr>
                      <th style={styles.th}>ID</th>
                      <th style={styles.th}>Data</th>
                      <th style={styles.th}>Doctor</th>
                      <th style={styles.th}>Status</th>
                      <th style={styles.th}>Descriere</th>
                      <th style={styles.th}>Acțiuni</th>
                    </tr>
                  </thead>

                  <tbody>
                    {appts.map((a) => {
                      const dt = getDateField(a);
                      const doctorName = a.doctor
                        ? `${a.doctor.firstName || ""} ${a.doctor.lastName || ""}`.trim()
                        : "-";

                      return (
                        <tr key={a.id}>
                          <td style={styles.tdMono}>{a.id}</td>
                          <td style={styles.td}>{formatDateTime(dt)}</td>

                          <td style={styles.td}>
                            {doctorName || "-"}
                            <div style={styles.tdSub}>{a.doctor?.specialization?.name || ""}</div>
                          </td>

                          <td style={styles.td}>
                            <StatusPill status={a.status} />
                          </td>

                          <td style={styles.td}>
                            <div style={styles.descCell}>{a.description || "-"}</div>
                          </td>

                          <td style={styles.td}>
                            <div style={styles.actionsInline}>
                              <select
                                value={String(a.status || "PENDING").toUpperCase()}
                                disabled={busyApptId === a.id}
                                onChange={(e) => updateAppointmentStatus(a.id, e.target.value)}
                                style={styles.select}
                              >
                                {STATUSES.map((s) => (
                                  <option key={s} value={s}>
                                    {s}
                                  </option>
                                ))}
                              </select>

                              <button
                                onClick={() => deleteAppointment(a.id)}
                                disabled={busyApptId === a.id}
                                style={styles.deleteSmallBtn}
                              >
                                {busyApptId === a.id ? "..." : "Șterge"}
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
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
    background: "linear-gradient(rgba(6,58,98,0.7), rgba(6,58,98,0.7)), url('/images/HomePage.jpg')",
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
    width: 1100,
    maxWidth: "100%",
    boxShadow: "0 10px 25px rgba(0,0,0,0.15)",
  },

  headerRow: { display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "center" },
  actionsRow: { display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap", justifyContent: "flex-end" },

  name: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 28, lineHeight: 1.2 },
  sub: { marginTop: 6, color: "#556", fontSize: 14 },

  refreshBtn: {
    backgroundColor: "rgba(10, 77, 128, 0.1)",
    color: "#0a4d80",
    border: "1px solid rgba(10, 77, 128, 0.18)",
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

  error: {
    marginTop: 14,
    background: "#ffe8e8",
    padding: 10,
    borderRadius: 12,
    color: "#7a1b1b",
    border: "1px solid #ffb3b3",
  },

  grid: { marginTop: 16, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 },
  infoBox: { border: "1px solid #e6eef6", borderRadius: 12, padding: 12, background: "#fbfdff" },
  infoLabel: { color: "#0a4d80", fontWeight: 700, fontSize: 13 },
  infoValue: { marginTop: 6, color: "#334", fontSize: 15 },

  infoNote: {
    marginTop: 12,
    padding: 12,
    borderRadius: 12,
    background: "#f5f9fc",
    border: "1px solid #e6eef6",
    color: "#445",
    fontSize: 14,
    lineHeight: 1.45,
  },

  sectionTitleRow: {
    marginTop: 8,
    display: "flex",
    justifyContent: "space-between",
    gap: 12,
    flexWrap: "wrap",
    alignItems: "baseline",
  },
  sectionTitle: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 20 },

  tableWrap: { marginTop: 12, overflowX: "auto", border: "1px solid #e6eef6", borderRadius: 14 },
  table: { width: "100%", borderCollapse: "collapse", minWidth: 980 },
  th: {
    textAlign: "left",
    padding: 12,
    fontSize: 13,
    color: "#0a4d80",
    background: "#f3f8fd",
    borderBottom: "1px solid #e6eef6",
    position: "sticky",
    top: 0,
    zIndex: 1,
  },
  td: { padding: 12, borderBottom: "1px solid #eef4fb", verticalAlign: "top", fontSize: 14, color: "#223" },
  tdMono: {
    padding: 12,
    borderBottom: "1px solid #eef4fb",
    verticalAlign: "top",
    fontSize: 13,
    fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
    color: "#223",
  },
  tdSub: { marginTop: 4, fontSize: 12, color: "#667" },

  actionsInline: { display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center" },
  select: {
    height: 38,
    padding: "0 10px",
    borderRadius: 10,
    border: "1px solid #d7e3ee",
    background: "#fbfdff",
    outline: "none",
    fontWeight: 600,
  },
  deleteSmallBtn: {
    backgroundColor: "#b42318",
    color: "white",
    border: "none",
    padding: "8px 10px",
    borderRadius: 10,
    fontWeight: 700,
    cursor: "pointer",
  },
  descCell: { maxWidth: 420, whiteSpace: "pre-wrap", lineHeight: 1.4 },

  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
