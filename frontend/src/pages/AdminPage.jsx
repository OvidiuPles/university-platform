import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch, clearAuth } from '../auth';
import '../styles/admin.css';

const SELECT_PLACEHODLER = '-- Select --';

const TABS = [
  { key: 'users', label: 'Users' },
  { key: 'courses', label: 'Courses' },
  { key: 'sessions', label: 'Sessions' },
  { key: 'attendance', label: 'Attendance' },
  { key: 'grades', label: 'Grades' },
];

function formatDate(iso) {
  if (!iso) return '-';
  return new Date(iso).toLocaleString();
}

function toLocalInput(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function AdminPage() {
  const [tab, setTab] = useState('users');
  const [alert, setAlert] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!alert) return;
    const t = setTimeout(() => setAlert(null), 4000);
    return () => clearTimeout(t);
  }, [alert]);

  const logout = async () => {
    try {
      await apiFetch('/api/auth/logout', { method: 'POST' });
    } catch {}
    clearAuth();
    navigate('/login', { replace: true });
  };

  return (
    <div className="container">
      <div className="header admin-header">
        <h1>Admin Control</h1>
        <button className="nav-logout" onClick={logout}>Logout</button>
      </div>
      <div className="content admin-content">
        <div className="admin-tabs">
          {TABS.map((t) => (
            <button
              key={t.key}
              className={'admin-tab' + (tab === t.key ? ' active' : '')}
              onClick={() => setTab(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>

        {alert && (
          <div className={`alert alert-${alert.type === 'success' ? 'success' : 'danger'}`}>
            {alert.message}
          </div>
        )}

        {tab === 'users' && <UsersSection setAlert={setAlert} />}
        {tab === 'courses' && <CoursesSection setAlert={setAlert} />}
        {tab === 'sessions' && <SessionsSection setAlert={setAlert} />}
        {tab === 'attendance' && <AttendanceSection setAlert={setAlert} />}
        {tab === 'grades' && <GradesSection setAlert={setAlert} />}
      </div>
    </div>
  );
}

function useResource(endpoint, setAlert) {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);

  const reload = async () => {
    setLoading(true);
    try {
      const res = await apiFetch(endpoint);
      const data = await res.json();
      if (!res.ok || data.status === 'error') {
        setAlert({ message: data.message || 'Failed to load!', type: 'danger' });
        setRows([]);
      } else {
        setRows(Array.isArray(data) ? data : []);
      }
    } catch {
      setAlert({ message: 'Connection error!', type: 'danger' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    reload();
  }, []);

  const save = async (id, body) => {
    const url = id ? `${endpoint}/${id}` : endpoint;
    const method = id ? 'PUT' : 'POST';
    try {
      const res = await apiFetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const data = await res.json();
      if (!res.ok || data.status === 'error') {
        setAlert({ message: data.message || 'Save failed', type: 'danger' });
        return false;
      }

      setAlert({ message: id ? 'Updated' : 'Created', type: 'success' });
      reload();
      return true;
    } catch {
      setAlert({ message: 'Connection error', type: 'danger' });
      return false;
    }
  };

  const remove = async (id, label) => {
    if (!confirm(`Delete this ${label}?`)) return;
    try {
      const res = await apiFetch(`${endpoint}/${id}`, { method: 'DELETE' });
      const data = await res.json();
      if (!res.ok || data.status === 'error') {
        setAlert({ message: data.message || 'Delete failed', type: 'danger' });
      } else {
        setAlert({ message: 'Deleted', type: 'success' });
        reload();
      }
    } catch {
      setAlert({ message: 'Connection error', type: 'danger' });
    }
  };

  return { rows, loading, reload, save, remove };
}


function CrudTable({ columns, rows, onEdit, onDelete }) {
  if (!rows.length) {
    return <div className="admin-empty">No rows</div>;
  }
  return (
    <div className="admin-table-scroll">
      <table className="admin-table">
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c.key}>{c.label}</th>
            ))}
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}>
              {columns.map((c) => (
                <td key={c.key}>{c.render ? c.render(r) : (r[c.key] ?? '-')}</td>
              ))}
              <td className="admin-actions">
                <button onClick={() => onEdit(r)}>Edit</button>
                <button className="danger" onClick={() => onDelete(r.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function FormShell({ editingId, onCancel, onSubmit, children }) {
  return (
    <form
      className="admin-form"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit();
      }}
    >
      <div className="admin-form-title">{editingId ? `Edit ID=${editingId}` : 'Create new'}</div>
      <div className="admin-form-grid">{children}</div>
      <div className="admin-form-actions">
        <button type="submit">{editingId ? 'Save' : 'Create'}</button>
        {editingId && (
          <button type="button" className="secondary" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}

function UsersSection({ setAlert }) {
  const { rows, save, remove } = useResource('/api/admin/users', setAlert);
  const empty = { name: '', email: '', role: 'STUDENT', studentId: '' };
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);

  const setField = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const cancel = () => {
    setForm(empty);
    setEditingId(null);
  };
  const submit = async () => {
    const body = {
      name: form.name,
      email: form.email,
      studentId: form.studentId || null,
    };
    const ok = await save(editingId, body);
    if (ok) cancel();
  };

  const startEdit = (r) => {
    setEditingId(r.id);
    setForm({ name: r.name ?? '', email: r.email ?? '', role: r.role ?? 'STUDENT', studentId: r.studentId ?? '' });
  };

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'name', label: 'Name' },
    { key: 'email', label: 'Email' },
    { key: 'role', label: 'Role' },
    { key: 'studentId', label: 'Student ID' },
    { key: 'createdAt', label: 'Created', render: (r) => formatDate(r.createdAt) },
  ];

  return (
    <>
      {editingId && (
        <FormShell editingId={editingId} onCancel={cancel} onSubmit={submit}>
          <label>Name
            <input value={form.name} onChange={(e) => setField('name', e.target.value)} required />
          </label>
          <label>Email
            <input type="email" value={form.email} onChange={(e) => setField('email', e.target.value)} required />
          </label>
          <label>Role
            <input value={form.role} readOnly disabled />
          </label>
          <label>Student ID
            <input value={form.studentId} onChange={(e) => setField('studentId', e.target.value)} />
          </label>
        </FormShell>
      )}
      <CrudTable columns={columns} rows={rows} onEdit={startEdit} onDelete={(id) => remove(id, 'user')} />
    </>
  );
}

function CoursesSection({ setAlert }) {
  const { rows, save, remove } = useResource('/api/admin/courses', setAlert);
  const empty = { courseCode: '', courseName: '', professorName: '' };
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);

  const setField = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const cancel = () => {
    setForm(empty);
    setEditingId(null);
  };
  const submit = async () => {
    const ok = await save(editingId, form);
    if (ok) cancel();
  };
  const startEdit = (r) => {
    setEditingId(r.id);
    setForm({ courseCode: r.courseCode, courseName: r.courseName, professorName: r.professorName });
  };

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'courseCode', label: 'Code' },
    { key: 'courseName', label: 'Name' },
    { key: 'professorName', label: 'Professor' },
    { key: 'createdAt', label: 'Created', render: (r) => formatDate(r.createdAt) },
  ];

  return (
    <>
      <FormShell editingId={editingId} onCancel={cancel} onSubmit={submit}>
        <label>Course Code
          <input value={form.courseCode} onChange={(e) => setField('courseCode', e.target.value)} required />
        </label>
        <label>Course Name
          <input value={form.courseName} onChange={(e) => setField('courseName', e.target.value)} required />
        </label>
        <label>Professor Name
          <input value={form.professorName} onChange={(e) => setField('professorName', e.target.value)} required />
        </label>
      </FormShell>
      <CrudTable columns={columns} rows={rows} onEdit={startEdit} onDelete={(id) => remove(id, 'course')} />
    </>
  );
}

