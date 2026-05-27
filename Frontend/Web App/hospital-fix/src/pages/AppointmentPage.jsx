import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

const API_BASE = "http://localhost:8080";

function formatDateTime(dt) {
  if (!dt) return "-";
  const s = String(dt);
  const date = s.slice(0, 10);
  const time = s.slice(11, 16);
  return `${date} ${time}`;
}

function getDateField(a) {
  // suppotrs multiple variants
  return (
    a?.appointmentDatetime ||
    a?.startAt ||
    a?.appointmentDateTime ||
    a?.appointment_datetime ||
    null
  );
}

function StatusPill({ status }) {
  const st = (status || "").toUpperCase();

  const style =
    st === "ACCEPTED"
      ? {
          background: "rgba(10,168,98,0.12)",
          border: "1px solid rgba(10,168,98,0.35)",
          color: "#0b5a2a",
        }
      : st === "DENIED"
      ? {
          background: "rgba(180,35,24,0.12)",
          border: "1px solid rgba(180,35,24,0.35)",
          color: "#7a1b1b",
        }
      : st === "CANCELLED"
      ? {
          background: "rgba(10,77,128,0.10)",
          border: "1px solid rgba(10,77,128,0.25)",
          color: "#0a4d80",
        }
      : st === "FINISHED"
      ? {
          background: "rgba(15,23,42,0.10)",
          border: "1px solid rgba(15,23,42,0.18)",
          color: "#0f172a",
        }
      : {
          background: "rgba(245,158,11,0.14)",
          border: "1px solid rgba(245,158,11,0.35)",
          color: "#7a4b00",
        };

  return (
    <span
      style={{
        display: "inline-block",
        padding: "5px 10px",
        borderRadius: 999,
        fontSize: 12,
        fontWeight: 800,
        letterSpacing: 0.2,
        ...style,
      }}
    >
      {st || "-"}
    </span>
  );
}

