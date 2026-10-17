package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ActualizaClienteRequest;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.validation.Curp;
import com.proyecto.servicios.validation.Rfc;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Los parametros de ruta y de consulta se validan aqui, antes de llegar al service.
 * Un valor invalido se responde con 400 y un mensaje especifico (ver GlobalExceptionHandler).
 */
@Validated
@RestController
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClienteController {

    private static final String LETRAS =
            "A-Za-z\u00C1\u00C9\u00CD\u00D3\u00DA\u00DC\u00D1\u00E1\u00E9\u00ED\u00F3\u00FA\u00FC\u00F1";
    private static final String PATRON_NOMBRE = "^\\s*[" + LETRAS + "]+(\\s+[" + LETRAS + "]+)*\\s*$";
    private static final String PATRON_CORREO =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";
    private static final String PATRON_CUENTA = "^[A-Za-z0-9]{6,30}$";

    private static final String MSG_ID = "El id debe ser un numero entero positivo";

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> crearCliente(@Valid @RequestBody ClienteRequest request) {
        return new ResponseEntity<>(clienteService.creaCliente(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> obtenerTodos() {
        return ResponseEntity.ok(clienteService.obtenerTodos());
    }

    /**
     * Busqueda por uno o varios criterios (se combinan con "y").
     * Ejemplos: /clientes/buscar?nombre=juana camarillo  |  /clientes/buscar?rfc=CAOJ950518AB9&activo=true
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteResponse>> buscar(
            @RequestParam(required = false)
            @Size(min = 2, max = 100, message = "El nombre a buscar debe tener entre 2 y 100 caracteres")
            @Pattern(regexp = PATRON_NOMBRE,
                    message = "El nombre a buscar solo debe contener letras (sin numeros ni simbolos)")
            String nombre,
            @RequestParam(required = false) @Curp String curp,
            @RequestParam(required = false) @Rfc String rfc,
            @RequestParam(required = false)
            @Size(max = 100, message = "El correo a buscar no puede tener mas de 100 caracteres")
            @Pattern(regexp = PATRON_CORREO,
                    message = "El correo a buscar no tiene un formato valido (ejemplo: nombre@dominio.com)")
            String correo,
            @RequestParam(required = false)
            @Pattern(regexp = PATRON_CUENTA,
                    message = "El numero de cuenta debe tener entre 6 y 30 caracteres, solo letras y numeros")
            String numeroCuenta,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(clienteService.buscar(nombre, curp, rfc, correo, numeroCuenta, activo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerPorId(
            @PathVariable @Min(value = 1, message = MSG_ID) Integer id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    @GetMapping("/curp/{curp}")
    public ResponseEntity<ClienteResponse> obtenerPorCurp(
            @PathVariable @NotBlank(message = "La CURP es obligatoria") @Curp String curp) {
        return ResponseEntity.ok(clienteService.obtenerPorCurp(curp));
    }

    @GetMapping("/rfc/{rfc}")
    public ResponseEntity<ClienteResponse> obtenerPorRfc(
            @PathVariable @NotBlank(message = "El RFC es obligatorio") @Rfc String rfc) {
        return ResponseEntity.ok(clienteService.obtenerPorRfc(rfc));
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<ClienteResponse> obtenerPorNumeroCuenta(
            @PathVariable
            @Pattern(regexp = PATRON_CUENTA,
                    message = "El numero de cuenta debe tener entre 6 y 30 caracteres, solo letras y numeros")
            String numeroCuenta) {
        return ResponseEntity.ok(clienteService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ClienteResponse>> obtenerActivos() {
        return ResponseEntity.ok(clienteService.obtenerActivos());
    }

    /** Fechas en formato yyyy/MM/dd (ejemplo: desde=2026/01/01&hasta=2026/12/31). */
    @GetMapping("/rango-fechas")
    public ResponseEntity<List<ClienteResponse>> obtenerPorRangoDeFechas(
            @RequestParam @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate desde,
            @RequestParam @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate hasta) {
        return ResponseEntity.ok(clienteService.obtenerPorRangoDeFechas(
                desde.atStartOfDay(), hasta.atTime(LocalTime.MAX)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @PathVariable @Min(value = 1, message = MSG_ID) Integer id,
            @Valid @RequestBody ActualizaClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizaCliente(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> bajaLogica(
            @PathVariable @Min(value = 1, message = MSG_ID) Integer id) {
        return ResponseEntity.ok(clienteService.bajaLogica(id));
    }
}