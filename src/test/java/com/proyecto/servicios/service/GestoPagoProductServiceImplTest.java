package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.exception.gestopago.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoBadResponseException;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.mapper.GestoPagoProductMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoMensajeXml;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListXmlResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoXml;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.Impl.GestoPagoProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GestoPagoProductServiceImplTest {

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;
    @Mock
    private GestoPagoTokenService gestoPagoTokenService;
    @Mock
    private GestoPagoProductoRepository productoRepository;
    @Mock
    private GestoPagoProductMapper productMapper;

    private GestoPagoProductServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GestoPagoProductServiceImpl(
                gestoPagoProductClient, gestoPagoTokenService, productoRepository, productMapper);
    }

    private GestoPagoProductListXmlResponse respuestaExitosa(List<GestoPagoProductoXml> productos) {
        GestoPagoMensajeXml mensaje = new GestoPagoMensajeXml();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito");

        GestoPagoProductListXmlResponse response = new GestoPagoProductListXmlResponse();
        response.setMensaje(mensaje);
        response.setProductos(productos);
        return response;
    }

    @Test
    void sincronizarCatalogo_respuestaExitosa_guardaProductosNuevos() {
        GestoPagoProductoXml productoXml = new GestoPagoProductoXml();
        productoXml.setIdProducto(200);
        productoXml.setIdServicio(71);

        when(gestoPagoTokenService.obtenerTokenBearer()).thenReturn("token-valido");
        when(gestoPagoProductClient.obtenerListaProductos("Bearer token-valido"))
                .thenReturn(respuestaExitosa(List.of(productoXml)));
        when(productoRepository.findByIdProductoAndIdServicio(200, 71)).thenReturn(Optional.empty());
        when(productMapper.toEntity(productoXml)).thenReturn(new com.proyecto.servicios.entity.gestopago.GestoPagoProducto());
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.sincronizarCatalogo();

        verify(productoRepository, times(1)).save(any());
    }

    @Test
    void sincronizarCatalogo_codigoDiferenteDeExito_lanzaBadResponseException() {
        GestoPagoMensajeXml mensaje = new GestoPagoMensajeXml();
        mensaje.setCodigo("82");
        mensaje.setTexto("Tiempo de espera alcanzado");

        GestoPagoProductListXmlResponse response = new GestoPagoProductListXmlResponse();
        response.setMensaje(mensaje);

        when(gestoPagoTokenService.obtenerTokenBearer()).thenReturn("token-valido");
        when(gestoPagoProductClient.obtenerListaProductos(anyString())).thenReturn(response);

        assertThrows(GestoPagoBadResponseException.class, () -> service.sincronizarCatalogo());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void sincronizarCatalogo_tokenExpiradoEnLlamada_renuevaYReintentaUnaVez() {
        when(gestoPagoTokenService.obtenerTokenBearer())
                .thenReturn("token-viejo")
                .thenReturn("token-nuevo");
        when(gestoPagoProductClient.obtenerListaProductos("Bearer token-viejo"))
                .thenThrow(new GestoPagoAuthenticationException(GestoPagoAuthenticationException.Motivo.TOKEN_EXPIRADO));
        when(gestoPagoProductClient.obtenerListaProductos("Bearer token-nuevo"))
                .thenReturn(respuestaExitosa(List.of()));

        service.sincronizarCatalogo();

        verify(gestoPagoTokenService, times(1)).renovarToken();
        verify(gestoPagoProductClient, times(1)).obtenerListaProductos("Bearer token-viejo");
        verify(gestoPagoProductClient, times(1)).obtenerListaProductos("Bearer token-nuevo");
    }

    @Test
    void sincronizarCatalogo_credencialesInvalidas_propagaMotivoEspecifico() {
        when(gestoPagoTokenService.obtenerTokenBearer()).thenReturn("token-valido");
        when(gestoPagoProductClient.obtenerListaProductos(anyString()))
                .thenThrow(new GestoPagoAuthenticationException(GestoPagoAuthenticationException.Motivo.PASSWORD_INVALIDO));

        GestoPagoAuthenticationException ex = assertThrows(GestoPagoAuthenticationException.class,
                () -> service.sincronizarCatalogo());

        assertEquals(GestoPagoAuthenticationException.Motivo.PASSWORD_INVALIDO, ex.getMotivo());
    }

    @Test
    void sincronizarCatalogo_errorInesperado_seEnvuelveComoComunicacion() {
        when(gestoPagoTokenService.obtenerTokenBearer()).thenReturn("token-valido");
        when(gestoPagoProductClient.obtenerListaProductos(anyString()))
                .thenThrow(new RuntimeException("fallo inesperado"));

        assertThrows(GestoPagoCommunicationException.class, () -> service.sincronizarCatalogo());
    }

    @Test
    void listarProductosDisponibles_leeDesdeRepositorio_noLlamaAGestoPago() {
        com.proyecto.servicios.entity.gestopago.GestoPagoProducto entidad =
                new com.proyecto.servicios.entity.gestopago.GestoPagoProducto();
        entidad.setIdProducto(200);
        entidad.setIdServicio(71);
        entidad.setNombreProducto("Amazon $100");

        when(productoRepository.findByActivoTrue()).thenReturn(List.of(entidad));

        List<GestoPagoProductoResponse> resultado = service.listarProductosDisponibles();

        assertEquals(1, resultado.size());
        assertEquals("Amazon $100", resultado.get(0).getNombreProducto());
        verifyNoInteractions(gestoPagoProductClient, gestoPagoTokenService);
    }
}