import { Navigate } from 'react-router-dom';
import { getAuth, homePathFor } from '../auth';

export default function ProtectedRoute({ allow, children }) {
  const auth = getAuth();

  if (!auth?.token) {
    return <Navigate to="/login" replace />;
  }

  if (allow && !allow.includes(auth.role)) {
    return <Navigate to={homePathFor(auth.role)} replace />;
  }

  return children;
}
