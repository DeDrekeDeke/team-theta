import './setup';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { App } from '../app/App';
import { SettingsPage } from '../features/admin/SettingsPage';
import { AppSetting } from '../features/admin/adminApi';
import { AiActionPanel } from '../features/ai/AiActionPanel';
import { AiSuggestion } from '../features/ai/aiApi';
import { LoginPage } from '../features/auth/LoginPage';
import { LoginResponse } from '../features/auth/authApi';
import { saveCurrentUser } from '../features/auth/authStore';
import { CvDetailPage } from '../features/cv/CvDetailPage';
import { CvListPage } from '../features/cv/CvListPage';
import { CvUploadPage } from '../features/cv/CvUploadPage';
import { Cv } from '../features/cv/cvApi';

vi.mock('../features/auth/authApi', () => ({
  login: vi.fn()
}));

vi.mock('../features/cv/cvApi', async () => {
  const actual = await vi.importActual<typeof import('../features/cv/cvApi')>('../features/cv/cvApi');
  return {
    ...actual,
    listCvs: vi.fn(),
    searchCvs: vi.fn(),
    getCv: vi.fn(),
    getCvHtml: vi.fn(),
    getCvHtmlUrl: vi.fn((id: number | string) => `http://localhost:8080/api/cvs/${id}/html`),
    uploadCv: vi.fn()
  };
});

vi.mock('../features/admin/adminApi', () => ({
  listSettings: vi.fn()
}));

vi.mock('../features/ai/aiApi', () => ({
  improveSummary: vi.fn(),
  listSuggestions: vi.fn()
}));

const authApi = await import('../features/auth/authApi');
const cvApi = await import('../features/cv/cvApi');
const adminApi = await import('../features/admin/adminApi');
const aiApi = await import('../features/ai/aiApi');

const aliceUser: LoginResponse = {
  userId: 2,
  email: 'alice@example.com',
  displayName: 'Alice Student',
  admin: false,
  token: 'demo-token-2'
};

const adminUser: LoginResponse = {
  userId: 1,
  email: 'admin@example.com',
  displayName: 'Admin User',
  admin: true,
  token: 'demo-token-1'
};

const aliceCv: Cv = {
  id: 10,
  ownerUserId: 2,
  ownerEmail: 'alice@example.com',
  title: 'Alice CV',
  uploadedHtmlFilePath: 'uploads/alice.html',
  createdAt: '2026-06-10T09:00:00',
  updatedAt: '2026-06-10T10:00:00'
};

