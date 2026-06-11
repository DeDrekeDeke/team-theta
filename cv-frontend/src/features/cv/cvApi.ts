import { apiRequest, API_BASE_URL, readErrorMessage } from '../../app/apiClient';
import { getAuthToken } from '../auth/authStore';

export type Cv = {
  id: number;
  ownerUserId: number;
  ownerEmail: string;
  title: string;
  uploadedHtmlFilePath: string;
  createdAt: string;
  updatedAt: string;
  archivedAt: string | null;
};

export function listCvs() {
  return apiRequest<Cv[]>('/api/cvs');
}

export function searchCvs(query: string) {
  return apiRequest<Cv[]>(`/api/cvs/search?q=${encodeURIComponent(query)}`);
}

export function getCv(id: string) {
  return apiRequest<Cv>(`/api/cvs/${id}`);
}

export function getCvHtmlUrl(id: number | string) {
  return `${API_BASE_URL}/api/cvs/${id}/html`;
}

export async function getCvHtml(id: number | string) {
  const token = getAuthToken();
  const response = await fetch(getCvHtmlUrl(id), {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return response.text();
}

export function uploadCv(formData: FormData) {
  return apiRequest<Cv>('/api/cvs/upload', {
    method: 'POST',
    body: formData
  });
}

export type CvUpdateRequest = {
  title: string;
  uploadedHtmlFilePath: string;
};

export function updateCv(id: number | string, request: CvUpdateRequest) {
  return apiRequest<Cv>(`/api/cvs/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export function archiveCv(id: number | string) {
  return apiRequest<void>(`/api/cvs/${id}`, {
    method: 'DELETE'
  });
}
