import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

const API_BASE = "http://localhost:8080";
const HOURS = Array.from({ length: 9 }, (_, i) => 8 + i); // 08..16
const BUFFER_MIN = 15; // if it's past 10:00, don't allow aapointment for 10:00

function pad2(n) {
  return String(n).padStart(2, "0");
}
function todayISO() {
  const d = new Date();
  const y = d.getFullYear();
  const m = pad2(d.getMonth() + 1);
  const day = pad2(d.getDate());
  return `${y}-${m}-${day}`;
}
function buildDateTime(dateStr, hour) {
  return `${dateStr}T${pad2(hour)}:00`;
}
function getLoggedPatientId() {
  try {
    const u = JSON.parse(localStorage.getItem("user") || "null");
    return u?.id ?? null;
  } catch {
    return null;
  }
}

/** --- date helpers --- */
function isWeekend(dateStr) {
  const d = new Date(`${dateStr}T00:00:00`);
  const day = d.getDay(); // 0=Sun, 6=Sat
  return day === 0 || day === 6;
}
function isPastDate(dateStr) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const d = new Date(`${dateStr}T00:00:00`);
  return d < today;
}
function isToday(dateStr) {
  return dateStr === todayISO();
}
function getNowHourForSlotsBufferMinutes(bufferMinutes = 0) {
  const now = new Date();
  const limit = new Date(now.getTime() + bufferMinutes * 60 * 1000);
  return limit.getHours();
}

