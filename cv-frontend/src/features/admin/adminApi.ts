import { apiRequest } from '../../app/apiClient';

export type AppSetting = {
  key: string;
  value: string;
  description: string | null;
};

export type AdminUser = {
  id: number;
  email: string;
  displayName: string;
  admin: boolean;
  createdAt: string;
};

export type UserCreateRequest = {
  email: string;
  displayName: string;
  password: string;
};

export type UserUpdateRequest = {
  email: string;
  displayName: string;
  admin: boolean;
};

export function listSettings() {
  return apiRequest<AppSetting[]>('/api/admin/settings');
}

export function listUsers() {
  return apiRequest<AdminUser[]>('/api/users');
}

export function createUser(request: UserCreateRequest) {
  return apiRequest<AdminUser>('/api/users', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export function updateUser(id: number, request: UserUpdateRequest) {
  return apiRequest<AdminUser>(`/api/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}
