import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

export default function HomePage() {
  const [search, setSearch] = useState("");
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  // ===== CHAT STATE =====
  const [chatOpen, setChatOpen] = useState(false);
  const [chatInput, setChatInput] = useState("");
  const [chatLoading, setChatLoading] = useState(false);
  const [chatMessages, setChatMessages] = useState([
    {
      role: "ai",
      text: "Salut! Sunt asistentul TW Hospital. Te pot ajuta cu programări, doctori și specializări.",
    },
  ]);
  const chatBodyRef = useRef(null);

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

  // if not pacient, do not show chat
const role = (user?.role || "").toString().toUpperCase();
const CHAT_DISABLED_ROLES = ["DOCTOR", "ADMIN"];
const isChatDisabled = CHAT_DISABLED_ROLES.includes(role);

useEffect(() => {
  if (isChatDisabled && chatOpen) setChatOpen(false);
}, [isChatDisabled, chatOpen]);

  useEffect(() => {
    // scroll to the last message when new message comes
    if (chatOpen && chatBodyRef.current) {
      chatBodyRef.current.scrollTop = chatBodyRef.current.scrollHeight;
    }
  }, [chatOpen, chatMessages]);

  const displayName = user ? `${user.firstName} ${user.lastName}` : "";

  function handleSearchSubmit() {
    const q = search.trim();
    if (!q) {
      navigate("/consultation");
      return;
    }
    navigate(`/consultation?search=${encodeURIComponent(q)}`);
  }

  async function sendChatMessage() {
    const msg = chatInput.trim();
    if (!msg || chatLoading) return;

    const uiContext = {
    page: "HomePage",
    actions: ["Caută", "Programează-te", "Contul meu", "Chat AI"],
    steps: [
      "Apasă pe 'Programează-te' (te duce la /consultation)",
      "Caută specializarea sau medicul",
      "Selectează medicul",
      "Alege data și ora (08:00–16:00, minute 00)",
      "Confirmă programarea"
    ]
  };

    setChatInput("");
    setChatMessages((prev) => [...prev, { role: "user", text: msg }]);
    setChatLoading(true);

    try {
      const role = (user?.role || "PATIENT").toString().toUpperCase(); // "PATIENT" / "DOCTOR"
      const userId = user?.id ?? user?.userId ?? null;

      const res = await fetch("http://localhost:8080/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message: msg,
          role: role,
          userId: userId,
        }),
      });

      let data = null;
      try {
        data = await res.json();
      } catch {
        data = { answer: "Eroare: răspuns invalid de la server." };
      }

      if (!res.ok) {
        const errText = data?.answer || "A apărut o eroare la server.";
        setChatMessages((prev) => [...prev, { role: "ai", text: errText }]);
      } else {
        setChatMessages((prev) => [
          ...prev,
          { role: "ai", text: data?.answer ?? "(Fără răspuns)" },
        ]);
      }
    } catch (e) {
      setChatMessages((prev) => [
        ...prev,
        {
          role: "ai",
          text: "Nu pot contacta serverul acum. Verifică dacă backend-ul rulează.",
        },
      ]);
    } finally {
      setChatLoading(false);
    }
  }

  return (
    <div
      style={{
        fontFamily: "Arial, sans-serif",
        backgroundColor: "#f5f9fc",
        minHeight: "100vh",
        maxWidth: "100vw",
        margin: 0,
        padding: 0,
      }}
    >
      {/* TOP BAR */}
      <div
        style={{
          width: "100vw",
          overflow: "hidden",
          backgroundColor: "#063a62",
          color: "white",
          padding: "10px 10px",
          display: "flex",
          justifyContent: "flex-end",
          alignItems: "center",
          fontSize: "14px",
          gap: "20px",
          boxSizing: "border-box",
        }}
      >
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
            marginRight: "50px",
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

      {/* HERO SECTION */}
      <section
        style={{
          width: "100%",
          backgroundImage: "url('/images/HomePage.jpg')",
          backgroundSize: "cover",
          backgroundPosition: "center -400px",
          backgroundRepeat: "no-repeat",
          height: "500px",
          paddingTop: "80px",
          paddingBottom: "80px",
          paddingLeft: "20px",
          paddingRight: "20px",
          color: "white",
        }}
      >
        <div
          style={{
            maxWidth: "1000px",
            margin: "0 auto",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
          }}
        >
          <h1
            style={{
              fontSize: "58px",
              fontWeight: "600",
              textAlign: "center",
              marginBottom: "10px",
            }}
          >
            Spitalul Central TW
          </h1>

          <p
            style={{
              fontSize: "24px",
              fontWeight: "300",
              opacity: 0.95,
              maxWidth: "750px",
              textAlign: "center",
              lineHeight: "1.4",
            }}
          >
            Îngrijire medicală la standarde internaționale.
          </p>

          {/* Search bar */}
          <div
            style={{
              marginTop: "30px",
              width: "100%",
              display: "flex",
              justifyContent: "center",
              gap: "12px",
              flexWrap: "wrap",
            }}
          >
            <input
              type="text"
              placeholder="Caută un medic, o specializare sau un serviciu..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleSearchSubmit();
              }}
              style={{
                width: "100%",
                maxWidth: "600px",
                padding: "18px",
                borderRadius: "12px",
                border: "none",
                fontSize: "16px",
              }}
            />

            <button
              onClick={handleSearchSubmit}
              style={{
                backgroundColor: "#0a4d80",
                color: "white",
                border: "none",
                padding: "16px 18px",
                borderRadius: "12px",
                cursor: "pointer",
                fontSize: "16px",
                fontWeight: "700",
              }}
            >
              Caută
            </button>
          </div>
        </div>
      </section>

      {/* INTRO DESCRIPTION */}
      <section
        style={{
          padding: "70px 20px 10px 20px",
          maxWidth: "1200px",
          margin: "0 auto",
          textAlign: "center",
        }}
      >
        <p
          style={{
            fontSize: "22px",
            color: "#0a4d80",
            fontWeight: "600",
            marginBottom: "18px",
          }}
        >
          Sănătatea ta merită mai mult decât o soluție „rapidă”.
        </p>

        <p
          style={{
            fontSize: "18px",
            color: "#555",
            lineHeight: "1.7",
            maxWidth: "950px",
            margin: "0 auto",
          }}
        >
          La Spitalul Central TW, combinăm expertiza medicală cu tehnologii
          moderne și o abordare orientată către pacient. Fie că ai nevoie de o
          consultație de rutină, o a doua opinie sau un plan complet de tratament,
          îți punem la dispoziție specialiști cu experiență, investigații rapide
          și recomandări clare, pe înțelesul tău.
          <br />
          <br />
          Ne concentrăm pe prevenție, diagnostic corect și continuitate în îngrijire,
          astfel încât să iei decizii informate, cu încredere. Programările sunt
          simple, iar timpul tău este respectat — pentru că știm cât de important
          este să primești ajutor la momentul potrivit.
        </p>
      </section>

      {/* CARDS SECTION */}
      <section
        style={{
          padding: "80px 20px",
          maxWidth: "1800px",
          margin: "0 auto",
        }}
      >
        <h2
          style={{
            fontSize: "50px",
            textAlign: "center",
            color: "#0a4d80",
            marginBottom: "100px",
            fontWeight: "700",
          }}
        >
          Serviciile noastre medicale
        </h2>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))",
            gap: "30px",
          }}
        >
          {/* CONSULTATION CARD */}
          <div
            style={{
              backgroundColor: "white",
              padding: "35px",
              borderRadius: "18px",
              boxShadow: "0px 10px 20px rgba(0,0,0,0.12)",
              textAlign: "center",
            }}
          >
            <img
              src="/images/medicalService.jpg"
              alt="Consultații"
              style={{
                width: "100%",
                height: "260px",
                objectFit: "cover",
                borderRadius: "14px",
                marginBottom: "25px",
              }}
            />
            <h3 style={{ fontSize: "26px", color: "#0a4d80" }}>
              Consultații medicale
            </h3>
            <p style={{ fontSize: "20px", color: "#555" }}>
              Acces rapid la medici specialiști.
            </p>
          </div>

          {/* TREATMENT CARD */}
          <div
            style={{
              backgroundColor: "white",
              padding: "35px",
              borderRadius: "18px",
              boxShadow: "0px 10px 20px rgba(0,0,0,0.12)",
              textAlign: "center",
            }}
          >
            <img
              src="/images/treatment.jpg"
              alt="Tratamente"
              style={{
                width: "100%",
                height: "260px",
                objectFit: "cover",
                borderRadius: "14px",
                marginBottom: "25px",
              }}
            />
            <h3 style={{ fontSize: "26px", color: "#0a4d80" }}>
              Tratamente medicale
            </h3>
            <p style={{ fontSize: "20px", color: "#555" }}>
              Soluții moderne și eficiente.
            </p>
          </div>

          {/* APPOINTMENT CARD */}
          <div
            style={{
              backgroundColor: "white",
              padding: "35px",
              borderRadius: "18px",
              boxShadow: "0px 10px 20px rgba(0,0,0,0.12)",
              textAlign: "center",
            }}
          >
            <img
              src="/images/appointment.jpg"
              alt="Programări"
              style={{
                width: "100%",
                height: "260px",
                objectFit: "cover",
                borderRadius: "14px",
                marginBottom: "25px",
              }}
            />
            <h3 style={{ fontSize: "26px", color: "#0a4d80" }}>
              Programări online
            </h3>
            <p style={{ fontSize: "20px", color: "#555" }}>
              Rezervă consultații rapid, fără așteptare.
            </p>
          </div>

          {/* MEDICAL TEAM CARD */}
          <div
            style={{
              backgroundColor: "white",
              padding: "35px",
              borderRadius: "18px",
              boxShadow: "0px 10px 20px rgba(0,0,0,0.12)",
              textAlign: "center",
            }}
          >
            <img
              src="/images/medicalTeam.jpg"
              alt="Echipă medicală"
              style={{
                width: "100%",
                height: "260px",
                objectFit: "cover",
                borderRadius: "14px",
                marginBottom: "25px",
              }}
            />
            <h3 style={{ fontSize: "26px", color: "#0a4d80" }}>
              Echipă medicală
            </h3>
            <p style={{ fontSize: "20px", color: "#555" }}>
              Medici experimentați și dedicați.
            </p>
          </div>
        </div>

        {/* CTA DOWN */}
        <div
          style={{
            marginTop: "70px",
            textAlign: "center",
            maxWidth: "1100px",
            marginLeft: "auto",
            marginRight: "auto",
          }}
        >
          <p
            style={{
              fontSize: "22px",
              color: "#0a4d80",
              fontWeight: "600",
              marginBottom: "18px",
              lineHeight: "1.5",
            }}
          >
            Ai nevoie de o consultație? Programează-te gratuit la unul dintre
            medicii noștri specialiști și primești rapid recomandări clare,
            adaptate nevoilor tale.
          </p>

          <button
            onClick={() => navigate("/consultation")}
            style={{
              backgroundColor: "#0a4d80",
              color: "white",
              border: "none",
              padding: "14px 26px",
              borderRadius: "12px",
              cursor: "pointer",
              fontSize: "18px",
              fontWeight: "600",
              boxShadow: "0px 10px 20px rgba(0,0,0,0.12)",
            }}
          >
            Programează-te
          </button>
        </div>
      </section>

      {/* FOOTER */}
      <footer
        style={{
          textAlign: "center",
          padding: "20px",
          marginTop: "40px",
          color: "#0a4d80",
          fontSize: "14px",
        }}
      >
        © 2026 Spitalul Central TW — Toate drepturile rezervate
      </footer>

      {/* =========================
          FLOATING CHAT WIDGET
         ========================= */}
      {/* round button */}
      <button
        onClick={() => setChatOpen((v) => !v)}
        style={{
          position: "fixed",
          right: "22px",
          bottom: "22px",
          width: "60px",
          height: "60px",
          borderRadius: "50%",
          backgroundColor: "#0a4d80",
          color: "white",
          border: "none",
          cursor: "pointer",
          boxShadow: "0px 10px 20px rgba(0,0,0,0.25)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          zIndex: 9999,
        }}
        title={chatOpen ? "Închide chat" : "Deschide chat"}
      >
        {/* Chat icon */}
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
          <path
            d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4v8z"
            stroke="white"
            strokeWidth="2"
            fill="none"
          />
        </svg>
      </button>

      {/* Chat window */}
      {chatOpen && (
        <div
          style={{
            position: "fixed",
            right: "22px",
            bottom: "95px",
            width: "340px",
            maxWidth: "calc(100vw - 44px)",
            height: "420px",
            backgroundColor: "white",
            borderRadius: "16px",
            boxShadow: "0px 12px 30px rgba(0,0,0,0.22)",
            overflow: "hidden",
            zIndex: 9999,
            display: "flex",
            flexDirection: "column",
          }}
        >
          {/* Header */}
          <div
            style={{
              backgroundColor: "#063a62",
              color: "white",
              padding: "12px 14px",
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              fontWeight: 700,
            }}
          >
            <span>Asistent AI</span>
            <button
              onClick={() => setChatOpen(false)}
              style={{
                background: "transparent",
                border: "none",
                color: "white",
                fontSize: "18px",
                cursor: "pointer",
                lineHeight: 1,
              }}
              title="Închide"
            >
              ✕
            </button>
          </div>

          {/* Body */}
          <div
            ref={chatBodyRef}
            style={{
              flex: 1,
              padding: "12px",
              overflowY: "auto",
              backgroundColor: "#f5f9fc",
            }}
          >
            {chatMessages.map((m, idx) => (
              <div
                key={idx}
                style={{
                  display: "flex",
                  justifyContent: m.role === "user" ? "flex-end" : "flex-start",
                  marginBottom: "10px",
                }}
              >
                <div
                  style={{
                    maxWidth: "85%",
                    padding: "10px 12px",
                    borderRadius: "14px",
                    backgroundColor: m.role === "user" ? "#0a4d80" : "white",
                    color: m.role === "user" ? "white" : "#1b2a3a",
                    boxShadow: "0px 6px 14px rgba(0,0,0,0.10)",
                    whiteSpace: "pre-wrap",
                    lineHeight: 1.35,
                    fontSize: "14px",
                  }}
                >
                  {m.text}
                </div>
              </div>
            ))}

            {chatLoading && (
              <div style={{ color: "#555", fontSize: "13px", padding: "6px 2px" }}>
                AI scrie...
              </div>
            )}
          </div>

          {/* Input */}
          <div
            style={{
              padding: "10px",
              borderTop: "1px solid #e6eef6",
              display: "flex",
              gap: "8px",
              alignItems: "center",
            }}
          >
            <input
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") sendChatMessage();
              }}
              placeholder="Scrie un mesaj..."
              style={{
                flex: 1,
                padding: "10px 12px",
                borderRadius: "12px",
                border: "1px solid #cfe0ef",
                outline: "none",
                fontSize: "14px",
              }}
            />
            <button
              onClick={sendChatMessage}
              disabled={chatLoading}
              style={{
                backgroundColor: chatLoading ? "#9bb7cd" : "#0a4d80",
                color: "white",
                border: "none",
                padding: "10px 12px",
                borderRadius: "12px",
                cursor: chatLoading ? "not-allowed" : "pointer",
                fontWeight: 700,
              }}
              title="Trimite"
            >
              ➤
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
