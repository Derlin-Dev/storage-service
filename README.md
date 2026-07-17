# Storage Service

> 🚧 **Proyecto en desarrollo activo.** Algunas funcionalidades (permisos de acceso a archivos, validación de subida, eliminación de archivos) todavía no están implementadas. Ver la sección [Estado actual del proyecto / Roadmap](#estado-actual-del-proyecto--roadmap) para el detalle completo.

Servicio de almacenamiento de archivos con backend en **Spring Boot** y frontend en **React**. Permite a usuarios autenticados subir, listar y descargar archivos, delegando el almacenamiento real a un backend compatible con S3 (AWS S3 o MinIO) mediante **URLs firmadas (presigned URLs)**, en lugar de hacer proxy de los bytes a través del backend.

## Arquitectura

```
storage-service/
├── Backend/storage-service/    → API REST (Spring Boot 3.5, Java 21)
└── Frontend/storage-webapp/    → Cliente web (React 19 + Vite)
```

```
┌─────────────────┐       JWT      ┌──────────────────┐
│  Frontend React  │ ─────────────▶│  Backend Spring   │
└─────────────────┘                │      Boot         │
                                    └──────┬───────────┘
                                           │
                              ┌────────────┼─────────────┐
                              ▼            ▼             ▼
                        PostgreSQL   S3 / MinIO     (metadata +
                        (metadatos)  (bytes reales)  presigned URLs)
```

El backend nunca recibe el archivo en sí: genera una URL firmada de subida (`PUT`) o descarga (`GET`) contra el bucket S3, y el cliente sube/descarga directamente contra el storage. Esto evita cargar el servidor de aplicación con tráfico de archivos pesados — es el patrón recomendado para este tipo de servicios.

## Stack tecnológico

**Backend**
- Java 21 · Spring Boot 3.5
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL (metadatos de archivos y usuarios)
- AWS SDK v2 (`software.amazon.awssdk:s3`) — compatible con MinIO como sustituto local de S3
- JWT (`io.jsonwebtoken` / jjwt 0.12.6) para autenticación stateless
- Lombok

**Frontend**
- React 19 + Vite
- ESLint

## Requisitos previos

- Java 21 (JDK)
- Maven (o usar el wrapper incluido `./mvnw`)
- Node.js 18+ y npm
- PostgreSQL 14+
- Un servicio compatible con S3 — [MinIO](https://min.io/) para desarrollo local, o un bucket real de AWS S3 en producción

## Configuración

El backend lee su configuración desde `Backend/storage-service/src/main/resources/application.properties`:

| Propiedad | Descripción |
|---|---|
| `jwt.secret` | Clave usada para firmar los JWT |
| `jwt.expiration-minutes` | Minutos de validez del token (por defecto: 60) |
| `spring.datasource.url` / `username` / `password` | Conexión a PostgreSQL |
| `storage.endpoint` | Endpoint del servicio S3/MinIO |
| `storage.region` | Región (usar `us-east-1` para MinIO local) |
| `storage.bucket` | Nombre del bucket donde se guardan los archivos |
| `storage.access-key` / `storage.secret-key` | Credenciales del servicio de almacenamiento |

> ⚠️ **Importante:** el archivo `application.properties` versionado en este repo contiene valores de ejemplo para desarrollo local (incluido un `jwt.secret` y credenciales de MinIO). **Antes de desplegar a producción, estos valores deben moverse a variables de entorno** (o a un `.env` no versionado) y regenerarse — nunca usar en producción un `jwt.secret` que estuvo público en el historial de Git.

### Levantar PostgreSQL y MinIO localmente (ejemplo)

```bash
docker run -d --name storage-postgres \
  -e POSTGRES_DB=file_store_db \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin123 \
  -p 5432:5432 postgres:16

docker run -d --name storage-minio \
  -e MINIO_ROOT_USER=admin \
  -e MINIO_ROOT_PASSWORD=admin123456 \
  -p 9000:9000 -p 9001:9001 \
  minio/minio server /data --console-address ":9001"
```

*(El repo incluye un `docker-compose.yml`, pero aún está vacío — este es un buen próximo paso para reemplazar los comandos manuales de arriba por un solo `docker-compose up`.)*

## Instalación y ejecución

### Backend

```bash
cd Backend/storage-service
./mvnw spring-boot:run
```

El servicio arranca por defecto en `http://localhost:8080`.

### Frontend

```bash
cd Frontend/storage-webapp
npm install
npm run dev
```

Vite expone el frontend en `http://localhost:5173` (por defecto). Configura la variable `VITE_API_BASE_URL` en un archivo `.env` dentro de `Frontend/storage-webapp` apuntando al backend, por ejemplo:

```
VITE_API_BASE_URL=http://localhost:8080
```

## Documentación de la API

Todos los endpoints están bajo el prefijo `/file-store/api/v1`. Las respuestas siguen un formato estándar:

```json
{
  "success": true,
  "message": "Descripción del resultado",
  "data": { }
}
```

### Autenticación (`/auth`)

#### `POST /file-store/api/v1/auth/signup`
Registra un nuevo usuario.

**Request body:**
```json
{
  "name": "Derlin Valera",
  "email": "derlin@example.com",
  "pass": "contraseña123"
}
```

**Response:** `201 Created`

---

#### `POST /file-store/api/v1/auth/login`
Autentica un usuario y devuelve un JWT.

**Request body:**
```json
{
  "email": "derlin@example.com",
  "pass": "contraseña123"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Inicio exitoso",
  "data": { "token": "eyJhbGciOiJIUzI1NiJ9..." }
}
```

El token debe enviarse en las siguientes peticiones mediante el header:
```
Authorization: Bearer <token>
```

### Archivos (`/files`)

> Todos los endpoints de esta sección requieren autenticación.

#### `POST /file-store/api/v1/files/upload-request`
Solicita una URL firmada para subir un archivo directamente a S3/MinIO.

**Request body:**
```json
{
  "filename": "documento.pdf",
  "contentType": "application/pdf",
  "size": 204800
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Url generada correctamente",
  "data": {
    "fileId": "b3f1c2...",
    "uploadUrl": "https://.../files/upload/....pdf?X-Amz-Signature=...",
    "objectKey": "upload/....pdf"
  }
}
```

El cliente debe hacer un `PUT` directamente a `uploadUrl` con el binario del archivo (esta URL es válida por 10 minutos).

---

#### `GET /file-store/api/v1/files`
Lista todos los archivos subidos por el usuario autenticado.

**Response:** `200 OK` — arreglo de objetos con `id`, `bucket`, `originalName`, `contentType`, `extension`, `size`, `createdAt`, `uploadedAt`.

---

#### `GET /file-store/api/v1/files/{id}/download`
Genera una URL firmada de descarga para un archivo específico.

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Archivos encontrados",
  "data": {
    "fileId": "b3f1c2...",
    "fileName": "documento.pdf",
    "contentType": "application/pdf",
    "size": 204800,
    "downloadUrl": "https://.../files/upload/....pdf?X-Amz-Signature=..."
  }
}
```

## Modelo de datos

**`files`**

| Campo | Tipo | Notas |
|---|---|---|
| `id` | UUID | PK |
| `bucket` | String | Bucket S3/MinIO |
| `objectKey` | String | Ruta única del objeto (`upload/<uuid>.<ext>`) |
| `originalName` | String | Nombre original del archivo |
| `contentType` | String | MIME type |
| `extension` | String | Extensión del archivo |
| `size` | Long | Tamaño en bytes |
| `status` | Enum | `PENDING_UPLOAD`, `UPLOADED`, `FAILED`, `DELETED` |
| `createdAt` | LocalDateTime | Timestamp de creación del registro |
| `uploadedAt` | LocalDateTime | Timestamp de subida confirmada |
| `uploadedBy` | FK → `users` | Dueño del archivo |

## Estado actual del proyecto / Roadmap

Este proyecto está en desarrollo activo. Estado actual de cada componente:

- ✅ Registro y login con JWT
- ✅ Generación de URL firmada de subida y almacenamiento de metadatos
- ✅ Listado de archivos por usuario
- ✅ Generación de URL firmada de descarga
- 🚧 **`FilePermissionService`** — clase creada, aún sin implementar (control de acceso a archivos de otros usuarios)
- 🚧 **`FileValidationService`** — clase creada, aún sin implementar (validación de tipo/tamaño de archivo antes de generar la URL de subida)
- 🚧 **Eliminación de archivos** (`S3StorageProvider.delete()`) — método presente pero sin lógica
- 🚧 **Verificación de existencia** (`S3StorageProvider.exists()`) — método presente pero sin lógica
- 🚧 `docker-compose.yml` — archivo presente pero vacío; pendiente de definir servicios (backend, Postgres, MinIO)
- 🚧 Confirmación de subida — actualmente no hay un endpoint que marque el archivo como `UPLOADED` tras el `PUT` exitoso a S3; el estado queda en `PENDING_UPLOAD`

## Notas de seguridad

- Las contraseñas se almacenan con `BCryptPasswordEncoder`.
- La sesión es completamente stateless (JWT), sin `HttpSession`.
- **Pendiente:** actualmente cualquier usuario autenticado puede solicitar la descarga de cualquier `fileId` (ver `downloadFile` en `FileService`), sin verificar que el archivo le pertenezca. Esto es exactamente lo que `FilePermissionService` debería resolver una vez implementado.