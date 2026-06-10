import { getAuthToken } from '../features/auth/authStore';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      Accept: 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...options.headers
    },
    ...options
  });

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function readErrorMessage(response: Response) {
  const text = await response.text();
  if (!text) {
    return '';
  }

  try {
    const parsed = JSON.parse(text) as { message?: string; details?: string[] };
    const details = parsed.details?.length ? ` ${parsed.details.join(' ')}` : '';
    return `${parsed.message ?? ''}${details}`.trim();
  } catch {
    return text;
  }
}
