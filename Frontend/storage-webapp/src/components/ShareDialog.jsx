import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import { getPublicShareLink, getUsersWithAccess, grantAccess, revokeAccess } from '../api.js'

export function ShareDialog({ file, onClose }) {
  const { token } = useAuth()
  const [publicLink, setPublicLink] = useState(null)
  const [sharedUsers, setSharedUsers] = useState([])
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState({ type: 'info', text: '' })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadSharedUsers()
  }, [file.id])

  async function loadSharedUsers() {
    try {
      const payload = await getUsersWithAccess(token, file.id)
      setSharedUsers(payload?.data || [])
    } catch (error) {
      setMessage({ type: 'error', text: error.message })
    }
  }

  async function handleGeneratePublicLink() {
    setLoading(true)
    setMessage({ type: 'info', text: '' })
    try {
      const payload = await getPublicShareLink(token, file.id)
      setPublicLink(payload?.data)
      setMessage({ type: 'success', text: 'Link público generado. Cualquiera con este link puede descargar el archivo.' })
    } catch (error) {
      setMessage({ type: 'error', text: error.message })
    } finally {
      setLoading(false)
    }
  }

  async function handleCopyLink() {
    if (!publicLink) return
    await navigator.clipboard.writeText(publicLink)
    setMessage({ type: 'success', text: 'Link copiado al portapapeles.' })
  }

  async function handleGrantAccess(event) {
    event.preventDefault()
    setLoading(true)
    setMessage({ type: 'info', text: '' })

    try {
      const payload = await grantAccess(token, file.id, email)
      setEmail('')
      await loadSharedUsers()
      setMessage({
        type: 'success',
        text: `Acceso otorgado. Comparte este link con esa persona: ${payload?.data}`,
      })
    } catch (error) {
      // 404 aquí normalmente significa que el correo no tiene cuenta todavía
      setMessage({ type: 'error', text: error.message })
    } finally {
      setLoading(false)
    }
  }

  async function handleRevoke(userId) {
    setLoading(true)
    try {
      await revokeAccess(token, file.id, userId)
      await loadSharedUsers()
      setMessage({ type: 'success', text: 'Acceso revocado.' })
    } catch (error) {
      setMessage({ type: 'error', text: error.message })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Compartir</p>
            <h2>{file.originalName}</h2>
          </div>
          <button type="button" className="secondary-btn" onClick={onClose}>
            Cerrar
          </button>
        </div>

        <div className="share-section">
          <p className="section-title">Link público</p>
          <p className="section-copy">Cualquiera con el link puede descargar el archivo, sin necesidad de cuenta.</p>

          {!publicLink ? (
            <button type="button" className="primary-btn" onClick={handleGeneratePublicLink} disabled={loading}>
              Generar link público
            </button>
          ) : (
            <div className="share-link-row">
              <input type="text" readOnly value={publicLink} />
              <button type="button" className="secondary-btn" onClick={handleCopyLink}>
                Copiar
              </button>
            </div>
          )}
        </div>

        <div className="share-section">
          <p className="section-title">Compartir con usuarios específicos</p>
          <p className="section-copy">
            Solo funciona con correos que ya tienen cuenta en la plataforma — la persona deberá iniciar sesión para acceder.
          </p>

          <form onSubmit={handleGrantAccess} className="stacked-form">
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="correo@ejemplo.com"
              required
            />
            <button type="submit" className="primary-btn" disabled={loading}>
              Otorgar acceso
            </button>
          </form>

          {sharedUsers.length > 0 && (
            <ul className="file-list">
              {sharedUsers.map((user) => (
                <li key={user.userId} className="file-item">
                  <div>
                    <strong>{user.email}</strong>
                  </div>
                  <button type="button" className="secondary-btn" onClick={() => handleRevoke(user.userId)}>
                    Revocar
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        {message.text && <div className={`message ${message.type}`}>{message.text}</div>}
      </div>
    </div>
  )
}