export default function BookingModal({ open, doctor, onClose }) {
  const navigate = useNavigate();
  const location = useLocation();

  const [selectedDate, setSelectedDate] = useState(todayISO());
  const [busyHours, setBusyHours] = useState(new Set());
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [selectedHour, setSelectedHour] = useState(null);
  const [description, setDescription] = useState("");

  const [submitBusy, setSubmitBusy] = useState(false);
  const [modalErr, setModalErr] = useState("");
  const [modalOk, setModalOk] = useState("");

  // if invalid date selected, keep the last valid date
  const [lastValidDate, setLastValidDate] = useState(todayISO());

  async function fetchAvailability(doctorId, dateStr) {
    setLoadingSlots(true);
    setModalErr("");

    try {
      const res = await fetch(
        `${API_BASE}/api/appointments/availability?doctorId=${doctorId}&date=${dateStr}`
      );

      if (!res.ok) {
        setModalErr(`Nu pot încărca disponibilitatea (${res.status}).`);
        setBusyHours(new Set());
        return;
      }

      const appts = await res.json();
      const set = new Set();

      for (const a of appts) {
        const dt = a.appointmentDatetime || a.startAt;
        if (!dt) continue;
        const hour = Number(String(dt).slice(11, 13));
        if (!Number.isNaN(hour)) set.add(hour);
      }

      setBusyHours(set);
    } catch {
      setModalErr("Backend indisponibil / CORS / eroare rețea.");
      setBusyHours(new Set());
    } finally {
      setLoadingSlots(false);
    }
  }

  // when login is valid, reset and load slots
  useEffect(() => {
    if (!open || !doctor?.id) return;

    const role = (localStorage.getItem("role") || "").toLowerCase();
    const patientId = getLoggedPatientId();

    if (role !== "patient" || !patientId) {
      navigate("/login", {
        state: { from: location.pathname + location.search + location.hash },
      });
      return;
    }

    const d = todayISO();
    setSelectedDate(d);
    setLastValidDate(d);
    setSelectedHour(null);
    setDescription("");
    setModalErr("");
    setModalOk("");
    setBusyHours(new Set());

    fetchAvailability(doctor.id, d);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, doctor?.id, location.pathname, location.search, location.hash, navigate]);

  // visible hours depending on current hour
  const visibleHours = useMemo(() => {
    return HOURS.filter((h) => {
      if (!selectedDate) return true;
      if (!isToday(selectedDate)) return true;
      const nowHour = getNowHourForSlotsBufferMinutes(BUFFER_MIN);
      return h > nowHour;
    });
  }, [selectedDate]);

  async function submitAppointment() {
    setModalErr("");
    setModalOk("");

    const patientId = getLoggedPatientId();
    if (!patientId) {
      setModalErr("Nu ești autentificat ca pacient.");
      return;
    }
    if (!doctor?.id) {
      setModalErr("Doctor invalid.");
      return;
    }
    if (!selectedDate) {
      setModalErr("Alege o dată.");
      return;
    }

    // validations: past days + weekend + past hours from current day
    if (isPastDate(selectedDate)) {
      setModalErr("Nu poți selecta o dată din trecut.");
      return;
    }
    if (isWeekend(selectedDate)) {
      setModalErr("Nu poți face programări în weekend.");
      return;
    }

    if (selectedHour == null) {
      setModalErr("Alege o oră.");
      return;
    }

    if (isToday(selectedDate)) {
      const nowHour = getNowHourForSlotsBufferMinutes(BUFFER_MIN);
      if (selectedHour <= nowHour) {
        setModalErr("Pentru ziua de azi poți alege doar orele care urmează.");
        return;
      }
    }

    if (busyHours.has(selectedHour)) {
      setModalErr("Ora selectată este ocupată.");
      return;
    }

    const appointmentDatetime = buildDateTime(selectedDate, selectedHour);

    setSubmitBusy(true);
    try {
      const res = await fetch(`${API_BASE}/api/appointments`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          patientId,
          doctorId: doctor.id,
          appointmentDatetime,
          description: description.trim() || null,
        }),
      });

      if (res.status === 409) {
        setModalErr("Slot ocupat. Alege altă oră.");
        await fetchAvailability(doctor.id, selectedDate);
        return;
      }

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        setModalErr(txt || `Eroare la creare (${res.status})`);
        return;
      }

      setModalOk("Solicitarea a fost trimisă (PENDING).");
      await fetchAvailability(doctor.id, selectedDate);

      setTimeout(() => onClose?.(), 600);
    } catch {
      setModalErr("Backend indisponibil / eroare rețea.");
    } finally {
      setSubmitBusy(false);
    }
  }

  if (!open || !doctor) return null;

  return (
    <div className="booking-modal__overlay" onMouseDown={() => onClose?.()}>
      <div
        className="booking-modal"
        onMouseDown={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
      >
        <div className="booking-modal__head">
          <div>
            <h3 className="booking-modal__title">
              Programare la Dr. {doctor.firstName} {doctor.lastName}
            </h3>
            <div className="booking-modal__sub">
              {doctor.specialization?.name || "Specializare"}
            </div>
          </div>

          <button className="booking-modal__close" onClick={() => onClose?.()}>
            ✕
          </button>
        </div>

        <div className="booking-modal__body">
          <div className="booking-modal__grid">
            <div className="booking-panel">
              <div className="booking-panel__label">Data</div>

              <input
                className="booking-input"
                type="date"
                value={selectedDate}
                min={todayISO()} // blocks the past
                onChange={(e) => {
                  const d = e.target.value;

                  // do not allow for selecting past dates
                  if (isPastDate(d)) {
                    setModalErr("Nu poți selecta o dată din trecut.");
                    // come back to last valid date
                    setSelectedDate(lastValidDate);
                    return;
                  }

                  // do not allow for selecting weekends
                  if (isWeekend(d)) {
                    setModalErr("Weekend-ul este indisponibil. Alege o zi lucrătoare.");
                    // come back to last valid date
                    setSelectedDate(lastValidDate);
                    return;
                  }

                  setModalErr("");
                  setSelectedDate(d);
                  setLastValidDate(d);
                  setSelectedHour(null);
                  fetchAvailability(doctor.id, d);
                }}
              />

              <div className="booking-note">
                Weekend-ul este blocat. Ore ocupate (PENDING/ACCEPTED) sunt blocate.
              </div>
            </div>

            <div className="booking-panel">
              <div className="booking-panel__label">Alege ora</div>

              <div className="booking-slots">
                {visibleHours.map((h) => {
                  const busy = busyHours.has(h);
                  const selected = selectedHour === h;

                  let cls = "booking-slot";
                  if (busy) cls += " booking-slot--busy";
                  else cls += " booking-slot--free";
                  if (selected) cls += " booking-slot--selected";

                  return (
                    <button
                      key={h}
                      type="button"
                      className={cls}
                      disabled={busy || loadingSlots}
                      onClick={() => setSelectedHour(h)}
                    >
                      {pad2(h)}:00
                    </button>
                  );
                })}
              </div>

              {isToday(selectedDate) && visibleHours.length === 0 && (
                <div className="booking-note">
                  Nu mai sunt ore disponibile astăzi. Alege o altă zi.
                </div>
              )}

              {loadingSlots && (
                <div className="booking-note">Se încarcă disponibilitatea...</div>
              )}
            </div>

            <div className="booking-panel booking-panel--wide">
              <div className="booking-panel__label">Descriere (opțional)</div>
              <textarea
                className="booking-textarea"
                rows={4}
                placeholder="Ex: consult de rutină / durere de cap de 3 zile..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
          </div>

          {modalErr && <div className="booking-alert booking-alert--error">{modalErr}</div>}
          {modalOk && <div className="booking-alert booking-alert--ok">{modalOk}</div>}
        </div>

        <div className="booking-modal__footer">
          <button className="btn btn--secondary" onClick={() => onClose?.()} disabled={submitBusy}>
            Anulează
          </button>
          <button className="btn btn--primary" onClick={submitAppointment} disabled={submitBusy}>
            {submitBusy ? "Se trimite..." : "Trimite solicitarea"}
          </button>
        </div>
      </div>
    </div>
  );
}
