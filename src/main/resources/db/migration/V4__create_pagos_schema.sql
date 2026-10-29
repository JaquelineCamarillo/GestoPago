-- Columna para registrar el monto total retenido en operaciones en curso
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS saldo_retenido NUMERIC(12, 2) NOT NULL DEFAULT 0.00;

-- Tabla principal de pagos y recargas hacia GestoPago
CREATE TABLE IF NOT EXISTS pagos (
    id                      SERIAL PRIMARY KEY,
    cliente_id              INTEGER         NOT NULL REFERENCES clientes(id),
    cuenta_id               INTEGER         NOT NULL REFERENCES cuentas(id),
    numero_cuenta           VARCHAR(16)     NOT NULL,
    tipo_pago               VARCHAR(20)     NOT NULL,
    id_servicio             INTEGER         NOT NULL,
    id_producto             INTEGER         NOT NULL,
    nombre_servicio         VARCHAR(256),
    nombre_producto         VARCHAR(256),
    monto                   NUMERIC(12, 2)  NOT NULL,
    telefono                VARCHAR(20),
    referencia              VARCHAR(100),
    trace                   VARCHAR(15)     NOT NULL,
    hora_local              VARCHAR(15)     NOT NULL,
    unidad                  VARCHAR(25)     NOT NULL,
    estado                  VARCHAR(30)     NOT NULL,
    codigo_cifrado          TEXT            NOT NULL,
    codigo_expiracion       TIMESTAMP       NOT NULL,
    intentos_codigo         INTEGER         NOT NULL DEFAULT 0,
    monto_retenido          BOOLEAN         NOT NULL DEFAULT FALSE,
    id_tx_proveedor         VARCHAR(60),
    num_autorizacion        VARCHAR(60),
    codigo_proveedor        VARCHAR(20),
    mensaje_proveedor       VARCHAR(500),
    pin_proveedor           VARCHAR(250),
    leyenda_proveedor       TEXT,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_confirmacion      TIMESTAMP,
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pagos_trace UNIQUE (trace)
);

CREATE INDEX IF NOT EXISTS idx_pagos_cliente ON pagos(cliente_id);
CREATE INDEX IF NOT EXISTS idx_pagos_cuenta ON pagos(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_pagos_estado ON pagos(estado);

-- Bitacora de transiciones de estado del pago (sin datos sensibles)
CREATE TABLE IF NOT EXISTS pagos_bitacora (
    id                      SERIAL PRIMARY KEY,
    pago_id                 INTEGER         NOT NULL REFERENCES pagos(id),
    estado_anterior         VARCHAR(30),
    estado_nuevo            VARCHAR(30)     NOT NULL,
    detalle                 VARCHAR(500)    NOT NULL,
    fecha_registro          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pagos_bitacora_pago ON pagos_bitacora(pago_id);
