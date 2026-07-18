import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { login as loginRequest, signup as signupRequest } from '../api.js'

export function AuthPage() {
  const [authMode, setAuthMode] = useState('login')
  const [form, setForm] = useState({ name: '', email: '', pass: '' })
  const [message, setMessage] = useState({ type: 'info', text: '' })
  const [loading, setLoading] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const redirectTo = searchParams.get('redirect') || '/'

  async function handleSubmit(event) {
    event.preventDefault()
    setLoading(true)
    setMessage({ type: 'info', text: '' })

    try {
      if (authMode === 'signup') {
        await signupRequest({ name: form.name, email: form.email, pass: form.pass })
        setMessage({ type: 'success', text: 'Usuario registrado correctamente. Ahora puedes iniciar sesión.' })
        setAuthMode('login')
        setForm((current) => ({ ...current, name: '' }))
      } else {
        const payload = await loginRequest({ email: form.email, pass: form.pass })
        const token = payload?.data?.token

        if (!token) {
          throw new Error('El backend no devolvió un token válido.')
        }

        login(token)
        navigate(redirectTo, { replace: true })
      }
    } catch (error) {
      setMessage({ type: 'error', text: error.message })
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="app-shell">
      <section className="hero-card">
        <div>
          <p className="eyebrow">Storage Service</p>
          <h1>Gestiona tus archivos con autenticación y enlaces de subida/descarga.</h1>
          <p className="hero-copy">
            Registra usuarios, inicia sesión y gestiona archivos desde un panel simple conectado con el flujo de S3/MinIO.
          </p>
        </div>
        <div className="hero-badge">API ready</div>
      </section>

      <section className="panel auth-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Acceso</p>
            <h2>{authMode === 'login' ? 'Iniciar sesión' : 'Crear cuenta'}</h2>
          </div>
          <div className="toggle-group" role="tablist" aria-label="Modo de autenticación">
            <button
              type="button"
              className={authMode === 'login' ? 'toggle active' : 'toggle'}
              onClick={() => setAuthMode('login')}
            >
              Login
            </button>
            <button
              type="button"
              className={authMode === 'signup' ? 'toggle active' : 'toggle'}
              onClick={() => setAuthMode('signup')}
            >
              Signup
            </button>
          </div>
        </div>

        {redirectTo !== '/' && (
          <p className="section-copy">
            Inicia sesión para continuar — te llevaremos de regreso a lo que estabas viendo.
          </p>
        )}

        <form onSubmit={handleSubmit} className="stacked-form">
          {authMode === 'signup' && (
            <label>
              <span>Nombre</span>
              <input
                type="text"
                value={form.name ?? ''}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Tu nombre"
                required
              />
            </label>
          )}

          <label>
            <span>Correo electrónico</span>
            <input
              type="email"
              value={form.email ?? ''}
              onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
              placeholder="usuario@correo.com"
              required
            />
          </label>

          <label>
            <span>Contraseña</span>
            <input
              type="password"
              value={form.pass ?? ''}
              onChange={(event) => setForm((current) => ({ ...current, pass: event.target.value }))}
              placeholder="••••••"
              required
            />
          </label>

          <button type="submit" className="primary-btn" disabled={loading}>
            {loading ? 'Procesando...' : authMode === 'login' ? 'Ingresar' : 'Registrar'}
          </button>
        </form>
      </section>

      {message.text && <div className={`message ${message.type}`}>{message.text}</div>}
    </main>
  )
}
