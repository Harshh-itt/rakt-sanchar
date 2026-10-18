const STORAGE_KEY = 'rakt_demo_identity';
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export const DEMO_PRESETS = [
  {
    key: 'guest',
    label: 'Guest (Public Visitor)',
    userId: '',
    role: '',
    description: 'No headers sent — browse stock, hospitals & public requests',
  },
  {
    key: 'donor-1',
    label: 'Priya Sharma — Donor (O+, Mumbai, Eligible)',
    userId: 'donor-1',
    role: 'DONOR',
    description: 'Eligible O+ donor in Mumbai with 120d since last donation',
  },
  {
    key: 'donor-2',
    label: 'Rohan Verma — Donor (A+, Delhi, Cooling Off)',
    userId: 'donor-2',
    role: 'DONOR',
    description: 'Ineligible A+ donor in Delhi (donated 45 days ago, Hindi UI)',
  },
  {
    key: 'donor-3',
    label: 'Ananya Nair — Donor (O-, Bengaluru, Universal)',
    userId: 'donor-3',
    role: 'DONOR',
    description: 'Universal O- donor in Bengaluru with verified donation & badges',
  },
  {
    key: 'donor-4',
    label: 'Siddharth Kulkarni — Donor (B+, Hyderabad)',
    userId: 'donor-4',
    role: 'DONOR',
    description: 'Eligible B+ donor in Hyderabad',
  },
  {
    key: 'donor-5',
    label: 'Fatima Sheikh — Donor (AB+, Pune, First-Time)',
    userId: 'donor-5',
    role: 'DONOR',
    description: 'Eligible first-time AB+ donor in Pune',
  },
  {
    key: 'hospital-1',
    label: 'City Care Hospital — Verified (Mumbai)',
    userId: 'hospital-1',
    role: 'HOSPITAL',
    description: 'Verified hospital in Mumbai managing blood stock & verifying donations',
  },
  {
    key: 'hospital-2',
    label: 'Janata Seva Hospital — Pending (Delhi)',
    userId: 'hospital-2',
    role: 'HOSPITAL',
    description: 'Pending hospital in Delhi awaiting Admin approval',
  },
  {
    key: 'hospital-3',
    label: 'Kaveri Heart & Trauma — Verified (Bengaluru)',
    userId: 'hospital-3',
    role: 'HOSPITAL',
    description: 'Verified hospital in Bengaluru',
  },
  {
    key: 'admin-1',
    label: 'Aarav Mehta — System Admin',
    userId: 'admin-1',
    role: 'ADMIN',
    description: 'System administrator able to approve or reject hospitals',
  },
];

export function getStoredIdentity() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return { userId: 'donor-1', role: 'DONOR' };
    }
    const parsed = JSON.parse(raw);
    return {
      userId: parsed.userId || '',
      role: parsed.role || '',
    };
  } catch {
    return { userId: 'donor-1', role: 'DONOR' };
  }
}

export function setStoredIdentity(identity) {
  if (!identity || !identity.userId || !identity.role) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ userId: '', role: '' }));
  } else {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        userId: identity.userId.trim(),
        role: identity.role.trim().toUpperCase(),
      })
    );
  }
}

export function buildHeaders(extraHeaders = {}) {
  const headers = {
    Accept: 'application/json',
    ...extraHeaders,
  };
  const identity = getStoredIdentity();
  if (identity.userId && identity.role) {
    headers['X-Demo-User-Id'] = identity.userId;
    headers['X-Demo-Role'] = identity.role;
  }
  return headers;
}

export async function apiRequest(path, options = {}) {
  const url = path.startsWith('http')
    ? path
    : `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;

  const customHeaders = { ...(options.headers || {}) };
  let body = options.body;
  if (body !== undefined && body !== null && typeof body === 'object' && !(body instanceof FormData)) {
    customHeaders['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }

  const response = await fetch(url, {
    ...options,
    headers: buildHeaders(customHeaders),
    body,
  });

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!response.ok) {
    const error = new Error((data && data.message) || `Request failed with status ${response.status}`);
    error.status = response.status;
    error.code = (data && data.code) || 'ERROR';
    error.fieldErrors = (data && data.fieldErrors) || {};
    throw error;
  }

  return data;
}

export const api = {
  get: (path) => apiRequest(path, { method: 'GET' }),
  post: (path, body) => apiRequest(path, { method: 'POST', body }),
  put: (path, body) => apiRequest(path, { method: 'PUT', body }),
  patch: (path, body) => apiRequest(path, { method: 'PATCH', body }),
  delete: (path) => apiRequest(path, { method: 'DELETE' }),
};
