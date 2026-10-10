package com.proyecto.servicios.service;

import com.proyecto.servicios.model.ActualizaClienteRequest;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.GenericResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ClienteService {
    ClienteResponse creaCliente(ClienteRequest request);
    ClienteResponse obtenerPorId(Integer id);
    ClienteResponse obtenerPorCurp(String curp);
    ClienteResponse obtenerPorRfc(String rfc);
    ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta);
    List<ClienteResponse> obtenerTodos();
    List<ClienteResponse> obtenerActivos();
    List<ClienteResponse> obtenerPorRangoDeFechas(LocalDateTime desde, LocalDateTime hasta);

    /**
     * Busca clientes combinando (con "y") los criterios que no sean nulos.
     * Debe venir al menos uno. Valida el formato de cada criterio antes de buscar.
     */
    List<ClienteResponse> buscar(String nombre, String curp, String rfc, String correo,
                                 String numeroCuenta, Boolean activo);

    ClienteResponse actualizaCliente(Integer id, ActualizaClienteRequest request);
    GenericResponse bajaLogica(Integer id);
}