import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { downloadFile } from '../api.js'

export function SharedFilePage() {
  const { fileId } = useParams()
  const { token } = useAuth()
  const [state, setState] = useState({ status: 'loading' }) // loading | forbidden | notfound | ready | error

  useEffect(() => {
    downloadFile(token, fileId)
      .then((payload) => setState({ status: 'ready', data: payload.data }))
      .catch((error) => {
        if (error.status === 403) return setState({ status: 'forbidden' })
        if (error.status === 404) return setState({ status: 'notfound' })
        setState({ status: 'error', message: error.message })
      })
  }, [fileId, token])

  return (
    <main className="app-shell">
      <section className="panel">
        {state.status === 'loading' && <p>Verificando acceso…</p>}

        {state.status === 'forbidden' && (
          <div className="empty-state">No tienes permiso para ver este archivo.</div>
        )}

        {state.status === 'notfound' && <div className="empty-state">Este archivo no existe.</div>}

        {state.status === 'error' && <div className="message error">{state.message}</div>}

        {state.status === 'ready' && (
          <div>
            <p className="eyebrow">Archivo compartido</p>
            <h2>{state.data.fileName}</h2>
            <a className="primary-btn" href={state.data.downloadUrl} target="_blank" rel="noreferrer">
              Descargar archivo
            </a>
          </div>
        )}
      </section>
    </main>
  )
}
