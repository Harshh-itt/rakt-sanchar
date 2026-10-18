import React from 'react';
import { Routes, Route } from 'react-router-dom';
import DemoIdentityBanner from './components/DemoIdentityBanner';
import Navbar from './components/Navbar';
import RoleRoute from './components/RoleRoute';
import HomePage from './pages/HomePage';
import StockSearchPage from './pages/StockSearchPage';
import HospitalsPage from './pages/HospitalsPage';
import RequestsPage from './pages/RequestsPage';
import OnboardingPage from './pages/OnboardingPage';
import DonorDashboardPage from './pages/DonorDashboardPage';
import { HospitalDashboardPage } from './pages/HospitalDashboardPage';
import { AdminHospitalsPage } from './pages/AdminHospitalsPage';

export default function App() {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900">
      <DemoIdentityBanner />
      <Navbar />
      <main className="flex-1">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/stock" element={<StockSearchPage />} />
          <Route path="/hospitals" element={<HospitalsPage />} />
          <Route path="/requests" element={<RequestsPage />} />
          <Route
            path="/onboarding"
            element={
              <RoleRoute allowedRoles={['DONOR', 'HOSPITAL', 'ADMIN']}>
                <OnboardingPage />
              </RoleRoute>
            }
          />
          <Route
            path="/donor/dashboard"
            element={
              <RoleRoute allowedRoles={['DONOR']}>
                <DonorDashboardPage />
              </RoleRoute>
            }
          />
          <Route
            path="/hospital/dashboard"
            element={
              <RoleRoute allowedRoles={['HOSPITAL']}>
                <HospitalDashboardPage />
              </RoleRoute>
            }
          />
          <Route
            path="/admin/hospitals"
            element={
              <RoleRoute allowedRoles={['ADMIN']}>
                <AdminHospitalsPage />
              </RoleRoute>
            }
          />
        </Routes>
      </main>
    </div>
  );
}
