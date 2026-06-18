const STORAGE_KEY = 'auth';

export function getAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setAuth(auth) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
}

export function clearAuth() {
  localStorage.removeItem(STORAGE_KEY);
}

export function isLoggedIn() {
  return !!getAuth()?.token;
}

export function getRole() {
  return getAuth()?.role || null;
}

export function homePathFor(role) {
  if (role === 'ADMIN') return '/admin';
  if (role === 'PROFESSOR') return '/professor';
  return '/student/grades';
}

export async function apiFetch(url, options = {}) {
  const auth = getAuth();
  const headers = { ...(options.headers || {}) };
  if (auth?.token) headers.Authorization = `Bearer ${auth.token}`;

  const res = await fetch(url, { ...options, headers });
  if (res.status === 401) {
    clearAuth();
    if (window.location.pathname !== '/login') window.location.href = '/login';
  }
  return res;
}
