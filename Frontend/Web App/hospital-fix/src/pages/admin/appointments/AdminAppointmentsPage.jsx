import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

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

const STATUSES = ["PENDING", "ACCEPTED", "DENIED", "CANCELLED", "FINISHED"];

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
                fontWeight: 700,
                letterSpacing: 0.2,
                ...style,
            }}
        >
            {st || "-"}
        </span>
    );
}

export default function AdminAppointmentsPage() {
    const navigate = useNavigate();
    const stored = getStoredSession();
    const role = stored.role;

    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");

    const [busyId, setBusyId] = useState(null);
    const [search, setSearch] = useState("");

    async function load() {
        setLoading(true);
        setErr("");

        try {
            const res = await fetch(`${API_BASE}/api/admin/appointments`, { headers: authHeaders() });

            if (res.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("role");
                localStorage.removeItem("user");
                navigate("/login", { replace: true, state: { from: "/admin/appointments" } });
                return;
            }

            if (!res.ok) {
                const txt = await res.text().catch(() => "");
                setErr(txt || `Eroare backend: ${res.status}`);
                setItems([]);
                return;
            }

            const data = await res.json();
            setItems(Array.isArray(data) ? data : []);
        } catch {
            setErr("Backend indisponibil / CORS / eroare rețea.");
            setItems([]);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        if (role !== "admin") {
            navigate("/login", { replace: true, state: { from: "/admin/appointments" } });
            return;
        }
        load();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [role, navigate]);

    async function updateStatus(appt, status) {
        if (!appt?.id) return;

        setBusyId(appt.id);
        setErr("");

        try {
            const res = await fetch(
                `${API_BASE}/api/admin/appointments/${appt.id}/status`,
                {
                    method: "PUT",
                    headers: authHeaders(true),
                    body: JSON.stringify({ status }), // just status
                }
            );

            if (res.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("role");
                localStorage.removeItem("user");
                navigate("/login", {
                    replace: true,
                    state: { from: "/admin/appointments" },
                });
                return;
            }

            if (!res.ok) {
                const txt = await res.text().catch(() => "");
                setErr(txt || `Eroare update status (${res.status})`);
                return;
            }

            const updated = await res.json();

            // update local state
            setAppts((xs) => xs.map((x) => (x.id === updated.id ? updated : x)));
        } catch (e) {
            console.error(e);
            setErr("Backend indisponibil / eroare rețea.");
        }
    }

    async function deleteAppointment(apptId) {
        const ok = window.confirm("Sigur vrei să ștergi această programare?");
        if (!ok) return;

        setBusyId(apptId);
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
                navigate("/login", { replace: true, state: { from: "/admin/appointments" } });
                return;
            }

            if (!(res.status === 204 || res.ok)) {
                const txt = await res.text().catch(() => "");
                setErr(txt || `Eroare ștergere (${res.status})`);
                return;
            }

            setItems((xs) => xs.filter((x) => x.id !== apptId));
        } catch {
            setErr("Backend indisponibil / eroare rețea.");
        } finally {
            setBusyId(null);
        }
    }

    const filtered = useMemo(() => {
        const q = search.trim().toLowerCase();
        if (!q) return items;

        return items.filter((a) => {
            const patientName = a.patient ? `${a.patient.firstName || ""} ${a.patient.lastName || ""}`.trim() : "";
            const doctorName = a.doctor ? `${a.doctor.firstName || ""} ${a.doctor.lastName || ""}`.trim() : "";
            const spec = a.doctor?.specialization?.name || "";
            const dt = formatDateTime(getDateField(a));
            const st = String(a.status || "");
            const desc = String(a.description || "");

            return (
                String(a.id).includes(q) ||
                patientName.toLowerCase().includes(q) ||
                doctorName.toLowerCase().includes(q) ||
                spec.toLowerCase().includes(q) ||
                dt.toLowerCase().includes(q) ||
                st.toLowerCase().includes(q) ||
                desc.toLowerCase().includes(q)
            );
        });
    }, [items, search]);

    return (
        <div style={styles.page}>
            {/* TOP BAR */}
            <div style={styles.topBar}>
                <button onClick={() => navigate("/admin")} style={styles.backBtn}>
                    ← Admin
                </button>
                <span>Contact: 0740 123 456</span>
            </div>

            {/* HERO */}
            <section style={styles.hero}>
                <h1 style={styles.heroTitle}>Programări</h1>
                <p style={styles.heroText}>Listă globală cu programări. Modifică status sau șterge.</p>
            </section>

            <div style={styles.cardWrapper}>
                <div style={styles.card}>
                    <div style={styles.headerRow}>
                        <div>
                            <h2 style={styles.cardTitle}>Listă programări</h2>
                            <div style={styles.sub}>{loading ? "Se încarcă..." : `${filtered.length} programări`}</div>
                        </div>

                        <div style={styles.actionsRow}>
                            <input
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                placeholder="Caută după pacient / doctor / specializare / status / dată..."
                                style={styles.search}
                            />

                            <button onClick={load} style={styles.refreshBtn} disabled={loading}>
                                Reîncarcă
                            </button>
                        </div>
                    </div>

                    {err && <div style={styles.error}>{err}</div>}

                    {!loading && !err && filtered.length === 0 && (
                        <div style={styles.infoNote}>Nu există programări.</div>
                    )}

                    {!loading && filtered.length > 0 && (
                        <div style={styles.tableWrap}>
                            <table style={styles.table}>
                                <thead>
                                    <tr>
                                        <th style={styles.th}>ID</th>
                                        <th style={styles.th}>Data</th>
                                        <th style={styles.th}>Pacient</th>
                                        <th style={styles.th}>Doctor</th>
                                        <th style={styles.th}>Status</th>
                                        <th style={styles.th}>Descriere</th>
                                        <th style={styles.th}>Acțiuni</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {filtered.map((a) => {
                                        const dt = getDateField(a);
                                        const patientName = a.patient
                                            ? `${a.patient.firstName || ""} ${a.patient.lastName || ""}`.trim()
                                            : "-";
                                        const doctorName = a.doctor
                                            ? `${a.doctor.firstName || ""} ${a.doctor.lastName || ""}`.trim()
                                            : "-";
                                        const spec = a.doctor?.specialization?.name || "";

                                        return (
                                            <tr key={a.id}>
                                                <td style={styles.tdMono}>{a.id}</td>
                                                <td style={styles.td}>{formatDateTime(dt)}</td>

                                                <td style={styles.td}>
                                                    {patientName || "-"}
                                                    <div style={styles.tdSub}>ID: {a.patient?.id ?? "-"}</div>
                                                </td>

                                                <td style={styles.td}>
                                                    {doctorName || "-"}
                                                    <div style={styles.tdSub}>
                                                        {spec ? `${spec} • ` : ""}
                                                        ID: {a.doctor?.id ?? "-"}
                                                    </div>
                                                </td>

                                                <td style={styles.td}>
                                                    <div style={{ display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap" }}>
                                                        <StatusPill status={a.status} />
                                                    </div>
                                                </td>

                                                <td style={styles.td}>
                                                    <div style={styles.descCell}>{a.description || "-"}</div>
                                                </td>

                                                <td style={styles.td}>
                                                    <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                                                        <select
                                                            value={String(a.status || "PENDING").toUpperCase()}
                                                            disabled={busyId === a.id}
                                                            onChange={(e) => updateStatus(a, e.target.value)}
                                                            style={styles.select}
                                                        >
                                                            {STATUSES.map((s) => (
                                                                <option key={s} value={s}>{s}</option>
                                                            ))}
                                                        </select>

                                                        <button
                                                            onClick={() => deleteAppointment(a.id)}
                                                            disabled={busyId === a.id}
                                                            style={{
                                                                ...styles.deleteBtn2,
                                                                opacity: busyId === a.id ? 0.7 : 1,
                                                                cursor: busyId === a.id ? "not-allowed" : "pointer",
                                                            }}
                                                        >
                                                            {busyId === a.id ? "..." : "Șterge"}
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

    cardWrapper: { display: "flex", justifyContent: "center", marginTop: -40, padding: "0 20px" },
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

    actionsRow: { display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap", justifyContent: "flex-end" },

    search: {
        width: 360,
        maxWidth: "90vw",
        padding: 12,
        borderRadius: 12,
        border: "1px solid #d7e3ee",
        backgroundColor: "#fbfdff",
        fontSize: 14,
        outline: "none",
        boxSizing: "border-box",
    },

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

    tableWrap: { marginTop: 16, overflowX: "auto", border: "1px solid #e6eef6", borderRadius: 14 },
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

    select: {
        padding: 10,
        borderRadius: 12,
        border: "1px solid #d7e3ee",
        backgroundColor: "#fbfdff",
        fontSize: 14,
        outline: "none",
    },

    deleteBtn2: {
        backgroundColor: "#b42318",
        color: "white",
        border: "none",
        padding: "8px 10px",
        borderRadius: 10,
        fontWeight: 700,
    },

    descCell: { maxWidth: 360, whiteSpace: "pre-wrap", lineHeight: 1.4 },

    footer: { textAlign: "center", padding: 20, color: "#0a4d80" },
};
