const KEY = "clothing_auth";
const LEGACY_KEY = "auth";

function parseJSON(raw) {
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

export function saveAuthToStorage(data) {
  localStorage.setItem(KEY, JSON.stringify(data));
}

export function loadAuthFromStorage() {
  const scoped = parseJSON(localStorage.getItem(KEY));
  if (Object.keys(scoped).length) {
    return scoped;
  }

  const legacy = parseJSON(localStorage.getItem(LEGACY_KEY));
  if (!Object.keys(legacy).length) {
    return {};
  }

  const migrated = {
    token: legacy.token || "",
    roles: Array.isArray(legacy.roles) ? legacy.roles : [],
    username: legacy.username || "",
    userId: legacy.userId ?? null,
    adminSession: Boolean(legacy.adminSession)
  };

  if (migrated.token) {
    saveAuthToStorage(migrated);
  }

  return migrated;
}

export function clearAuthStorage() {
  localStorage.removeItem(KEY);
  localStorage.removeItem(LEGACY_KEY);
}
