# Integración GestoPago — GET `/sistema/service/getProductList.do`

## Objetivo
Habilitar el consumo del catálogo de productos/servicios de GestoPago, reutilizando la
infraestructura de autenticación ya existente (`GestoPagoTokenService`) y siguiendo la
misma arquitectura por capas del proyecto (Controller → Service → Client/Integration →
DTOs → Configuración).

## Errores corregidos en el proyecto base
- `application.properties` estaba **vacío**: la aplicación no arrancaba, porque ya se
  referenciaban propiedades (`spring.datasource.*`, `gestopago.auth.*`) con `@Value`/
  `${...}` en el código sin que existieran en ningún lado.
- `OpenApi.java` no tenía `@Configuration`, por lo que Spring nunca registraba el bean
  `OpenAPI`.

## Decisión clave: cachear el catálogo (Redis, con Postgres de respaldo)
La documentación oficial de GestoPago advierte explícitamente:

> *"This method can only be used up to 3 times in a single day, otherwise the IP will be
> blocked"* — y — *"should never be used directly as a source of information for your
> frontend"*.

Por eso el diseño NO llama a GestoPago en cada request del frontend. En su lugar:
- Un **job programado** (`@Scheduled`, una vez al día — configurable por
  `gestopago.productos.sync-cron`) sincroniza el catálogo completo hacia **Redis**, que es
  el caché principal: se guarda como un solo objeto (`GestoPagoCatalogoCacheDto`) bajo la
  llave `gestopago:productos`.
- Si Redis no está disponible al sincronizar, el catálogo se guarda en la tabla local
  `gestopago_productos` (respaldo en Postgres).
- El endpoint público `GET /gestopago/productos` **lee de Redis** y, si Redis no responde,
  **de la base de datos**. Nunca invoca a GestoPago en vivo.
- Se agregó `POST /gestopago/productos/sincronizar` para disparar la sincronización
  manualmente durante pruebas/demo, sin esperar al cron.

## Respuesta del catálogo
- **Orden:** los productos se devuelven por **precio ascendente**. El precio llega como
  texto (`"100.0"`), así que se convierte a número para ordenar; los precios vacíos o no
  numéricos van al final y, a igual precio, se ordena por nombre.
- **Filtro:** `GET /gestopago/productos?tipoFront=2` devuelve solo los productos de ese
  tipo; sin el parámetro devuelve todo el catálogo.
- Muchos productos del catálogo real tienen precio `0.0` (montos variables), por eso
  aparecen primero.

## Formato de respuesta: XML, no JSON
A diferencia de otros endpoints de la API que devuelven JSON, `getProductList.do`
responde **XML** (confirmado con la documentación oficial de PuntoRed/GestoPago). Se
agregó la dependencia `jackson-dataformat-xml` y los DTOs (`GestoPagoProductListXmlResponse`,
`GestoPagoMensajeXml`, `GestoPagoProductoXml`) están anotados con `@JacksonXmlProperty`
respetando que los datos del producto vienen como **atributos** del XML (no como
elementos hijos), excepto el nodo `legend`.

Además, GestoPago reporta éxito/error del negocio dentro del propio XML (`<MENSAJE><CODIGO>01</CODIGO>...`)
incluso con HTTP 200 — por eso la sincronización valida ese código y no solo el status HTTP.

## Configuración de propiedades
Se mantiene la convención `gestopago.auth.*` ya usada por el proyecto y se agrega
`gestopago.api.url` para el endpoint de negocio. Las credenciales (`id-distribuidor`,
`codigo-dispositivo`, `password`) se resuelven **siempre** desde variables de entorno —
nunca quedan hardcodeadas en el código ni en el repositorio. La conexión a Redis también
se configura por variables (`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL`).

