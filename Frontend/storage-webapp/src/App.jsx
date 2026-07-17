import { useEffect, useMemo, useRef, useState } from 'react'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

async function parseJson(response) {
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

async function requestJson(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      Accept: 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  })

  const payload = await parseJson(response)

  if (!response.ok) {
    throw new Error(payload?.message || 'No se pudo completar la solicitud.')
  }

  return payload
}

function formatBytes(size) {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1)
  const value = size / 1024 ** index
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

function App() {
  const [authMode, setAuthMode] = useState('login')
  const [form, setForm] = useState({ name: '', email: '', pass: '' })
  const [token, setToken] = useState(() => localStorage.getItem('storage-token') || '')
  const [files, setFiles] = useState([])
  const [selectedFile, setSelectedFile] = useState(null)
  const fileInputRef = useRef(null)
  const [message, setMessage] = useState({ type: 'info', text: '' })
  const [loading, setLoading] = useState(false)

  const isLoggedIn = useMemo(() => Boolean(token), [token])

  useEffect(() => {
    if (token) {
      loadFiles()
    }
  }, [token])

  async function loadFiles() {
    if (!token) return

    try {
      setLoading(true)
      const payload = await requestJson('/file-store/api/v1/files', {
        headers: { Authorization: `Bearer ${token}` },
      })

      setFiles(payload?.data || [])
      setMessage({ type: 'info', text: payload?.message || 'Archivos listados correctamente.' })
    } catch (error) {
      setMessage({ type: 'error', text: error.message })
    } finally {
      setLoading(false)
    }
  }

  async function handleAuthSubmit(event) {
    event.preventDefault()
    setLoading(true)
    setMessage({ type: 'info', text: '' })

    try {
      if (authMode === 'signup') {
        await requestJson('/file-store/api/v1/auth/signup', {
          method: 'POST',
          body: JSON.stringify({
            name: form.name,
            email: form.email,
            pass: form.pass,
          }),
          headers: { 'Content-Type': 'application/json' },
        })

        setMessage({ type: 'success', text: 'Usuario registrado correctamente. Ahora puedes iniciar sesión.' })
        setAuthMode('login')
        setForm((current) => ({ ...current, name: '' }))
      } else {
        const payload = await requestJson('/file-store/api/v1/auth/login', {
          method: 'POST',
          body: JSON.stringify({
            email: form.email,
            pass: form.pass,
          }),
          headers: { 'Content-Type': 'application/json' },
        })

        const nextToken = payload?.data?.token
        if (!nextToken) {
          throw new Error('El backend no devolvió un token válido.')
        }

        localStorage.setItem('storage-token', nextToken)
        setToken(nextToken)
        setMessage({ type: 'success', text: payload.message || 'Inicio de sesión exitoso.' })
      }
    } catch (error) {
      setMessage({ type: 'error', text: error.message })
    } finally {
      setLoading(false)
    }
  }

  async function handleUpload(event) {
    event.preventDefault()
    if (!selectedFile) {
      setMessage({ type: 'error', text: 'Selecciona un archivo para subir.' })
      return
    }

    try {
      setLoading(true)
      const uploadRequest = await requestJson('/file-store/api/v1/files/upload-request', {
        method: 'POST',
        body: JSON.stringify({
          filename: selectedFile.name,
          contentType: selectedFile.type || 'application/octet-stream',
          size: selectedFile.size,
        }),
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      })

      const uploadUrl = uploadRequest?.data?.uploadUrl
      if (!uploadUrl) {
        throw new Error('El backend no devolvió una URL de subida.')
      }

      const uploadResponse = await fetch(uploadUrl, {
        method: 'PUT',
        headers: { 'Content-Type': selectedFile.type || 'application/octet-stream' },
        body: selectedFile,
      })

      if (!uploadResponse.ok) {
        throw new Error('No fue posible subir el archivo al almacenamiento.')
      }

      await loadFiles()
      setMessage({ type: 'success', text: `Archivo "${selectedFile.name}" subido correctamente.` })
      setSelectedFile(null)
      if (fileInputRef.current) fileInputRef.current.value = ''
      event.currentTarget.reset()
    } catch (error) {
      setMessage({ type: 'error', text: error.message })
    } finally {
      setLoading(false)
    }
  }

  async function handleDownload(file) {
    try {
      setLoading(true)
      const payload = await requestJson(`/file-store/api/v1/files/${file.id}/download`, {
        headers: { Authorization: `Bearer ${token}` },
      })

      const downloadUrl = payload?.data?.downloadUrl
      if (!downloadUrl) {
        throw new Error('El backend no devolvió una URL de descarga.')
      }

      window.open(downloadUrl, '_blank', 'noopener,noreferrer')
      setMessage({ type: 'success', text: `Descarga iniciada para ${file.originalName}.` })
    } catch (error) {
      setMessage({ type: 'error', text: error.message })
    } finally {
      setLoading(false)
    }
  }

  function handleLogout() {
    localStorage.removeItem('storage-token')
    setToken('')
    setFiles([])
    setMessage({ type: 'info', text: 'Sesión cerrada.' })
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

      {!isLoggedIn ? (
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

          <form onSubmit={handleAuthSubmit} className="stacked-form">
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
      ) : (
        <section className="panel files-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Archivos</p>
              <h2>Operaciones de almacenamiento</h2>
            </div>
            <button type="button" className="secondary-btn" onClick={handleLogout}>
              Cerrar sesión
            </button>
          </div>

          <form onSubmit={handleUpload} className="upload-card">
            <div>
              <p className="section-title">Subir archivo</p>
              <p className="section-copy">Se solicita una URL al backend y luego se sube al servicio de almacenamiento.</p>
            </div>
            <label className="file-picker">
              <span>{selectedFile ? selectedFile.name : 'Selecciona un archivo'}</span>
              <input
                ref={fileInputRef}
                type="file"
                onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
              />
            </label>
            <button type="submit" className="primary-btn" disabled={loading}>
              {loading ? 'Subiendo...' : 'Subir archivo'}
            </button>
          </form>

          <div className="files-list-card">
            <div className="list-header">
              <p className="section-title">Archivos del usuario</p>
              <button type="button" className="secondary-btn" onClick={loadFiles} disabled={loading}>
                Refrescar
              </button>
            </div>

            {files.length === 0 ? (
              <div className="empty-state">Aún no hay archivos para mostrar.</div>
            ) : (
              <ul className="file-list">
                {files.map((file) => (
                  <li key={file.id} className="file-item">
                    <div>
                      <strong>{file.originalName}</strong>
                      <p>
                        {file.contentType} · {formatBytes(file.size)} · {file.createdAt?.slice(0, 10) || '—'}
                      </p>
                    </div>
                    <button type="button" className="secondary-btn" onClick={() => handleDownload(file)}>
                      Descargar
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>
      )}

      {message.text && (
        <div className={`message ${message.type}`}>{message.text}</div>
      )}
    </main>
  )
}

export default App
