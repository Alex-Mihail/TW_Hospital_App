import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

const API_BASE = "http://localhost:8080";

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

export default function AdminDoctorDetailsPage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [profile, setProfile] = useState(null);
  const [appointments, setAppointments] = useState([]);

  const [loading, setLoading] = useState(true);
  const [loadingAppts, setLoadingAppts] = useState(true);
  const [err, setErr] = useState("");

  const [busyApptId, setBusyApptId] = useState(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  async function loadDoctor() {
    setErr("");
    try {
      const res = await fetch(`${API_BASE}/api/admin/doctors/${id}`);
      if (res.status === 404) {
        setErr("Doctorul nu a fost găsit (404).");
        setProfile(null);
        return;
      }
      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare backend: ${res.status}`);
        setProfile(null);
        return;
      }
      const data = await res.json();
      setProfile(data);
    } catch {
      setErr("Backend indisponibil / CORS / eroare rețea.");
      setProfile(null);
    }
  }

  async function loadAppointments() {
    setLoadingAppts(true);
    setErr("");
    try {
      const res = await fetch(`${API_BASE}/api/admin/doctors/${id}/appointments`);
      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare încărcare programări (${res.status})`);
        setAppointments([]);
        return;
      }
      const data = await res.json();
      setAppointments(Array.isArray(data) ? data : []);
    } catch {
      setErr("Backend indisponibil / eroare rețea la programări.");
      setAppointments([]);
    } finally {
      setLoadingAppts(false);
    }
  }

  async function loadAll() {
    setLoading(true);
    await loadDoctor();
    await loadAppointments();
    setLoading(false);
  }

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const displayName = useMemo(() => {
    if (!profile) return `Doctor #${id}`;
    const full = `${profile.firstName || ""} ${profile.lastName || ""}`.trim();
    return full || profile.username || `Doctor #${id}`;
  }, [profile, id]);

  async function handleDeleteDoctor() {
    const ok = window.confirm("Sigur vrei să ștergi acest doctor? Se vor șterge și programările lui.");
    if (!ok) return;

    setDeleteBusy(true);
    setErr("");

    try {
      const res = await fetch(`${API_BASE}/api/admin/doctors/${id}`, { method: "DELETE" });

      if (res.status === 404) {
        setErr("Doctorul nu a fost găsit (404).");
        return;
      }

      if (!(res.status === 204 || res.ok)) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare ștergere (${res.status})`);
        return;
      }

      navigate("/admin/doctors", { replace: true });
    } catch {
      setErr("Backend indisponibil / eroare rețea la ștergere.");
    } finally {
      setDeleteBusy(false);
    }
  }

  async function updateStatus(apptId, status) {
    setBusyApptId(apptId);
    setErr("");

    try {
      const res = await fetch(`${API_BASE}/api/admin/appointments/${apptId}/status`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status }),
      });

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare update status (${res.status})`);
        return;
      }

      await loadAppointments();
    } catch {
      setErr("Backend indisponibil / eroare rețea la update status.");
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
      const res = await fetch(`${API_BASE}/api/admin/appointments/${apptId}`, { method: "DELETE" });

      if (!(res.status === 204 || res.ok)) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare ștergere programare (${res.status})`);
        return;
      }

      await loadAppointments();
    } catch {
      setErr("Backend indisponibil / eroare rețea la ștergere programare.");
    } finally {
      setBusyApptId(null);
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
        <h1 style={styles.heroTitle}>Detalii doctor</h1>
        <p style={styles.heroText}>Vezi contul + gestionează programările.</p>
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
              <button onClick={loadAll} style={styles.secondaryBtn} disabled={loading}>
                Reîncarcă
              </button>

              <button
                onClick={() => navigate(`/admin/doctors/${id}/edit`, { state: { profile } })}
                style={styles.editBtn}
                disabled={!profile}
              >
                Editează
              </button>

              <button
                onClick={handleDeleteDoctor}
                disabled={deleteBusy}
                style={{
                  ...styles.deleteBtn,
                  opacity: deleteBusy ? 0.7 : 1,
                  cursor: deleteBusy ? "not-allowed" : "pointer",
                }}
              >
                {deleteBusy ? "Se șterge..." : "Șterge doctor"}
              </button>
            </div>
          </div>

          {err && <div style={styles.error}>{err}</div>}

          {/* PROFILE */}
          {!loading && profile && (
            <div style={styles.grid}>
              <Info label="ID" value={profile.id} />
              <Info label="Username" value={profile.username} />
              <Info label="Prenume" value={profile.firstName} />
              <Info label="Nume" value={profile.lastName} />
              <Info label="Email" value={profile.email} />
              <Info label="Specializare" value={profile.specialization?.name || "-"} />
              {"ratingAvg" in profile && <Info label="Rating" value={profile.ratingAvg} />}
            </div>
          )}

          {/* APPOINTMENTS */}
          <div style={{ marginTop: 18 }}>
            <div style={styles.sectionTitleRow}>
              <h3 style={styles.sectionTitle}>Programări doctor</h3>
              <div style={styles.sub}>
                {loadingAppts ? "Se încarcă..." : `${appointments.length} programări`}
              </div>
            </div>

            {!loadingAppts && appointments.length === 0 && (
              <div style={styles.infoNote}>Nu există programări pentru acest doctor.</div>
            )}

            {!loadingAppts && appointments.length > 0 && (
              <div style={styles.tableWrap}>
                <table style={styles.table}>
                  <thead>
                    <tr>
                      <th style={styles.th}>ID</th>
                      <th style={styles.th}>Data</th>
                      <th style={styles.th}>Pacient</th>
                      <th style={styles.th}>Status</th>
                      <th style={styles.th}>Descriere</th>
                      <th style={styles.th}>Acțiuni</th>
                    </tr>
                  </thead>

                  <tbody>
                    {appointments.map((a) => {
                      const dt = getDateField(a);
                      const patientName = a.patient
                        ? `${a.patient.firstName || ""} ${a.patient.lastName || ""}`.trim()
                        : "-";

                      return (
                        <tr key={a.id}>
                          <td style={styles.tdMono}>{a.id}</td>
                          <td style={styles.td}>{formatDateTime(dt)}</td>
                          <td style={styles.td}>{patientName || "-"}</td>

                          <td style={styles.td}>
                            <StatusPill status={a.status} />
                          </td>

                          <td style={styles.td}>
                            <div style={styles.descCell}>{a.description || "-"}</div>
                          </td>

                          <td style={styles.td}>
                            <div style={styles.actionsInline}>
                              <select
                                value={(a.status || "").toUpperCase()}
                                onChange={(e) => updateStatus(a.id, e.target.value)}
                                disabled={busyApptId === a.id}
                                style={styles.select}
                              >
                                <option value="PENDING">PENDING</option>
                                <option value="ACCEPTED">ACCEPTED</option>
                                <option value="DENIED">DENIED</option>
                                <option value="CANCELLED">CANCELLED</option>
                                <option value="FINISHED">FINISHED</option>
                              </select>

                              <button
                                style={styles.deleteSmallBtn}
                                onClick={() => deleteAppointment(a.id)}
                                disabled={busyApptId === a.id}
                              >
                                Șterge
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
    width: 1100,
    maxWidth: "100%",
    boxShadow: "0 10px 25px rgba(0,0,0,0.15)",
  },
  headerRow: { display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "center" },
  actionsRow: { display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap", justifyContent: "flex-end" },

  name: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 28, lineHeight: 1.2 },
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

  sectionTitleRow: { marginTop: 8, display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "baseline" },
  sectionTitle: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 20 },

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
