const KEY = "clothing_auth";

export function saveAuthToStorage(data) {
  localStorage.setItem(KEY, JSON.stringify(data));
}

export function loadAuthFromStorage() {
  const raw = localStorage.getItem(KEY);
  if (!raw) {
    return {};
  }
  try {
    return JSON.parse(raw);
  } catch {
    return {};
  }
}

export function clearAuthStorage() {
  localStorage.removeItem(KEY);
}
