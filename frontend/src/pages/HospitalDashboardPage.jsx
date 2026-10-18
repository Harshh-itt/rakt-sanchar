import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../api/client';
import { useIdentity } from '../context/IdentityContext';
import { useLanguage } from '../context/LanguageContext';

const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-'];

export function HospitalDashboardPage() {
  const { identity } = useIdentity();
  const { t } = useLanguage();

  const [hospital, setHospital] = useState(null);
  const [stock, setStock] = useState([]);
  const [donations, setDonations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  // Profile form state
  const [profileForm, setProfileForm] = useState({
    name: '',
    registrationNumber: '',
    contactPerson: '',
    address: '',
    city: '',
    state: 'Delhi',
    pincode: '110029',
    phone: '',
    latitude: '28.5672',
    longitude: '77.2100',
  });
  const [savingProfile, setSavingProfile] = useState(false);

  // Stock editing state per bloodGroup: { [bg]: { units, minThreshold } }
  const [stockDrafts, setStockDrafts] = useState({});
  const [savingStockBg, setSavingStockBg] = useState('');

  // Direct donation recording form
  const [recordForm, setRecordForm] = useState({
    donorUserId: 'donor-1',
    requestId: '',
    units: 1,
    donatedAt: new Date().toISOString().split('T')[0],
  });
  const [recordingDonation, setRecordingDonation] = useState(false);

  // Rejection modal/input per donation
  const [rejectingId, setRejectingId] = useState(null);
  const [rejectReason, setRejectReason] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const h = await api.get('/api/hospitals/me');
      setHospital(h);
      setProfileForm({
        name: h.name || '',
        registrationNumber: h.registrationNumber || '',
        contactPerson: h.contactPerson || '',
        address: h.address || '',
        city: h.city || '',
        state: h.state || 'Delhi',
        pincode: h.pincode || '110029',
        phone: h.phone || '',
        latitude: h.latitude ?? '28.5672',
        longitude: h.longitude ?? '77.2100',
      });

      const [stockRes, donationsRes] = await Promise.all([
        api.get('/api/hospitals/me/stock').catch(() => []),
        api.get('/api/hospitals/me/donations').catch(() => []),
      ]);
      const stockList = Array.isArray(stockRes) ? stockRes : [];
      setStock(stockList);

      const drafts = {};
      BLOOD_GROUPS.forEach((bg) => {
        const found = stockList.find((s) => s.bloodGroup === bg);
        drafts[bg] = {
          units: found ? found.units : 0,
          minThreshold: found ? found.minThreshold : 5,
        };
      });
      setStockDrafts(drafts);
      setDonations(Array.isArray(donationsRes) ? donationsRes : []);
    } catch (err) {
      if (err.status === 404) {
        setHospital(null);
      } else {
        setError(err.message || 'Failed to load hospital portal');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData, identity.userId]);

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setSavingProfile(true);
    setError('');
    setMessage('');
    try {
      const payload = {
        ...profileForm,
        latitude: profileForm.latitude !== '' ? parseFloat(profileForm.latitude) : null,
        longitude: profileForm.longitude !== '' ? parseFloat(profileForm.longitude) : null,
      };
      await api.put('/api/hospitals/me', payload);
      setMessage('Hospital profile saved successfully.');
      await loadData();
    } catch (err) {
      setError(err.message || 'Failed to save hospital profile');
    } finally {
      setSavingProfile(false);
    }
  };

  const handleSaveStock = async (bg) => {
    const draft = stockDrafts[bg] || { units: 0, minThreshold: 5 };
    setSavingStockBg(bg);
    setError('');
    setMessage('');
    try {
      await api.put(`/api/hospitals/me/stock/${encodeURIComponent(bg)}`, {
        units: Number(draft.units),
        minThreshold: Number(draft.minThreshold),
      });
      setMessage(`Updated stock for ${bg}.`);
      await loadData();
    } catch (err) {
      setError(err.message || `Failed to update stock for ${bg}`);
    } finally {
      setSavingStockBg('');
    }
  };

  const handleRecordDonation = async (e) => {
    e.preventDefault();
    setRecordingDonation(true);
    setError('');
    setMessage('');
    try {
      await api.post('/api/hospitals/me/donations', {
        donorUserId: recordForm.donorUserId.trim(),
        requestId: recordForm.requestId.trim() || null,
        units: Number(recordForm.units),
        donatedAt: recordForm.donatedAt,
      });
      setMessage('Direct donation recorded and verified.');
      setRecordForm((prev) => ({ ...prev, requestId: '' }));
      await loadData();
    } catch (err) {
      setError(err.message || 'Failed to record donation');
    } finally {
      setRecordingDonation(false);
    }
  };

  const handleVerifyDonation = async (donationId, status, reason = null) => {
    setError('');
    setMessage('');
    try {
      await api.patch(`/api/hospitals/me/donations/${donationId}/verify`, {
        status,
        rejectionReason: reason,
      });
      setMessage(`Donation ${status === 'VERIFIED' ? 'verified' : 'rejected'}.`);
      setRejectingId(null);
      setRejectReason('');
      await loadData();
    } catch (err) {
      setError(err.message || 'Failed to update donation verification');
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-12 text-center text-slate-500">
        {t('common.loading')}
      </div>
    );
  }

  const isVerified = hospital?.status === 'VERIFIED';

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header & Verification Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white border border-slate-200 rounded-2xl p-6 shadow-xs">
        <div>
          <div className="flex items-center gap-2.5 flex-wrap">
            <h1 className="text-2xl font-bold text-slate-900">
              {hospital ? hospital.name : t('hospital.title')}
            </h1>
            {hospital && (
              <span
                className={`text-xs font-bold px-2.5 py-1 rounded-full ${
                  hospital.status === 'VERIFIED'
                    ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                    : hospital.status === 'PENDING'
                    ? 'bg-amber-100 text-amber-800 border border-amber-300'
                    : 'bg-red-100 text-red-800 border border-red-300'
                }`}
              >
                {hospital.status === 'VERIFIED'
                  ? t('hospital.statusVerified')
                  : hospital.status === 'PENDING'
                  ? t('hospital.statusPending')
                  : t('hospital.statusRejected')}
              </span>
            )}
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Acting Hospital Owner ID: <code className="font-mono text-xs bg-slate-100 px-1.5 py-0.5 rounded">{identity.userId}</code>
          </p>
          {hospital?.status === 'REJECTED' && hospital.rejectionReason && (
            <p className="text-xs font-medium text-red-600 mt-2">
              Rejection Reason: {hospital.rejectionReason}
            </p>
          )}
          {hospital?.status === 'PENDING' && (
            <p className="text-xs font-medium text-amber-700 mt-2">
              Note: Only VERIFIED hospitals can publish blood stock or verify donations. Switch to the Admin persona in the top bar to approve this hospital.
            </p>
          )}
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm">
          {error}
        </div>
      )}
      {message && (
        <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 px-4 py-3 rounded-xl text-sm font-medium">
          {message}
        </div>
      )}

      {/* Blood Stock Management Grid */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-xs">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900">{t('hospital.stockTitle')}</h2>
            <p className="text-xs text-slate-500">
              Update live blood unit inventory and low-stock alert thresholds across all 8 ABO/Rh blood groups.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {BLOOD_GROUPS.map((bg) => {
            const existing = stock.find((s) => s.bloodGroup === bg);
            const draft = stockDrafts[bg] || { units: 0, minThreshold: 5 };
            const isLow = Number(draft.units) <= Number(draft.minThreshold);

            return (
              <div
                key={bg}
                className={`p-4 rounded-xl border transition-colors ${
                  isLow
                    ? 'bg-red-50/40 border-red-200'
                    : 'bg-slate-50/60 border-slate-200'
                }`}
              >
                <div className="flex items-center justify-between mb-3">
                  <span className="inline-flex items-center justify-center w-10 h-10 rounded-xl bg-red-600 text-white font-extrabold text-sm shadow-xs">
                    {bg}
                  </span>
                  <div className="text-right">
                    {isLow ? (
                      <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-red-100 text-red-700">
                        Low Stock Alert
                      </span>
                    ) : (
                      <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700">
                        Healthy
                      </span>
                    )}
                    {existing?.stale && (
                      <div className="text-[10px] text-amber-700 font-medium mt-0.5">
                        Stale (&gt;48h)
                      </div>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 mb-3">
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                      Units Available
                    </label>
                    <input
                      type="number"
                      min="0"
                      disabled={!isVerified}
                      value={draft.units}
                      onChange={(e) =>
                        setStockDrafts({
                          ...stockDrafts,
                          [bg]: { ...draft, units: e.target.value },
                        })
                      }
                      className="w-full text-sm border border-slate-300 rounded-lg px-2.5 py-1.5 bg-white disabled:bg-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                      Min Threshold
                    </label>
                    <input
                      type="number"
                      min="0"
                      disabled={!isVerified}
                      value={draft.minThreshold}
                      onChange={(e) =>
                        setStockDrafts({
                          ...stockDrafts,
                          [bg]: { ...draft, minThreshold: e.target.value },
                        })
                      }
                      className="w-full text-sm border border-slate-300 rounded-lg px-2.5 py-1.5 bg-white disabled:bg-slate-100"
                    />
                  </div>
                </div>

                <button
                  type="button"
                  disabled={!isVerified || savingStockBg === bg}
                  onClick={() => handleSaveStock(bg)}
                  className="w-full text-xs font-semibold bg-slate-900 hover:bg-slate-800 disabled:opacity-40 text-white py-2 rounded-lg transition-colors cursor-pointer"
                >
                  {savingStockBg === bg ? 'Saving...' : `Update ${bg} Stock`}
                </button>
              </div>
            );
          })}
        </div>
      </div>

      {/* Donations Verification & Direct Recording */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Pending & Historical Donations */}
        <div className="lg:col-span-2 bg-white border border-slate-200 rounded-2xl p-6 shadow-xs">
          <h2 className="text-lg font-bold text-slate-900 mb-4">{t('hospital.claimsTitle')}</h2>
          {donations.length === 0 ? (
            <p className="text-sm text-slate-500 py-6 text-center">
              No donor claims or recorded donations for this hospital yet.
            </p>
          ) : (
            <div className="space-y-3">
              {donations.map((d) => (
                <div
                  key={d.id}
                  className="p-4 rounded-xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="inline-flex items-center justify-center px-2 py-0.5 rounded bg-red-50 text-red-700 font-bold text-xs border border-red-200">
                        {d.bloodGroup}
                      </span>
                      <span className="font-semibold text-sm text-slate-900">
                        {d.donorName || d.donorUserId}
                      </span>
                      <span className="text-xs text-slate-400">({d.donorUserId})</span>
                      <span
                        className={`text-[11px] font-bold px-2 py-0.5 rounded-full ${
                          d.status === 'VERIFIED'
                            ? 'bg-emerald-100 text-emerald-800'
                            : d.status === 'PENDING_VERIFICATION'
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {d.status}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-1">
                      Donated Date: <strong>{d.donatedAt}</strong> • Units: <strong>{d.units}</strong>
                      {d.requestId && ` • Request: ${d.requestId}`}
                    </p>
                    {d.rejectionReason && (
                      <p className="text-xs text-red-600 mt-1">Reason: {d.rejectionReason}</p>
                    )}
                  </div>

                  {d.status === 'PENDING_VERIFICATION' && isVerified && (
                    <div className="flex flex-col sm:items-end gap-2">
                      {rejectingId === d.id ? (
                        <div className="flex items-center gap-2">
                          <input
                            type="text"
                            placeholder="Rejection reason..."
                            value={rejectReason}
                            onChange={(e) => setRejectReason(e.target.value)}
                            className="text-xs border border-slate-300 rounded-lg px-2.5 py-1.5"
                          />
                          <button
                            type="button"
                            onClick={() => handleVerifyDonation(d.id, 'REJECTED', rejectReason || 'Could not verify record')}
                            className="text-xs font-semibold bg-red-600 text-white px-2.5 py-1.5 rounded-lg cursor-pointer"
                          >
                            Confirm
                          </button>
                          <button
                            type="button"
                            onClick={() => setRejectingId(null)}
                            className="text-xs text-slate-500 px-2 py-1 cursor-pointer"
                          >
                            Cancel
                          </button>
                        </div>
                      ) : (
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => handleVerifyDonation(d.id, 'VERIFIED')}
                            className="text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-1.5 rounded-lg transition-colors cursor-pointer"
                          >
                            Verify Claim
                          </button>
                          <button
                            type="button"
                            onClick={() => setRejectingId(d.id)}
                            className="text-xs font-semibold bg-red-50 hover:bg-red-100 text-red-700 px-3 py-1.5 rounded-lg transition-colors cursor-pointer"
                          >
                            Reject
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Direct Hospital Donation Recording */}
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-xs">
          <h2 className="text-lg font-bold text-slate-900 mb-2">{t('hospital.recordDonation')}</h2>
          <p className="text-xs text-slate-500 mb-4">
            Directly record an in-hospital blood donation for a registered donor. This immediately verifies the donation, updates donor last-donation date, and awards milestone badges.
          </p>
          <form onSubmit={handleRecordDonation} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Donor User ID *
              </label>
              <input
                type="text"
                required
                disabled={!isVerified}
                value={recordForm.donorUserId}
                onChange={(e) => setRecordForm({ ...recordForm, donorUserId: e.target.value })}
                placeholder="e.g. donor-1"
                className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Linked Blood Request ID (Optional)
              </label>
              <input
                type="text"
                disabled={!isVerified}
                value={recordForm.requestId}
                onChange={(e) => setRecordForm({ ...recordForm, requestId: e.target.value })}
                placeholder="e.g. req-demo-1"
                className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Units (1-5) *
                </label>
                <input
                  type="number"
                  min="1"
                  max="5"
                  required
                  disabled={!isVerified}
                  value={recordForm.units}
                  onChange={(e) => setRecordForm({ ...recordForm, units: e.target.value })}
                  className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Donation Date *
                </label>
                <input
                  type="date"
                  required
                  disabled={!isVerified}
                  value={recordForm.donatedAt}
                  onChange={(e) => setRecordForm({ ...recordForm, donatedAt: e.target.value })}
                  className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2"
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={!isVerified || recordingDonation}
              className="w-full bg-red-600 hover:bg-red-700 disabled:opacity-40 text-white font-semibold text-sm py-2.5 rounded-lg transition-colors cursor-pointer"
            >
              {recordingDonation ? 'Recording...' : 'Record Verified Donation'}
            </button>
          </form>
        </div>
      </div>

      {/* Hospital Profile Editor */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-xs">
        <h2 className="text-lg font-bold text-slate-900 mb-4">
          {hospital ? 'Update Hospital Details' : 'Register Hospital Profile'}
        </h2>
        <form onSubmit={handleSaveProfile} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Hospital Name *</label>
            <input
              type="text"
              required
              value={profileForm.name}
              onChange={(e) => setProfileForm({ ...profileForm, name: e.target.value })}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Registration Number *</label>
            <input
              type="text"
              required
              value={profileForm.registrationNumber}
              onChange={(e) => setProfileForm({ ...profileForm, registrationNumber: e.target.value })}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Contact Person</label>
            <input
              type="text"
              value={profileForm.contactPerson}
              onChange={(e) => setProfileForm({ ...profileForm, contactPerson: e.target.value })}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Phone (10-digit Indian) *</label>
            <input
              type="text"
              required
              value={profileForm.phone}
              onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">City *</label>
            <input
              type="text"
              required
              value={profileForm.city}
              onChange={(e) => setProfileForm({ ...profileForm, city: e.target.value })}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Address *</label>
            <input
              type="text"
              required
              value={profileForm.address}
              onChange={(e) => setProfileForm({ ...profileForm, address: e.target.value })}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2"
            />
          </div>
          <div className="sm:col-span-2 lg:col-span-3 flex justify-end">
            <button
              type="submit"
              disabled={savingProfile}
              className="bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm px-6 py-2.5 rounded-lg transition-colors cursor-pointer"
            >
              {savingProfile ? 'Saving...' : t('common.save')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
