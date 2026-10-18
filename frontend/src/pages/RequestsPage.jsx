import React, { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';
import { useIdentity } from '../context/IdentityContext';
import { useLanguage } from '../context/LanguageContext';

const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
const URGENCIES = ['CRITICAL', 'URGENT', 'NORMAL'];

export default function RequestsPage() {
  const { identity, isAuthenticated } = useIdentity();
  const { t } = useLanguage();

  const [tab, setTab] = useState('all'); // 'all' | 'mine'
  const [filterBg, setFilterBg] = useState('');
  const [filterCity, setFilterCity] = useState('');
  const [filterStatus, setFilterStatus] = useState('OPEN');
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(false);

  // Create form state
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [patientName, setPatientName] = useState('');
  const [bloodGroup, setBloodGroup] = useState('O+');
  const [unitsNeeded, setUnitsNeeded] = useState(1);
  const [hospital, setHospital] = useState('City Care Super Specialty Hospital');
  const [city, setCity] = useState('Mumbai');
  const [urgency, setUrgency] = useState('URGENT');
  const [neededBy, setNeededBy] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 2);
    return d.toISOString().split('T')[0];
  });
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Selected request for viewing ranked matches
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [matches, setMatches] = useState([]);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [actionMessage, setActionMessage] = useState(null);

  const loadRequests = useCallback(async () => {
    setLoading(true);
    try {
      if (tab === 'mine' && isAuthenticated) {
        const res = await api.get('/requests/mine?page=0&size=30');
        setRequests(res?.content || []);
      } else {
        const params = new URLSearchParams();
        if (filterBg) params.set('bloodGroup', filterBg);
        if (filterCity.trim()) params.set('city', filterCity.trim());
        if (filterStatus) params.set('status', filterStatus);
        params.set('page', '0');
        params.set('size', '30');
        const res = await api.get(`/requests?${params.toString()}`);
        setRequests(res?.content || []);
      }
    } catch {
      setRequests([]);
    } finally {
      setLoading(false);
    }
  }, [tab, isAuthenticated, filterBg, filterCity, filterStatus]);

  useEffect(() => {
    loadRequests();
  }, [loadRequests, identity]);

  const handleCreateRequest = async (e) => {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      await api.post('/requests', {
        patientName,
        bloodGroup,
        unitsNeeded: Number(unitsNeeded),
        hospital,
        city,
        urgency,
        neededBy,
      });
      setPatientName('');
      setShowCreateForm(false);
      setActionMessage('Blood request created and matching donors notified!');
      await loadRequests();
    } catch (err) {
      setFormError(err.message || 'Failed to create blood request');
    } finally {
      setSubmitting(false);
    }
  };

  const handleViewMatches = async (req) => {
    setSelectedRequest(req);
    setLoadingMatches(true);
    try {
      const res = await api.get(`/requests/${req.id}/matches`);
      setMatches(Array.isArray(res) ? res : []);
    } catch (err) {
      setMatches([]);
      setActionMessage(err.message || 'Could not load matches');
    } finally {
      setLoadingMatches(false);
    }
  };

  const handleCancelRequest = async (requestId) => {
    try {
      await api.patch(`/requests/${requestId}/cancel`);
      setActionMessage('Request cancelled.');
      await loadRequests();
    } catch (err) {
      setActionMessage(err.message || 'Failed to cancel request');
    }
  };

  const handleDonorRespond = async (requestId, status) => {
    try {
      await api.post(`/requests/${requestId}/respond`, { status });
      setActionMessage(`You ${status.toLowerCase()} the blood request.`);
      await loadRequests();
      if (selectedRequest && selectedRequest.id === requestId) {
        handleViewMatches(selectedRequest);
      }
    } catch (err) {
      setActionMessage(err.message || 'Could not record response');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{t('requests.title')}</h1>
          <p className="text-sm text-slate-600 mt-1">
            Raise emergency blood requests, inspect compatible donor matches, or respond as an
            eligible donor.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowCreateForm(!showCreateForm)}
          className="px-4 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-bold shadow-xs transition"
        >
          {showCreateForm ? '✕ Close Form' : `+ ${t('requests.newRequest')}`}
        </button>
      </div>

      {actionMessage && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-sm flex items-center justify-between">
          <span>{actionMessage}</span>
          <button
            type="button"
            onClick={() => setActionMessage(null)}
            className="text-xs font-semibold text-emerald-700 hover:underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* New Blood Request Form */}
      {showCreateForm && (
        <form
          onSubmit={handleCreateRequest}
          className="bg-white rounded-xl border border-red-200 p-6 shadow-sm space-y-4"
        >
          <h2 className="text-lg font-bold text-slate-900">Create Blood Request</h2>
          {!isAuthenticated && (
            <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-900 text-xs">
              Note: Select a Donor, Hospital, or Admin identity in the top Demo Banner to own and
              manage this request.
            </div>
          )}
          {formError && (
            <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Patient Name
              </label>
              <input
                type="text"
                required
                value={patientName}
                onChange={(e) => setPatientName(e.target.value)}
                placeholder="e.g. Suresh Kulkarni"
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Required Blood Group
              </label>
              <select
                value={bloodGroup}
                onChange={(e) => setBloodGroup(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              >
                {BLOOD_GROUPS.map((bg) => (
                  <option key={bg} value={bg}>
                    {bg}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Units Needed (1–20)
              </label>
              <input
                type="number"
                min="1"
                max="20"
                required
                value={unitsNeeded}
                onChange={(e) => setUnitsNeeded(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Hospital Name
              </label>
              <input
                type="text"
                required
                value={hospital}
                onChange={(e) => setHospital(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">City</label>
              <input
                type="text"
                required
                value={city}
                onChange={(e) => setCity(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Urgency</label>
              <select
                value={urgency}
                onChange={(e) => setUrgency(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              >
                {URGENCIES.map((u) => (
                  <option key={u} value={u}>
                    {u}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex flex-wrap items-end justify-between gap-4 pt-2">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Needed By Date
              </label>
              <input
                type="date"
                required
                value={neededBy}
                onChange={(e) => setNeededBy(e.target.value)}
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <button
              type="submit"
              disabled={submitting}
              className="px-5 py-2.5 rounded-lg bg-red-600 hover:bg-red-700 text-white text-sm font-bold transition disabled:opacity-50"
            >
              {submitting ? 'Submitting...' : 'Submit Blood Request'}
            </button>
          </div>
        </form>
      )}

      {/* Tabs & Filters */}
      <div className="bg-white rounded-xl border border-slate-200 p-4 flex flex-wrap items-center justify-between gap-4">
        <div className="inline-flex rounded-lg bg-slate-100 p-1 text-xs font-semibold">
          <button
            type="button"
            onClick={() => setTab('all')}
            className={`px-3 py-1.5 rounded-md transition ${
              tab === 'all' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600'
            }`}
          >
            All Requests
          </button>
          {isAuthenticated && (
            <button
              type="button"
              onClick={() => setTab('mine')}
              className={`px-3 py-1.5 rounded-md transition ${
                tab === 'mine' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600'
              }`}
            >
              My Requests ({identity.userId})
            </button>
          )}
        </div>

        {tab === 'all' && (
          <div className="flex flex-wrap items-center gap-3">
            <select
              aria-label="Filter Blood Group"
              value={filterBg}
              onChange={(e) => setFilterBg(e.target.value)}
              className="rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs"
            >
              <option value="">All Blood Groups</option>
              {BLOOD_GROUPS.map((bg) => (
                <option key={bg} value={bg}>
                  {bg}
                </option>
              ))}
            </select>

            <input
              type="text"
              placeholder="Filter by city..."
              value={filterCity}
              onChange={(e) => setFilterCity(e.target.value)}
              className="rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs"
            />

            <select
              aria-label="Filter Status"
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs"
            >
              <option value="">All Statuses</option>
              <option value="OPEN">OPEN</option>
              <option value="FULFILLED">FULFILLED</option>
              <option value="CANCELLED">CANCELLED</option>
              <option value="EXPIRED">EXPIRED</option>
            </select>
          </div>
        )}
      </div>

      {/* Request Cards */}
      {loading ? (
        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-sm text-slate-500">
          Loading blood requests...
        </div>
      ) : requests.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-sm text-slate-500">
          No blood requests match the selected criteria.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {requests.map((req) => {
            const isOwnerOrAdmin =
              isAuthenticated &&
              (req.requesterUserId === identity.userId || identity.role === 'ADMIN');

            return (
              <div
                key={req.id}
                className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs flex flex-col justify-between gap-4"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
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
                      <span
                        className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                          req.status === 'OPEN'
                            ? 'bg-emerald-100 text-emerald-800'
                            : req.status === 'FULFILLED'
                            ? 'bg-blue-100 text-blue-800'
                            : 'bg-slate-100 text-slate-600'
                        }`}
                      >
                        {req.status}
                      </span>
                      <span className="text-xs text-slate-500">📍 {req.city}</span>
                    </div>

                    <h3 className="text-base font-bold text-slate-900">
                      Patient: {req.patientName}
                    </h3>
                    <p className="text-xs text-slate-600">
                      Hospital: <strong>{req.hospital}</strong>
                    </p>
                    <p className="text-xs text-slate-500">
                      Units: {req.unitsFulfilled || 0} / {req.unitsNeeded} fulfilled • Needed by:{' '}
                      {req.neededBy || 'Immediate'}
                    </p>
                  </div>

                  <div className="w-12 h-12 rounded-xl bg-red-600 text-white font-extrabold text-lg flex items-center justify-center shrink-0">
                    {req.bloodGroup}
                  </div>
                </div>

                <div className="border-t border-slate-100 pt-3 flex flex-wrap items-center justify-between gap-2 text-xs">
                  <div className="flex flex-wrap items-center gap-2">
                    {isOwnerOrAdmin && (
                      <button
                        type="button"
                        onClick={() => handleViewMatches(req)}
                        className="px-3 py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-semibold transition"
                      >
                        🔍 View Compatible Matches
                      </button>
                    )}
                    {isOwnerOrAdmin && req.status === 'OPEN' && (
                      <button
                        type="button"
                        onClick={() => handleCancelRequest(req.id)}
                        className="px-3 py-1.5 rounded-lg border border-red-200 text-red-700 hover:bg-red-50 font-semibold transition"
                      >
                        Cancel Request
                      </button>
                    )}
                  </div>

                  {identity.role === 'DONOR' && req.status === 'OPEN' && (
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => handleDonorRespond(req.id, 'ACCEPTED')}
                        className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-semibold transition"
                      >
                        ✓ Accept to Donate
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDonorRespond(req.id, 'DECLINED')}
                        className="px-2.5 py-1.5 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100 font-medium transition"
                      >
                        Decline
                      </button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Ranked Compatible Donor Matches Panel */}
      {selectedRequest && (
        <div className="bg-white rounded-xl border-2 border-slate-900 p-6 shadow-lg space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-slate-900">
                Ranked Compatible Donors for {selectedRequest.patientName} (
                {selectedRequest.bloodGroup})
              </h2>
              <p className="text-xs text-slate-500">
                Phone number is masked until the donor explicitly accepts the request.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setSelectedRequest(null)}
              className="text-sm font-semibold text-slate-500 hover:text-slate-900"
            >
              ✕ Close
            </button>
          </div>

          {loadingMatches ? (
            <p className="text-sm text-slate-500">Computing ABO/Rh & Haversine matches...</p>
          ) : matches.length === 0 ? (
            <p className="text-sm text-slate-500">
              No available, eligible compatible donors found in {selectedRequest.city}.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500 uppercase">
                    <th className="py-2 pr-4">Donor</th>
                    <th className="py-2 pr-4">Blood Group</th>
                    <th className="py-2 pr-4">Match Type</th>
                    <th className="py-2 pr-4">Distance</th>
                    <th className="py-2 pr-4">Response Status</th>
                    <th className="py-2 pr-4">Contact Phone</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {matches.map((m) => (
                    <tr key={m.donorId} className="hover:bg-slate-50">
                      <td className="py-2.5 pr-4 font-semibold text-slate-900">{m.name}</td>
                      <td className="py-2.5 pr-4 font-bold text-red-600">{m.bloodGroup}</td>
                      <td className="py-2.5 pr-4">
                        {m.exactBloodGroupMatch ? (
                          <span className="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 font-semibold">
                            Exact Match
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded bg-blue-100 text-blue-800 font-semibold">
                            Compatible
                          </span>
                        )}
                      </td>
                      <td className="py-2.5 pr-4">
                        {m.distanceKm !== null && m.distanceKm !== undefined
                          ? `${m.distanceKm.toFixed(1)} km`
                          : m.city}
                      </td>
                      <td className="py-2.5 pr-4 font-semibold">
                        {m.responseStatus || 'AWAITING'}
                      </td>
                      <td className="py-2.5 pr-4 font-mono">{m.phone}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