export default function AppointmentsPage() {
  const navigate = useNavigate();

  const [role, setRole] = useState(""); // "patient" / "doctor" / "admin"
  const [userId, setUserId] = useState(null);

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    const roleRaw = localStorage.getItem("role");
    const userRaw = localStorage.getItem("user");

    if (!roleRaw || !userRaw) {
      navigate("/login", { replace: true, state: { from: "/appointments" } });
      return;
    }

    let parsed = null;
    try {
      parsed = JSON.parse(userRaw);
    } catch {
      parsed = null;
    }

    const id = parsed?.id ?? null;
    const r = (roleRaw || "").toLowerCase();

    if (!id || !r) {
      navigate("/login", { replace: true, state: { from: "/appointments" } });
      return;
    }

    // page only available for patient or doctor
    if (r !== "patient" && r !== "doctor") {
      setErr("Pagina este disponibilă doar pentru pacient / doctor.");
      setLoading(false);
      return;
    }

    setRole(r);
    setUserId(id);
  }, [navigate]);

  async function load() {
    if (!role || !userId) return;

    setLoading(true);
    setErr("");

    const endpoint =
      role === "patient"
        ? `/api/appointments/patient/${userId}`
        : `/api/appointments/doctor/${userId}`;

    try {
      const res = await fetch(API_BASE + endpoint);
      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare backend: ${res.status}`);
        setItems([]);
        return;
      }
      const data = await res.json();
      setItems(Array.isArray(data) ? data : []);
    } catch {
      setErr("Backend indisponibil / eroare rețea.");
      setItems([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [role, userId]);

  async function updateStatus(apptId, status) {
    setBusyId(apptId);
    setErr("");

    try {
      const res = await fetch(`${API_BASE}/api/appointments/${apptId}/status`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status }),
      });

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare update status (${res.status})`);
        return;
      }

      await load();
    } catch {
      setErr("Backend indisponibil / eroare rețea.");
    } finally {
      setBusyId(null);
    }
  }

  async function cancelAppointment(apptId) {
    const ok = window.confirm("Sigur vrei să anulezi această programare?");
    if (!ok) return;

    setBusyId(apptId);
    setErr("");

    try {
      const res = await fetch(`${API_BASE}/api/appointments/${apptId}/cancel`, {
        method: "PUT",
      });

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare anulare (${res.status})`);
        return;
      }

      await load();
    } catch {
      setErr("Backend indisponibil / eroare rețea.");
    } finally {
      setBusyId(null);
    }
  }

  async function deleteAppointment(apptId) {
    const ok = window.confirm("Sigur vrei să ștergi această programare?");
    if (!ok) return;

    setBusyId(apptId);
    setErr("");

    try {
      const res = await fetch(`${API_BASE}/api/appointments/${apptId}`, {
        method: "DELETE",
      });

      if (!(res.status === 204 || res.ok)) {
        const txt = await res.text().catch(() => "");
        setErr(txt || `Eroare ștergere (${res.status})`);
        return;
      }

      await load();
    } catch {
      setErr("Backend indisponibil / eroare rețea.");
    } finally {
      setBusyId(null);
    }
  }

  const title = useMemo(() => {
    return role === "doctor" ? "Programările mele" : "Programările mele";
  }, [role]);

  return (
    <div style={styles.page}>
      {/* TOP BAR */}
      <div style={styles.topBar}>
        <button onClick={() => navigate("/account")} style={styles.backBtn}>
          ← Cont
        </button>
        <span>Contact: 0740 123 456</span>
      </div>

      {/* HERO */}
      <section style={styles.hero}>
        <h1 style={styles.heroTitle}>{title}</h1>
        <p style={styles.heroText}>
          {role === "doctor"
            ? "Acceptă sau respinge solicitările primite."
            : "Vezi statusul programărilor tale și gestionează-le."}
        </p>
      </section>

      <div style={styles.cardWrapper}>
        <div style={styles.card}>
          <div style={styles.headerRow}>
            <div>
              <h2 style={styles.cardTitle}>Listă programări</h2>
              <div style={styles.sub}>
                {loading ? "Se încarcă..." : `${items.length} programări`}
              </div>
            </div>

            <button onClick={load} style={styles.refreshBtn} disabled={loading}>
              Reîncarcă
            </button>
          </div>

          {err && <div style={styles.error}>{err}</div>}

          {!loading && !err && items.length === 0 && (
            <div style={styles.infoNote}>Nu există programări.</div>
          )}

          {!loading && items.length > 0 && (
            <div style={styles.tableWrap}>
              <table style={styles.table}>
                <thead>
                  <tr>
                    <th style={styles.th}>ID</th>
                    <th style={styles.th}>Data</th>
                    <th style={styles.th}>
                      {role === "doctor" ? "Pacient" : "Doctor"}
                    </th>
                    <th style={styles.th}>Status</th>
                    <th style={styles.th}>Descriere</th>
                    <th style={styles.th}>Acțiuni</th>
                  </tr>
                </thead>

                <tbody>
                  {items.map((a) => {
                    const dt = getDateField(a);
                    const st = String(a.status || "").toUpperCase();

                    const patientName = a.patient
                      ? `${a.patient.firstName || ""} ${a.patient.lastName || ""}`.trim()
                      : "-";

                    const doctorName = a.doctor
                      ? `${a.doctor.firstName || ""} ${a.doctor.lastName || ""}`.trim()
                      : "-";

                    // patient: cancel if pending/accepted
                    const canCancel = st === "PENDING" || st === "ACCEPTED";
                    // pacient: delete if denied/cancelled/finished
                    const canDelete =
                      st === "DENIED" || st === "CANCELLED" || st === "FINISHED";

                    const isPending = st === "PENDING";

                    return (
                      <tr key={a.id}>
                        <td style={styles.tdMono}>{a.id}</td>
                        <td style={styles.td}>{formatDateTime(dt)}</td>

                        {role === "doctor" ? (
                          <td style={styles.td}>{patientName || "-"}</td>
                        ) : (
                          <td style={styles.td}>
                            {doctorName || "-"}
                            <div style={styles.tdSub}>
                              {a.doctor?.specialization?.name || ""}
                            </div>
                          </td>
                        )}

                        <td style={styles.td}>
                          <StatusPill status={a.status} />
                        </td>

                        <td style={styles.td}>
                          <div style={styles.descCell}>{a.description || "-"}</div>
                        </td>

                        <td style={styles.td}>
                          <div style={styles.actions}>
                            {/* DOCTOR: Accept/Deny on PENDING */}
                            {role === "doctor" && isPending && (
                              <>
                                <button
                                  style={styles.acceptBtn}
                                  disabled={busyId === a.id}
                                  onClick={() => updateStatus(a.id, "ACCEPTED")}
                                >
                                  Acceptă
                                </button>
                                <button
                                  style={styles.denyBtn}
                                  disabled={busyId === a.id}
                                  onClick={() => updateStatus(a.id, "DENIED")}
                                >
                                  Respinge
                                </button>
                              </>
                            )}

                            {/* DOCTOR: otherwise has no actions */}
                            {role === "doctor" && !isPending && (
                              <span style={styles.muted}>—</span>
                            )}

                            {/* PATIENT: Cancel / Delete */}
                            {role === "patient" && canCancel && (
                              <button
                                style={styles.cancelBtn}
                                disabled={busyId === a.id}
                                onClick={() => cancelAppointment(a.id)}
                              >
                                Anulează
                              </button>
                            )}

                            {role === "patient" && canDelete && (
                              <button
                                style={styles.deleteBtn2}
                                disabled={busyId === a.id}
                                onClick={() => deleteAppointment(a.id)}
                              >
                                Șterge
                              </button>
                            )}

                            {role === "patient" && !canCancel && !canDelete && (
                              <span style={styles.muted}>—</span>
                            )}
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
    padding: 24,
    borderRadius: 18,
    width: 1100,
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
  cardTitle: { margin: 0, color: "#0a4d80", fontWeight: 600, fontSize: 24 },
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

  tableWrap: {
    marginTop: 16,
    overflowX: "auto",
    border: "1px solid #e6eef6",
    borderRadius: 14,
  },
  table: {
    width: "100%",
    borderCollapse: "collapse",
    minWidth: 980,
  },
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
  td: {
    padding: 12,
    borderBottom: "1px solid #eef4fb",
    verticalAlign: "top",
    fontSize: 14,
    color: "#223",
  },
  tdMono: {
    padding: 12,
    borderBottom: "1px solid #eef4fb",
    verticalAlign: "top",
    fontSize: 13,
    fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
    color: "#223",
  },
  tdSub: { marginTop: 4, fontSize: 12, color: "#667" },

  actions: { display: "flex", gap: 8, flexWrap: "wrap" },

  acceptBtn: {
    backgroundColor: "#0aa862",
    color: "white",
    border: "none",
    padding: "8px 10px",
    borderRadius: 10,
    fontWeight: 600,
    cursor: "pointer",
  },
  denyBtn: {
    backgroundColor: "#b42318",
    color: "white",
    border: "none",
    padding: "8px 10px",
    borderRadius: 10,
    fontWeight: 600,
    cursor: "pointer",
  },

  cancelBtn: {
    backgroundColor: "rgba(10, 77, 128, 0.1)",
    color: "#0a4d80",
    border: "1px solid rgba(10, 77, 128, 0.25)",
    padding: "8px 10px",
    borderRadius: 10,
    fontWeight: 600,
    cursor: "pointer",
  },
  deleteBtn2: {
    backgroundColor: "#b42318",
    color: "white",
    border: "none",
    padding: "8px 10px",
    borderRadius: 10,
    fontWeight: 600,
    cursor: "pointer",
  },

  muted: { color: "#889" },
  descCell: { maxWidth: 420, whiteSpace: "pre-wrap", lineHeight: 1.4 },

  footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