function SessionsSection({ setAlert }) {
  const { rows, save, remove } = useResource('/api/admin/sessions', setAlert);
  const [courses, setCourses] = useState([]);
  const empty = { courseId: '', sessionToken: '', startTime: '', expirationTime: '', isActive: true };
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    apiFetch('/api/admin/courses')
      .then((r) => r.json())
      .then((d) => setCourses(Array.isArray(d) ? d : []))
      .catch(() => {});
  }, []);

  const setField = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const cancel = () => {
    setForm(empty);
    setEditingId(null);
  };
  const submit = async () => {
    const body = {
      courseId: Number(form.courseId),
      sessionToken: form.sessionToken,
      startTime: form.startTime,
      expirationTime: form.expirationTime,
      isActive: form.isActive,
    };
    const ok = await save(editingId, body);
    if (ok) cancel();
  };
  const startEdit = (r) => {
    setEditingId(r.id);
    setForm({
      courseId: r.course?.id ?? '',
      sessionToken: r.sessionToken ?? '',
      startTime: toLocalInput(r.startTime),
      expirationTime: toLocalInput(r.expirationTime), 
      isActive: !!r.isActive,
    });
  };

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'courseCode', label: 'Course', render: (r) => r.course?.courseCode ?? '-' },
    { key: 'sessionToken', label: 'Token' },
    { key: 'startTime', label:  'Start', render: (r) => formatDate(r.startTime) },
    { key: 'expirationTime', label: 'Expires', render: (r) => formatDate(r.expirationTime) },
    { key: 'isActive', label: 'Active', render: (r) => (r.isActive ? 'yes' : 'no') },
  ];

  return (
    <>
      <FormShell editingId={editingId} onCancel={cancel} onSubmit={submit}>
        <label>Course
          <select value={form.courseId} onChange={(e) => setField('courseId', e.target.value)} required>
            <option value="">{SELECT_PLACEHODLER}</option>
            {courses.map((c) => (
              <option key={c.id} value={c.id}>{c.courseCode} - {c.courseName}</option>
            ))}
          </select>
        </label>
        <label>Session Token
          <input value={form.sessionToken} onChange={(e) => setField('sessionToken', e.target.value)} required />
        </label>
        <label>Start Time
          <input type="datetime-local" value={form.startTime} onChange={(e) => setField('startTime', e.target.value)} required />
        </label>
        <label>Expiration Time
          <input type="datetime-local" value={form.expirationTime} onChange={(e) => setField('expirationTime', e.target.value)} required />
        </label>
        <label className="admin-checkbox">
          <input type="checkbox" checked={form.isActive} onChange={(e) => setField('isActive', e.target.checked)} />
          Active
        </label>
      </FormShell>
      <CrudTable columns={columns} rows={rows} onEdit={startEdit} onDelete={(id) => remove(id, 'session')} />
    </>
  );
}

