import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import en from '../i18n/en.json';
import hi from '../i18n/hi.json';
import { api } from '../api/client';
import { useIdentity } from './IdentityContext';

const dictionaries = { en, hi };
const LANG_STORAGE_KEY = 'rakt_lang';

const LanguageContext = createContext(null);

export function LanguageProvider({ children }) {
  const { userProfile, isAuthenticated } = useIdentity();
  const manualOverrideRef = useRef(false);
  const lastUserIdRef = useRef(null);
  const [lang, setLangState] = useState(() => {
    return localStorage.getItem(LANG_STORAGE_KEY) || 'en';
  });

  useEffect(() => {
    if (userProfile && userProfile.preferredLanguage) {
      if (lastUserIdRef.current !== userProfile.id) {
        lastUserIdRef.current = userProfile.id;
        if (!manualOverrideRef.current) {
          const serverLang = userProfile.preferredLanguage.toLowerCase() === 'hi' ? 'hi' : 'en';
          setLangState(serverLang);
          localStorage.setItem(LANG_STORAGE_KEY, serverLang);
        }
      }
    }
  }, [userProfile]);

  const setLanguage = useCallback(
    async (nextLang) => {
      manualOverrideRef.current = true;
      const normalized = nextLang === 'hi' ? 'hi' : 'en';
      setLangState(normalized);
      localStorage.setItem(LANG_STORAGE_KEY, normalized);
      if (isAuthenticated) {
        try {
          await api.patch('/users/me/language', { preferredLanguage: normalized });
        } catch {
          // Ignore backend error if offline or unauthenticated
        }
      }
    },
    [isAuthenticated]
  );

  const t = useCallback(
    (key, fallback) => {
      const dict = dictionaries[lang] || dictionaries.en;
      return dict[key] || dictionaries.en[key] || fallback || key;
    },
    [lang]
  );

  return (
    <LanguageContext.Provider value={{ lang, setLanguage, t }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const ctx = useContext(LanguageContext);
  if (!ctx) {
    throw new Error('useLanguage must be used within a LanguageProvider');
  }
  return ctx;
}
