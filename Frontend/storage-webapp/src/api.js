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

async function requestJson(path, token, options = {}) {

  const headers = {
    Accept: 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  const payload = await parseJson(response)

  if (!response.ok) {
    const error = new Error(payload?.message || 'No se pudo completar la solicitud.')
    error.status = response.status
    throw error
  }

  return payload
}

// ── Auth ──────────────────────────────────────────────
export function signup({ name, email, pass }) {
  return requestJson('/file-store/api/v1/auth/signup', null, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, pass }),
  })
}

export function login({ email, pass }) {
  return requestJson('/file-store/api/v1/auth/login', null, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, pass }),
  })
}

// ── Archivos ──────────────────────────────────────────
export function getMyFiles(token) {
  return requestJson('/file-store/api/v1/files', token)
}


// ── Subida de archivos ─────────────────────────────────
export function requestUpload(token, { filename, contentType, size }) {
  
  return requestJson('/file-store/api/v1/files/upload-request', token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ filename, contentType, size }),
  })
}

// subida de archivos a S3 (o almacenamiento compatible con S3)
export async function uploadToStorage(uploadUrl, file) {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type || 'application/octet-stream' },
    body: file,
  })

  if (!response.ok) {
    throw new Error('No fue posible subir el archivo al almacenamiento.')
  }
}

// confirmación de subida de archivos (para que el backend registre el archivo como subido)
export function confirmUpload(token, fileId) {
  return requestJson(`/file-store/api/v1/files/${fileId}/confirm-upload`, token, {
    method: 'PUT',
  })
}

export function downloadFile(token, fileId) {
  return requestJson(`/file-store/api/v1/files/${fileId}/download`, token)
}

export function deleteFile(token, fileId) {
  return requestJson(`/file-store/api/v1/files/${fileId}/delete`, token, {
    method: 'DELETE',
  })
}

// ── Compartir: público (link para cualquiera) ────────
export function getPublicShareLink(token, fileId) {
  return requestJson(`/file-store/api/v1/files/${fileId}/get-sharelink`, token)
}

// ── Compartir: protegido (usuarios específicos) ──────
export function grantAccess(token, fileId, email) {
  return requestJson(`/file-store/api/v1/files/${fileId}/share-with`, token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  })
}

export function revokeAccess(token, fileId, userId) {
  return requestJson(`/file-store/api/v1/files/${fileId}/share-with/${userId}`, token, {
    method: 'DELETE',
  })
}

export function getUsersWithAccess(token, fileId) {
  return requestJson(`/file-store/api/v1/files/${fileId}/shared-with`, token)
}
