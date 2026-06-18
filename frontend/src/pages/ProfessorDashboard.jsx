import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import NavBar from '../components/NavBar';
import { apiFetch } from '../auth';

const SELECT_COURSE_PLACEHODLER = '-- Select a Course --';

export default function ProfessorDashboard() {
  const [courseId, setCourseId] = useState('');
  const [courses, setCourses] = useState([]);
  const [expirationMinutes, setExpirationMinutes] = useState('');
  const [sessionData, setSessionData] = useState(null);
  const [attendees, setAttendees] = useState([]);
  const [count, setCount] = useState(0);
  const [alert, setAlert] = useState(null);
  const stompClientRef = useRef(null);

  useEffect(() => {
    if (!alert) return;
    const t = setTimeout(() => setAlert(null), 5000);
    return () => clearTimeout(t);
  }, [alert]);

  useEffect(() => {
    apiFetch('/api/professor/courses')
      .then((res) => {
        if (!res.ok) throw new Error('Failed to load courses');
        return res.json();
      })
      .then(setCourses)
      .catch((e) => {
        console.error(e);
        setAlert({ message: 'Error loading courses', type: 'danger' });
      });

    apiFetch('/api/professor/session/config')
      .then((res) => {
        if (!res.ok) throw new Error('Failed to load session config');
        return res.json();
      })
      .then((cfg) => setExpirationMinutes(cfg.defaultExpirationMinutes))
      .catch((e) => console.error(e));
  }, []);

  useEffect(() => {
    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
        stompClientRef.current = null;
      }
    };
  }, []);

  const startSession = async () => {
    if (!courseId) {
      setAlert({ message: 'Please select a course', type: 'danger' });
      return;
    }
    try {
      const res = await apiFetch(
        `/api/professor/session/start?courseId=${courseId}&expirationMinutes=${expirationMinutes}`,
        { method: 'POST' }
      );
      if (!res.ok) throw new Error('Failed');
      const data = await res.json();
      setSessionData(data);
      setCount(data.attendanceCount);
      setAttendees([]);
      connectWebSocket(data.sessionId);
      setAlert({ message: 'Session started successfully!', type: 'success' });
    } catch (e) {
      setAlert({ message: 'Error starting session', type: 'danger' });
      console.error(e);
    }
  };

  const connectWebSocket = (sessionId) => {
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
    }
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/attendance/' + sessionId, (msg) => {
          const update = JSON.parse(msg.body);
          setCount(update.totalCount);
          setAttendees((prev) => [
            {
              name: update.studentName,
              time: new Date(update.checkInTime).toLocaleTimeString('ro-RO', { timeZone: 'Europe/Bucharest' }),
            },
            ...prev,
          ]);
        });
      },
    });
    client.activate();
    stompClientRef.current = client;
  };

  const endSession = async () => {
    if (!sessionData) return;
    try {
      const res = await apiFetch(
        `/api/professor/session/${sessionData.sessionId}/end`,
        { method: 'POST' }
      );
      if (!res.ok) throw new Error('Failed');
      setAlert({ message: 'Session ended successfully', type: 'success' });
      setSessionData(null);
      setAttendees([]);
      setCount(0);
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
        stompClientRef.current = null;
      }
    } catch (e) {
      setAlert({ message: 'Error ending session', type: 'danger' });
      console.error(e);
    }
  };

  return (
    <>
      <NavBar />
      <div className="container">
      <div className="header">
        <h1>Professor Dashboard</h1>
        <p>Real-Time Attendance Management System</p>
      </div>

      <div className="content">
        <div className="session-controls">
          <h2>Start New Session</h2>
          <div className="form-group">
            <label htmlFor="courseSelect">Select Course:</label>
            <select
              id="courseSelect"
              value={courseId}
              onChange={(e) => setCourseId(e.target.value)}
            >
              <option value="">{SELECT_COURSE_PLACEHODLER}</option>
              {courses.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.courseCode} - {c.courseName}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="expirationMinutes">QR Code Expiration (minutes):</label>
            <input
              type="number"
              id="expirationMinutes"
              value={expirationMinutes}
              min={1}
              max={60}
              onChange={(e) => setExpirationMinutes(e.target.value)}
            />
          </div>
          <button className="btn btn-primary" onClick={startSession}>
            Start Session
          </button>
        </div>

        {alert && (
          <div className={`alert alert-${alert.type}`}>{alert.message}</div>
        )}

        {sessionData && (
          <div className="qr-section">
            <h2>Active Session</h2>
            <div className="qr-code">
              <img
                src={`data:image/png;base64,${sessionData.qrCodeBase64}`}
                alt="QR Code"
              />
            </div>

            <div className="session-info">
              <div className="info-row">
                <span className="info-label">Course:</span>
                <span className="info-value">{sessionData.courseName}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Session Started:</span>
                <span className="info-value">
                  {new Date(sessionData.startTime).toLocaleString()}
                </span>
              </div>
              <div className="info-row">
                <span className="info-label">Expires At:</span>
                <span className="info-value">
                  {new Date(sessionData.expirationTime).toLocaleString()}
                </span>
              </div>
            </div>

            <div className="attendance-counter">{count}</div>
            <p>Students Checked In</p>

            <button className="btn btn-danger" onClick={endSession}>
              End Session
            </button>

            <div className="attendance-list">
              <h3>Attendance List</h3>
              <div>
                {attendees.map((a, i) => (
                  <div className="student-item" key={i}>
                    <div>
                      <div className="student-name">{a.name}</div>
                      <div className="check-in-time">{a.time}</div>
                    </div>
                    <div className="status-badge">Present</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
    </>
  );
}
