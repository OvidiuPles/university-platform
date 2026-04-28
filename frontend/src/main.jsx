import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import ProfessorDashboard from './pages/ProfessorDashboard';
import ProfessorHistory from './pages/ProfessorHistory';
import StudentCheckin from './pages/StudentCheckin';
import StudentHistory from './pages/StudentHistory';
import './styles/global.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/professor" replace />} />
        <Route path="/professor" element={<ProfessorDashboard />} />
        <Route path="/professor/history" element={<ProfessorHistory />} />
        <Route path="/checkin" element={<StudentCheckin />} />
        <Route path="/student/history" element={<StudentHistory />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);
