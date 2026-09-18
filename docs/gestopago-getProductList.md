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

## Decisión clave: cachear el catálogo en base de datos
La documentación oficial de GestoPago advierte explícitamente:

> *"This method can only be used up to 3 times in a single day, otherwise the IP will be
> blocked"* — y — *"should never be used directly as a source of information for your
> frontend"*.

Por eso el diseño NO llama a GestoPago en cada request del frontend. En su lugar:
- Un **job programado** (`@Scheduled`, una vez al día — configurable por
  `gestopago.productos.sync-cron`) sincroniza el catálogo completo hacia la tabla local
  `gestopago_productos`.
- El endpoint público `GET /gestopago/productos` **siempre lee de la base de datos**,
  nunca invoca a GestoPago en vivo.
- Se agregó `POST /gestopago/productos/sincronizar` para disparar la sincronización
  manualmente durante pruebas/demo, sin esperar al cron.

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
nunca quedan hardcodeadas en el código ni en el repositorio.

## Arquitectura implementada
| Capa | Clase |
|---|---|
| Controller | `GestoPagoProductController` — `GET /gestopago/productos`, `POST /gestopago/productos/sincronizar` |
| Service | `GestoPagoProductService` / `GestoPagoProductServiceImpl` |
| Client/Integration | `GestoPagoProductClient` (Feign, XML) |
| DTOs | `GestoPagoProductoXml`, `GestoPagoMensajeXml`, `GestoPagoProductListXmlResponse`, `GestoPagoProductoResponse` |
| Entidad/Repositorio | `GestoPagoProducto`, `GestoPagoProductoRepository` |
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
sincronizados. Los errores se registran con `log.error(...)`, sin exponer password ni
token completo en ningún log.

## Calidad de código
- Inyección de dependencias por constructor en todas las clases nuevas.
- `GestoPagoFeignConfig` es compartida entre `GestoPagoAuthClient` y
  `GestoPagoProductClient` (sin duplicar timeouts ni manejo de errores).
- Excepciones de negocio separadas de las técnicas (Feign), para no acoplar la capa de
  servicio al detalle de transporte HTTP.

## Pruebas unitarias
`GestoPagoProductServiceImplTest` cubre: sincronización exitosa, código de negocio no
exitoso, token expirado con reintento automático, credenciales inválidas, error
inesperado no controlado, y lectura del catálogo desde la base de datos (sin llamar a
GestoPago).

## Cómo probar
1. Configurar variables de entorno: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
   `GESTOPAGO_ID_DISTRIBUIDOR`, `GESTOPAGO_CODIGO_DISPOSITIVO`, `GESTOPAGO_PASSWORD`.
2. Ejecutar `./gradlew test --tests "*.GestoPagoProductServiceImplTest"`.
3. Ejecutar `./gradlew bootRun`.
4. `POST http://localhost:8081/gestopago/productos/sincronizar` para forzar la
   sincronización.
5. `GET http://localhost:8081/gestopago/productos` para ver el catálogo guardado.