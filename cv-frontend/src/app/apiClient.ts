import { getAuthToken } from "../features/auth/authStore";

type ApiErrorResponse = {
  message?: string;
  code?: string;
  details?: string[];
};

function formatValidationDetail(detail: string) {
  const [field, rawMessage] = detail.split(": ", 2);

  if (field === "email" && rawMessage?.includes("well-formed email")) {
    return "Enter a valid email address.";
  }

  if (
    field === "password" &&
    rawMessage?.includes("size must be between 6 and 255")
  ) {
    return "Password must be between 6 and 255 characters.";
  }

  if (field === "title" && rawMessage?.includes("size must be between")) {
    return "Title is too long.";
  }

  return rawMessage ?? detail;
}

export async function readErrorMessage(response: Response) {
  const fallback = `Request failed with status ${response.status}`;
  const text = await response.text();
  if (!text) {
    return fallback;
  }

  try {
    const error = JSON.parse(text) as ApiErrorResponse;

    if (error.code === "VALIDATION_FAILED" && error.details?.length) {
      return error.details.map(formatValidationDetail).join("\n");
    }

    return error.message ?? fallback;
  } catch {
    return text;
  }
}

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getAuthToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      Accept: "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.body instanceof FormData
        ? {}
        : { "Content-Type": "application/json" }),
      ...options.headers,
    },
    ...options,
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

