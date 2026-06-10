import './setup';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { apiRequest } from '../app/apiClient';
import { listSettings } from '../features/admin/adminApi';
import { improveSummary, listSuggestions } from '../features/ai/aiApi';
import { login } from '../features/auth/authApi';
import {
  AUTH_CHANGED_EVENT,
  getAuthToken,
  getCurrentUser,
  logout,
  saveCurrentUser
} from '../features/auth/authStore';
import { getCv, getCvHtml, getCvHtmlUrl, listCvs, searchCvs, uploadCv } from '../features/cv/cvApi';
import { formatDateTime } from '../lib/formatters';
import { required } from '../lib/validation';

const aliceUser = {
  userId: 2,
  email: 'alice@example.com',
  displayName: 'Alice Student',
  admin: false,
  token: 'demo-token-2'
};

describe('API client and small frontend utilities', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  test('auth store saves, reads, clears, and repairs invalid local storage state', () => {
    const authChanged = vi.fn();
    window.addEventListener(AUTH_CHANGED_EVENT, authChanged);

    saveCurrentUser(aliceUser);
    expect(getCurrentUser()).toMatchObject({ email: 'alice@example.com' });
    expect(getAuthToken()).toBe('demo-token-2');
    expect(authChanged).toHaveBeenCalledTimes(1);

    logout();
    expect(getCurrentUser()).toBeNull();
    expect(authChanged).toHaveBeenCalledTimes(2);

    localStorage.setItem('cv-manager-auth', '{bad-json');
    expect(getCurrentUser()).toBeNull();
    expect(localStorage.getItem('cv-manager-auth')).toBeNull();

    window.removeEventListener(AUTH_CHANGED_EVENT, authChanged);
  });

  test('apiRequest sends JSON headers, bearer token, and parses JSON responses', async () => {
    saveCurrentUser(aliceUser);
    const fetchMock = vi.fn().mockResolvedValue(response(200, { ok: true }));
    vi.stubGlobal('fetch', fetchMock);

    const result = await apiRequest<{ ok: boolean }>('/api/example', {
      method: 'POST',
      body: JSON.stringify({ value: 1 })
    });

    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/example',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Accept: 'application/json',
          Authorization: 'Bearer demo-token-2',
          'Content-Type': 'application/json'
        })
      })
    );
  });

  test('apiRequest omits JSON content-type for FormData and handles empty/error responses', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response(204, undefined))
      .mockResolvedValueOnce(response(400, 'Bad request', false));
    vi.stubGlobal('fetch', fetchMock);

    const formData = new FormData();
    formData.append('title', 'CV');

    await expect(apiRequest('/api/upload', { method: 'POST', body: formData })).resolves.toBeUndefined();
    expect(fetchMock.mock.calls[0][1].headers).not.toHaveProperty('Content-Type');

    await expect(apiRequest('/api/fail')).rejects.toThrow('Bad request');
  });

  test('feature API wrappers call the expected backend paths', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(response(200, []));
    vi.stubGlobal('fetch', fetchMock);

    await listCvs();
    await searchCvs("Alice %'");
    await getCv('10');
    await login({ email: 'alice@example.com', password: 'user123' });
    await listSettings();
    await listSuggestions(10);
    await improveSummary(10);
    await uploadCv(new FormData());

    const calledUrls = fetchMock.mock.calls.map((call) => call[0]);
    expect(calledUrls).toEqual([
      'http://localhost:8080/api/cvs',
      "http://localhost:8080/api/cvs/search?q=Alice%20%25'",
      'http://localhost:8080/api/cvs/10',
      'http://localhost:8080/api/auth/login',
      'http://localhost:8080/api/admin/settings',
      'http://localhost:8080/api/cvs/10/ai-actions/suggestions',
      'http://localhost:8080/api/cvs/10/ai-actions/improve-summary',
      'http://localhost:8080/api/cvs/upload'
    ]);
    expect(fetchMock.mock.calls[3][1].method).toBe('POST');
    expect(fetchMock.mock.calls[6][1].method).toBe('POST');
  });

  test('CV HTML helpers build raw URL and send auth token for HTML fetches', async () => {
    saveCurrentUser(aliceUser);
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      text: () => Promise.resolve('<html>Alice</html>')
    });
    vi.stubGlobal('fetch', fetchMock);

    expect(getCvHtmlUrl(10)).toBe('http://localhost:8080/api/cvs/10/html');
    await expect(getCvHtml(10)).resolves.toBe('<html>Alice</html>');

    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/cvs/10/html', {
      headers: { Authorization: 'Bearer demo-token-2' }
    });

    fetchMock.mockResolvedValueOnce({ ok: false, status: 404 });
    await expect(getCvHtml(404)).rejects.toThrow('Could not load uploaded HTML: 404');
  });

  test('formatting and validation helpers return expected user-facing values', () => {
    expect(required('  value  ')).toBe(true);
    expect(required('   ')).toBe(false);
    expect(formatDateTime('2026-06-10T12:30:00')).toMatch(/2026|6\/10\/26|10\/06\/2026/);
  });
});

function response(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return {
    ok,
    status,
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
    json: () => Promise.resolve(body)
  };
}
