import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { FaCheckCircle } from 'react-icons/fa';
import { apiFetch } from '../auth';

export default function StudentCheckin() {
  const [searchParams] = useSearchParams();
  const sessionToken = searchParams.get('token');

  const [studentId, setStudentId] = useState('');
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [formVisible, setFormVisible] = useState(true);

  useEffect(() => {
    if (!sessionToken) {
      setMessage({ text: 'Invalid or missing session token', type: 'error' });
      setFormVisible(false);
      return;
    }
    setMessage({
      text: 'Session found! Please enter your Student ID to check in.',
      type: 'info',
    });

    apiFetch('/api/student/validate/checkin', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionToken }),
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.status === 'error') {
          setMessage({ text: data.message, type: 'error' });
          setFormVisible(false);
        }
      })
      .catch(() => {
        setMessage({
          text: 'An error occurred. Please try again.',
          type: 'error',
        });
      });
  }, [sessionToken]);

  const submitCheckIn = async () => {
    const id = studentId.trim();
    if (!id) {
      setMessage({ text: 'Please enter your Student ID', type: 'error' });
      return;
    }
    if (!sessionToken) {
      setMessage({ text: 'Invalid session token', type: 'error' });
      return;
    }
    setLoading(true);
    setMessage(null);

    try {
      const res = await apiFetch('/api/student/checkin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionToken, studentId: id }),
      });
      const data = await res.json();
      setLoading(false);

      if (data.status === 'success') {
        setSuccess(true);
        setFormVisible(false);
        setMessage(null);
      } else if (data.status === 'multiple-attempts') {
        setMessage({ text: data.message, type: 'info' });
      } else {
        setMessage({
          text: data.message || 'Check-in failed. Please try again.',
          type: 'error',
        });
      }
    } catch (e) {
      setLoading(false);
      setMessage({ text: 'An error occurred. Please try again.', type: 'error' });
    }
  };

  return (
    <div className="container compact">
      <div className="header tall compact">
        <h1>Student Check-In</h1>
        <p>Mark your attendance</p>
      </div>

      <div className="content tall">
        {loading ? (
          <div className="loader"></div>
        ) : (
          message && (
            <div className={`message ${message.type}`}>{message.text}</div>
          )
        )}

        {formVisible && !success && (
          <div>
            <div className="form-group tall">
              <label htmlFor="studentId">Student ID:</label>
              <input
                type="text"
                id="studentId"
                className="large"
                placeholder="Enter your student ID (e.g., S001)"
                value={studentId}
                onChange={(e) => setStudentId(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') submitCheckIn();
                }}
                required
              />
            </div>

            <button className="btn full" onClick={submitCheckIn}>
              Check In
            </button>
          </div>
        )}

        {success && (
          <div style={{ textAlign: 'center' }}>
            <div className="icon success-animation">
              <FaCheckCircle size={64} color="#28a745" />
            </div>
            <h2>Check-In Successful!</h2>
            <p style={{ marginTop: 15, color: '#666' }}>
              Your attendance has been recorded.
            </p>
            <p style={{ marginTop: 30 }}>
              <Link to="/student/history" className="text-link">
                View Attendance History
              </Link>
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
