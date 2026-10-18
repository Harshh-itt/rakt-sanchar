import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api, DEMO_PRESETS, getStoredIdentity, setStoredIdentity } from '../api/client';

const IdentityContext = createContext(null);

export function IdentityProvider({ children }) {
  const [identity, setIdentityState] = useState(() => getStoredIdentity());
  const [userProfile, setUserProfile] = useState(null);
  const [loadingProfile, setLoadingProfile] = useState(false);

  const refreshUserProfile = useCallback(async (activeIdentity = identity) => {
    if (!activeIdentity.userId || !activeIdentity.role) {
      setUserProfile(null);
      return null;
    }
    setLoadingProfile(true);
    try {
      const profile = await api.get('/users/me');
      setUserProfile(profile);
      return profile;
    } catch {
      setUserProfile(null);
      return null;
    } finally {
      setLoadingProfile(false);
    }
  }, [identity]);

  useEffect(() => {
    refreshUserProfile(identity);
  }, [identity, refreshUserProfile]);

  const switchIdentity = useCallback((nextUserId, nextRole) => {
    const normalized = {
      userId: nextUserId ? nextUserId.trim() : '',
      role: nextRole ? nextRole.trim().toUpperCase() : '',
    };
    setStoredIdentity(normalized);
    setIdentityState(normalized);
  }, []);

  const isAuthenticated = Boolean(identity.userId && identity.role);

  return (
    <IdentityContext.Provider
      value={{
        identity,
        userProfile,
        loadingProfile,
        isAuthenticated,
        presets: DEMO_PRESETS,
        switchIdentity,
        refreshUserProfile,
      }}
    >
      {children}
    </IdentityContext.Provider>
  );
}

export function useIdentity() {
  const ctx = useContext(IdentityContext);
  if (!ctx) {
    throw new Error('useIdentity must be used within an IdentityProvider');
  }
  return ctx;
}
