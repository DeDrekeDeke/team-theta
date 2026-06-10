import { createBrowserRouter } from 'react-router-dom';
import { App } from './App';
import { LoginPage } from '../features/auth/LoginPage';
import { CvDetailPage } from '../features/cv/CvDetailPage';
import { CvListPage } from '../features/cv/CvListPage';
import { CvUploadPage } from '../features/cv/CvUploadPage';
import { SettingsPage } from '../features/admin/SettingsPage';
import { UsersPage } from '../features/admin/UsersPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <CvListPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'upload', element: <CvUploadPage /> },
      { path: 'cvs/:id', element: <CvDetailPage /> },
      { path: 'admin/users', element: <UsersPage /> },
      { path: 'admin/settings', element: <SettingsPage /> }
    ]
  }
]);
