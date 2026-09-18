package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoXml;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Convierte el DTO XML que regresa GestoPago (GestoPagoProductoXml) a la entidad
 * que se persiste en la tabla local gestopago_productos.
 */
@Mapper(componentModel = "spring")
public interface GestoPagoProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(source = "servicio", target = "nombreServicio")
    @Mapping(source = "producto", target = "nombreProducto")
    GestoPagoProducto toEntity(GestoPagoProductoXml xml);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(source = "servicio", target = "nombreServicio")
    @Mapping(source = "producto", target = "nombreProducto")
    void updateEntity(GestoPagoProductoXml xml, @MappingTarget GestoPagoProducto entity);
}