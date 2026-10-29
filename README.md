# Proyecto Servicios — GestoPago, Pagos y Clientes

API REST en **Spring Boot 3.3 / Java 17** para la materia *Aplicaciones Web Progresivas*
(UTNG). Incluye tres módulos principales:

| Módulo | Descripción | Documentación |
|---|---|---|
| **GestoPago — Catálogo** | Consume el catálogo de productos (`getProductList.do` en XML), con token Bearer, caché en Redis y respaldo en Postgres | [docs/gestopago-getProductList.md](docs/gestopago-getProductList.md) |
| **GestoPago — Pagos (`sendTx.do`)** | Pagos de servicios y recargas con doble verificación (token de sesión + código de 6 dígitos), cifrado `AES-256-CBC`, bloqueo pesimista y estrategia **retener → cobrar → conciliar** (`confirmTx.do`) | [docs/pagos.md](docs/pagos.md) |
| **Clientes y Cuentas** | Onboarding de personas físicas: cliente + domicilio + cuenta automática de 16 dígitos (`@NumeroCuenta`), validaciones de CURP/RFC, catálogos, búsqueda y login (contraseña o rostro) | [docs/integracion-clientes.md](docs/integracion-clientes.md) |

## Tecnologías
Java 17 · Spring Boot 3.3 · Spring Data JPA · Flyway (V1–V4) · PostgreSQL · Redis · OpenFeign ·
MapStruct · Jackson (JSON y XML) · Bean Validation · springdoc (Swagger UI) · JUnit 5, Mockito y H2.

## Requisitos
- JDK 17 o superior
- PostgreSQL y Redis accesibles (en la nube o locales)
- No hace falta instalar Gradle: se usa el wrapper incluido (`gradlew`)

## Variables de entorno
Las credenciales y llaves de cifrado **nunca** van en el código ni en el repositorio: se inyectan exclusivamente por variables de entorno.

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC de PostgreSQL |
| `DB_USERNAME` / `DB_PASSWORD` | Usuario y contraseña de la base de datos |
| `GESTOPAGO_ID_DISTRIBUIDOR` | Id de distribuidor de GestoPago |
| `GESTOPAGO_CODIGO_DISPOSITIVO` | Código de dispositivo de GestoPago |
| `GESTOPAGO_PASSWORD` | Contraseña de GestoPago |
| `GESTOPAGO_PAGOS_PROVEEDOR` | Proveedor de pagos: `simulado` (por defecto) o `real` |
| `GESTOPAGO_SECRET_KEY` | Llave AES-256 en Base64 para cifrar el parámetro `signed` de `sendTx.do` y `confirmTx.do` |
| `GESTOPAGO_IV` | Vector de inicialización (IV) en Base64 para el cifrado `AES-256-CBC` de GestoPago |
| `GESTOPAGO_UNIDAD` | Identificador de unidad comercial o sucursal enviado en `sendTx.do` (`083` por defecto) |
| `GESTOPAGO_CONCILIACION_CRON` | Expresión cron para conciliar pagos `EN_REVISION` (`0 */2 * * * *` por defecto) |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_SSL` | Conexión a Redis (`REDIS_SSL=true` para TLS) |
| `APP_ENCRYPTION_PASSWORD` | Clave obligatoria del cifrado AES local (`TextEncryptor`: contraseñas, tokens, datos faciales y códigos 2FA) |
| `APP_ENCRYPTION_SALT` | Salt en hexadecimal obligatorio para `TextEncryptor`. **No cambiarlo una vez que haya datos cifrados** |
| `RESTCOUNTRIES_API_KEY` | Key de la API REST Countries v5 (opcional: sin ella se usa la lista ISO de respaldo) |
| `RESTCOUNTRIES_CACHE_HOURS` | Vigencia del catálogo de países en memoria (24 por defecto) |
| `PORT` | Puerto HTTP (8081 por defecto) |

## Ejecutar en local
```bash
# Linux / macOS
./gradlew bootRun

# Windows
.\gradlew.bat bootRun
```
Swagger UI: <http://localhost:8081/swagger-ui/index.html>

## Pruebas
```bash
./gradlew test
```
Cubren los validadores de CURP, RFC y `@NumeroCuenta` (16 dígitos), el cifrado `AES-256-CBC` de GestoPago, la lógica de clientes, el catálogo de productos, todos los escenarios unitarios del servicio de pagos y las pruebas de integración de concurrencia y conciliación con H2.

## Convenciones de la API
- Todos los datos viajan en el cuerpo JSON (`@RequestBody` + `@Valid`); no se usan `@PathVariable` ni `@RequestParam` en controladores.
- Números de cuenta de **exactamente 16 dígitos numéricos** (`@NumeroCuenta`), generados con `SecureRandom`.
- Una sola búsqueda de clientes con criterios combinables: `POST /clientes/buscar`.
- Flujo de pagos en dos pasos con retención previa en BD y llamada a GestoPago fuera de transacción: `POST /pagos/iniciar` → `POST /pagos/confirmar` → `POST /pagos/consultar` / `POST /pagos/conciliar`.
- Respuesta uniforme en éxito y error: `{ "codigo", "mensaje", "datos" }` (`codigo` 0 = éxito).
- Fechas en formato `yyyy/MM/dd`.
- Errores centralizados en `GlobalExceptionHandler` (`@RestControllerAdvice`).

Detalle de endpoints y reglas en [docs/integracion-clientes.md](docs/integracion-clientes.md), [docs/gestopago-getProductList.md](docs/gestopago-getProductList.md) y [docs/pagos.md](docs/pagos.md).

## Despliegue
El `Dockerfile` construye la aplicación en dos etapas (JDK para compilar, JRE para ejecutar).
Se despliega en **Render** (servicio web con Docker) desde la rama `develop`; la base de
datos (Neon) y Redis (Upstash) son servicios externos. En Render se definen las mismas
variables de entorno de la tabla anterior.

> En el plan gratuito de Render el servicio se duerme tras unos 15 minutos sin tráfico; la
> primera petición después de eso tarda en responder y las tareas programadas no corren
> mientras duerme.

## Flujo de trabajo con Git
- `main`: versión estable para entrega.
- `develop`: integración; de aquí se despliega.
- `feature/*`: una rama por funcionalidad, que se integra a `develop` con Pull Request.

## Estructura
```
src/main/java/com/proyecto/servicios
├── client/        Clientes Feign y ProveedorPagoClient (GestoPago real/simulado, REST Countries)
├── config/        Configuración (OpenAPI, cifrado, Feign, Redis, Flyway)
├── controller/    Endpoints REST (Clientes, Cuentas, Login, Catálogos, GestoPago, Pagos)
├── entity/        Entidades JPA (Cliente, Domicilio, Cuenta, Login, Pago, PagoBitacora, GestoPago*)
├── enums/         Enumeraciones (Genero, TipoPago, EstadoPago, GestoPagoCodigoResultado)
├── exception/     Excepciones de negocio y GlobalExceptionHandler
├── mapper/        MapStruct
├── model/         DTOs de petición y respuesta
├── repositorys/   Repositorios JPA (con bloqueo pesimista en Cuenta y Pago)
├── service/       Lógica de negocio y transacciones cortas (PagoTransaccionHelper)
├── util/          CurpValidador, RfcValidador, GeneradorNumeroCuenta y GestoPagoAesCipher
└── validation/    Anotaciones @Curp, @Rfc, @CodigoPais y @NumeroCuenta
```