function AttendanceSection({ setAlert }) {
  const { rows, save, remove } = useResource('/api/admin/attendance', setAlert);
  const [sessions, setSessions] = useState([]);
  const [students, setStudents] = useState([]);
  const empty = { sessionId: '', studentId: '', checkInTime: '' };
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    apiFetch('/api/admin/sessions').then((r) => r.json()).then((d) => setSessions(Array.isArray(d) ? d : [])).catch(() => {});
    apiFetch('/api/admin/students').then((r) => r.json()).then((d) => setStudents(Array.isArray(d) ? d : [])).catch(() => {});
  }, []);

  const setField = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const cancel = () => {
    setForm(empty);
    setEditingId(null);
  };
  const submit = async () => {
    const body = {
      sessionId: Number(form.sessionId),
      studentId: form.studentId,
    };
    if (form.checkInTime) body.checkInTime = form.checkInTime;
    const ok = await save(editingId, body);
    if (ok) cancel();
  };
  const startEdit = (r) => {
    setEditingId(r.id);
    setForm({
      sessionId: r.session?.id ?? '',
      studentId: r.student?.studentId ?? '',
      checkInTime: toLocalInput(r.checkInTime),
    });
  };

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'sessionToken', label: 'Session', render: (r) => r.session?.sessionToken ?? '-' },
    { key: 'studentId', label: 'Student ID', render: (r) => r.student?.studentId ?? '-' },
    { key: 'studentName', label: 'Student', render: (r) => r.student?.name ?? '-' },
    { key: 'checkInTime', label: 'Check-in', render: (r) => formatDate(r.checkInTime) },
  ];

  return (
    <>
      <FormShell editingId={editingId} onCancel={cancel} onSubmit={submit}>
        <label>Session
          <select value={form.sessionId} onChange={(e) => setField('sessionId', e.target.value)} required>
            <option value="">{SELECT_PLACEHODLER}</option>
            {sessions.map((s) => (
              <option key={s.id} value={s.id}>#{s.id} {s.sessionToken}</option>
            ))}
          </select>
        </label>
        <label>Student
          <select value={form.studentId} onChange={(e) => setField('studentId', e.target.value)} required>
            <option value="">{SELECT_PLACEHODLER}</option>
            {students.map((s) => (
              <option key={s.id} value={s.studentId}>{s.studentId} - {s.name}</option>
            ))}
          </select>
        </label>
        <label>Check-in Time (optional)
          <input type="datetime-local" value={form.checkInTime} onChange={(e) => setField('checkInTime', e.target.value)} />
        </label>
      </FormShell>
      <CrudTable columns={columns} rows={rows} onEdit={startEdit} onDelete={(id) => remove(id, 'attendance')} />
    </>
  );
}