describe('frontend flows', () => {
  beforeEach(() => {
    vi.mocked(authApi.login).mockReset();
    vi.mocked(cvApi.listCvs).mockReset();
    vi.mocked(cvApi.searchCvs).mockReset();
    vi.mocked(cvApi.getCv).mockReset();
    vi.mocked(cvApi.getCvHtml).mockReset();
    vi.mocked(cvApi.getCvHtmlUrl).mockClear();
    vi.mocked(cvApi.uploadCv).mockReset();
    vi.mocked(adminApi.listSettings).mockReset();
    vi.mocked(aiApi.improveSummary).mockReset();
    vi.mocked(aiApi.listSuggestions).mockReset();
  });

  test('login form submits credentials, stores the user, and navigates home', async () => {
    vi.mocked(authApi.login).mockResolvedValue(aliceUser);

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPageWrapper />} />
          <Route path="/" element={<h2>CVs home</h2>} />
        </Routes>
      </MemoryRouter>
    );

    await userEvent.clear(screen.getByLabelText('Email'));
    await userEvent.type(screen.getByLabelText('Email'), 'bob@example.com');
    await userEvent.clear(screen.getByLabelText('Password'));
    await userEvent.type(screen.getByLabelText('Password'), 'secret');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(authApi.login).toHaveBeenCalledWith({ email: 'bob@example.com', password: 'secret' });
    expect(await screen.findByRole('heading', { name: 'CVs home' })).toBeInTheDocument();
    expect(JSON.parse(localStorage.getItem('cv-manager-auth') ?? '{}')).toMatchObject({
      email: 'alice@example.com',
      token: 'demo-token-2'
    });
  });

  test('login form shows API errors and re-enables submit', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error('Invalid email or password'));

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByText('Invalid email or password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Log in' })).toBeEnabled();
  });

  test('CV list renders empty state and searches with the current query', async () => {
    vi.mocked(cvApi.listCvs).mockResolvedValue([]);
    vi.mocked(cvApi.searchCvs).mockResolvedValue([aliceCv]);

    render(
      <MemoryRouter>
        <CvListPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('No CVs found.')).toBeInTheDocument();

    await userEvent.type(screen.getByPlaceholderText('Search title, owner, or file path'), "Alice_%'");
    await userEvent.click(screen.getByRole('button', { name: 'Search' }));

    expect(cvApi.searchCvs).toHaveBeenCalledWith("Alice_%'");
    expect(await screen.findByRole('link', { name: 'Alice CV' })).toHaveAttribute('href', '/cvs/10');
  });

  test('CV upload form validates missing file before submitting', async () => {
    render(
      <MemoryRouter>
        <CvUploadPage />
      </MemoryRouter>
    );

    await userEvent.click(screen.getByRole('button', { name: 'Upload' }));

    expect(await screen.findByText('Choose an HTML file first.')).toBeInTheDocument();
    expect(cvApi.uploadCv).not.toHaveBeenCalled();
  });

  test('CV upload form submits selected file and navigates to the created CV', async () => {
    vi.mocked(cvApi.uploadCv).mockResolvedValue(aliceCv);

    render(
      <MemoryRouter initialEntries={['/upload']}>
        <Routes>
          <Route path="/upload" element={<CvUploadPage />} />
          <Route path="/cvs/:id" element={<h2>CV detail route</h2>} />
        </Routes>
      </MemoryRouter>
    );

    await userEvent.type(screen.getByLabelText('Title'), 'Uploaded CV');
    await userEvent.upload(
      screen.getByLabelText('HTML file'),
      new File(['<html>Alice</html>'], 'alice.html', { type: 'text/html' })
    );
    await userEvent.click(screen.getByRole('button', { name: 'Upload' }));

    expect(cvApi.uploadCv).toHaveBeenCalledTimes(1);
    const submitted = vi.mocked(cvApi.uploadCv).mock.calls[0][0] as FormData;
    expect(submitted.get('ownerUserId')).toBe('2');
    expect(submitted.get('title')).toBe('Uploaded CV');
    expect((submitted.get('file') as File).name).toBe('alice.html');
    expect(await screen.findByRole('heading', { name: 'CV detail route' })).toBeInTheDocument();
  });

  test('current navigation shows settings link and auth state changes', async () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<App />}>
            <Route index element={<p>Shell content</p>} />
          </Route>
          <Route path="/login" element={<p>Login route</p>} />
        </Routes>
      </MemoryRouter>
    );

    const nav = screen.getByRole('navigation', { name: 'Main navigation' });
    expect(within(nav).getByRole('link', { name: 'Settings' })).toHaveAttribute('href', '/admin/settings');
    expect(screen.getByRole('link', { name: 'Log in' })).toBeInTheDocument();

    saveCurrentUser(adminUser);
    expect(await screen.findByText('admin@example.com')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Log out' }));
    expect(await screen.findByText('Login route')).toBeInTheDocument();
  });

  test('settings page renders loaded admin settings and errors', async () => {
    const settings: AppSetting[] = [
      { key: 'application.displayName', value: 'CV Manager', description: 'Shown to users' }
    ];
    vi.mocked(adminApi.listSettings).mockResolvedValueOnce(settings);

    const { unmount } = render(<SettingsPage />);

    expect(await screen.findByText('application.displayName')).toBeInTheDocument();
    expect(screen.getByText('CV Manager')).toBeInTheDocument();

    unmount();
    vi.mocked(adminApi.listSettings).mockRejectedValueOnce(new Error('Could not load settings'));
    render(<SettingsPage />);

    expect(await screen.findByText('Could not load settings')).toBeInTheDocument();
  });

  test('CV detail page loads CV, raw HTML, and AI panel suggestions', async () => {
    const suggestion: AiSuggestion = {
      id: 1,
      cvId: 10,
      actionType: 'IMPROVE_SUMMARY',
      originalText: 'before',
      suggestedText: 'after',
      status: 'PENDING',
      createdAt: '2026-06-10T10:00:00'
    };
    vi.mocked(cvApi.getCv).mockResolvedValue(aliceCv);
    vi.mocked(cvApi.getCvHtml).mockResolvedValue('<p>Alice summary</p>');
    vi.mocked(aiApi.listSuggestions).mockResolvedValue([suggestion]);

    render(
      <MemoryRouter initialEntries={['/cvs/10']}>
        <Routes>
          <Route path="/cvs/:id" element={<CvDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('heading', { name: 'Alice CV' })).toBeInTheDocument();
    expect(screen.getByText('Alice summary')).toBeInTheDocument();
    expect(screen.getByText('IMPROVE_SUMMARY')).toBeInTheDocument();
    expect(screen.getByText('after')).toBeInTheDocument();
  });

  test('AI panel supports implemented and placeholder actions', async () => {
    const suggestion: AiSuggestion = {
      id: 2,
      cvId: 10,
      actionType: 'IMPROVE_SUMMARY',
      originalText: 'before',
      suggestedText: 'new summary',
      status: 'PENDING',
      createdAt: '2026-06-10T10:00:00'
    };
    vi.mocked(aiApi.listSuggestions).mockResolvedValue([]);
    vi.mocked(aiApi.improveSummary).mockResolvedValue(suggestion);

    render(<AiActionPanel cvId={10} />);

    expect(await screen.findByText('No suggestions yet.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Improve summary' }));
    expect(await screen.findByText('new summary')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Improve education' }));
    expect(screen.getByText(/planned AI action/)).toBeInTheDocument();
  });
});

function LoginPageWrapper() {
  return <LoginPage />;
}
