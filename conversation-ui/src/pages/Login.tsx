import { useLogin } from '@/hooks/useLogin'
import './Login.css'

interface LoginProps {
  onSuccess: () => void
}

export default function Login({ onSuccess }: LoginProps) {
  const { token, setToken, error, login } = useLogin(onSuccess)

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    login()
  }

  return (
    <div className="login-page">
      <div className="login-container">
        <h1 className="login-title">登录</h1>
        <p className="login-subtitle">使用固定 Token 登录后开始 AI 对话</p>

        <form className="login-form" onSubmit={handleSubmit}>
          {error && <div className="login-error" role="alert">{error}</div>}

          <div className="login-field">
            <label htmlFor="token">固定 Token</label>
            <input
              id="token"
              type="password"
              autoComplete="off"
              spellCheck={false}
              placeholder="请输入固定 Token"
              value={token}
              onChange={(event) => setToken(event.target.value)}
            />
          </div>

          <button type="submit" className="login-submit" disabled={!token.trim()}>
            登录
          </button>
        </form>
      </div>
    </div>
  )
}
