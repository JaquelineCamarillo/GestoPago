# Proyecto Servicios — GestoPago y Clientes

API REST en **Spring Boot 3.3 / Java 17** para la materia *Aplicaciones Web Progresivas*
(UTNG). Incluye dos módulos:

| Módulo | Descripción | Documentación |
|---|---|---|
| **GestoPago / PuntoRed** | Consume el catálogo de productos (XML), con token Bearer, caché en Redis y respaldo en Postgres | [docs/gestopago-getProductList.md](docs/gestopago-getProductList.md) |
| **Clientes** | Onboarding de personas físicas: cliente + domicilio + cuenta automática, validaciones de CURP/RFC, catálogos, búsqueda y login (contraseña o rostro) | [docs/integracion-clientes.md](docs/integracion-clientes.md) |

## Tecnologías
Java 17 · Spring Boot 3.3 · Spring Data JPA · Flyway · PostgreSQL · Redis · OpenFeign ·
MapStruct · Jackson (JSON y XML) · Bean Validation · springdoc (Swagger UI) · JUnit 5 y Mockito.

## Requisitos
- JDK 17 o superior
- PostgreSQL y Redis accesibles (en la nube o locales)
- No hace falta instalar Gradle: se usa el wrapper incluido (`gradlew`)

## Variables de entorno
Las credenciales **nunca** van en el código ni en el repositorio: se inyectan por variables
de entorno.

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC de PostgreSQL |
| `DB_USERNAME` / `DB_PASSWORD` | Usuario y contraseña de la base de datos |
| `GESTOPAGO_ID_DISTRIBUIDOR` | Id de distribuidor de GestoPago |
| `GESTOPAGO_CODIGO_DISPOSITIVO` | Código de dispositivo de GestoPago |
| `GESTOPAGO_PASSWORD` | Contraseña de GestoPago |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_SSL` | Conexión a Redis (`REDIS_SSL=true` para TLS) |
| `APP_ENCRYPTION_PASSWORD` | Clave del cifrado AES (contraseñas, tokens y datos faciales) |
| `APP_ENCRYPTION_SALT` | Salt en hexadecimal. **No cambiarlo una vez que haya datos cifrados** |
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
Cubren los validadores de CURP y RFC, la lógica de clientes y el catálogo de productos de GestoPago.

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
├── client/        Clientes Feign (GestoPago, REST Countries)
├── config/        Configuración (OpenAPI, cifrado, Feign)
├── controller/    Endpoints REST
├── entity/        Entidades JPA
├── enums/         Enumeraciones (Genero)
├── exception/     Excepciones y GlobalExceptionHandler
├── mapper/        MapStruct
├── model/         DTOs de petición y respuesta
├── repositorys/   Repositorios JPA
├── service/       Lógica de negocio
├── util/          CurpValidador y RfcValidador
└── validation/    Anotaciones @Curp, @Rfc y @CodigoPais
```