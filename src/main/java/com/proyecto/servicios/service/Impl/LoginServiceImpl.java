package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Login;
import com.proyecto.servicios.exception.clientes.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.clientes.CredencialesInvalidasException;
import com.proyecto.servicios.exception.clientes.LoginNoEncontradoException;
import com.proyecto.servicios.model.*;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.LoginRepository;
import com.proyecto.servicios.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    private static final long MINUTOS_INACTIVIDAD = 5;
    /** Distancia euclidiana maxima entre embeddings para considerarlos la misma persona. */
    private static final double UMBRAL_DISTANCIA_FACIAL = 0.6;

    private final LoginRepository loginRepository;
    private final ClienteRepository clienteRepository;
    private final TextEncryptor textEncryptor;

    public LoginServiceImpl(LoginRepository loginRepository,
                            ClienteRepository clienteRepository,
                            TextEncryptor textEncryptor) {
        this.loginRepository = loginRepository;
        this.clienteRepository = clienteRepository;
        this.textEncryptor = textEncryptor;
    }

    @Override
    @Transactional
    public GenericResponse registrarCredenciales(RegistrarCredencialesRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con id: " + request.getClienteId()));

        Login login = loginRepository.findByClienteId(cliente.getId()).orElseGet(Login::new);
        login.setCliente(cliente);
        login.setPasswordCifrado(textEncryptor.encrypt(request.getPassword()));
        if (request.getEmbeddingFacial() != null && !request.getEmbeddingFacial().isBlank()) {
            login.setEmbeddingFacialCifrado(textEncryptor.encrypt(request.getEmbeddingFacial()));
        }
        if (login.getActivo() == null) login.setActivo(false);
        loginRepository.save(login);

        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Credenciales registradas correctamente");
        return response;
    }

    @Override
    @Transactional
    public LoginResponse iniciarSesion(LoginRequest request) {
        Login login = buscarLogin(request.getClienteId());

        String passwordGuardado = textEncryptor.decrypt(login.getPasswordCifrado());
        if (!passwordGuardado.equals(request.getPassword())) {
            throw new CredencialesInvalidasException("La contrasena es incorrecta");
        }

        return activarSesion(login);
    }

    @Override
    @Transactional
    public LoginResponse iniciarSesionFacial(LoginFacialRequest request) {
        Login login = buscarLogin(request.getClienteId());

        if (login.getEmbeddingFacialCifrado() == null) {
            throw new CredencialesInvalidasException("El cliente no tiene datos faciales registrados");
        }

        String embeddingGuardado = textEncryptor.decrypt(login.getEmbeddingFacialCifrado());
        double distancia = distanciaEuclidiana(embeddingGuardado, request.getEmbeddingFacial());
        if (distancia > UMBRAL_DISTANCIA_FACIAL) {
            throw new CredencialesInvalidasException("El rostro no coincide con el registrado");
        }

        return activarSesion(login);
    }

    @Override
    @Transactional
    public GenericResponse cerrarSesion(Integer clienteId) {
        Login login = buscarLogin(clienteId);
        login.setActivo(false);
        login.setTokenCifrado(null);
        loginRepository.save(login);

        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Sesion cerrada correctamente");
        return response;
    }

    @Override
    @Transactional
    public void registrarActividad(Integer clienteId) {
        loginRepository.findByClienteId(clienteId).ifPresent(login -> {
            login.setUltimaActividad(LocalDateTime.now());
            loginRepository.save(login);
        });
    }

    /** Corre cada minuto: apaga (activo=false) las sesiones sin actividad en los ultimos 5 minutos. */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cerrarSesionesInactivas() {
        List<Login> activos = loginRepository.findByActivoTrue();
        LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_INACTIVIDAD);

        for (Login login : activos) {
            if (login.getUltimaActividad() == null || login.getUltimaActividad().isBefore(limite)) {
                login.setActivo(false);
                loginRepository.save(login);
                log.info("Sesion cerrada por inactividad, clienteId={}", login.getCliente().getId());
            }
        }
    }

    private LoginResponse activarSesion(Login login) {
        String token = UUID.randomUUID().toString();
        login.setTokenCifrado(textEncryptor.encrypt(token));
        login.setActivo(true);
        login.setUltimaActividad(LocalDateTime.now());
        loginRepository.save(login);
        return new LoginResponse(login.getCliente().getId(), token, true);
    }

    private Login buscarLogin(Integer clienteId) {
        return loginRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new LoginNoEncontradoException(clienteId));
    }

    private double distanciaEuclidiana(String vectorA, String vectorB) {
        String[] a = vectorA.split(",");
        String[] b = vectorB.split(",");
        if (a.length != b.length) {
            throw new CredencialesInvalidasException("El formato del embedding facial no coincide");
        }
        double suma = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = Double.parseDouble(a[i].trim()) - Double.parseDouble(b[i].trim());
            suma += diff * diff;
        }
        return Math.sqrt(suma);
    }
}