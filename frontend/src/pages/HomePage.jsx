import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useLanguage } from '../context/LanguageContext';

export default function HomePage() {
  const { t } = useLanguage();
  const [stats, setStats] = useState(null);
  const [urgentRequests, setUrgentRequests] = useState([]);

  useEffect(() => {
    api
      .get('/public/stats')
      .then(setStats)
      .catch(() => setStats(null));

    api
      .get('/requests?status=OPEN&page=0&size=4')
      .then((res) => setUrgentRequests(res?.content || []))
      .catch(() => setUrgentRequests([]));
  }, []);

  const statCards = [
    {
      label: t('stats.registeredDonors'),
      value: stats?.registeredDonors ?? '—',
      accent: 'text-red-600',
    },
    {
      label: t('stats.availableDonors'),
      value: stats?.availableDonors ?? '—',
      accent: 'text-emerald-600',
    },
    {
      label: t('stats.verifiedHospitals'),
      value: stats?.verifiedHospitals ?? '—',
      accent: 'text-blue-600',
    },
    {
      label: t('stats.totalUnits'),
      value: stats?.totalUnitsInStock ?? '—',
      accent: 'text-amber-600',
    },
    {
      label: t('stats.openRequests'),
      value: stats?.openRequests ?? '—',
      accent: 'text-rose-600',
    },
    {
      label: t('stats.verifiedDonations'),
      value: stats?.verifiedDonations ?? '—',
      accent: 'text-indigo-600',
    },
  ];

  return (
    <div>
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-red-700 via-red-800 to-slate-900 text-white py-16 px-4">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          <div className="lg:col-span-7 space-y-5">
            <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-600/40 border border-red-400/30 text-xs font-semibold tracking-wide uppercase">
              Zero-Cost Public Health Network
            </span>
            <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight leading-tight">
              {t('home.heroTitle')}
            </h1>
            <p className="text-red-100 text-base sm:text-lg max-w-2xl leading-relaxed">
              {t('home.heroSubtitle')}
            </p>
            <div className="flex flex-wrap gap-3 pt-2">
              <Link
                to="/stock"
                className="px-5 py-3 rounded-xl bg-white text-red-800 font-bold text-sm shadow-md hover:bg-red-50 transition"
              >
                🔍 {t('home.findBlood')}
              </Link>
              <Link
                to="/requests"
                className="px-5 py-3 rounded-xl bg-red-600 text-white font-bold text-sm border border-red-400/40 hover:bg-red-500 transition"
              >
                🚨 {t('home.postRequest')}
              </Link>
              <Link
                to="/donor/dashboard"
                className="px-5 py-3 rounded-xl bg-slate-900/60 text-white font-semibold text-sm border border-white/20 hover:bg-slate-900 transition"
              >
                ❤️ {t('home.becomeDonor')}
              </Link>
            </div>
          </div>

          <div className="lg:col-span-5 bg-white/10 backdrop-blur-xs border border-white/15 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-bold uppercase tracking-wider text-red-200">
              ABO / Rh Universal Compatibility Quick Guide
            </h2>
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="bg-slate-900/50 p-3.5 rounded-xl border border-white/10">
                <span className="text-lg font-extrabold text-emerald-300 block">O-</span>
                <span className="font-semibold text-white">Universal Red Cell Donor</span>
                <p className="text-red-200 mt-1">Can donate red cells to all 8 blood groups.</p>
              </div>
              <div className="bg-slate-900/50 p-3.5 rounded-xl border border-white/10">
                <span className="text-lg font-extrabold text-amber-300 block">AB+</span>
                <span className="font-semibold text-white">Universal Recipient</span>
                <p className="text-red-200 mt-1">Can receive red cells from all 8 blood groups.</p>
              </div>
            </div>
            <p className="text-xs text-red-200">
              All donor phone numbers and exact coordinates are protected by privacy masking on
              public endpoints until an eligible donor explicitly accepts a blood request.
            </p>
          </div>
        </div>
      </section>

      {/* Live Stats Grid */}
      <section className="max-w-7xl mx-auto px-4 -mt-8">
        <div
          className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4"
          data-testid="public-stats-grid"
        >
          {statCards.map((card) => (
            <div
              key={card.label}
              className="bg-white rounded-xl p-4 shadow-sm border border-slate-200 flex flex-col justify-between"
            >
              <span className="text-xs font-medium text-slate-500">{card.label}</span>
              <span className={`text-2xl sm:text-3xl font-extrabold mt-2 ${card.accent}`}>
                {card.value}
              </span>
            </div>
          ))}
        </div>
      </section>

      {/* Active Emergency Requests Preview */}
      <section className="max-w-7xl mx-auto px-4 py-12">
        <div className="flex items-center justify-between mb-5">
          <div>
            <h2 className="text-xl font-bold text-slate-900">Active Open Blood Requests</h2>
            <p className="text-xs text-slate-500">
              Sorted by urgency (CRITICAL → URGENT → NORMAL)
            </p>
          </div>
          <Link
            to="/requests"
            className="text-sm font-semibold text-red-600 hover:text-red-700 hover:underline"
          >
            View all requests →
          </Link>
        </div>

        {urgentRequests.length === 0 ? (
          <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-sm text-slate-500">
            No open blood requests at the moment.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {urgentRequests.map((req) => (
              <div
                key={req.id}
                className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs flex items-start justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span
                      className={`px-2 py-0.5 rounded text-[11px] font-bold uppercase ${
                        req.urgency === 'CRITICAL'
                          ? 'bg-red-100 text-red-800'
                          : req.urgency === 'URGENT'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-slate-100 text-slate-700'
                      }`}
                    >
                      {req.urgency}
                    </span>
                    <span className="text-xs text-slate-500">📍 {req.city}</span>
                  </div>
                  <h3 className="font-bold text-slate-900 text-base">
                    Patient: {req.patientName} ({req.unitsNeeded} unit(s) needed)
                  </h3>
                  <p className="text-xs text-slate-600">Hospital: {req.hospital}</p>
                  {req.neededBy && (
                    <p className="text-xs text-slate-500">Needed by: {req.neededBy}</p>
                  )}
                </div>

                <div className="flex flex-col items-end gap-2 shrink-0">
                  <span className="w-12 h-12 rounded-xl bg-red-600 text-white font-extrabold text-lg flex items-center justify-center shadow-2xs">
                    {req.bloodGroup}
                  </span>
                  <Link
                    to="/requests"
                    className="text-xs font-semibold text-red-600 hover:underline"
                  >
                    Respond / Details
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
