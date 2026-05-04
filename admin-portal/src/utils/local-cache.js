const isClient = typeof window !== 'undefined'

export function getLocalJson(key, fallback = null) {
  if (!isClient || !key) return fallback
  try {
    const raw = window.localStorage.getItem(key)
    if (!raw) return fallback
    return JSON.parse(raw)
  } catch (error) {
    return fallback
  }
}

export function setLocalJson(key, value) {
  if (!isClient || !key) return
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
    // ignore quota or serialization errors
  }
}

