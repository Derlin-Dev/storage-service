# Storage Service

> 🚧 **Proyecto en desarrollo activo.** El flujo principal (auth, subida, descarga, link público y compartir con usuarios específicos) ya funciona de punta a punta. Quedan pendientes de limpieza menores — ver [Pendientes de limpieza](#pendientes-de-limpieza).

**NOTA**
Revisa el README-docker.md para mas informacion sobre como correr la aplicacion!!

Servicio de almacenamiento de archivos con backend en **Spring Boot** y frontend en **React**. Permite a usuarios autenticados subir, listar, descargar y compartir archivos — de forma pública (con link) o protegida (solo para usuarios específicos) — delegando el almacenamiento real a un backend compatible con S3 (AWS S3 o MinIO) mediante **URLs firmadas (presigned URLs)**.

## Arquitectura

```
storage-service/
├── Backend/storage-service/    → API REST (Spring Boot 3.5, Java 21)
└── Frontend/storage-webapp/    → Cliente web (React 19 + Vite + React Router)
```

```
┌──────────────────┐       JWT (proxy Vite)     ┌──────────────────┐
│  Frontend React   │ ─────────────────────────▶│  Backend Spring   │
│  (localhost:5173) │                            │  (localhost:8080) │
└──────────────────┘                            └──────┬───────────┘
                                                        │
                                          ┌─────────────┼──────────────┐
                                          ▼             ▼              ▼
                                    PostgreSQL    S3 / MinIO      (metadata +
                                    (metadatos)   (bytes reales)  presigned URLs)
```

El backend nunca recibe el archivo en sí: genera una URL firmada de subida (`PUT`) o descarga (`GET`) contra el bucket S3, y el cliente sube/descarga directamente contra el storage.

En desarrollo, el frontend **no le habla directo al backend** — Vite corre un proxy interno (`vite.config.js`) que redirige todo lo que empieza con `/file-store` hacia `http://localhost:8080`. Por eso, en las DevTools del navegador siempre vas a ver las peticiones yendo a `localhost:5173`, aunque el backend responda desde el `8080` — es el comportamiento esperado, no un error.

## Stack tecnológico

**Backend**
- Java 21 · Spring Boot 3.5
- Spring Web, Spring Data JPA, Spring Security (con CORS configurado vía `CorsConfigurationSource`)
- PostgreSQL (metadatos de archivos, usuarios y permisos)
- AWS SDK v2 (`software.amazon.awssdk:s3`) — compatible con MinIO como sustituto local de S3
- JWT (`io.jsonwebtoken` / jjwt 0.12.6) para autenticación stateless
- Lombok

**Frontend**
- React 19 + Vite
- React Router (rutas protegidas, redirección post-login)
- ESLint

## Requisitos previos

- Java 21 (JDK)
- Maven (o usar el wrapper incluido `./mvnw`)
- Node.js 18+ y npm
- PostgreSQL 14+
- Un servicio compatible con S3 — [MinIO](https://min.io/) para desarrollo local, o un bucket real de AWS S3 en producción

## Configuración

**`Backend/storage-service/src/main/resources/application.properties`:**

| Propiedad | Descripción |
|---|---|
| `jwt.secret` | Clave usada para firmar los JWT |
| `jwt.expiration-minutes` | Minutos de validez del token (por defecto: 60) |
| `spring.datasource.url` / `username` / `password` | Conexión a PostgreSQL |
| `storage.endpoint` | Endpoint del servicio S3/MinIO |
| `storage.region` | Región (usar `us-east-1` para MinIO local) |
| `storage.bucket` | Nombre del bucket donde se guardan los archivos |
| `storage.access-key` / `storage.secret-key` | Credenciales del servicio de almacenamiento |
| `app.base-url` | URL pública del **backend** (usada para armar los links de descarga pública, `/files/share/{token}`) |
| `app.frontend-url` | URL del **frontend** (usada para armar los links de "compartir con usuario específico", `/files/{id}`) |

> ⚠️ **Importante:** los valores de `jwt.secret` y credenciales de MinIO en este repo son de ejemplo para desarrollo local. **Antes de desplegar a producción, muévelos a variables de entorno** y regenera el `jwt.secret` — nunca uses en producción una clave que estuvo pública en el historial de Git.

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

## Instalación y ejecución

### Backend

```bash
cd Backend/storage-service
./mvnw spring-boot:run
```

Arranca en `http://localhost:8080`.

### Frontend

```bash
cd Frontend/storage-webapp
npm install
npm run dev
```

Arranca en `http://localhost:5173`, con el proxy hacia el backend ya configurado — **no hace falta ninguna variable de entorno adicional** en desarrollo local.

> **Nota:** `npm run build` solo genera los archivos estáticos de producción — no levanta el proxy ni ningún servidor. Para desarrollar y probar el flujo completo (login, subida, compartir), siempre usa `npm run dev`.

## Docker

Todo el stack (backend, frontend, PostgreSQL y MinIO) también se puede levantar con un solo comando usando Docker Compose — ver **[README-docker.md](./README-docker.md)** para la guía completa, incluyendo un paso de configuración de red necesario para que las URLs firmadas de MinIO funcionen correctamente.

```bash
cp .env.example .env   # y define al menos JWT_SECRET
make up                 # o: docker compose up --build
```

## Generar ejecutables (sin Docker)

Si prefieres correr los artefactos compilados directamente en tu máquina, sin Docker ni los comandos de desarrollo (`spring-boot:run` / `npm run dev`):

**Backend** — genera un jar autocontenido (con servidor embebido):
```bash
cd Backend/storage-service
./mvnw clean package -DskipTests
java -jar target/storage-service-0.0.1-SNAPSHOT.jar
```

**Frontend** — genera los archivos estáticos de producción en `dist/` (necesitan un servidor web para servirse, no son un ejecutable por sí solos):
```bash
cd Frontend/storage-webapp
npm install
npm run build
npx serve dist -l 5173   # o servirlos con Nginx/Apache/etc.
```

En ambos casos, sin Docker de por medio, necesitas PostgreSQL y MinIO accesibles en los endpoints configurados en `application.properties` (por defecto, `localhost`).

## Documentación de la API

Todos los endpoints están bajo el prefijo `/file-store/api/v1`. Las respuestas siguen el formato:
```json
{ "success": true, "message": "...", "data": { } }
```

### Autenticación (`/auth`) — públicos

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/auth/signup` | Registra un usuario nuevo (`name`, `email`, `pass`) |
| `POST` | `/auth/login` | Autentica y devuelve un JWT en `data.token` |

El token va en cada petición siguiente como `Authorization: Bearer <token>`.

### Archivos (`/files`) — requieren autenticación, salvo donde se indique

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/files/upload-request` | Solicita una URL firmada de subida (`filename`, `contentType`, `size`) |
| `PUT` | `/files/{id}/confirm-upload` | Marca el archivo como `UPLOADED` tras subirlo a S3/MinIO |
| `GET` | `/files` | Lista los archivos del usuario autenticado |
| `GET` | `/files/{id}/download` | Genera una URL firmada de descarga (dueño, o con permiso `PROTECTED`/`PUBLIC`) |
| `DELETE` | `/files/{id}/delete` | Elimina el archivo (dueño únicamente) |
| `PUT` | `/files/{id}/update-permission` | Marca el archivo como `PUBLIC` y genera su `shareToken` (dueño únicamente) |
| `GET` | `/files/{id}/get-sharelink` | Devuelve el link público completo del archivo (dueño únicamente) |
| `GET` | `/files/share/{token}` | **Público, sin autenticación.** Redirige (302) a la URL firmada de descarga si el archivo es `PUBLIC` |
| `POST` | `/files/{id}/share-with` | Otorga acceso a un usuario específico por email (dueño únicamente). Pasa el archivo a `PROTECTED` |
| `DELETE` | `/files/{id}/share-with/{userId}` | Revoca el acceso de un usuario específico (dueño únicamente) |
| `GET` | `/files/{id}/shared-with` | Lista los usuarios con acceso `PROTECTED` a un archivo (dueño únicamente) |

## Visibilidad de archivos: los 3 niveles

| Nivel | ¿Quién puede descargarlo? | Cómo se accede |
|---|---|---|
| `PRIVATE` (por defecto) | Solo el dueño | `GET /files/{id}/download`, con su propio JWT |
| `PROTECTED` | El dueño + usuarios con acceso otorgado explícitamente | La persona inicia sesión en su cuenta y usa el link normal del archivo (`{frontendUrl}/files/{id}`); el backend valida el permiso vía `FilePermissionEntity` |
| `PUBLIC` | Cualquiera con el link, sin necesidad de cuenta | Link con token opaco (`{baseUrl}/file-store/api/v1/files/share/{token}`), no expone el `id` interno del archivo |

**Nota importante sobre `PROTECTED`:** no usa un link ni token especial — el "candado" no está en la URL, está en el backend verificando la sesión de quien la abre. Por eso, reenviar ese link a un tercero sin cuenta no le da acceso a nada.

## Frontend: rutas

| Ruta | Página | Protegida |
|---|---|---|
| `/login` | Login / registro. Respeta `?redirect=` para volver a donde iba el usuario | No |
| `/` | Dashboard: subir, listar, descargar, eliminar y compartir archivos propios | Sí |
| `/files/:fileId` | Vista de un archivo compartido con permiso `PROTECTED` | Sí |

## Modelo de datos

**`files`**

| Campo | Tipo | Notas |
|---|---|---|
| `id` | UUID | PK |
| `bucket` / `objectKey` | String | Ubicación real en S3/MinIO |
| `originalName` / `contentType` / `extension` / `size` | — | Metadatos del archivo |
| `status` | Enum | `PENDING_UPLOAD`, `UPLOADED`, `FAILED`, `DELETED` |
| `visibility` | Enum | `PRIVATE`, `PROTECTED`, `PUBLIC` |
| `shareToken` | String, nullable | Token opaco para el link público (solo si `visibility = PUBLIC`) |
| `uploadedBy` | FK → `users` | Dueño del archivo |

**`file_permissions`** — registra accesos otorgados explícitamente (caso `PROTECTED`)

| Campo | Tipo | Notas |
|---|---|---|
| `id` | UUID | PK |
| `file_id` | FK → `files` | |
| `user_id` | FK → `users` | Usuario con acceso otorgado |
| `grantedAt` | LocalDateTime | |

## Notas de seguridad

- Contraseñas con `BCryptPasswordEncoder`. Sesión stateless (JWT), sin `HttpSession`.
- El acceso a cada archivo se valida centralizadamente en `FilePermissionService.checkOwnership()` — cubre los 3 niveles de visibilidad en un solo lugar.
- `JwtFilter` captura cualquier error de token (vencido, mal formado, firma inválida) sin tumbar el filtro, devolviendo un `403` controlado en vez de un error sin manejar.

## Pendientes de limpieza

No afectan la funcionalidad, pero conviene resolverlos antes de considerar esto "listo para producción":

- [ ] **`DebugController`** (`/file-store/api/v1/debug/auth-info`) queda público (`permitAll`) y sin uso real fuera de desarrollo — quitar antes de desplegar.
- [ ] Quedan `System.out.println` de depuración en `JwtServices` (2) y `S3StorageProvider` (1) — reemplazar por el logger (`slf4j`) que ya se usa en `JwtFilter`, o quitarlos.
- [ ] No hay tests automatizados todavía, ni para el backend ni para el frontend.