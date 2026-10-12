# FrontendTmp

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.1.5.

## SCRUM-39: PWA de EDS Bellavista

La PWA usa el service worker oficial de Angular, un manifest con `display: standalone`
y los iconos del surtidor ya aprobado en el login y el dashboard. Solo se almacenan
el HTML estático, bundles CSS/JS, manifest e iconos. No hay grupos de caché de datos,
patrones de URL externos ni sincronización offline. Las rutas `/api` se excluyen de
la navegación del worker. Iniciar sesión y las operaciones del negocio requieren
conexión al backend; instalar la app no proporciona datos de negocio sin conexión.

Desde `frontend`, en PowerShell:

```powershell
# Si node_modules no existe o no coincide con el lockfile:
npm ci
npm run build -- --configuration production
npm run test:pwa
npm run preview:pwa
```

El preview sirve `dist/frontend-tmp/browser` en `http://localhost:4300`, con tipos
MIME adecuados, sin caché HTTP y con fallback de rutas Angular. No sirve ni simula
la API. Para probar login con el origen local habitual, detén primero `npm start`
y usa `npm run preview:pwa -- 4200`; las autorizaciones CORS del backend se mantienen.

En Chrome o Edge compatibles, abre el preview con un perfil de prueba:

1. DevTools → Application → Manifest: comprobar nombre, iconos y ausencia de errores.
2. Application → Service Workers: esperar hasta 30 segundos y comprobar `activated`
   y el scope del frontend. Recargar si la pestaña inicial todavía no está controlada.
3. En consola: `navigator.serviceWorker.controller?.scriptURL` debe terminar en
   `ngsw-worker.js`. Network debe cargar manifest, iconos y `ngsw.json` correctamente.
4. Usar «Instalar EDS Bellavista» en la barra o menú del navegador. Abrir la app
   instalada y comprobar `matchMedia('(display-mode: standalone)').matches === true`.
5. Comprobar que no hay errores de consola y que Cache Storage solo contiene
   archivos estáticos. No esperar respuestas de API disponibles sin conexión.

`npm start` / `ng serve` usa desarrollo, sin generar ni registrar el worker. La
comprobación principal es el build de producción servido localmente. Si un worker
anterior controla ese mismo origen, elimínalo en DevTools o usa un perfil limpio.
Fuera de localhost se requiere HTTPS. La disponibilidad del botón de instalación
depende del navegador y del sistema operativo; el manifest por sí solo no acredita
que se haya instalado o abierto en standalone.

La publicación actual usa `<base href="/">`. `id`, `start_url`, `scope` e iconos son
relativos al manifest, por lo que corresponden al directorio publicado. Si se publica
en un subdirectorio, se debe recompilar con `--base-href /subdirectorio/` y servir
todos los artefactos bajo ese directorio, con fallback SPA limitado al frontend.

Los PNG (192, 512 y Apple 180) incluyen variantes maskable con fondo opaco y margen
seguro. Para regenerarlos en Windows sin instalar librerías:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/generate-pwa-icons.ps1
```

Este comando solo establece la política para ese proceso. El SVG fuente conserva
los paths del símbolo de marca y los colores provienen de `src/assets/css/tokens.css`.
Si se actualiza Angular, mantener `@angular/service-worker` en la misma versión
exacta que `@angular/core` en el lockfile.

Referencia: [integración oficial del service worker de Angular](https://angular.dev/ecosystem/service-workers/getting-started).

Validación de SCRUM-39 (2026-10-11): build de producción, cinco tests `test:pwa` y
lint acotado aprobados. Chrome headless con perfil aislado en localhost:4300 cargó
manifest e iconos con HTTP 200, activó el worker y controló `/login`; reportó cero
errores del manifest o de instalabilidad y emitió `beforeinstallprompt`. No hubo
excepciones JS ni mensajes `console.error` capturados. Una consulta de prueba a la
API del servidor estático devolvió 404 y no se almacenó en Cache Storage. No se
instaló la app en el sistema: apertura real en standalone pendiente de prueba manual.
La suite Angular existente pasa creación de App pero falla en `app.spec.ts`, que
todavía espera el antiguo `Hello, frontend-tmp` en una plantilla sin ese título.
Ese test y la plantilla no se modificaron en SCRUM-39.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
