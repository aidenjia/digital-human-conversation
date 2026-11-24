const TOKEN_KEY = 'auth_token'
const EXPIRES_KEY = 'auth_token_expires_at'

export const AUTH_UNAUTHORIZED_EVENT = 'auth:unauthorized'

export function getToken(): string | null {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return null

  const expiresAt = localStorage.getItem(EXPIRES_KEY)
  if (expiresAt && Date.now() >= Number(expiresAt)) {
    clearToken()
    return null
  }

  return token
}

export function setToken(token: string, expiresIn?: number): void {
  localStorage.setItem(TOKEN_KEY, token)
  if (expiresIn) {
    localStorage.setItem(EXPIRES_KEY, String(Date.now() + expiresIn * 1000))
  } else {
    localStorage.removeItem(EXPIRES_KEY)
  }
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(EXPIRES_KEY)
}

export function loginWithFixedToken(value: string): void {
  const token = value.trim().replace(/^Bearer(?:[ \t]+|$)/i, '').trim()
  if (!token || /\s/.test(token)) throw new Error('请输入有效的固定 Token')
  setToken(token)
}

export function isAuthenticated(): boolean {
  return !!getToken()
}

export function notifyUnauthorized(): void {
  clearToken()
  window.dispatchEvent(new CustomEvent(AUTH_UNAUTHORIZED_EVENT))
}
