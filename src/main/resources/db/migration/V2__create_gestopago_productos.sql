CREATE TABLE IF NOT EXISTS gestopago_productos (
    id                    SERIAL PRIMARY KEY,
    id_producto           INTEGER         NOT NULL,
    id_servicio           INTEGER         NOT NULL,
    id_cat_tipo_servicio  INTEGER,
    tipo_front            INTEGER,
    nombre_servicio       VARCHAR(256),
    nombre_producto       VARCHAR(256),
    precio                VARCHAR(20),
    tipo_referencia       VARCHAR(5),
    legend                TEXT,
    activo                BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_actualizacion   TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_gestopago_productos UNIQUE (id_producto, id_servicio)
    );