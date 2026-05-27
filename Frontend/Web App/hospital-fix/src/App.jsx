import { Routes, Route, Navigate } from "react-router-dom";

import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import Consultation from "./pages/Consultation";
import AccountPage from "./pages/AccountPage.jsx";
import SignUpPage from "./pages/SignUpPage";
import EditAccountPage from "./pages/EditAccountPage";
import AppointmentsPage from "./pages/AppointmentPage";
import DoctorDetailsPage from "./pages/DoctorDetailsPage.jsx";
import ResetPasswordPage from "./pages/ResetPasswordPages.jsx";

import AdminDashboardPage from "./pages/admin/AdminDashboardPage";

import AdminPatientsPage from "./pages/admin/patient/AdminPatientsPage.jsx";
import AdminPatientDetailsPage from "./pages/admin/patient/AdminPatientDetailsPage.jsx";
import AdminPatientEditPage from "./pages/admin/patient/AdminPatientEditPage.jsx";
import AdminPatientRegisterPage from "./pages/admin/patient/AdminPatientRegisterPage";

import AdminDoctorPage from "./pages/admin/doctor/AdminDoctorPage.jsx";
import AdminDoctorDetailsPage from "./pages/admin/doctor/AdminDoctorDetailsPage.jsx";
import AdminDoctorEditPage from "./pages/admin/doctor/AdminDoctorEditPage.jsx";
import AdminDoctorRegisterPage from "./pages/admin/doctor/AdminDoctorRegisterPage";

import AdminAdminsPage from "./pages/admin/admin/AdminAdminsPage.jsx";
import AdminAdminDetailsPage from "./pages/admin/admin/AdminAdminDetailsPage.jsx";
import AdminAdminEditPage from "./pages/admin/admin/AdminAdminEditPage.jsx";
import AdminAdminRegisterPage from "./pages/admin/admin/AdminAdminRegisterPage.jsx";

import AdminAppointmentsPage from "./pages/admin/appointments/AdminAppointmentsPage.jsx";
import AdminSpecializationPage from "./pages/admin/specializations/AdminSpecializationPage.jsx";

import AdminGuard from "./pages/guards/AdminGuard";

export default function App() {
  return (
    <Routes>
      {/* PUBLIC */}
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignUpPage />} />
      <Route path="/consultation" element={<Consultation />} />

      {/* ACCOUNT */}
      <Route path="/account" element={<AccountPage />} />
      <Route path="/account/edit" element={<EditAccountPage />} />
      <Route path="/appointments" element={<AppointmentsPage />} />
      <Route path="/doctor/:id" element={<DoctorDetailsPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      {/* ADMIN (PROTECTED) */}
      <Route element={<AdminGuard />}>
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/patients" element={<AdminPatientsPage />} />
        <Route path="/admin/patients/register" element={<AdminPatientRegisterPage />} />
        <Route path="/admin/patients/:id" element={<AdminPatientDetailsPage />} />
        <Route path="/admin/patients/:id/edit" element={<AdminPatientEditPage />} />

        <Route path="/admin/doctors" element={<AdminDoctorPage />} />
        <Route path="/admin/doctors/register" element={<AdminDoctorRegisterPage />} />
        <Route path="/admin/doctors/:id" element={<AdminDoctorDetailsPage />} />
        <Route path="/admin/doctors/:id/edit" element={<AdminDoctorEditPage />} />

        <Route path="/admin/admins" element={<AdminAdminsPage />} />
        <Route path="/admin/admins/register" element={<AdminAdminRegisterPage />} />
        <Route path="/admin/admins/:id" element={<AdminAdminDetailsPage />} />
        <Route path="/admin/admins/:id/edit" element={<AdminAdminEditPage />} />

        <Route path="/admin/appointments" element={<AdminAppointmentsPage />} />
        <Route path="/admin/specializations" element={<AdminSpecializationPage />} />
      </Route>

      {/* FALLBACK */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
