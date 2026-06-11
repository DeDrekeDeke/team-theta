export function required(value: string) {
  return value.trim().length > 0;
}

export const MAX_TITLE_LENGTH = 255;
export const MAX_HTML_UPLOAD_BYTES = 1_000_000;

export function validateEmail(value: string) {
  const trimmed = value.trim();

  if (!trimmed) {
    return 'Email is required.';
  }

  if (trimmed.length > 255) {
    return 'Email must be 255 characters or fewer.';
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
    return 'Enter a valid email address.';
  }

  return '';
}

export function validatePassword(value: string) {
  if (!value) {
    return 'Password is required.';
  }

  if (value.length < 6 || value.length > 255) {
    return 'Password must be between 6 and 255 characters.';
  }

  return '';
}

export function validateRequiredTitle(value: string) {
  if (!required(value)) {
    return 'Title is required.';
  }

  if (value.trim().length > MAX_TITLE_LENGTH) {
    return 'Title must be 255 characters or fewer.';
  }

  return '';
}

export function validateOptionalTitle(value: string) {
  if (value.trim().length > MAX_TITLE_LENGTH) {
    return 'Title must be 255 characters or fewer.';
  }

  return '';
}

export function validateOwnerUserId(value: string) {
  const trimmed = value.trim();

  if (!trimmed) {
    return 'Owner user id is required.';
  }

  if (!/^\d+$/.test(trimmed) || Number(trimmed) <= 0) {
    return 'Owner user id must be a positive number.';
  }

  return '';
}

export function validateHtmlFile(file: File | null) {
  if (!file) {
    return 'Choose an HTML file first.';
  }

  const lowerName = file.name.toLowerCase();
  const looksHtml =
    lowerName.endsWith('.html') ||
    lowerName.endsWith('.htm') ||
    file.type.toLowerCase().includes('html');

  if (!looksHtml) {
    return 'Only HTML files are accepted.';
  }

  if (file.size > MAX_HTML_UPLOAD_BYTES) {
    return 'HTML file must be 1 MB or smaller.';
  }

  return '';
}

export function compactErrors(errors: string[]) {
  return errors.filter(Boolean);
}
