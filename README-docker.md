# Cómo levantar el proyecto con Docker

## 1. Configurar variables de entorno

```bash
cp .env.example .env
# edita .env y como mínimo cambia JWT_SECRET
```

## 2. El paso obligatorio: alias de host para MinIO

Las URLs firmadas (presigned) las abre **el navegador**, no el backend. Por eso
`storage.endpoint` tiene que resolver al mismo host tanto desde dentro del
contenedor del backend (que lo resuelve solo vía DNS de Docker, como `minio`)
como desde tu navegador (que no sabe qué es `minio`).

Agrega esta línea a tu archivo de hosts:

- Linux/macOS: `/etc/hosts`
- Windows: `C:\Windows\System32\drivers\etc\hosts` (como administrador)

```
127.0.0.1 minio
```

Con eso, el navegador resuelve `minio` a `127.0.0.1`, y como el puerto 9000
de MinIO está publicado (`9000:9000`), la petición llega al contenedor
correcto. La firma SigV4 de la URL queda consistente en ambos lados porque
ambos usan el mismo host (`minio:9000`).

> Si no quieres tocar tu `/etc/hosts`, la alternativa es usar un bucket S3
> real de AWS en vez de MinIO — ahí el endpoint ya es público y este problema
> no existe (ver sección "Producción" más abajo).

### Forma automática

Hay un script que revisa si la entrada ya existe y solo la agrega si falta
(te va a pedir tu contraseña / permisos de administrador la primera vez):

- Linux/macOS: `bash scripts/setup-hosts.sh`
- Windows (PowerShell como Administrador): `powershell -ExecutionPolicy Bypass -File scripts\setup-hosts.ps1`

O directamente, si tienes `make` instalado, `make up` corre el chequeo del
hosts y levanta `docker compose` en un solo paso (ver Makefile en la raíz).

## 3. Levantar todo

**Variabres de entorno .env**

--- Postgres ---

POSTGRES_DB=file_store_db
POSTGRES_USER=admin
POSTGRES_PASSWORD=admin123

--- JWT --- Genera uno nuevo, por ejemplo: openssl rand -hex 32

JWT_SECRET=EJEMPLO_DEL_KEY_LARGA
JWT_EXPIRATION_MINUTES=60

--- URLs públicas ---

APP_BASE_URL=http://localhost:8080
APP_FRONTEND_URL=http://localhost:5173

--- MinIO / S3 --- minio" debe existir en tu /etc/hosts apuntando a 127.0.0.1

STORAGE_ENDPOINT=http://minio:9000
STORAGE_REGION=us-east-1
STORAGE_BUCKET=files
STORAGE_ACCESS_KEY=admin
STORAGE_SECRET_KEY=admin123456

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Consola de MinIO: http://localhost:9001 (usuario/clave = `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY`)
- Postgres: localhost:5432

El bucket se crea solo al arrancar el backend (`BucketInitializer`), no hace
falta ningún paso manual en MinIO.

## 4. Apagar

```bash
docker compose down          # detiene los contenedores
docker compose down -v       # además borra los volúmenes (datos de Postgres y MinIO)
```

## Notas para producción

- Si usas **AWS S3 real** en vez de MinIO: quita el contenedor `minio` del
  compose, y en `STORAGE_ENDPOINT` deja el valor vacío (el `S3Config` del
  backend usa el endpoint por defecto de AWS cuando no hay `customEndpoint`).
  Como el endpoint de S3 es público, el problema de `/etc/hosts` desaparece.
- Cambia `jwt.secret` por uno generado para ese entorno — nunca reuses el que
  viene de ejemplo en `application.properties` ni el de `.env.example`.
- Elimina o protege el `DebugController` (`/file-store/api/v1/debug/auth-info`)
  antes de desplegar — expone el header `Authorization` completo.
- Considera poner un proxy TLS (Caddy, Traefik, o un load balancer) delante
  del `frontend` (nginx) en vez de exponer el puerto 5173 directo.
