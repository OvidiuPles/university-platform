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
import './styles/global.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/professor" replace />} />
        <Route path="/professor" element={<ProfessorDashboard />} />
        <Route path="/professor/history" element={<ProfessorHistory />} />
        <Route path="/professor/grades" element={<ProfessorGrades />} />
        <Route path="/checkin" element={<StudentCheckin />} />
        <Route path="/student/history" element={<StudentHistory />} />
        <Route path="/student/grades" element={<StudentGrades />} />
        <Route path="/admin" element={<AdminPage />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);
