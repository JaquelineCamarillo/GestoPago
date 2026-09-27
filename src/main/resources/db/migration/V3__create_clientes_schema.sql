-- Renombra la tabla vieja de "personas" a "clientes" y le agrega todos los campos nuevos
ALTER TABLE personas RENAME TO clientes;

ALTER TABLE clientes RENAME COLUMN apellido_paterno TO apellido_paterno_tmp;
ALTER TABLE clientes RENAME COLUMN apellido_paterno_tmp TO apellido_paterno;

ALTER TABLE clientes
    ADD COLUMN segundo_nombre VARCHAR(50),
    ADD COLUMN fecha_nacimiento DATE,
    ADD COLUMN curp VARCHAR(18),
    ADD COLUMN rfc VARCHAR(13),
    ADD COLUMN sexo VARCHAR(1),
    ADD COLUMN nacionalidad VARCHAR(50),
    ADD COLUMN estado_civil VARCHAR(20),
    ADD COLUMN correo_electronico VARCHAR(100),
    ADD COLUMN telefono_movil VARCHAR(10),
    ADD COLUMN telefono_alternativo VARCHAR(10),
    ADD COLUMN ocupacion VARCHAR(50),
    ADD COLUMN empresa VARCHAR(100),
    ADD COLUMN ingreso_mensual DOUBLE PRECISION,
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN fecha_registro TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN fecha_actualizacion TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE clientes ADD CONSTRAINT uq_clientes_curp UNIQUE (curp);
ALTER TABLE clientes ADD CONSTRAINT uq_clientes_rfc UNIQUE (rfc);
ALTER TABLE clientes ADD CONSTRAINT uq_clientes_correo UNIQUE (correo_electronico);

CREATE TABLE domicilios (
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

CREATE TABLE cuentas (
                         id                 SERIAL PRIMARY KEY,
                         cliente_id         INTEGER NOT NULL UNIQUE REFERENCES clientes(id),
                         numero_cuenta      VARCHAR(20) NOT NULL UNIQUE,
                         saldo              DOUBLE PRECISION NOT NULL DEFAULT 0,
                         estatus            VARCHAR(10) NOT NULL DEFAULT 'ACTIVA',
                         fecha_apertura     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE login (
                       id                          SERIAL PRIMARY KEY,
                       cliente_id                  INTEGER NOT NULL UNIQUE REFERENCES clientes(id),
                       password_cifrado            TEXT NOT NULL,
                       token_cifrado               TEXT,
                       embedding_facial_cifrado    TEXT,
                       activo                      BOOLEAN NOT NULL DEFAULT FALSE,
                       ultima_actividad            TIMESTAMP
);

CREATE INDEX idx_clientes_curp ON clientes(curp);
CREATE INDEX idx_clientes_rfc ON clientes(rfc);
CREATE INDEX idx_cuentas_numero ON cuentas(numero_cuenta);