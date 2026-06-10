import { apiRequest } from '../../app/apiClient';

export type Cv = {
  id: number;
  ownerUserId: number;
  ownerEmail: string;
  title: string;
  summary: string | null;
  createdAt: string;
  updatedAt: string;
  archivedAt: string | null;
};

export type CvCreateRequest = {
  ownerUserId: number;
  title: string;
  summary: string;
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

export function createCv(request: CvCreateRequest) {
  return apiRequest<Cv>('/api/cvs', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export type CvUpdateRequest = {
  title: string;
  summary: string;
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
