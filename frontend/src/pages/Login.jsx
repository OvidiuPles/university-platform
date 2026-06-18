import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { setAuth, homePathFor } from '../auth';

export default function Login() {
  const navigate = useNavigate();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    role: 'STUDENT',
    studentId: '',
  });
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const setField = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const isRegister = mode === 'register';

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const url = isRegister ? '/api/auth/register' : '/api/auth/login';
      const body = isRegister
        ? {
            name: form.name,
            email: form.email,
            password: form.password,
            role: form.role,
            studentId: form.role === 'STUDENT' ? form.studentId : null,
          }
        : { email: form.email, password: form.password };

      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const data = await res.json();
      if (!res.ok || data.status === 'error') {
        setError(data.message || 'Something went wrong');
        return;
      }
      setAuth(data);
      navigate(homePathFor(data.role), { replace: true });
    } catch {
      setError('Connection error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container compact">
      <div className="header tall compact">
        <h1>{isRegister ? 'Create Account' : 'Sign In'}</h1>
        <p>{import.meta.env.VITE_UNIVERSITY_NAME || 'University Platform'}</p>
      </div>

      <div className="content tall">
        {error && <div className="message error">{error}</div>}

        <form onSubmit={submit}>
          {isRegister && (
            <div className="form-group tall">
              <label htmlFor="name">Full Name</label>
              <input
                id="name"
                value={form.name}
                onChange={(e) => setField('name', e.target.value)}
                required
              />
            </div>
          )}

          <div className="form-group tall">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              value={form.email}
              onChange={(e) => setField('email', e.target.value)}
              required
            />
          </div>

          <div className="form-group tall">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={form.password}
              onChange={(e) => setField('password', e.target.value)}
              required
            />
          </div>

          {isRegister && (
            <>
              <div className="form-group tall">
                <label htmlFor="role">Account Type</label>
                <select
                  id="role"
                  value={form.role}
                  onChange={(e) => setField('role', e.target.value)}
                >
                  <option value="STUDENT">Student</option>
                  <option value="PROFESSOR">Professor</option>
                </select>
              </div>

              {form.role === 'STUDENT' && (
                <div className="form-group tall">
                  <label htmlFor="studentId">Student ID (e.g. S001)</label>
                  <input
                    id="studentId"
                    value={form.studentId}
                    onChange={(e) => setField('studentId', e.target.value)}
                  />
                </div>
              )}
            </>
          )}

          <button className="btn full" type="submit" disabled={loading}>
            {loading ? 'Please wait...' : isRegister ? 'Create Account' : 'Sign In'}
          </button>
        </form>

        <p style={{ marginTop: 20, textAlign: 'center', color: '#666' }}>
          {isRegister ? 'Already have an account?' : "Don't have an account?"}{' '}
          <button
            type="button"
            className="link-button"
            onClick={() => {
              setError(null);
              setMode(isRegister ? 'login' : 'register');
            }}
          >
            {isRegister ? 'Sign in' : 'Register'}
          </button>
        </p>
      </div>
    </div>
  );
}
