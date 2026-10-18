import React from 'react';
import { useIdentity } from '../context/IdentityContext';

export default function RoleRoute({ allowedRoles = [], children }) {
  const { identity, isAuthenticated, presets, switchIdentity } = useIdentity();

  if (!isAuthenticated) {
    const suggested = presets.filter((p) =>
      allowedRoles.length === 0 ? Boolean(p.role) : allowedRoles.includes(p.role)
    );
    return (
      <div className="max-w-xl mx-auto my-12 bg-white rounded-xl shadow-xs border border-slate-200 p-6 text-center">
        <div className="w-12 h-12 rounded-full bg-amber-100 text-amber-800 flex items-center justify-center mx-auto mb-3 text-xl">
          🔒
        </div>
        <h2 className="text-lg font-bold text-slate-900">Demo Identity Required</h2>
        <p className="text-sm text-slate-600 mt-1">
          You are currently browsing as a Guest. Select a demo persona below (or in the top banner)
          to access this screen.
        </p>
        <div className="mt-4 flex flex-wrap justify-center gap-2">
          {suggested.slice(0, 3).map((p) => (
            <button
              key={p.key}
              type="button"
              onClick={() => switchIdentity(p.userId, p.role)}
              className="px-3 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white text-xs font-semibold transition"
            >
              Switch to {p.label}
            </button>
          ))}
        </div>
      </div>
    );
  }

  if (allowedRoles.length > 0 && !allowedRoles.includes(identity.role)) {
    const suggested = presets.filter((p) => allowedRoles.includes(p.role));
    return (
      <div className="max-w-xl mx-auto my-12 bg-white rounded-xl shadow-xs border border-red-200 p-6 text-center">
        <div className="w-12 h-12 rounded-full bg-red-100 text-red-700 flex items-center justify-center mx-auto mb-3 text-xl">
          ⚠️
        </div>
        <h2 className="text-lg font-bold text-slate-900">
          Role Restricted ({allowedRoles.join(' / ')} Only)
        </h2>
        <p className="text-sm text-slate-600 mt-1">
          Your current demo identity is{' '}
          <span className="font-mono font-semibold text-slate-800">
            {identity.userId} ({identity.role})
          </span>
          . Switch to a {allowedRoles.join(' or ')} persona to view this page:
        </p>
        <div className="mt-4 flex flex-wrap justify-center gap-2">
          {suggested.map((p) => (
            <button
              key={p.key}
              type="button"
              onClick={() => switchIdentity(p.userId, p.role)}
              className="px-3 py-2 rounded-lg bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold transition"
            >
              Act as {p.label}
            </button>
          ))}
        </div>
      </div>
    );
  }

  return children;
}
