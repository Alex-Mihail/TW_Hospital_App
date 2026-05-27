import { Navigate, Outlet, useLocation } from "react-router-dom";

export default function AdminGuard() {
  const location = useLocation();
  const role = (localStorage.getItem("role") || "").toLowerCase();

  // if not admin -> login
  if (role !== "admin") {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
