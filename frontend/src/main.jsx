import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import ProfessorDashboard from './pages/ProfessorDashboard';
import ProfessorHistory from './pages/ProfessorHistory';
import ProfessorGrades from './pages/ProfessorGrades';
import StudentCheckin from './pages/StudentCheckin';
import StudentHistory from './pages/StudentHistory';
import StudentGrades from './pages/StudentGrades';
import AdminPage from './pages/AdminPage';
import Login from './pages/Login';
import ProtectedRoute from './components/ProtectedRoute';
import { getAuth, homePathFor } from './auth';
import './styles/global.css';

function Home() {
  const auth = getAuth();
  return <Navigate to={auth?.token ? homePathFor(auth.role) : '/login'} replace />;
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />

        <Route
          path="/professor"
          element={
            <ProtectedRoute allow={['PROFESSOR', 'ADMIN']}>
              <ProfessorDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/professor/history"
          element={
            <ProtectedRoute allow={['PROFESSOR', 'ADMIN']}>
              <ProfessorHistory />
            </ProtectedRoute>
          }
        />
        <Route
          path="/professor/grades"
          element={
            <ProtectedRoute allow={['PROFESSOR', 'ADMIN']}>
              <ProfessorGrades />
            </ProtectedRoute>
          }
        />

        <Route path="/checkin" element={<StudentCheckin />} />
        <Route
          path="/student/history"
          element={
            <ProtectedRoute allow={['STUDENT', 'ADMIN']}>
              <StudentHistory />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/grades"
          element={
            <ProtectedRoute allow={['STUDENT', 'ADMIN']}>
              <StudentGrades />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin"
          element={
            <ProtectedRoute allow={['ADMIN']}>
              <AdminPage />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);
