import { NavLink, useLocation } from 'react-router-dom';

export default function NavBar() {
  const { pathname } = useLocation();
  const isStudent = pathname.startsWith('/student') || pathname.startsWith('/checkin');

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <div className="navbar-brand">
          <span className="brand-text">University Platform</span>
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
        </div>
      </div>
    </nav>
  );
}
