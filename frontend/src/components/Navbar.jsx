import React, { useState, useEffect, useCallback } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { useIdentity } from '../context/IdentityContext';
import { useLanguage } from '../context/LanguageContext';
import { api } from '../api/client';

export default function Navbar() {
  const { identity, isAuthenticated } = useIdentity();
  const { lang, setLanguage, t } = useLanguage();
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [loadingNotifs, setLoadingNotifs] = useState(false);

  const fetchUnreadCount = useCallback(async () => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      setNotifications([]);
      return;
    }
    try {
      const res = await api.get('/notifications/unread-count');
      setUnreadCount(res?.unreadCount ?? 0);
    } catch {
      setUnreadCount(0);
    }
  }, [isAuthenticated, identity]);

  const fetchNotifications = useCallback(async () => {
    if (!isAuthenticated) return;
    setLoadingNotifs(true);
    try {
      const res = await api.get('/notifications?page=0&size=20');
      setNotifications(res?.content || []);
      const countRes = await api.get('/notifications/unread-count');
      setUnreadCount(countRes?.unreadCount ?? 0);
    } catch {
      setNotifications([]);
    } finally {
      setLoadingNotifs(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchUnreadCount();
  }, [fetchUnreadCount, lang]);

  const handleToggleDrawer = () => {
    const next = !drawerOpen;
    setDrawerOpen(next);
    if (next) {
      fetchNotifications();
    }
  };

  const handleMarkRead = async (id) => {
    try {
      await api.patch(`/notifications/${id}/read`);
      await fetchNotifications();
    } catch {
      // ignore
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await api.patch('/notifications/read-all');
      await fetchNotifications();
    } catch {
      // ignore
    }
  };

  const navLinkClass = ({ isActive }) =>
    `px-3 py-1.5 rounded-md text-sm font-medium transition ${
      isActive
        ? 'bg-red-700 text-white'
        : 'text-slate-700 hover:bg-red-50 hover:text-red-700'
    }`;

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-xs">
      <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between gap-4">
        <div className="flex items-center gap-6">
          <Link to="/" className="flex items-center gap-2.5 group">
            <span className="w-9 h-9 rounded-lg bg-red-600 text-white flex items-center justify-center font-bold text-lg shadow-xs group-hover:bg-red-700 transition">
              🩸
            </span>
            <div>
              <span className="font-bold text-lg tracking-tight text-slate-900 block leading-none">
                {t('app.title')}
              </span>
              <span className="text-[11px] text-slate-500 hidden sm:inline">
                Zero-Cost Blood Network
              </span>
            </div>
          </Link>

          <nav className="hidden md:flex items-center gap-1" aria-label="Main Navigation">
            <NavLink to="/" end className={navLinkClass}>
              {t('nav.home')}
            </NavLink>
            <NavLink to="/stock" className={navLinkClass}>
              {t('nav.stock')}
            </NavLink>
            <NavLink to="/hospitals" className={navLinkClass}>
              {t('nav.hospitals')}
            </NavLink>
            <NavLink to="/requests" className={navLinkClass}>
              {t('nav.requests')}
            </NavLink>
            {identity.role === 'DONOR' && (
              <NavLink to="/donor/dashboard" className={navLinkClass}>
                {t('nav.donorDashboard')}
              </NavLink>
            )}
            {identity.role === 'HOSPITAL' && (
              <NavLink to="/hospital/dashboard" className={navLinkClass}>
                {t('nav.hospitalDashboard')}
              </NavLink>
            )}
            {identity.role === 'ADMIN' && (
              <NavLink to="/admin/hospitals" className={navLinkClass}>
                {t('nav.adminPanel')}
              </NavLink>
            )}
          </nav>
        </div>

        <div className="flex items-center gap-3">
          {/* Language Toggle */}
          <div
            className="inline-flex rounded-lg border border-slate-200 p-0.5 bg-slate-100 text-xs"
            role="group"
            aria-label="Language Switcher"
          >
            <button
              type="button"
              onClick={() => setLanguage('en')}
              className={`px-2.5 py-1 rounded-md font-medium transition ${
                lang === 'en' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              EN
            </button>
            <button
              type="button"
              onClick={() => setLanguage('hi')}
              className={`px-2.5 py-1 rounded-md font-medium transition ${
                lang === 'hi' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              हिन्दी
            </button>
          </div>

          {/* Notification Bell & Drawer */}
          {isAuthenticated && (
            <div className="relative">
              <button
                type="button"
                onClick={handleToggleDrawer}
                aria-label="Notifications"
                data-testid="notification-bell"
                className="relative p-2 rounded-lg text-slate-700 hover:bg-slate-100 border border-slate-200 transition"
              >
                <span role="img" aria-hidden="true">
                  🔔
                </span>
                {unreadCount > 0 && (
                  <span
                    data-testid="unread-badge"
                    className="absolute -top-1.5 -right-1.5 min-w-5 h-5 px-1 rounded-full bg-red-600 text-white text-[11px] font-bold flex items-center justify-center"
                  >
                    {unreadCount}
                  </span>
                )}
              </button>

              {drawerOpen && (
                <div
                  data-testid="notification-drawer"
                  className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-xl shadow-xl border border-slate-200 z-50 overflow-hidden"
                >
                  <div className="px-4 py-3 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
                    <h3 className="font-semibold text-sm text-slate-900">
                      {t('nav.notifications')} ({unreadCount})
                    </h3>
                    <div className="flex items-center gap-2">
                      {unreadCount > 0 && (
                        <button
                          type="button"
                          onClick={handleMarkAllRead}
                          className="text-xs text-red-600 hover:text-red-700 font-medium"
                        >
                          {t('nav.markAllRead')}
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => setDrawerOpen(false)}
                        className="text-slate-400 hover:text-slate-600 text-sm px-1"
                        aria-label="Close notifications"
                      >
                        ✕
                      </button>
                    </div>
                  </div>

                  <div className="max-h-96 overflow-y-auto divide-y divide-slate-100">
                    {loadingNotifs ? (
                      <div className="p-4 text-center text-xs text-slate-500">Loading...</div>
                    ) : notifications.length === 0 ? (
                      <div className="p-6 text-center text-sm text-slate-500">
                        {t('nav.noNotifications')}
                      </div>
                    ) : (
                      notifications.map((n) => (
                        <div
                          key={n.id}
                          className={`p-3.5 text-xs transition ${
                            n.read ? 'bg-white text-slate-600' : 'bg-red-50/60 text-slate-900'
                          }`}
                        >
                          <div className="flex items-start justify-between gap-2">
                            <span className="font-semibold text-[11px] uppercase tracking-wider text-red-700">
                              {n.type}
                            </span>
                            {!n.read && (
                              <button
                                type="button"
                                onClick={() => handleMarkRead(n.id)}
                                className="text-[11px] text-red-600 hover:underline font-medium shrink-0"
                              >
                                Mark read
                              </button>
                            )}
                          </div>
                          <p className="mt-1 text-sm leading-snug">{n.message}</p>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Mobile Navigation Bar */}
      <div className="md:hidden border-t border-slate-100 px-4 py-2 flex flex-wrap gap-1.5 bg-slate-50">
        <NavLink to="/" end className={navLinkClass}>
          {t('nav.home')}
        </NavLink>
        <NavLink to="/stock" className={navLinkClass}>
          {t('nav.stock')}
        </NavLink>
        <NavLink to="/hospitals" className={navLinkClass}>
          {t('nav.hospitals')}
        </NavLink>
        <NavLink to="/requests" className={navLinkClass}>
          {t('nav.requests')}
        </NavLink>
        {identity.role === 'DONOR' && (
          <NavLink to="/donor/dashboard" className={navLinkClass}>
            {t('nav.donorDashboard')}
          </NavLink>
        )}
        {identity.role === 'HOSPITAL' && (
          <NavLink to="/hospital/dashboard" className={navLinkClass}>
            {t('nav.hospitalDashboard')}
          </NavLink>
        )}
        {identity.role === 'ADMIN' && (
          <NavLink to="/admin/hospitals" className={navLinkClass}>
            {t('nav.adminPanel')}
          </NavLink>
        )}
      </div>
    </header>
  );
}
