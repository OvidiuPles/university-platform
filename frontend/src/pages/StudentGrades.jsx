import { useState, useEffect } from 'react';
import NavBar from '../components/NavBar';
import { apiFetch } from '../auth';

function gradeTone(value) {
  if (value == null) return 'tone-neutral';
  const v = Number(value);
  if (v >= 9) return 'tone-excellent';
  if (v >= 7) return 'tone-good';
  if (v >= 5) return 'tone-pass';
  return 'tone-fail';
}

function formatDate(iso) {
  if (!iso) return '-';
  return new Date(iso).toLocaleString();
}

export default function StudentGrades() {
  const [data, setData] = useState(null);
  const [alert, setAlert] = useState(null);
  const [loading, setLoading] = useState(false);

  const loadGrades = async () => {
    setAlert(null);
    setLoading(true);
    setData(null);
    try {
      const res = await apiFetch('/api/student/grades');
      const payload = await res.json();
      if (!res.ok || payload.status === 'error') {
        setAlert({ message: payload.message || 'Could not load grades', type: 'danger' });
      } else {
        setData(payload);
      }
    } catch {
      setAlert({ message: 'Connection error. Please try again.', type: 'danger' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGrades();
  }, []);

  const hasResults = data && (data.courses || []).length > 0;

  return (
    <>
      <NavBar />
      <div className="container narrow">
        <div className="header">
          <h1>My Grades</h1>
          <p>View your grades across all courses</p>
        </div>

        <div className="content">
          {alert && <div className={`alert alert-${alert.type}`}>{alert.message}</div>}

          {loading && (
            <div>
              <div className="loader"></div>
              <p className="loading-text">Loading grades...</p>
            </div>
          )}

          {data && hasResults && (
            <>
              <div className="stats-row">
                <div className="stat-card">
                  <div className="stat-number">{data.courses.length}</div>
                  <div className="stat-label">Courses</div>
                </div>
                <div className="stat-card">
                  <div className="stat-number">{data.totalGrades}</div>
                  <div className="stat-label">Total grades</div>
                </div>
              </div>

              <h2 className="section-title">Per-Course Grades</h2>

              <div className="course-grade-list">
                {data.courses.map((course) => (
                  <div className="course-grade-card" key={course.courseId}>
                    <div className="course-grade-head">
                      <div>
                        <div className="course-grade-title">
                          {course.courseCode} - {course.courseName}
                        </div>
                        <div className="course-grade-prof">Professor: {course.professorName}</div>
                      </div>
                    </div>

                    <div className="grade-rows">
                      {course.grades.map((g) => (
                        <div className="grade-row" key={g.id}>
                          <div className="grade-row-left">
                            <span className={`grade-badge ${gradeTone(g.gradeValue)}`}>
                              {Number(g.gradeValue).toFixed(2)}
                            </span>
                            <div>
                              <div className="grade-row-type">{g.gradeType}</div>
                              {g.description && (
                                <div className="grade-row-desc">{g.description}</div>
                              )}
                            </div>
                          </div>
                          <div className="grade-row-time">{formatDate(g.createdAt)}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}

          {data && !hasResults && (
            <div className="empty-state">
              <h3>No grades yet</h3>
              <p>You haven't received any grades. Check back later.</p>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
