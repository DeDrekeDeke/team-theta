import { createBrowserRouter } from 'react-router-dom';
import { App } from './App';
import { LoginPage } from '../features/auth/LoginPage';
import { CvDetailPage } from '../features/cv/CvDetailPage';
import { CvListPage } from '../features/cv/CvListPage';
import { CvUploadPage } from '../features/cv/CvUploadPage';
import { SettingsPage } from '../features/admin/SettingsPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <CvListPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'create', element: <CvUploadPage /> },
      { path: 'cvs/:id', element: <CvDetailPage /> },
      { path: 'admin/settings', element: <SettingsPage /> }
    ]
  }
]);
