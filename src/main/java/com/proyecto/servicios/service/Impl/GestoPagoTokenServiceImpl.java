package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.gestopago.GestoPagoTokenUnavailableException;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void renovarToken() {
        // Job programado: no debe tumbar la app, solo registrar el error para revision.
        try {
            autenticarYPersistir();
            log.info("Token GestoPago renovado correctamente (job programado)");
        } catch (Exception e) {
            log.error("Error al renovar token GestoPago desde el job programado: {}", e.getMessage(), e);
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
    }

    @Override
    public String obtenerTokenBearer() {
        Optional<GestoPagoToken> tokenActivo = obtenerTokenActivo(idDistribuidor, codigoDispositivo);

        if (tokenActivo.isPresent() && !estaExpirado(tokenActivo.get())) {
            return tokenActivo.get().getToken();
        }

        log.info("No hay token GestoPago valido en cache para distribuidor={}, solicitando uno nuevo", idDistribuidor);
        // A diferencia del job programado, aqui SI se propaga la excepcion especifica.
        GestoPagoToken nuevoToken = autenticarYPersistir();

        if (nuevoToken == null || nuevoToken.getToken() == null || nuevoToken.getToken().isBlank()) {
            throw new GestoPagoTokenUnavailableException("GestoPago no devolvio un token utilizable tras autenticar");
        }
        return nuevoToken.getToken();
    }

    /**
     * Autentica contra GestoPago y persiste (crea o actualiza) el token del distribuidor.
     * No captura excepciones: cualquier falla se propaga tal cual la clasifico el
     * GestoPagoFeignErrorDecoder.
     */
    private GestoPagoToken autenticarYPersistir() {
        log.info("Autenticando contra GestoPago para distribuidor={}", idDistribuidor);

        GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                idDistribuidor, codigoDispositivo, password);

        if (response == null || response.getToken() == null || response.getToken().isBlank()) {
            throw new GestoPagoTokenUnavailableException(
                    "GestoPago respondio sin un token valido en el cuerpo de la respuesta");
        }

        GestoPagoToken tokenEntity = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .map(existing -> {
                    tokenMapper.updateEntity(response, existing);
                    return existing;
                })
                .orElseGet(() -> {
                    GestoPagoToken nuevo = tokenMapper.toEntity(response);
                    nuevo.setIdDistribuidor(idDistribuidor);
                    nuevo.setCodigoDispositivo(codigoDispositivo);
                    nuevo.setActivo(true);
                    return nuevo;
                });

        return tokenRepository.save(tokenEntity);
    }

    private boolean estaExpirado(GestoPagoToken token) {
        if (token.getExpiresIn() == null || token.getFechaActualizacion() == null) {
            return true;
        }
        LocalDateTime expiraEn = token.getFechaActualizacion().plusSeconds(token.getExpiresIn());
        // Margen de seguridad de 60s para no usar un token que expira "a la mitad" de la llamada.
        return LocalDateTime.now().plusSeconds(60).isAfter(expiraEn);
    }
}