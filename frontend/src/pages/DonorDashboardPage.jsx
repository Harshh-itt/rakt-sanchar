import React, { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';
import { useIdentity } from '../context/IdentityContext';
import { useLanguage } from '../context/LanguageContext';

const ALL_BADGES = [
  {
    code: 'FIRST_DROP',
    title: 'First Drop',
    icon: '🩸',
    description: 'Completed 1st verified blood donation',
  },
  {
    code: 'LIFE_SAVER_3',
    title: 'Life Saver (3+)',
    icon: '🎖️',
    description: 'Completed 3 verified blood donations',
  },
  {
    code: 'HERO_5',
    title: 'Community Hero (5+)',
    icon: '🏅',
    description: 'Completed 5 verified blood donations',
  },
  {
    code: 'CENTURION_10',
    title: 'Vital Champion (10+)',
    icon: '🏆',
    description: 'Completed 10 verified blood donations',
  },
  {
    code: 'EMERGENCY_RESPONDER',
    title: 'Emergency Responder',
    icon: '🚑',
    description: 'Donated for a CRITICAL or URGENT request, or universal O- donor',
  },
  {
    code: 'REGULAR_DONOR',
    title: 'Regular Donor',
    icon: '🌟',
    description: '2+ verified donations within 365 days',
  },
];

const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export default function DonorDashboardPage() {
  const { identity } = useIdentity();
  const { t } = useLanguage();

  const [profile, setProfile] = useState(null);
  const [summary, setSummary] = useState(null);
  const [donations, setDonations] = useState([]);
  const [hospitals, setHospitals] = useState([]);
  const [openRequests, setOpenRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  // Edit profile form state
  const [editingProfile, setEditingProfile] = useState(false);
  const [editName, setEditName] = useState('');
  const [editBg, setEditBg] = useState('O+');
  const [editDob, setEditDob] = useState('1996-05-15');
  const [editPhone, setEditPhone] = useState('');
  const [editCity, setEditCity] = useState('');
  const [editPincode, setEditPincode] = useState('');
  const [editLastDonation, setEditLastDonation] = useState('');

  // Submit donation claim form state
  const [claimHospitalId, setClaimHospitalId] = useState('');
  const [claimUnits, setClaimUnits] = useState(1);
  const [claimDate, setClaimDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [submittingClaim, setSubmittingClaim] = useState(false);

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [prof, sum, dons, hosps, reqs] = await Promise.all([
        api.get('/donors/me').catch(() => null),
        api.get('/donors/me/summary').catch(() => null),
        api.get('/donors/me/donations').catch(() => []),
        api.get('/hospitals?page=0&size=50').catch(() => ({ content: [] })),
        api.get('/requests?status=OPEN&page=0&size=20').catch(() => ({ content: [] })),
      ]);

      setProfile(prof);
      setSummary(sum);
      setDonations(Array.isArray(dons) ? dons : []);
      const hospList = hosps?.content || [];
      setHospitals(hospList);
      if (hospList.length > 0 && !claimHospitalId) {
        setClaimHospitalId(hospList[0].id);
      }
      setOpenRequests(reqs?.content || []);

      if (prof) {
        setEditName(prof.name || '');
        setEditBg(prof.bloodGroup || 'O+');
        setEditDob(prof.dateOfBirth || '1996-05-15');
        setEditPhone(prof.phone || '9876543210');
        setEditCity(prof.city || 'Mumbai');
        setEditPincode(prof.pincode || '400001');
        setEditLastDonation(prof.declaredLastDonationDate || '');
      }
    } catch (err) {
      setError(err.message || 'Failed to load donor dashboard');
    } finally {
      setLoading(false);
    }
  }, [claimHospitalId]);

  useEffect(() => {
    loadDashboard();
  }, [identity, loadDashboard]);

  const handleToggleAvailability = async () => {
    if (!profile) return;
    try {
      const updated = await api.patch('/donors/me/availability', {
        available: !profile.available,
      });
      setProfile(updated);
      setMessage(
        `Availability updated to ${updated.available ? 'Available' : 'Unavailable'}.`
      );
    } catch (err) {
      setError(err.message || 'Could not update availability');
    }
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await api.put('/donors/me', {
        name: editName,
        bloodGroup: editBg,
        dateOfBirth: editDob,
        phone: editPhone,
        city: editCity,
        pincode: editPincode,
        declaredLastDonationDate: editLastDonation || null,
        medicalConditionsConfirmed: true,
        available: profile ? profile.available : true,
      });
      setEditingProfile(false);
      setMessage('Donor profile updated.');
      await loadDashboard();
    } catch (err) {
      setError(err.message || 'Failed to update profile');
    }
  };

  const handleSubmitClaim = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmittingClaim(true);
    try {
      await api.post('/donors/me/donations', {
        hospitalId: claimHospitalId,
        units: Number(claimUnits),
        donatedAt: claimDate,
      });
      setMessage('Donation claim submitted! Awaiting hospital verification.');
      await loadDashboard();
    } catch (err) {
      setError(err.message || 'Could not submit donation claim');
    } finally {
      setSubmittingClaim(false);
    }
  };

  const handleRespondToRequest = async (requestId, status) => {
    try {
      await api.post(`/requests/${requestId}/respond`, { status });
      setMessage(`Response (${status}) recorded for request.`);
      await loadDashboard();
    } catch (err) {
      setError(err.message || 'Could not respond to request');
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-10 text-center text-sm text-slate-500">
        Loading donor dashboard...
      </div>
    );
  }

  const earnedCodes = new Set((summary?.badges || profile?.badges || []).map((b) => b.code));

  // Compute days remaining if in cooling-off period
  let coolingOffDays = 0;
  if (profile && !profile.eligible && profile.nextEligibleDate) {
    const today = new Date();
    const nextDate = new Date(profile.nextEligibleDate);
    const diffMs = nextDate.getTime() - today.getTime();
    coolingOffDays = Math.max(0, Math.ceil(diffMs / (1000 * 60 * 60 * 24)));
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{t('donor.title')}</h1>
          <p className="text-sm text-slate-600 mt-0.5">
            Manage your donation eligibility, privacy-protected profile, verified donation history,
            and earned badges.
          </p>
        </div>

        {profile && (
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={handleToggleAvailability}
              data-testid="availability-toggle"
              className={`px-4 py-2 rounded-xl text-xs font-bold transition shadow-2xs ${
                profile.available
                  ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                  : 'bg-slate-200 hover:bg-slate-300 text-slate-800'
              }`}
            >
              {profile.available ? '● Available for Calls' : '○ Marked Unavailable'}
            </button>
            <button
              type="button"
              onClick={() => setEditingProfile(!editingProfile)}
              className="px-3.5 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-xs font-semibold text-slate-700"
            >
              {editingProfile ? 'Cancel Edit' : '✎ Edit Profile'}
            </button>
          </div>
        )}
      </div>

      {message && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-sm flex items-center justify-between">
          <span>{message}</span>
          <button
            type="button"
            onClick={() => setMessage(null)}
            className="text-xs font-semibold text-emerald-700"
          >
            Dismiss
          </button>
        </div>
      )}

      {error && (
        <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm">
          {error}
        </div>
      )}

      {/* Eligibility & Summary Cards */}
      {profile && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div
            data-testid="eligibility-card"
            className={`md:col-span-2 rounded-xl p-5 border ${
              profile.eligible
                ? 'bg-emerald-50/70 border-emerald-200'
                : 'bg-amber-50/80 border-amber-200'
            }`}
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <span
                  className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-bold uppercase ${
                    profile.eligible
                      ? 'bg-emerald-600 text-white'
                      : 'bg-amber-500 text-slate-950'
                  }`}
                >
                  {profile.eligible ? '✓ Eligible to Donate Now' : '⏳ Cooling-Off / Ineligible'}
                </span>
                <h2 className="text-lg font-bold text-slate-900 mt-2">
                  {profile.name} ({profile.bloodGroup})
                </h2>
                <p className="text-xs text-slate-600 mt-1">
                  Reason Code: <code className="font-mono">{profile.eligibilityReason}</code>
                </p>
                {!profile.eligible && profile.nextEligibleDate && (
                  <p className="text-xs font-semibold text-amber-900 mt-1">
                    Next Eligible Date: {profile.nextEligibleDate} ({coolingOffDays} day(s)
                    remaining in 90-day recovery window)
                  </p>
                )}
              </div>
              <div className="w-14 h-14 rounded-2xl bg-red-600 text-white font-extrabold text-xl flex items-center justify-center shrink-0 shadow-xs">
                {profile.bloodGroup}
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-5 flex flex-col justify-between">
            <span className="text-xs font-medium text-slate-500">Verified Donations</span>
            <span className="text-3xl font-extrabold text-slate-900 mt-2">
              {summary?.totalVerifiedDonations ?? 0}
            </span>
            <span className="text-xs text-slate-500 mt-1">
              Total Units: {summary?.totalVerifiedUnits ?? 0} • Pending Claims:{' '}
              {summary?.pendingClaimsCount ?? 0}
            </span>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-5 flex flex-col justify-between">
            <span className="text-xs font-medium text-slate-500">Private Profile Info</span>
            <div className="text-xs text-slate-700 space-y-1 mt-2">
              <p>
                <strong>Phone:</strong> {profile.phone}
              </p>
              <p>
                <strong>City:</strong> {profile.city} ({profile.pincode})
              </p>
              <p>
                <strong>DOB:</strong> {profile.dateOfBirth || '—'}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Edit Profile Form */}
      {editingProfile && (
        <form
          onSubmit={handleSaveProfile}
          className="bg-white rounded-xl border border-slate-300 p-6 space-y-4 shadow-xs"
        >
          <h2 className="text-base font-bold text-slate-900">Update Donor Profile</h2>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Full Name</label>
              <input
                type="text"
                required
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Blood Group
              </label>
              <select
                value={editBg}
                onChange={(e) => setEditBg(e.target.value)}
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
                Date of Birth
              </label>
              <input
                type="date"
                required
                value={editDob}
                onChange={(e) => setEditDob(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Phone</label>
              <input
                type="text"
                required
                value={editPhone}
                onChange={(e) => setEditPhone(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">City</label>
              <input
                type="text"
                required
                value={editCity}
                onChange={(e) => setEditCity(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Pincode</label>
              <input
                type="text"
                value={editPincode}
                onChange={(e) => setEditPincode(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Declared Last Donation
              </label>
              <input
                type="date"
                value={editLastDonation}
                onChange={(e) => setEditLastDonation(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
          </div>

          <button
            type="submit"
            className="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white text-xs font-bold"
          >
            Save Profile Changes
          </button>
        </form>
      )}

      {/* Badges & Milestones Showcase */}
      <section className="bg-white rounded-xl border border-slate-200 p-6 space-y-4">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Donor Recognition Badges</h2>
          <p className="text-xs text-slate-500">
            Badges are awarded automatically when a hospital verifies your blood donation.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {ALL_BADGES.map((b) => {
            const isEarned = earnedCodes.has(b.code);
            return (
              <div
                key={b.code}
                className={`p-4 rounded-xl border flex items-start gap-3 ${
                  isEarned
                    ? 'bg-amber-50/70 border-amber-300 text-slate-900'
                    : 'bg-slate-50 border-slate-200 text-slate-400 opacity-75'
                }`}
              >
                <span className="text-2xl">{b.icon}</span>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-sm">{b.title}</h3>
                    <span
                      className={`text-[10px] px-1.5 py-0.5 rounded font-semibold uppercase ${
                        isEarned
                          ? 'bg-amber-400 text-amber-950'
                          : 'bg-slate-200 text-slate-600'
                      }`}
                    >
                      {isEarned ? 'Earned' : 'Locked'}
                    </span>
                  </div>
                  <p className="text-xs mt-1">{b.description}</p>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Open Requests Feed & Submit Claim */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Matching Open Requests */}
        <div className="lg:col-span-7 bg-white rounded-xl border border-slate-200 p-6 space-y-4">
          <h2 className="text-lg font-bold text-slate-900">Active Open Blood Requests</h2>
          {openRequests.length === 0 ? (
            <p className="text-xs text-slate-500">No open requests right now.</p>
          ) : (
            <div className="divide-y divide-slate-100">
              {openRequests.map((req) => (
                <div
                  key={req.id}
                  className="py-3.5 flex flex-wrap items-center justify-between gap-3"
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 rounded bg-red-100 text-red-800 font-bold text-[11px]">
                        {req.bloodGroup}
                      </span>
                      <span className="text-xs font-bold text-slate-900">
                        {req.patientName} ({req.unitsNeeded} unit(s))
                      </span>
                      <span className="text-[11px] text-slate-500">• {req.urgency}</span>
                    </div>
                    <p className="text-xs text-slate-600 mt-0.5">
                      {req.hospital} ({req.city})
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => handleRespondToRequest(req.id, 'ACCEPTED')}
                      className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold"
                    >
                      ✓ Accept
                    </button>
                    <button
                      type="button"
                      onClick={() => handleRespondToRequest(req.id, 'DECLINED')}
                      className="px-2.5 py-1.5 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100 text-xs"
                    >
                      Decline
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Submit Donation Claim & History */}
        <div className="lg:col-span-5 space-y-6">
          <form
            onSubmit={handleSubmitClaim}
            className="bg-white rounded-xl border border-slate-200 p-6 space-y-4"
          >
            <h2 className="text-lg font-bold text-slate-900">Submit Donation Claim</h2>
            <p className="text-xs text-slate-500">
              Donated at a verified hospital? Submit a claim so the hospital can verify it and award
              your badges.
            </p>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Verified Hospital
              </label>
              <select
                value={claimHospitalId}
                onChange={(e) => setClaimHospitalId(e.target.value)}
                required
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              >
                {hospitals.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.name} ({h.city})
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Units</label>
                <input
                  type="number"
                  min="1"
                  max="5"
                  value={claimUnits}
                  onChange={(e) => setClaimUnits(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Donation Date
                </label>
                <input
                  type="date"
                  value={claimDate}
                  onChange={(e) => setClaimDate(e.target.value)}
                  required
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={submittingClaim || hospitals.length === 0}
              className="w-full py-2.5 rounded-lg bg-red-600 hover:bg-red-700 text-white text-xs font-bold transition disabled:opacity-50"
            >
              {submittingClaim ? 'Submitting...' : 'Submit Claim for Verification'}
            </button>
          </form>

          <div className="bg-white rounded-xl border border-slate-200 p-6 space-y-3">
            <h2 className="text-base font-bold text-slate-900">My Donation History</h2>
            {donations.length === 0 ? (
              <p className="text-xs text-slate-500">No donation records yet.</p>
            ) : (
              <div className="divide-y divide-slate-100 text-xs">
                {donations.map((d) => (
                  <div key={d.id} className="py-2.5 flex items-center justify-between gap-2">
                    <div>
                      <p className="font-semibold text-slate-900">{d.hospitalName}</p>
                      <p className="text-slate-500">
                        {d.donatedAt} • {d.units} unit(s) ({d.bloodGroup})
                      </p>
                    </div>
                    <span
                      className={`px-2 py-0.5 rounded font-semibold ${
                        d.status === 'VERIFIED'
                          ? 'bg-emerald-100 text-emerald-800'
                          : d.status === 'PENDING_HOSPITAL_VERIFICATION'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-red-100 text-red-800'
                      }`}
                    >
                      {d.status}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
