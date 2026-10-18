import React from 'react';
import { Routes, Route } from 'react-router-dom';
import DemoIdentityBanner from './components/DemoIdentityBanner';
import Navbar from './components/Navbar';
import RoleRoute from './components/RoleRoute';
import OnboardingPage from './pages/OnboardingPage';

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
          <Route path="/" element={<PlaceholderPage title="Rakt Sanchar Home" />} />
          <Route path="/stock" element={<PlaceholderPage title="Blood Stock Search" />} />
          <Route path="/hospitals" element={<PlaceholderPage title="Verified Hospitals" />} />
          <Route path="/requests" element={<PlaceholderPage title="Blood Requests" />} />
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
                <PlaceholderPage title="Donor Dashboard" />
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
