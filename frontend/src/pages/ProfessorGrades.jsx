import { useEffect, useMemo, useState } from 'react';
import NavBar from '../components/NavBar';

const GRADE_TYPES = ['Exam', 'Project', 'Homework', 'Quiz'];

function initials(name) {
  return name
    .split(' ')
    .map((w) => w[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

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

export default function ProfessorGrades() {
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState('');
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [alert, setAlert] = useState(null);
  const [search, setSearch] = useState('');
  const [drafts, setDrafts] = useState({});
  const [savingFor, setSavingFor] = useState(null);

  useEffect(() => {
    if (!alert) return;
    const t = setTimeout(() => setAlert(null), 4000);
    return () => clearTimeout(t);
  }, [alert]);

  useEffect(() => {
    fetch('/api/professor/courses')
      .then((r) => r.json())
      .then(setCourses)
      .catch(() => setAlert({ message: 'Could not load courses', type: 'danger' }));
  }, []);

  useEffect(() => {
    if (!courseId) {
      setStudents([]);
      return;
    }
    loadCourseGrades(courseId);
  }, [courseId]);

  const loadCourseGrades = async (id) => {
    setLoading(true);
    try {
      const res = await fetch(`/api/professor/grades?courseId=${id}`);
      const data = await res.json();
      if (data.status === 'error') {
        setAlert({ message: data.message, type: 'danger' });
        setStudents([]);
      } else {
        setStudents(data.students || []);
      }
    } catch {
      setAlert({ message: 'Connection error while loading grades', type: 'danger' });
    } finally {
      setLoading(false);
    }
  };

  const draftFor = (studentId) =>
    drafts[studentId] || { gradeValue: '', gradeType: 'Exam', description: '' };

  const updateDraft = (studentId, patch) => {
    setDrafts((prev) => ({
      ...prev,
      [studentId]: { ...draftFor(studentId), ...patch },
    }));
  };

  const submitGrade = async (studentId) => {
    const draft = draftFor(studentId);
    const value = parseFloat(draft.gradeValue);
    if (isNaN(value) || value < 1 || value > 10) {
      setAlert({ message: 'Grade must be a number between 1 and 10', type: 'danger' });
      return;
    }
    if (!draft.gradeType?.trim()) {
      setAlert({ message: 'Please select a grade type', type: 'danger' });
      return;
    }
    setSavingFor(studentId);
    try {
      const res = await fetch('/api/professor/grades', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          studentId,
          courseId: Number(courseId),
          gradeValue: value,
          gradeType: draft.gradeType.trim(),
          description: draft.description?.trim() || null,
        }),
      });
      const data = await res.json();
      if (!res.ok || data.status === 'error') {
        setAlert({ message: data.message || 'Failed to save grade', type: 'danger' });
      } else {
        setAlert({ message: `Grade saved for ${studentId}`, type: 'success' });
        setDrafts((prev) => ({ ...prev, [studentId]: { gradeValue: '', gradeType: draft.gradeType, description: '' } }));
        loadCourseGrades(courseId);
      }
    } catch {
      setAlert({ message: 'Connection error', type: 'danger' });
    } finally {
      setSavingFor(null);
    }
  };

  const deleteGrade = async (gradeId) => {
    if (!confirm('Delete this grade?')) return;
    try {
      const res = await fetch(`/api/professor/grades/${gradeId}`, { method: 'DELETE' });
      const data = await res.json();
      if (!res.ok || data.status === 'error') {
        setAlert({ message: data.message || 'Failed to delete', type: 'danger' });
      } else {
        setAlert({ message: 'Grade deleted', type: 'success' });
        loadCourseGrades(courseId);
      }
    } catch {
      setAlert({ message: 'Connection error', type: 'danger' });
    }
  };

  const filteredStudents = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return students;
    return students.filter(
      (s) =>
        s.name.toLowerCase().includes(q) ||
        s.studentId.toLowerCase().includes(q) ||
        s.email.toLowerCase().includes(q)
    );
  }, [students, search]);

  const overallStats = useMemo(() => {
    const allGrades = students.flatMap((s) => s.grades || []);
    if (!allGrades.length) return null;
    const sum = allGrades.reduce((acc, g) => acc + Number(g.gradeValue), 0);
    const avg = sum / allGrades.length;
    const graded = students.filter((s) => (s.grades || []).length > 0).length;
    return { avg: avg.toFixed(2), total: allGrades.length, graded };
  }, [students]);

  const selectedCourse = courses.find((c) => String(c.id) === String(courseId));

  return (
    <>
      <NavBar />
      <div className="container">
        <div className="header">
          <h1>Grade Management</h1>
          <p>Record and review student grades for each course</p>
        </div>

        <div className="content">
          <div className="session-controls">
            <h2>Course</h2>
            <div className="form-group">
              <label htmlFor="gradeCourseSelect">Select a course to manage grades:</label>
              <select
                id="gradeCourseSelect"
                value={courseId}
                onChange={(e) => setCourseId(e.target.value)}
              >
                <option value="">-- Select a Course --</option>
                {courses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.courseCode} - {c.courseName}
                  </option>
                ))}
              </select>
            </div>
            {selectedCourse && (
              <div className="course-meta-line">
                <span className="info-label">Professor:</span>
                <span className="info-value">{selectedCourse.professorName}</span>
              </div>
            )}
          </div>

          {alert && <div className={`alert alert-${alert.type}`}>{alert.message}</div>}

          {loading && (
            <div>
              <div className="loader"></div>
              <p className="loading-text">Loading grades...</p>
            </div>
          )}

          {!loading && courseId && students.length > 0 && (
            <>
              {overallStats && (
                <div className="stats-row">
                  <div className="stat-card">
                    <div className="stat-number">{overallStats.graded}</div>
                    <div className="stat-label">Students with grades</div>
                  </div>
                  <div className="stat-card">
                    <div className="stat-number">{overallStats.total}</div>
                    <div className="stat-label">Total grades</div>
                  </div>
                  <div className="stat-card">
                    <div className="stat-number">{overallStats.avg}</div>
                    <div className="stat-label">Class average</div>
                  </div>
                </div>
              )}

              <div className="search-bar">
                <input
                  type="text"
                  placeholder="Search students by name, ID, or email..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </div>

              <div className="grade-grid">
                {filteredStudents.map((s) => {
                  const draft = draftFor(s.studentId);
                  return (
                    <div className="grade-card" key={s.studentId}>
                      <div className="grade-card-head">
                        <div className="student-left">
                          <div className="student-avatar">{initials(s.name)}</div>
                          <div>
                            <div className="student-name">{s.name}</div>
                            <div className="student-meta">
                              ID: {s.studentId} | {s.email}
                            </div>
                          </div>
                        </div>
                        {s.average != null && (
                          <div className={`grade-pill ${gradeTone(s.average)}`}>
                            <span className="pill-label">Avg</span>
                            <span className="pill-value">{Number(s.average).toFixed(2)}</span>
                          </div>
                        )}
                      </div>

                      {s.grades && s.grades.length > 0 ? (
                        <div className="grade-chips">
                          {s.grades.map((g) => (
                            <div className={`grade-chip ${gradeTone(g.gradeValue)}`} key={g.id} title={formatDate(g.createdAt)}>
                              <div className="chip-row">
                                <span className="chip-type">{g.gradeType}</span>
                                <span className="chip-value">{Number(g.gradeValue).toFixed(2)}</span>
                              </div>
                              {g.description && <div className="chip-desc">{g.description}</div>}
                              <button
                                className="chip-delete"
                                onClick={() => deleteGrade(g.id)}
                                title="Delete grade"
                                aria-label="Delete grade"
                              >
                                x
                              </button>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <div className="no-grades-hint">No grades yet</div>
                      )}

                      <div className="grade-entry-row">
                        <input
                          type="number"
                          step="0.01"
                          min="1"
                          max="10"
                          placeholder="Grade"
                          className="grade-input-value"
                          value={draft.gradeValue}
                          onChange={(e) => updateDraft(s.studentId, { gradeValue: e.target.value })}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') submitGrade(s.studentId);
                          }}
                        />
                        <select
                          className="grade-input-type"
                          value={draft.gradeType}
                          onChange={(e) => updateDraft(s.studentId, { gradeType: e.target.value })}
                        >
                          {GRADE_TYPES.map((t) => (
                            <option key={t} value={t}>
                              {t}
                            </option>
                          ))}
                        </select>
                        <input
                          type="text"
                          placeholder="Note (optional)"
                          className="grade-input-desc"
                          value={draft.description}
                          onChange={(e) => updateDraft(s.studentId, { description: e.target.value })}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') submitGrade(s.studentId);
                          }}
                          maxLength={100}
                        />
                        <button
                          className="btn btn-primary btn-compact"
                          onClick={() => submitGrade(s.studentId)}
                          disabled={savingFor === s.studentId}
                        >
                          {savingFor === s.studentId ? 'Saving...' : 'Add'}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>

              {filteredStudents.length === 0 && (
                <div className="empty-state">
                  <h3>No students match your search</h3>
                  <p>Try a different keyword</p>
                </div>
              )}
            </>
          )}

          {!loading && courseId && students.length === 0 && (
            <div className="empty-state">
              <h3>No students found</h3>
              <p>The system has no students enrolled.</p>
            </div>
          )}

          {!courseId && (
            <div className="empty-state">
              <h3>Select a course to begin</h3>
              <p>Pick a course from the dropdown above to see students and record grades.</p>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
