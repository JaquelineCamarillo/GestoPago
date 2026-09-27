CREATE TABLE IF NOT EXISTS clientes (
                                        id                      SERIAL PRIMARY KEY,
                                        nombre                  VARCHAR(50) NOT NULL,
    segundo_nombre          VARCHAR(50),
    apellido_paterno        VARCHAR(50) NOT NULL,
    apellido_materno        VARCHAR(50) NOT NULL,
    fecha_nacimiento        DATE NOT NULL,
    curp                    VARCHAR(18) NOT NULL,
    rfc                     VARCHAR(13) NOT NULL,
    sexo                    VARCHAR(1) NOT NULL,
    nacionalidad            VARCHAR(50) NOT NULL,
    estado_civil            VARCHAR(20) NOT NULL,
    correo_electronico      VARCHAR(100) NOT NULL,
    telefono_movil          VARCHAR(10) NOT NULL,
    telefono_alternativo    VARCHAR(10),
    ocupacion               VARCHAR(50),
    empresa                 VARCHAR(100),
    ingreso_mensual         DOUBLE PRECISION NOT NULL,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro          TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_clientes_curp UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo_electronico)
    );

CREATE TABLE IF NOT EXISTS domicilios (
                                          id                 SERIAL PRIMARY KEY,
                                          cliente_id         INTEGER NOT NULL UNIQUE REFERENCES clientes(id),
    calle              VARCHAR(100) NOT NULL,
    numero_exterior    VARCHAR(10) NOT NULL,
    numero_interior    VARCHAR(10),
    colonia            VARCHAR(100) NOT NULL,
    municipio          VARCHAR(100) NOT NULL,
    estado             VARCHAR(100) NOT NULL,
    codigo_postal      VARCHAR(5) NOT NULL,
    pais               VARCHAR(50) NOT NULL
    );

CREATE TABLE IF NOT EXISTS cuentas (
                                       id                 SERIAL PRIMARY KEY,
                                       cliente_id         INTEGER NOT NULL UNIQUE REFERENCES clientes(id),
    numero_cuenta      VARCHAR(20) NOT NULL UNIQUE,
    saldo              DOUBLE PRECISION NOT NULL DEFAULT 0,
    estatus            VARCHAR(10) NOT NULL DEFAULT 'ACTIVA',
    fecha_apertura     TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS login (
                                     id                          SERIAL PRIMARY KEY,
                                     cliente_id                  INTEGER NOT NULL UNIQUE REFERENCES clientes(id),
    password_cifrado            TEXT NOT NULL,
    token_cifrado               TEXT,
    embedding_facial_cifrado    TEXT,
    activo                      BOOLEAN NOT NULL DEFAULT FALSE,
    ultima_actividad            TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_clientes_curp ON clientes(curp);
CREATE INDEX IF NOT EXISTS idx_clientes_rfc ON clientes(rfc);
CREATE INDEX IF NOT EXISTS idx_cuentas_numero ON cuentas(numero_cuenta);