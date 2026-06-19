import { Navigate, useLocation } from 'react-router-dom';
import { getAuth, homePathFor } from '../auth';

export default function ProtectedRoute({ allow, children }) {
  const auth = getAuth();
  const location = useLocation();

  if (!auth?.token) {
    return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />;
  }

  if (allow && !allow.includes(auth.role)) {
    return <Navigate to={homePathFor(auth.role)} replace />;
  }

  return children;
}
