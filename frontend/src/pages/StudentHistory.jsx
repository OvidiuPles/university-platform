import { useState } from 'react';
import NavBar from '../components/NavBar';
import { apiFetch } from '../auth';

export default function StudentHistory() {
  const [studentId, setStudentId] = useState('');
  const [history, setHistory] = useState(null);
  const [sessionsCount, setSessionsCount] = useState(0);
  const [alert, setAlert] = useState(null);

  const loadAttendanceHistory = async () => {
    const id = studentId.trim();
    if (!id) {
      setAlert({ message: 'Please enter your Student ID', type: 'danger' });
      return;
    }

    try {
      const res = await apiFetch(
        `/api/student/attendance/history?studentId=${encodeURIComponent(id)}`
      );
      if (!res.ok) throw new Error('Student not found');
      const data = await res.json();
      setAlert(null);
      setHistory(data.history);
      setSessionsCount(data.sessionsCount);
    } catch (e) {
      setAlert({
        message: 'Error loading attendance history. Please check your Student ID.',
        type: 'danger',
      });
      setHistory(null);
      setSessionsCount(0);
    }
  };

  const showStats = history !== null && history.length > 0;
  const attendanceRate =
    sessionsCount > 0 && history
      ? ((history.length / sessionsCount) * 100).toFixed(0) + '%'
      : '0%';

  return (
    <>
      <NavBar />
      <div className="container narrow">
      <div className="header">
        <h1>Attendance History</h1>
        <p>View your attendance records</p>
      </div>

      <div className="content">
        <div className="search-box">
          <div className="form-group">
            <input
              type="text"
              placeholder="Enter your Student ID (e.g., S001)"
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') loadAttendanceHistory();
              }}
            />
            <button className="btn btn-primary" onClick={loadAttendanceHistory}>
              View History
            </button>
          </div>
        </div>

        {alert && (
          <div className={`alert alert-${alert.type}`}>{alert.message}</div>
        )}

        {showStats && (
          <div className="stats">
            <div className="stat-card">
              <div className="stat-number">{history.length}</div>
              <div className="stat-label">Total Sessions</div>
            </div>
            <div className="stat-card">
              <div className="stat-number">{attendanceRate}</div>
              <div className="stat-label">Attendance Rate</div>
            </div>
          </div>
        )}

        {history !== null && history.length === 0 && (
          <div className="empty-state">
            <h3>No Attendance Records</h3>
            <p>You haven't checked in to any sessions yet.</p>
          </div>
        )}

        {history !== null && history.length > 0 && (
          <div>
            <h2 className="section-title">Your Attendance Records</h2>
            {history.map((record) => (
              <div className="attendance-record" key={record.id}>
                <div className="record-header">
                  <div className="course-name">
                    {record.session.course.courseName}
                  </div>
                  <div className="check-in-time">
                    {new Date(record.checkInTime).toLocaleString()}
                  </div>
                </div>
                <div className="record-details">
                  Course Code: {record.session.course.courseCode} | Professor:{' '}
                  {record.session.course.professorName}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
    </>
  );
}
