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

function PlaceholderPage({ title }) {
  return (
    <div className="max-w-7xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
    </div>
  );
}

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
                <PlaceholderPage title="Hospital Portal" />
              </RoleRoute>
            }
          />
          <Route
            path="/admin/hospitals"
            element={
              <RoleRoute allowedRoles={['ADMIN']}>
                <PlaceholderPage title="Admin Verification" />
              </RoleRoute>
            }
          />
        </Routes>
      </main>
    </div>
  );
}
