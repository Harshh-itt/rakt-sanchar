import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';

export default function HospitalsPage() {
  const [city, setCity] = useState('');
  const [name, setName] = useState('');
  const [hospitals, setHospitals] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    const params = new URLSearchParams();
    if (city.trim()) params.set('city', city.trim());
    if (name.trim()) params.set('name', name.trim());
    api
      .get(`/hospitals?${params.toString()}`)
      .then((res) => setHospitals(res?.content || []))
      .catch(() => setHospitals([]))
      .finally(() => setLoading(false));
  }, [city, name]);

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Verified Hospital Directory</h1>
        <p className="text-sm text-slate-600 mt-1">
          Only hospitals verified by a Rakt Sanchar administrator appear in public directories.
        </p>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 p-4 flex flex-wrap gap-4">
        <div className="flex-1 min-w-48">
          <label className="block text-xs font-semibold text-slate-700 mb-1">Hospital Name</label>
          <input
            type="text"
            placeholder="Search by hospital name..."
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
        </div>
        <div className="w-full sm:w-64">
          <label className="block text-xs font-semibold text-slate-700 mb-1">City</label>
          <input
            type="text"
            placeholder="e.g. Mumbai, Bengaluru"
            value={city}
            onChange={(e) => setCity(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
        </div>
      </div>

      {loading ? (
        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-sm text-slate-500">
          Loading verified hospitals...
        </div>
      ) : hospitals.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-sm text-slate-500">
          No verified hospitals match your filter.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {hospitals.map((h) => (
            <div
              key={h.id}
              className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs space-y-3"
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <span className="inline-block px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-100 text-emerald-800">
                    ✓ {h.status}
                  </span>
                  <h2 className="text-lg font-bold text-slate-900 mt-1">{h.name}</h2>
                  <p className="text-xs text-slate-500">Reg. No: {h.registrationNumber}</p>
                </div>
                <Link
                  to="/stock"
                  className="px-3 py-1.5 rounded-lg bg-red-50 text-red-700 hover:bg-red-100 text-xs font-semibold transition"
                >
                  Check Stock →
                </Link>
              </div>
              <div className="text-xs text-slate-600 space-y-1 border-t border-slate-100 pt-3">
                <p>
                  <strong>Address:</strong> {h.address}, {h.city}{' '}
                  {h.pincode ? `- ${h.pincode}` : ''}
                </p>
                {h.contactPerson && (
                  <p>
                    <strong>Contact Person:</strong> {h.contactPerson}
                  </p>
                )}
                <p>
                  <strong>Phone:</strong> {h.phone}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
