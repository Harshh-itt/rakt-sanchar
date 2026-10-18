import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useIdentity } from '../context/IdentityContext';
import { useLanguage } from '../context/LanguageContext';

export default function DemoIdentityBanner() {
  const { identity, userProfile, presets, switchIdentity } = useIdentity();
  const { t } = useLanguage();
  const [customMode, setCustomMode] = useState(false);
  const [customUserId, setCustomUserId] = useState('');
  const [customRole, setCustomRole] = useState('DONOR');

  const currentPreset = presets.find(
    (p) => p.userId === identity.userId && p.role === identity.role
  );

  const handleSelectChange = (e) => {
    const val = e.target.value;
    if (val === '__custom__') {
      setCustomMode(true);
      return;
    }
    setCustomMode(false);
    const chosen = presets.find((p) => p.key === val);
    if (chosen) {
      switchIdentity(chosen.userId, chosen.role);
    }
  };

  const handleCustomSubmit = (e) => {
    e.preventDefault();
    if (!customUserId.trim()) return;
    switchIdentity(customUserId.trim(), customRole);
    setCustomMode(false);
  };

  return (
    <div
      className="bg-amber-950 text-amber-100 border-b border-amber-800 px-4 py-2 text-xs"
      data-testid="demo-identity-banner"
    >
      <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center px-2 py-0.5 rounded font-semibold bg-amber-400 text-amber-950 uppercase tracking-wider text-[10px]">
            Demo Stub
          </span>
          <span className="font-medium">{t('demo.bannerTitle')}</span>
          {identity.userId ? (
            <span className="hidden md:inline text-amber-300 font-mono">
              [X-Demo-User-Id: {identity.userId} | X-Demo-Role: {identity.role}]
            </span>
          ) : (
            <span className="hidden md:inline text-amber-300 font-mono">
              [No Identity Headers — Guest]
            </span>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <label htmlFor="demo-persona-select" className="text-amber-200 font-medium">
            {t('demo.actingAs')}
          </label>
          <select
            id="demo-persona-select"
            aria-label="Demo Identity Selector"
            value={customMode ? '__custom__' : currentPreset ? currentPreset.key : '__custom__'}
            onChange={handleSelectChange}
            className="bg-amber-900 text-white border border-amber-700 rounded px-2 py-1 text-xs focus:outline-none focus:ring-2 focus:ring-amber-400"
          >
            {presets.map((p) => (
              <option key={p.key} value={p.key}>
                {p.label}
              </option>
            ))}
            <option value="__custom__">{t('demo.custom')}</option>
          </select>

          {userProfile && !userProfile.onboarded && (
            <Link
              to="/onboarding"
              className="bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-semibold px-2.5 py-1 rounded transition"
            >
              {t('demo.completeOnboarding')}
            </Link>
          )}
        </div>
      </div>

      {customMode && (
        <form
          onSubmit={handleCustomSubmit}
          className="max-w-7xl mx-auto mt-2 pt-2 border-t border-amber-800/80 flex flex-wrap items-center gap-2"
        >
          <span className="text-amber-200">New/Custom Demo User:</span>
          <input
            type="text"
            placeholder="e.g. donor-new-99"
            value={customUserId}
            onChange={(e) => setCustomUserId(e.target.value)}
            className="bg-amber-900 text-white border border-amber-700 rounded px-2 py-1 text-xs"
            required
          />
          <select
            value={customRole}
            onChange={(e) => setCustomRole(e.target.value)}
            className="bg-amber-900 text-white border border-amber-700 rounded px-2 py-1 text-xs"
          >
            <option value="DONOR">DONOR</option>
            <option value="HOSPITAL">HOSPITAL</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <button
            type="submit"
            className="bg-amber-400 hover:bg-amber-300 text-amber-950 font-semibold px-2.5 py-1 rounded"
          >
            Apply Identity
          </button>
          <button
            type="button"
            onClick={() => setCustomMode(false)}
            className="text-amber-300 hover:text-white px-2 py-1"
          >
            Cancel
          </button>
        </form>
      )}
    </div>
  );
}
