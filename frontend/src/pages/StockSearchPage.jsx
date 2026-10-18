import React, { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';
import { useLanguage } from '../context/LanguageContext';

const BLOOD_GROUPS = ['', 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export default function StockSearchPage() {
  const { t } = useLanguage();
  const [bloodGroup, setBloodGroup] = useState('');
  const [city, setCity] = useState('');
  const [coords, setCoords] = useState(null);
  const [radiusKm, setRadiusKm] = useState('25');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [geoStatus, setGeoStatus] = useState('');

  const fetchStock = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (bloodGroup) params.set('bloodGroup', bloodGroup);
      if (city.trim()) params.set('city', city.trim());
      if (coords) {
        params.set('lat', String(coords.lat));
        params.set('lng', String(coords.lng));
        if (radiusKm) params.set('radiusKm', String(radiusKm));
      }
      const res = await api.get(`/stock?${params.toString()}`);
      setItems(Array.isArray(res) ? res : res?.content || []);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, [bloodGroup, city, coords, radiusKm]);

  useEffect(() => {
    fetchStock();
  }, [fetchStock]);

  const handleUseGeolocation = () => {
    if (!navigator.geolocation) {
      setGeoStatus('Browser geolocation not supported — using city filter.');
      return;
    }
    setGeoStatus('Locating...');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({
          lat: Number(pos.coords.latitude.toFixed(4)),
          lng: Number(pos.coords.longitude.toFixed(4)),
        });
        setGeoStatus(
          `Using coordinates (${pos.coords.latitude.toFixed(3)}, ${pos.coords.longitude.toFixed(3)})`
        );
      },
      () => {
        setGeoStatus('Location permission denied — falling back to city search.');
      }
    );
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{t('stock.title')}</h1>
        <p className="text-sm text-slate-600 mt-1">{t('stock.subtitle')}</p>
      </div>

      {/* Filter Bar */}
      <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs flex flex-wrap items-end gap-4">
        <div>
          <label
            htmlFor="stock-bg-select"
            className="block text-xs font-semibold text-slate-700 mb-1"
          >
            Blood Group
          </label>
          <select
            id="stock-bg-select"
            value={bloodGroup}
            onChange={(e) => setBloodGroup(e.target.value)}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm bg-white min-w-36"
          >
            <option value="">All Blood Groups</option>
            {BLOOD_GROUPS.filter(Boolean).map((bg) => (
              <option key={bg} value={bg}>
                {bg}
              </option>
            ))}
          </select>
        </div>

        <div className="flex-1 min-w-48">
          <label
            htmlFor="stock-city-input"
            className="block text-xs font-semibold text-slate-700 mb-1"
          >
            City
          </label>
          <input
            id="stock-city-input"
            type="text"
            placeholder="e.g. Mumbai, Bengaluru, Delhi"
            value={city}
            onChange={(e) => setCity(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleUseGeolocation}
            className="px-3.5 py-2 rounded-lg border border-slate-300 hover:bg-slate-100 text-xs font-semibold text-slate-700 transition"
          >
            📍 Nearby (HTML5 Geolocation)
          </button>
          {coords && (
            <button
              type="button"
              onClick={() => {
                setCoords(null);
                setGeoStatus('');
              }}
              className="text-xs text-red-600 hover:underline"
            >
              Clear GPS
            </button>
          )}
        </div>
      </div>

      {geoStatus && <p className="text-xs text-slate-600">{geoStatus}</p>}

      {/* Results Grid */}
      {loading ? (
        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-sm text-slate-500">
          Searching verified hospital blood stock...
        </div>
      ) : items.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-sm text-slate-500">
          No matching blood stock entries found in verified hospitals.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((item) => (
            <div
              key={item.id || `${item.hospitalId}-${item.bloodGroup}`}
              className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs flex flex-col justify-between gap-4"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                    ✓ Verified Hospital
                  </span>
                  <h2 className="font-bold text-slate-900 text-base mt-1">{item.hospitalName}</h2>
                  <p className="text-xs text-slate-500 mt-0.5">📍 {item.city}</p>
                </div>
                <div className="w-12 h-12 rounded-xl bg-red-600 text-white font-extrabold text-lg flex items-center justify-center shrink-0">
                  {item.bloodGroup}
                </div>
              </div>

              <div className="border-t border-slate-100 pt-3 flex items-center justify-between text-xs">
                <div>
                  <span className="text-slate-500 block">Available Units</span>
                  <span className="text-lg font-extrabold text-slate-900">
                    {item.units} unit(s)
                  </span>
                  {item.lowStock && (
                    <span className="ml-2 px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 font-semibold text-[10px]">
                      Low Stock
                    </span>
                  )}
                </div>
                {item.distanceKm !== null && item.distanceKm !== undefined && (
                  <div className="text-right">
                    <span className="text-slate-500 block">Distance</span>
                    <span className="font-bold text-slate-800">{item.distanceKm.toFixed(1)} km</span>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
