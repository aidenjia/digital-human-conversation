import { useCallback, useState } from 'react'
import { loginWithFixedToken } from '@/utils/auth'

export function useLogin(onSuccess: () => void) {
  const [token, setToken] = useState('')
  const [error, setError] = useState<string | null>(null)

  const login = useCallback(() => {
    setError(null)
    try {
      loginWithFixedToken(token)
      setToken('')
      onSuccess()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '登录失败，请重新输入 Token')
    }
  }, [token, onSuccess])

  return { token, setToken, error, login }
}
