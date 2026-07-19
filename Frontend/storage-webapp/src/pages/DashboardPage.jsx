import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import { ShareDialog } from '../components/ShareDialog.jsx'
import { UploadBox } from '../components/files/UploadBox.jsx'
import { FileGrid } from '../components/files/FileGrid.jsx'
import {
  confirmUpload,
  deleteFile as deleteFileRequest,
  downloadFile,
  getMyFiles,
  requestUpload,
  uploadToStorage,
} from '../api.js'

function formatBytes(size) {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1)
  const value = size / 1024 ** index
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

export function DashboardPage() {
  const { token, logout } = useAuth()
  const [files, setFiles] = useState([])
  const [selectedFile, setSelectedFile] = useState(null)
  const [shareTarget, setShareTarget] = useState(null)
  const fileInputRef = useRef(null)
  const [message, setMessage] = useState({ type: 'info', text: '' })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadFiles()
  }, [])

  async function loadFiles() {
    try {
      setLoading(true)
      const payload = await getMyFiles(token)
      setFiles(payload?.data || [])
    } catch (error) {
      // Un usuario nuevo sin archivos recibe 404 desde el backend — no es un error real, solo lista vacía
      if (error.status === 404) {
        setFiles([])
      } else {
        setMessage({ type: 'error', text: error.message })
      }
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
      const uploadRequest = await requestUpload(token, {
        filename: selectedFile.name,
        contentType: selectedFile.type || 'application/octet-stream',
        size: selectedFile.size,
      })

      const { uploadUrl, fileId } = uploadRequest?.data || {}
      if (!uploadUrl || !fileId) {
        throw new Error('El backend no devolvió una URL de subida.')
      }

      await uploadToStorage(uploadUrl, selectedFile)
      await confirmUpload(token, fileId)
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
      const payload = await downloadFile(token, file.id)
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

  async function handleDelete(file) {
    if (!window.confirm(`¿Eliminar "${file.originalName}"? Esta acción no se puede deshacer.`)) return

    try {
      setLoading(true)
      await deleteFileRequest(token, file.id)
      await loadFiles()
      setMessage({ type: 'success', text: `"${file.originalName}" eliminado.` })
    } catch (error) {
      setMessage({ type: 'error', text: error.message })
    } finally {
      setLoading(false)
    }
  }

  return (

  <main className="dashboard">


   <section className="dashboard-header">


      <div className="dashboard-title">


        <p className="eyebrow">
          STORAGE
        </p>


        <h1>
          Mis archivos
        </h1>


        <p className="section-copy">
          Administra tus documentos almacenados en la nube.
        </p>


      </div>

    </section>




    <UploadBox

      selectedFile={selectedFile}

      setSelectedFile={setSelectedFile}

      fileInputRef={fileInputRef}

      onUpload={handleUpload}

      loading={loading}

    />





    <section>


      <div className="panel-heading">


        <h2>
          Archivos recientes
        </h2>



        <button

          className="secondary-btn"

          onClick={loadFiles}

          disabled={loading}

        >

          Actualizar

        </button>



      </div>




      <FileGrid

        files={files}

        onShare={setShareTarget}

        onDownload={handleDownload}

        onDelete={handleDelete}

      />



    </section>





    {
      message.text &&

      <div className={`message ${message.type}`}>

        {message.text}

      </div>

    }





    {
      shareTarget &&

      <ShareDialog

        file={shareTarget}

        onClose={() => setShareTarget(null)}

      />

    }

    

  </main>

  )
}