function GradesSection({ setAlert }) {
  const { rows, save, remove } = useResource('/api/admin/grades', setAlert);
  const [students, setStudents] = useState([]);
  const [courses, setCourses] = useState([]);
  const empty = { studentId: '', courseId: '', gradeValue: '', gradeType: '', description: '' };
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    apiFetch('/api/admin/students').then((r) => r.json()).then((d) => setStudents(Array.isArray(d) ? d : [])).catch(() => {});
    apiFetch('/api/admin/courses').then((r) => r.json()).then((d) => setCourses(Array.isArray(d) ? d : [])).catch(() => {});
  }, []);

  const setField = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const cancel = () => {
    setForm(empty);
    setEditingId(null);
  };
  const submit = async () => {
    const body = {
      studentId: form.studentId,
      courseId: Number(form.courseId),
      gradeValue: form.gradeValue === '' ? null : Number(form.gradeValue),
      gradeType: form.gradeType,
      description: form.description || null,
    };
    const ok = await save(editingId, body);
    if (ok) cancel();
  };
  const startEdit = (r) => {
    setEditingId(r.id);
    setForm({
      studentId: r.student?.studentId ?? '',
      courseId: r.course?.id ?? '',
      gradeValue: r.gradeValue ?? '',
      gradeType: r.gradeType ?? '',
      description: r.description ?? '',
    });
  };

  const columns = useMemo(() => [
    { key: 'id', label: 'ID' },
    { key: 'studentId', label: 'Student ID', render: (r) => r.student?.studentId ?? '-' },
    { key: 'studentName', label: 'Student', render: (r) => r.student?.name ?? '-' },
    { key: 'courseCode', label: 'Course', render: (r) => r.course?.courseCode ?? '-' },
    { key: 'gradeValue', label: 'Grade', render: (r) => r.gradeValue != null ? Number(r.gradeValue).toFixed(2) : '-' },
    { key: 'gradeType', label: 'Type' },
    { key: 'description', label: 'Description' },
    { key: 'createdAt', label: 'Created', render: (r) => formatDate(r.createdAt) },
  ], []);

  return (
    <>
      <FormShell editingId={editingId} onCancel={cancel} onSubmit={submit}>
        <label>Student
          <select value={form.studentId} onChange={(e) => setField('studentId', e.target.value)} required>
            <option value="">{SELECT_PLACEHODLER}</option>
            {students.map((s) => (
              <option key={s.id} value={s.studentId}>{s.studentId} - {s.name}</option>
            ))}
          </select>
        </label>
        <label>Course
          <select value={form.courseId} onChange={(e) => setField('courseId', e.target.value)} required>
            <option value="">{SELECT_PLACEHODLER}</option>
            {courses.map((c) => (
              <option key={c.id} value={c.id}>{c.courseCode} - {c.courseName}</option>
            ))}
          </select>
        </label>
        <label>Grade (1-10)
          <input type="number" step="0.1" min="1" max="10" value={form.gradeValue} onChange={(e) => setField('gradeValue', e.target.value)} required />
        </label>
        <label>Type
          <input value={form.gradeType} onChange={(e) => setField('gradeType', e.target.value)} maxLength={50} required />
        </label>
        <label>Description
          <input value={form.description} onChange={(e) => setField('description', e.target.value)} maxLength={70} />
        </label>
      </FormShell>
      <CrudTable columns={columns} rows={rows} onEdit={startEdit} onDelete={(id) => remove(id, 'grade')} />
    </>
  );
}
