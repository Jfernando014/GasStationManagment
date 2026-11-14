# Respaldos y restauración de la base de datos

## Alcance

El módulo `backup` del backend genera y restaura copias de la base PostgreSQL de la estación. La exportación la puede descargar un administrador desde la interfaz y la importación reemplaza la información actual por la del archivo.

El respaldo usa el formato `custom` de PostgreSQL (`.dump`) con compresión interna. No usa el socket de Docker: el backend ejecuta `pg_dump` y `pg_restore` como procesos hijos contra el host de la base (`db` en Docker Compose).

## Requisitos

Las imágenes `backend/Dockerfile` y `backend/Dockerfile.dev` instalan `postgresql16-client`. El directorio de trabajo es `/app/backups` en producción y se crea automáticamente en desarrollo.

## Endpoints

Todos exigen un token con autoridad `ADMIN`.

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/v1/backups/export` | Descarga un archivo `gas-station-YYYYMMDD-HHmmss.dump`. |
| `POST` | `/api/v1/backups/import` | Recibe `multipart/form-data` en el campo `file`. Exige la cabecera `X-Confirm-Restore: RESTAURAR`. |
| `GET` | `/api/v1/backups/audit` | Devuelve las últimas operaciones registradas. |

Ejemplo de restauración:

```bash
curl -X POST http://localhost:8080/api/v1/backups/import \
  -H "Authorization: Bearer <token>" \
  -H "X-Confirm-Restore: RESTAURAR" \
  -F "file=@gas-station-20261113-153000.dump"
```

## Flujo de exportación

1. El servicio toma un lock para impedir otra operación simultánea.
2. Ejecuta `pg_dump` con `--format=custom --compress=9`, sin dueños ni privilegios.
3. Excluye la tabla `backup_audit` y su secuencia `backup_audit_id_seq` para que el historial no se pierda al restaurar.
4. Valida la cabecera `PGDMP` y registra la operación en `backup_audit`.
5. El controller transmite el archivo y lo elimina del servidor al terminar.

## Flujo de restauración

1. Verifica que la restauración esté habilitada, la frase de confirmación y el tamaño máximo.
2. Toma el lock y activa el modo mantenimiento: las peticiones normales reciben `503` con código `BACKUP_RESTORE_IN_PROGRESS`.
3. Guarda un respaldo de seguridad automático antes de tocar la base.
4. Termina las sesiones activas y vacía las conexiones de Hikari.
5. Genera un TOC filtrado con `pg_restore --list` y ejecuta `pg_restore` con `--clean --if-exists --no-owner --no-privileges --single-transaction --exit-on-error --use-list`, omitiendo las entradas de `backup_audit` y `backup_audit_id_seq`. Esto conserva el historial local y permite importar dumps antiguos que todavía incluyen la secuencia.
6. Ejecuta Flyway para aplicar las migraciones que falten. Si falla, restaura el respaldo de seguridad y reporta `MIGRATION_FAILED`.
7. Registra el resultado, desactiva el mantenimiento y reinicia:
   - Desarrollo: DevTools Restarter.
   - Producción: la aplicación termina y `restart: always` la inicia de nuevo.
   - CI: `BACKUP_RESTART_AFTER_RESTORE=false`.

La restauración solo reemplaza las tablas presentes en el archivo. Las tablas que no estén en el dump se conservan. El historial de `backup_audit` siempre se conserva porque la tabla y su secuencia se excluyen del dump y del TOC de restauración.

## Variables de entorno

| Variable | Valor por defecto | Uso |
| :--- | :--- | :--- |
| `BACKUP_DIR` | `/app/backups` | Directorio temporal de los dumps. |
| `BACKUP_MAX_UPLOAD_BYTES` | `536870912` (512 MB) | Tamaño máximo del archivo de restauración. |
| `BACKUP_TIMEOUT_SECONDS` | `600` | Tiempo máximo de `pg_dump` o `pg_restore`. |
| `BACKUP_RESTORE_ENABLED` | `true` | Permite el endpoint de restauración. |
| `BACKUP_RESTART_AFTER_RESTORE` | `true` | Reinicia la aplicación después de restaurar. |
| `PG_DUMP_COMMAND` | `pg_dump` | Ruta o nombre del ejecutable. |
| `PG_RESTORE_COMMAND` | `pg_restore` | Ruta o nombre del ejecutable. |
| `BACKUP_CONFIRMATION_PHRASE` | `RESTAURAR` | Texto que debe enviar el cliente. |
| `BACKUP_AUDIT_LIMIT` | `20` | Filas devueltas por el historial. |

Los límites de subida de Spring (`spring.servlet.multipart.max-file-size` y `max-request-size`) también se pueden cambiar con `BACKUP_MAX_FILE_SIZE` y `BACKUP_MAX_REQUEST_SIZE`.

## Interfaz

La pantalla está en **Configuración › Respaldos** (`/configuracion/backup`). Permite descargar el dump, subir un archivo, escribir `RESTAURAR` y consultar el historial. Al terminar una restauración, la sesión se cierra para que el usuario vuelva a autenticarse.

## Pruebas

```bash
cd backend
mvn -B -ntp -Dtest=BackupServiceTest,PostgresBackupExecutorTest test

cd ../frontend
npx ng test --watch=false --include=src/core/services/backup.service.spec.ts
```

## Diagnóstico

- **`pg_dump: not found`**: la imagen no tiene `postgresql16-client`; reconstruye el contenedor.
- **`pg_restore failed: cannot drop sequence backup_audit_id_seq because other objects depend on it`**: el dump se generó antes del fix que excluye la secuencia. La restauración actual filtra esa entrada del TOC; si el error persiste, vuelve a exportar el respaldo con la versión corregida.
- **`409 BUSY`**: hay otra exportación o restauración en curso.
- **`403 RESTORE_DISABLED`**: `BACKUP_RESTORE_ENABLED=false`.
- **`400 CONFIRMATION_REQUIRED`**: falta la cabecera o el texto no es exactamente `RESTAURAR`.
- **`413 FILE_TOO_LARGE`**: el archivo supera el límite configurado.
- **Permisos en `/app/backups`**: el usuario del contenedor debe poder escribir en el directorio.
- **El contenedor de desarrollo no reinicia**: revisa que DevTools esté activo y que `spring.devtools.restart.enabled` no sea `false`.
