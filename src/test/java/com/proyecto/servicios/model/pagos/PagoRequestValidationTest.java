package com.proyecto.servicios.model.pagos;

import com.proyecto.servicios.enums.TipoPago;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PagoRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private IniciarPagoRequest requestRecargaValido() {
        IniciarPagoRequest r = new IniciarPagoRequest();
        r.setNumeroCuenta("1234567890123456");
        r.setIdUser(1);
        r.setTokenSesion(UUID.randomUUID().toString());
        r.setTipoPago(TipoPago.RECARGA);
        r.setIdServicio(71);
        r.setIdProd(200);
        r.setMonto(new BigDecimal("100.00"));
        r.setTelefono("5555555551");
        return r;
    }

    @Test
    void iniciarPago_recargaValida_sinViolaciones() {
        Set<ConstraintViolation<IniciarPagoRequest>> v = validator.validate(requestRecargaValido());
        assertTrue(v.isEmpty());
    }

    @Test
    void iniciarPago_servicioValido_sinViolaciones() {
        IniciarPagoRequest r = requestRecargaValido();
        r.setTipoPago(TipoPago.SERVICIO);
        r.setTelefono(null);
        r.setReferencia("CFE202699182");
        Set<ConstraintViolation<IniciarPagoRequest>> v = validator.validate(r);
        assertTrue(v.isEmpty());
    }

    @Test
    void iniciarPago_cuentaNoEsDe16Digitos_seRechaza() {
        IniciarPagoRequest r = requestRecargaValido();
        r.setNumeroCuenta("123456789012345");
        assertFalse(validator.validate(r).isEmpty());

        r.setNumeroCuenta("123456789012345A");
        assertFalse(validator.validate(r).isEmpty());
    }

    @Test
    void iniciarPago_montoNegativoCeroConMasDeDosDecimalesOFueraDeRango_seRechaza() {
        IniciarPagoRequest r = requestRecargaValido();

        r.setMonto(BigDecimal.ZERO);
        assertFalse(validator.validate(r).isEmpty());

        r.setMonto(new BigDecimal("-10.00"));
        assertFalse(validator.validate(r).isEmpty());

        r.setMonto(new BigDecimal("100.123"));
        assertFalse(validator.validate(r).isEmpty());

        r.setMonto(new BigDecimal("1000000.00"));
        assertFalse(validator.validate(r).isEmpty());
    }

    @Test
    void iniciarPago_recargaSinTelefonoOConReferencia_seRechaza() {
        IniciarPagoRequest sinTelefono = requestRecargaValido();
        sinTelefono.setTelefono(null);
        assertFalse(validator.validate(sinTelefono).isEmpty());

        IniciarPagoRequest conReferencia = requestRecargaValido();
        conReferencia.setReferencia("REF123");
        assertFalse(validator.validate(conReferencia).isEmpty());
    }

    @Test
    void iniciarPago_servicioSinReferenciaOConTelefono_seRechaza() {
        IniciarPagoRequest sinReferencia = requestRecargaValido();
        sinReferencia.setTipoPago(TipoPago.SERVICIO);
        sinReferencia.setTelefono(null);
        sinReferencia.setReferencia(null);
        assertFalse(validator.validate(sinReferencia).isEmpty());

        IniciarPagoRequest conTelefono = requestRecargaValido();
        conTelefono.setTipoPago(TipoPago.SERVICIO);
        conTelefono.setReferencia("REF123");
        assertFalse(validator.validate(conTelefono).isEmpty());
    }

    @Test
    void confirmarPago_codigoDe6DigitosYTokenUuidValidos() {
        ConfirmarPagoRequest r = new ConfirmarPagoRequest();
        r.setPagoId(1);
        r.setTokenSesion(UUID.randomUUID().toString());
        r.setCodigo("049281");
        assertTrue(validator.validate(r).isEmpty());

        r.setCodigo("12345");
        assertFalse(validator.validate(r).isEmpty());

        r.setCodigo("12345A");
        assertFalse(validator.validate(r).isEmpty());
    }
}
