import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import "./Consultation.css";
import BookingModal from "../components/BookingModal";

const API_BASE = "http://localhost:8080";

export default function Consultation() {
  const [specializations, setSpecializations] = useState([]);
  const [selectedSpec, setSelectedSpec] = useState("");
  const [doctors, setDoctors] = useState([]);
  const [search, setSearch] = useState("");

  const [user, setUser] = useState(null);

  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [modalOpen, setModalOpen] = useState(false);
  const [selectedDoctor, setSelectedDoctor] = useState(null);

  const allDoctorsCacheRef = useRef(null);
  const [loadingGlobal, setLoadingGlobal] = useState(false);

  // reads user from localStorage
  useEffect(() => {
    const raw = localStorage.getItem("user");
    if (raw) {
      try {
        setUser(JSON.parse(raw));
      } catch {
        setUser(null);
      }
    }
  }, []);

  const displayName = user ? `${user.firstName} ${user.lastName}` : "";

  useEffect(() => {
    fetch(`${API_BASE}/api/specializations`)
      .then((res) => res.json())
      .then((data) => {
        const list = Array.isArray(data) ? data : [];
        const sorted = [...list].sort((a, b) =>
          String(a?.name ?? "").localeCompare(String(b?.name ?? ""), "ro", {
            sensitivity: "base",
          })
        );
        setSpecializations(sorted);
      })
      .catch(console.error);
  }, []);

  const loadDoctorsBySpec = (spec) => {
    if (!spec) {
      setDoctors([]);
      return;
    }

    fetch(
      `${API_BASE}/api/doctor/by-specialization?specialization=${encodeURIComponent(
        spec
      )}`
    )
      .then((res) => res.json())
      .then((data) => setDoctors(Array.isArray(data) ? data : []))
      .catch(console.error);
  };

  async function loadAllDoctorsOnce() {
    if (allDoctorsCacheRef.current) return allDoctorsCacheRef.current;
    if (!specializations.length) return [];

    setLoadingGlobal(true);
    try {
      const results = await Promise.all(
        specializations.map(async (s) => {
          const res = await fetch(
            `${API_BASE}/api/doctor/by-specialization?specialization=${encodeURIComponent(
              s.name
            )}`
          );
          if (!res.ok) return [];
          const data = await res.json();
          return Array.isArray(data) ? data : [];
        })
      );

      const flat = results.flat();
      const map = new Map();
      for (const d of flat) {
        if (d?.id != null) map.set(d.id, d);
      }
      const unique = Array.from(map.values());
      allDoctorsCacheRef.current = unique;
      return unique;
    } catch (e) {
      console.error(e);
      return [];
    } finally {
      setLoadingGlobal(false);
    }
  }

  // interprets /consultation?search=...
  useEffect(() => {
    const q = (searchParams.get("search") || "").trim();
    if (!q) return;
    if (!specializations.length) return;

    const qLower = q.toLowerCase();

    const specMatch = specializations.find((s) =>
      String(s.name || "").toLowerCase().includes(qLower)
    );

    if (specMatch) {
      setSelectedSpec(specMatch.name);
      setSearch("");
      loadDoctorsBySpec(specMatch.name);
      return;
    }

    // otherwise: global seearch
    setSelectedSpec("");
    setSearch(q);

    (async () => {
      const all = await loadAllDoctorsOnce();
      setDoctors(all);
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [specializations, searchParams]);

  const filteredDoctors = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return doctors;

    return doctors.filter((d) => {
      const fullName = `${d.firstName || ""} ${d.lastName || ""}`.toLowerCase();
      const specName = String(d.specialization?.name || "").toLowerCase();
      return fullName.includes(q) || specName.includes(q);
    });
  }, [doctors, search]);

  function openBookingModal(doctor) {
    setSelectedDoctor(doctor);
    setModalOpen(true);
  }

  function closeModal() {
    setModalOpen(false);
    setSelectedDoctor(null);
  }

  async function applySearchSubmit() {
    const trimmed = search.trim();
    if (!trimmed) {
      setSearchParams({}, { replace: true });
      return;
    }

    setSearchParams({ search: trimmed }, { replace: true });

    if (!selectedSpec) {
      const all = await loadAllDoctorsOnce();
      setDoctors(all);
    }
  }

  return (
    <div className="consult-page">
      {/* TOP BAR */}
      <div
        style={{
          width: "100vw",
          backgroundColor: "#063a62",
          color: "white",
          padding: "10px 20px",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          fontSize: "14px",
          boxSizing: "border-box",
        }}
      >
        {/* STÂNGA – Acasă */}
        <button
          onClick={() => navigate("/")}
          style={{
            backgroundColor: "transparent",
            color: "white",
            border: "1px solid rgba(255,255,255,0.6)",
            padding: "6px 14px",
            borderRadius: "8px",
            cursor: "pointer",
            display: "flex",
            alignItems: "center",
            gap: "6px",
          }}
          title="Acasă"
        >
          ← Acasă
        </button>

        <div style={{ display: "flex", alignItems: "center", gap: "20px" }}>
          {/* Contact */}
          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path
                d="M22 16.92V20a2 2 0 0 1-2.18 2
           A19.79 19.79 0 0 1 2 4.18
           2 2 0 0 1 4 2h3.09
           a2 2 0 0 1 2 1.72
           c.12.81.3 1.6.57 2.36
           a2 2 0 0 1-.45 2.11L8.91 9.91
           a16 16 0 0 0 6.18 6.18l1.72-1.72
           a2 2 0 0 1 2.11-.45
           c.76.27 1.55.45 2.36.57
           A2 2 0 0 1 22 16.92z"
                fill="white"
              />
            </svg>
            <span>Contact: 0740 123 456</span>
          </div>

          {/* Contul meu */}
          <button
            onClick={() => navigate(user ? "/account" : "/login")}
            style={{
              backgroundColor: "transparent",
              color: "white",
              border: "1px solid rgba(255,255,255,0.6)",
              padding: "6px 14px",
              borderRadius: "8px",
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              gap: "8px",
              maxWidth: "220px",
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis",
            }}
            title={user ? displayName : "Contul meu"}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="7" r="4" stroke="white" strokeWidth="2" />
              <path
                d="M5.5 21a6.5 6.5 0 0 1 13 0"
                stroke="white"
                strokeWidth="2"
                fill="none"
              />
            </svg>
            {user ? displayName : "Contul meu"}
          </button>
        </div>
      </div>

      {/* HERO */}
      <header className="consult-hero">
        <div className="consult-hero__inner">
          <h1 className="consult-hero__title">Consultații Medicale</h1>
          <p className="consult-hero__subtitle">
            Alege specializarea și găsește medicul potrivit.
          </p>
        </div>
      </header>

      <main className="consult-container">
        {/* FILTERS */}
        <section className="filters">
          <div className="filters__head">
            <h2 className="filters__title">Caută rapid</h2>
            <p className="filters__desc">
              Poți selecta o specializare sau poți căuta global după nume / specializare.
            </p>
          </div>

          <div className="filters__grid">
            <div className="field">
              <label className="field__label">Specializare</label>
              <select
                className="field__control"
                value={selectedSpec}
                onChange={(e) => {
                  const val = e.target.value;
                  setSelectedSpec(val);
                  loadDoctorsBySpec(val);
                  setSearch("");
                  setSearchParams({}, { replace: true });
                }}
              >
                <option value="">(Căutare globală)</option>
                {specializations.map((spec) => (
                  <option key={spec.id} value={spec.name}>
                    {spec.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Search Enter + buton */}
            <form
              className="field"
              onSubmit={(e) => {
                e.preventDefault();
                applySearchSubmit();
              }}
            >
              <label className="field__label">Caută medic / specializare</label>

              <div style={{ display: "flex", gap: "10px" }}>
                <input
                  className="field__control"
                  type="text"
                  placeholder="Ex: Popescu / Cardiologie"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  style={{ flex: 1 }}
                />

                <button
                  type="submit"
                  className="btn btn--secondary"
                  style={{ padding: "0 18px" }}
                >
                  Caută
                </button>
              </div>
            </form>
          </div>

          {loadingGlobal && (
            <p className="hint-state" style={{ marginTop: 10 }}>
              Se încarcă lista globală de medici...
            </p>
          )}
        </section>

        {/* GRID */}
        <section className="doctors">
          <div className="doctors__grid">
            {filteredDoctors.map((doctor) => (
              <article key={doctor.id} className="doctor-card fade-in">
                <div className="doctor-card__top">
                  <div className="doctor-card__name-wrap">
                    <h3 className="doctor-card__name">
                      {doctor.firstName} {doctor.lastName}
                    </h3>
                  </div>

                  <span className="doctor-card__chip">
                    {doctor.specialization?.name || "Specializare"}
                  </span>
                </div>

                <div className="doctor-card__actions">
                  <button
                    className="btn btn--secondary"
                    onClick={() => navigate(`/doctor/${doctor.id}`, { state: { doctor } })}
                  >
                    Detalii
                  </button>

                  <button className="btn btn--primary" onClick={() => openBookingModal(doctor)}>
                    Programează-te
                  </button>
                </div>
              </article>
            ))}
          </div>

          {selectedSpec && filteredDoctors.length === 0 && (
            <p className="empty-state">Nu există doctori pentru această specializare.</p>
          )}

          {!selectedSpec && !search.trim() && (
            <p className="hint-state">
              Selectează o specializare sau caută global după nume / specializare.
            </p>
          )}

          {!selectedSpec && search.trim() && filteredDoctors.length === 0 && (
            <p className="empty-state">Nu am găsit rezultate pentru „{search}”.</p>
          )}
        </section>
      </main>

      {/* MODAL */}
      <BookingModal open={modalOpen} doctor={selectedDoctor} onClose={closeModal} />
    </div>
  );
}
