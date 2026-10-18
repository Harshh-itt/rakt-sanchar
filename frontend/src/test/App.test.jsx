import React from 'react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { buildHeaders, setStoredIdentity, getStoredIdentity } from '../api/client';
import { IdentityProvider } from '../context/IdentityContext';
import { LanguageProvider } from '../context/LanguageContext';
import App from '../App';

function renderWithProviders(initialEntries = ['/']) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <IdentityProvider>
        <LanguageProvider>
          <App />
        </LanguageProvider>
      </IdentityProvider>
    </MemoryRouter>
  );
}

describe('Rakt Sanchar Frontend & Demo Identity Suite', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();

    global.fetch = vi.fn(async (url) => {
      const urlStr = String(url);
      if (urlStr.includes('/users/me')) {
        const current = getStoredIdentity();
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              id: current.userId || 'donor-1',
              role: current.role || 'DONOR',
              displayName: 'Priya Sharma',
              preferredLanguage: 'en',
              onboarded: true,
            }),
        };
      }
      if (urlStr.includes('/notifications/unread-count')) {
        return {
          ok: true,
          status: 200,
          text: async () => JSON.stringify({ unreadCount: 2 }),
        };
      }
      if (urlStr.includes('/stats/public')) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              verifiedHospitals: 2,
              eligibleAvailableDonors: 4,
              openRequests: 2,
              fulfilledRequests: 1,
              verifiedDonations: 3,
              totalAvailableUnits: 42,
            }),
        };
      }
      if (urlStr.includes('/stock')) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify([
              {
                id: 'stock-1',
                hospitalId: 'hosp-1',
                hospitalName: 'City Care Hospital',
                city: 'Mumbai',
                phone: '9820112233',
                bloodGroup: 'O+',
                units: 14,
                minThreshold: 5,
                lowStock: false,
                stale: false,
                updatedAt: new Date().toISOString(),
              },
            ]),
        };
      }
      if (urlStr.includes('/donors/me/summary')) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              userId: 'donor-1',
              fullName: 'Priya Sharma',
              bloodGroup: 'O+',
              available: true,
              eligible: true,
              ineligibilityReasons: [],
              nextEligibleDate: '2026-06-01',
              daysUntilEligible: 0,
              verifiedDonationCount: 1,
              totalVerifiedUnits: 1,
              badges: ['FIRST_DROP'],
            }),
        };
      }
      if (urlStr.includes('/donors/me/donations')) {
        return {
          ok: true,
          status: 200,
          text: async () => JSON.stringify([]),
        };
      }
      if (urlStr.includes('/donors/me')) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              id: 'dp-1',
              userId: 'donor-1',
              fullName: 'Priya Sharma',
              bloodGroup: 'O+',
              city: 'Mumbai',
              phone: '9876500001',
              available: true,
              eligible: true,
              ineligibilityReasons: [],
              daysUntilEligible: 0,
            }),
        };
      }
      return {
        ok: true,
        status: 200,
        text: async () => JSON.stringify({ content: [] }),
      };
    });
  });

  it('attaches X-Demo-User-Id and X-Demo-Role headers when identity is set and omits them for Guest', () => {
    setStoredIdentity({ userId: 'hospital-1', role: 'HOSPITAL' });
    const headers = buildHeaders();
    expect(headers['X-Demo-User-Id']).toBe('hospital-1');
    expect(headers['X-Demo-Role']).toBe('HOSPITAL');

    setStoredIdentity({ userId: '', role: '' });
    const guestHeaders = buildHeaders();
    expect(guestHeaders['X-Demo-User-Id']).toBeUndefined();
    expect(guestHeaders['X-Demo-Role']).toBeUndefined();
  });

  it('switches demo persona via DemoIdentityBanner dropdown', async () => {
    renderWithProviders(['/']);
    const select = screen.getByLabelText('Demo Identity Selector');
    expect(select.value).toBe('donor-1');

    fireEvent.change(select, { target: { value: 'hospital-1' } });

    await waitFor(() => {
      expect(getStoredIdentity()).toEqual({ userId: 'hospital-1', role: 'HOSPITAL' });
    });
  });

  it('blocks unauthorized role in RoleRoute and offers quick-switch button', async () => {
    setStoredIdentity({ userId: 'donor-1', role: 'DONOR' });
    renderWithProviders(['/admin/hospitals']);

    expect(await screen.findByText(/Role Restricted \(ADMIN Only\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Act as Aarav Mehta/i)).toBeInTheDocument();
  });

  it('switches UI language between English and Hindi', async () => {
    renderWithProviders(['/']);
    expect(await screen.findByText('Rakt Sanchar')).toBeInTheDocument();

    const hiButton = screen.getByRole('button', { name: 'हिन्दी' });
    fireEvent.click(hiButton);

    expect(await screen.findByText(/रक्त संचार/)).toBeInTheDocument();
  });

  it('renders blood stock results on StockSearchPage', async () => {
    renderWithProviders(['/stock']);
    expect(await screen.findByText('City Care Hospital')).toBeInTheDocument();
    expect(screen.getByText('14 unit(s)')).toBeInTheDocument();
  });

  it('renders donor eligibility status and earned badges on DonorDashboardPage', async () => {
    setStoredIdentity({ userId: 'donor-1', role: 'DONOR' });
    renderWithProviders(['/donor/dashboard']);

    expect(await screen.findByText(/Eligible to Donate Now/i)).toBeInTheDocument();
    expect(screen.getByText('First Drop')).toBeInTheDocument();
  });
});
