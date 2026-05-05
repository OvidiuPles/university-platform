import { useState } from 'react';
import NavBar from '../components/NavBar';

function initials(name) {
  return name
    .split(' ')
    .map((w) => w[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

function formatDateTime(iso) {
  if (!iso) return '-';
  return new Date(iso).toLocaleString();
}

export default function ProfessorHistory() {
  const [token, setToken] = useState('');
  const [data, setData] = useState(null);
  const [alert, setAlert] = useState(null);
  const [loading, setLoading] = useState(false);
  const [sortKey, setSortKey] = useState('name-asc');

  const lookupSession = async () => {
    const trimmed = token.trim();
    if (!trimmed) {
      setAlert({ message: 'Please paste a session token first.', type: 'danger' });
      return;
    }
    setAlert(null);
    setData(null);
    setLoading(true);

    try {
      const res = await fetch(
        `/api/professor/session/history?token=${encodeURIComponent(trimmed)}`
      );
      const payload = await res.json();
      setLoading(false);
      if (payload.status === 'error') {
        setAlert({ message: payload.message, type: 'danger' });
        return;
      }
      setData(payload);
      setSortKey('name-asc');
    } catch (e) {
      setLoading(false);
      setAlert({
        message: 'Connection error. Please check your network and try again.',
        type: 'danger',
      });
    }
  };

  const clearAll = () => {
    setToken('');
    setAlert(null);
    setLoading(false);
    setData(null);
  };

  const sortedStudents = (() => {
    if (!data?.students) return [];
    const sorted = [...data.students];
    sorted.sort((a, b) => {
      switch (sortKey) {
        case 'name-asc':
          return a.name.localeCompare(b.name);
        case 'name-desc':
          return b.name.localeCompare(a.name);
        case 'time-asc':
          return new Date(a.checkInTime) - new Date(b.checkInTime);
        case 'time-desc':
          return new Date(b.checkInTime) - new Date(a.checkInTime);
        default:
          return 0;
      }
    });
    return sorted;
  })();

  return (
    <>
      <NavBar />
      <div className="container">
      <div className="header">
        <h1>Session History</h1>
        <p>View attendance for any past or active session</p>
      </div>

      <div className="content">
        <div className="session-controls">
          <h2>Look Up a Session</h2>
          <div className="input-row">
            <input
              type="text"
              placeholder="Paste session token here..."
              autoComplete="off"
              spellCheck={false}
              value={token}
              onChange={(e) => setToken(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') lookupSession();
              }}
            />
            <button className="btn btn-primary" onClick={lookupSession}>
              Look Up
            </button>
            <button className="btn btn-secondary" onClick={clearAll}>
              Clear
            </button>
          </div>
        </div>

        {alert && (
          <div className={`alert alert-${alert.type}`}>{alert.message}</div>
        )}

        {loading && (
          <div>
            <div className="loader"></div>
            <p className="loading-text">Loading...</p>
          </div>
        )}

        {data && (
          <div>
            <div className="session-info" style={{ marginBottom: 24 }}>
              <h3 style={{ marginBottom: 14 }}>Session Details</h3>
              <div className="info-row">
                <span className="info-label">Course</span>
                <span className="info-value">
                  {data.courseCode} - {data.courseName}
                </span>
              </div>
              <div className="info-row">
                <span className="info-label">Professor</span>
                <span className="info-value">{data.professorName}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Session Started</span>
                <span className="info-value">{formatDateTime(data.startTime)}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Session Expired At</span>
                <span className="info-value">
                  {formatDateTime(data.expirationTime)}
                </span>
              </div>
              <div className="info-row">
                <span className="info-label">Status</span>
                <span className="info-value">
                  {data.isActive ? (
                    <span className="badge-active">Active</span>
                  ) : (
                    <span className="badge-ended">Ended</span>
                  )}
                </span>
              </div>
            </div>

            <div className="stats-row">
              <div className="stat-card">
                <div className="stat-number">{data.totalAttendance}</div>
                <div className="stat-label">Students Present</div>
              </div>
            </div>

            <div className="list-header">
              <h3>Attendance List</h3>
              <select
                className="sort-select"
                value={sortKey}
                onChange={(e) => setSortKey(e.target.value)}
              >
                <option value="name-asc">Name (A-Z)</option>
                <option value="name-desc">Name (Z-A)</option>
                <option value="time-asc">Check-in (earliest first)</option>
                <option value="time-desc">Check-in (latest first)</option>
              </select>
            </div>

            <div>
              {sortedStudents.length === 0 ? (
                <div className="empty-state">
                  <h3>No students checked in</h3>
                  <p>Nobody attended this session.</p>
                </div>
              ) : (
                sortedStudents.map((s) => (
                  <div className="student-item" key={s.studentId}>
                    <div className="student-left">
                      <div className="student-avatar">{initials(s.name)}</div>
                      <div>
                        <div className="student-name">{s.name}</div>
                        <div className="student-meta">
                          ID: {s.studentId} &nbsp;|&nbsp; {s.email}
                        </div>
                      </div>
                    </div>
                    <div className="student-right">
                      <div className="status-badge">Present</div>
                      <div className="check-in-time" style={{ marginTop: 4 }}>
                        Checked in at {formatDateTime(s.checkInTime)}
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        )}
      </div>
    </div>
    </>
  );
}
