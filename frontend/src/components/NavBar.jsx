import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { getAuth, clearAuth, apiFetch } from '../auth';

export default function NavBar() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const auth = getAuth();
  const isStudent = pathname.startsWith('/student') || pathname.startsWith('/checkin');

  const logout = async () => {
    try {
      await apiFetch('/api/auth/logout', { method: 'POST' });
    } catch {}
    clearAuth();
    navigate('/login', { replace: true });
  };

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <div className="navbar-brand">
          <span className="brand-text">{import.meta.env.VITE_UNIVERSITY_NAME || 'University Platform'}</span>
        </div>

        <div className="navbar-links">
          {isStudent ? (
            <div className="nav-group">
              <span className="nav-group-label">Student</span>
              <NavLink to="/student/history" className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
                Attendance
              </NavLink>
              <NavLink to="/student/grades" className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
                Grades
              </NavLink>
            </div>
          ) : (
            <div className="nav-group">
              <span className="nav-group-label">Professor</span>
              <NavLink to="/professor" end className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
                Dashboard
              </NavLink>
              <NavLink to="/professor/history" className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
                Sessions
              </NavLink>
              <NavLink to="/professor/grades" className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
                Grades
              </NavLink>
            </div>
          )}

          {auth && (
            <div className="nav-user">
              <span className="nav-user-name">{auth.name}</span>
              <span className="nav-user-role">{auth.role}</span>
              <button className="nav-logout" onClick={logout}>Logout</button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
