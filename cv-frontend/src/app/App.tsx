import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { AUTH_CHANGED_EVENT, getCurrentUser, logout } from '../features/auth/authStore';

type NavItem = {
  to: string;
  label: string;
  adminOnly?: boolean;
};

const navItems: NavItem[] = [
  { to: '/', label: 'CVs' },
  { to: '/upload', label: 'Upload' },
  { to: '/admin/users', label: 'Users', adminOnly: true },
  { to: '/admin/settings', label: 'Settings' }
];

export function App() {
  const navigate = useNavigate();
  const [user, setUser] = useState(getCurrentUser());

  useEffect(() => {
    function handleAuthChanged() {
      setUser(getCurrentUser());
    }

    window.addEventListener(AUTH_CHANGED_EVENT, handleAuthChanged);
    window.addEventListener('storage', handleAuthChanged);

    return () => {
      window.removeEventListener(AUTH_CHANGED_EVENT, handleAuthChanged);
      window.removeEventListener('storage', handleAuthChanged);
    };
  }, []);

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">Summer School</p>
          <h1>CV Manager</h1>
        </div>

        <nav className="nav-list" aria-label="Main navigation">
          {navItems
            .filter((item) => !item.adminOnly || user?.admin)
            .map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              >
                {item.label}
              </NavLink>
            ))}
        </nav>

        <div className="sidebar-footer">
          {user ? (
            <>
              <span>{user.email}</span>
              <button className="link-button" type="button" onClick={handleLogout}>
                Log out
              </button>
            </>
          ) : (
            <NavLink to="/login" className="nav-link">
              Log in
            </NavLink>
          )}
        </div>
      </aside>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
