import '@testing-library/jest-dom';

if (typeof globalThis.localStorage === 'undefined' || typeof globalThis.localStorage.clear !== 'function') {
  const store = new Map();
  const storageShim = {
    getItem: (key) => (store.has(String(key)) ? store.get(String(key)) : null),
    setItem: (key, value) => store.set(String(key), String(value)),
    removeItem: (key) => store.delete(String(key)),
    clear: () => store.clear(),
    key: (index) => Array.from(store.keys())[index] ?? null,
    get length() {
      return store.size;
    },
  };
  Object.defineProperty(globalThis, 'localStorage', {
    value: storageShim,
    writable: true,
    configurable: true,
  });
  if (typeof window !== 'undefined') {
    Object.defineProperty(window, 'localStorage', {
      value: storageShim,
      writable: true,
      configurable: true,
    });
  }
}
