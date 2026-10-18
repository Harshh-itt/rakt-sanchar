import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../api/client';
import { useLanguage } from '../context/LanguageContext';

const STATUS_FILTERS = ['', 'PENDING', 'VERIFIED', 'REJECTED'];

export function AdminHospitalsPage() {
  const { t } = useLanguage();
  const [statusFilter, setStatusFilter] = useState('');
  const [hospitals, setHospitals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const [rejectingId, setRejectingId] = useState(null);
  const [rejectReason, setRejectReason] = useState('');

  const fetchHospitals = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const qs = statusFilter ? `?status=${statusFilter}` : '';
      const res = await api.get(`/api/admin/hospitals${qs}`);
      setHospitals(Array.isArray(res) ? res : []);
    } catch (err) {
      setError(err.message || 'Failed to load hospital queue');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    fetchHospitals();
  }, [fetchHospitals]);

  const handleApprove = async (id, name) => {
    setError('');
    setMessage('');
    try {
      await api.post(`/api/admin/hospitals/${id}/approve`, {});
      setMessage(`Approved hospital: ${name}`);
      await fetchHospitals();
    } catch (err) {
      setError(err.message || 'Failed to approve hospital');
    }
  };

  const handleReject = async (id, name) => {
    if (!rejectReason.trim()) {
      setError('Please enter a rejection reason');
      return;
    }
    setError('');
    setMessage('');
    try {
      await api.post(`/api/admin/hospitals/${id}/reject`, {
        reason: rejectReason.trim(),
      });
      setMessage(`Rejected hospital: ${name}`);
      setRejectingId(null);
      setRejectReason('');
      await fetchHospitals();
    } catch (err) {
      setError(err.message || 'Failed to reject hospital');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white border border-slate-200 rounded-2xl p-6 shadow-xs">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{t('admin.title')}</h1>
          <p className="text-sm text-slate-500 mt-1">
            Review hospital registrations, verify license numbers, and approve or reject blood bank portals.
          </p>
        </div>
        <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-xl">
          {STATUS_FILTERS.map((st) => (
            <button
              key={st || 'ALL'}
              type="button"
              onClick={() => setStatusFilter(st)}
              className={`text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors cursor-pointer ${
                statusFilter === st
                  ? 'bg-white text-slate-900 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {st || 'ALL'}
            </button>
          ))}
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

      {loading ? (
        <div className="text-center py-12 text-slate-500">{t('common.loading')}</div>
      ) : hospitals.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-10 text-center text-slate-500">
          {t('common.noData')}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {hospitals.map((h) => (
            <div
              key={h.id}
              className="bg-white border border-slate-200 rounded-2xl p-6 shadow-xs flex flex-col lg:flex-row lg:items-center justify-between gap-4"
            >
              <div className="space-y-1">
                <div className="flex items-center gap-2.5 flex-wrap">
                  <h2 className="text-lg font-bold text-slate-900">{h.name}</h2>
                  <span
                    className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                      h.status === 'VERIFIED'
                        ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                        : h.status === 'PENDING'
                        ? 'bg-amber-100 text-amber-800 border border-amber-300'
                        : 'bg-red-100 text-red-800 border border-red-300'
                    }`}
                  >
                    {h.status}
                  </span>
                  <span className="text-xs font-mono bg-slate-100 text-slate-600 px-2 py-0.5 rounded">
                    Reg: {h.registrationNumber}
                  </span>
                </div>
                <p className="text-xs text-slate-600">
                  {h.address}, <strong>{h.city}</strong> {h.state ? `, ${h.state}` : ''} {h.pincode || ''}
                </p>
                <p className="text-xs text-slate-500">
                  Contact: {h.contactPerson || 'N/A'} • Phone: <strong>{h.phone}</strong> • Owner User ID:{' '}
                  <code className="font-mono bg-slate-100 px-1 rounded">{h.ownerUserId}</code>
                </p>
                {h.rejectionReason && (
                  <p className="text-xs font-medium text-red-600 mt-1">
                    Rejection Reason: {h.rejectionReason}
                  </p>
                )}
              </div>

              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 shrink-0">
                {rejectingId === h.id ? (
                  <div className="flex items-center gap-2">
                    <input
                      type="text"
                      placeholder={t('admin.rejectReason')}
                      value={rejectReason}
                      onChange={(e) => setRejectReason(e.target.value)}
                      className="text-xs border border-slate-300 rounded-lg px-3 py-2 w-56"
                    />
                    <button
                      type="button"
                      onClick={() => handleReject(h.id, h.name)}
                      className="text-xs font-semibold bg-red-600 hover:bg-red-700 text-white px-3 py-2 rounded-lg cursor-pointer"
                    >
                      Confirm Reject
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setRejectingId(null);
                        setRejectReason('');
                      }}
                      className="text-xs text-slate-500 px-2 py-1 cursor-pointer"
                    >
                      {t('common.cancel')}
                    </button>
                  </div>
                ) : (
                  <>
                    {h.status !== 'VERIFIED' && (
                      <button
                        type="button"
                        onClick={() => handleApprove(h.id, h.name)}
                        className="text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-lg transition-colors cursor-pointer"
                      >
                        {t('admin.approve')}
                      </button>
                    )}
                    {h.status !== 'REJECTED' && (
                      <button
                        type="button"
                        onClick={() => {
                          setRejectingId(h.id);
                          setRejectReason('');
                        }}
                        className="text-xs font-semibold bg-red-50 hover:bg-red-100 text-red-700 border border-red-200 px-4 py-2 rounded-lg transition-colors cursor-pointer"
                      >
                        {t('admin.reject')}
                      </button>
                    )}
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
