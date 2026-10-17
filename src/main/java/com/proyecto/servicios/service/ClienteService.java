package com.proyecto.servicios.service;

import com.proyecto.servicios.model.ActualizaClienteRequest;
import com.proyecto.servicios.model.BuscarClienteRequest;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.GenericResponse;

import java.util.List;

public interface ClienteService {

    ClienteResponse creaCliente(ClienteRequest request);

    ClienteResponse obtenerPorId(Integer id);

    List<ClienteResponse> obtenerTodos();

    /**
     * Unica consulta de clientes: combina con "y" los criterios que vengan en la peticion
     * (id, nombre, CURP, RFC, correo, numero de cuenta, genero, nacionalidad, estatus y rango de
     * fecha de registro). Debe venir al menos uno. Si nadie coincide lanza ClienteNoEncontradoException.
     */
    List<ClienteResponse> buscar(BuscarClienteRequest criterios);

    /** Modifica solo los campos que vienen en la peticion; el cliente se identifica con request.getId(). */
    ClienteResponse actualizaCliente(ActualizaClienteRequest request);

    GenericResponse bajaLogica(Integer id);
}