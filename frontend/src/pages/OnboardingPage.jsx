import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useIdentity } from '../context/IdentityContext';
import { useLanguage } from '../context/LanguageContext';

const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export default function OnboardingPage() {
  const { identity, refreshUserProfile } = useIdentity();
  const { lang, t } = useLanguage();
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [bloodGroup, setBloodGroup] = useState('O+');
  const [dateOfBirth, setDateOfBirth] = useState('1996-05-15');
  const [phone, setPhone] = useState('9876543210');
  const [city, setCity] = useState('Mumbai');
  const [pincode, setPincode] = useState('400001');
  const [medicalConfirmed, setMedicalConfirmed] = useState(true);

  const [hospitalName, setHospitalName] = useState('');
  const [registrationNumber, setRegistrationNumber] = useState('MH-REG-2026-100');
  const [contactPerson, setContactPerson] = useState('');
  const [address, setAddress] = useState('');
  const [stateName, setStateName] = useState('Maharashtra');

  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const role = identity.role || 'DONOR';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      if (role === 'DONOR') {
        await api.put('/donors/me', {
          name,
          bloodGroup,
          dateOfBirth,
          phone,
          city,
          pincode,
          medicalConditionsConfirmed: medicalConfirmed,
          available: true,
        });
        await api.post('/users/me/onboarding', {
          role: 'DONOR',
          name,
          preferredLanguage: lang,
          bloodGroup,
          dateOfBirth,
          phone,
          city,
          pincode,
          medicalConditionsConfirmed: medicalConfirmed,
          available: true,
        });
        await refreshUserProfile();
        navigate('/donor/dashboard');
      } else if (role === 'HOSPITAL') {
        await api.put('/hospitals/me', {
          name: hospitalName || name,
          registrationNumber,
          contactPerson: contactPerson || name,
          address,
          city,
          state: stateName,
          pincode,
          phone,
        });
        await api.post('/users/me/onboarding', {
          role: 'HOSPITAL',
          name: hospitalName || name,
          preferredLanguage: lang,
          hospitalName: hospitalName || name,
          registrationNumber,
          contactPerson: contactPerson || name,
          address,
          city,
          pincode,
          phone,
        });
        await refreshUserProfile();
        navigate('/hospital/dashboard');
      } else {
        await api.post('/users/me/onboarding', {
          role: 'ADMIN',
          name: name || 'System Admin',
          preferredLanguage: lang,
        });
        await refreshUserProfile();
        navigate('/admin/hospitals');
      }
    } catch (err) {
      setError(err.message || 'Onboarding failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto py-8 px-4">
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs p-6">
        <div className="mb-6">
          <span className="inline-block px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-700 mb-2">
            Role: {role}
          </span>
          <h1 className="text-2xl font-bold text-slate-900">{t('onboarding.title')}</h1>
          <p className="text-sm text-slate-600 mt-1">
            Completing profile setup for demo identity{' '}
            <code className="font-mono bg-slate-100 px-1.5 py-0.5 rounded">{identity.userId}</code>
          </p>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              {role === 'HOSPITAL' ? 'Contact Person / Administrator Name' : 'Full Name'}
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g.Kavita Deshmukh"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
            />
          </div>

          {role === 'DONOR' && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Blood Group
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
                    Date of Birth
                  </label>
                  <input
                    type="date"
                    required
                    value={dateOfBirth}
                    onChange={(e) => setDateOfBirth(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Mobile Number (10-digit)
                  </label>
                  <input
                    type="text"
                    required
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
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
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Pincode</label>
                  <input
                    type="text"
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
              </div>

              <label className="flex items-center gap-2 text-sm text-slate-700 pt-1">
                <input
                  type="checkbox"
                  checked={medicalConfirmed}
                  onChange={(e) => setMedicalConfirmed(e.target.checked)}
                  className="rounded border-slate-300 text-red-600"
                />
                I confirm that I meet standard medical conditions for blood donation.
              </label>
            </>
          )}

          {role === 'HOSPITAL' && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Hospital Name
                  </label>
                  <input
                    type="text"
                    required
                    value={hospitalName}
                    onChange={(e) => setHospitalName(e.target.value)}
                    placeholder="e.g. Sahyadri Blood Bank & Hospital"
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Registration Number
                  </label>
                  <input
                    type="text"
                    required
                    value={registrationNumber}
                    onChange={(e) => setRegistrationNumber(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Street Address
                </label>
                <input
                  type="text"
                  required
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="Plot 10, MG Road"
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
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
                  <label className="block text-xs font-semibold text-slate-700 mb-1">State</label>
                  <input
                    type="text"
                    value={stateName}
                    onChange={(e) => setStateName(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Pincode</label>
                  <input
                    type="text"
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Phone</label>
                  <input
                    type="text"
                    required
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
              </div>
            </>
          )}

          <div className="pt-2">
            <button
              type="submit"
              disabled={submitting}
              className="w-full sm:w-auto px-5 py-2.5 rounded-lg bg-red-600 hover:bg-red-700 text-white text-sm font-semibold transition disabled:opacity-50"
            >
              {submitting ? 'Saving...' : 'Complete Onboarding'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