## Arquitectura implementada
| Capa | Clase |
|---|---|
| Controller | `GestoPagoProductController` — `GET /gestopago/productos`, `POST /gestopago/productos/sincronizar` |
| Service | `GestoPagoProductService` / `GestoPagoProductServiceImpl` |
| Client/Integration | `GestoPagoProductClient` (Feign, XML) |
| DTOs | `GestoPagoProductoXml`, `GestoPagoMensajeXml`, `GestoPagoProductListXmlResponse`, `GestoPagoProductoResponse`, `GestoPagoCatalogoCacheDto` |
| Entidad/Repositorio | `GestoPagoProducto`, `GestoPagoProductoRepository` (respaldo en Postgres) |
| Caché | Redis (`RedisTemplate`), llave `gestopago:productos` |
| Mapper | `GestoPagoProductMapper` (MapStruct, XML → Entidad) |
| Configuración | `GestoPagoFeignConfig` (timeouts + `ErrorDecoder` compartido) |

El token se obtiene siempre desde `GestoPagoTokenService.obtenerTokenBearer()` (cache en
BD) y se inyecta como cabecera `Authorization: Bearer <token>`. Solo se renueva cuando no
hay uno vigente o cuando GestoPago reporta que expiró — nunca se pide un token nuevo por
cada llamada, cumpliendo la regla explícita de GestoPago.

## Manejo de errores — nunca un 400 genérico
`GestoPagoFeignErrorDecoder` interpreta la respuesta real de GestoPago y la traduce a una
excepción específica; `GlobalExceptionHandler` la convierte en un HTTP y mensaje
concretos:

| Causa real | Excepción | HTTP devuelto |
|---|---|---|
| `idDistribuidor` inválido | `GestoPagoAuthenticationException(USUARIO_O_DISTRIBUIDOR_INVALIDO)` | 401 |
| Password incorrecto | `GestoPagoAuthenticationException(PASSWORD_INVALIDO)` | 401 |
| `codigoDispositivo` no registrado | `GestoPagoAuthenticationException(DISPOSITIVO_INVALIDO)` | 401 |
| Token expirado (24h) | `GestoPagoAuthenticationException(TOKEN_EXPIRADO)` — se renueva y reintenta 1 vez automáticamente | 401 (solo si el reintento también falla) |
| Sin respuesta a tiempo | `GestoPagoTimeoutException` | 504 |
| GestoPago caído / 5xx | `GestoPagoCommunicationException` | 502 |
| CODIGO de negocio ≠ "01" | `GestoPagoBadResponseException` | 502 |
| No hay token utilizable | `GestoPagoTokenUnavailableException` | 503 |

El decoder infiere el motivo exacto (usuario/contraseña/dispositivo) leyendo el campo
`message` que regresa GestoPago; si no puede clasificarlo, usa `DESCONOCIDO` en vez de
inventar una causa.

## Registro y monitoreo
Se registra INICIO y FIN de cada sincronización, incluyendo el número de productos
sincronizados, y de dónde se leyó el catálogo (Redis o base de datos). Los errores se
registran con `log.error(...)`, sin exponer password ni token completo en ningún log.

## Calidad de código
- Inyección de dependencias por constructor en todas las clases nuevas.
- `GestoPagoFeignConfig` es compartida entre `GestoPagoAuthClient` y
  `GestoPagoProductClient` (sin duplicar timeouts ni manejo de errores).
- Excepciones de negocio separadas de las técnicas (Feign), para no acoplar la capa de
  servicio al detalle de transporte HTTP.
- La lectura del catálogo no falla si Redis cae: se degrada a la base de datos.

## Pruebas unitarias
`GestoPagoProductServiceImplTest` cubre: sincronización exitosa, código de negocio no
exitoso, token expirado con reintento automático, credenciales inválidas, error
inesperado no controlado, lectura del catálogo desde Redis, lectura desde la base de
datos cuando Redis no está disponible (sin llamar a GestoPago) y filtro por `tipoFront`.

## Cómo probar
1. Configurar variables de entorno: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
   `GESTOPAGO_ID_DISTRIBUIDOR`, `GESTOPAGO_CODIGO_DISPOSITIVO`, `GESTOPAGO_PASSWORD`,
   y las de Redis (`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL`).
2. Ejecutar `./gradlew test --tests "*.GestoPagoProductServiceImplTest"`.
3. Ejecutar `./gradlew bootRun`.
4. `POST http://localhost:8081/gestopago/productos/sincronizar` para forzar la
   sincronización.
5. `GET http://localhost:8081/gestopago/productos` para ver el catálogo (ordenado por
   precio ascendente) y `GET .../gestopago/productos?tipoFront=2` para filtrarlo.