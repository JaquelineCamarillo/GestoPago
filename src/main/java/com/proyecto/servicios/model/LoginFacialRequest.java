package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginFacialRequest {

    @NotBlank(message = "El correo electronico es obligatorio")
    @Size(max = 100, message = "El correo electronico no puede tener mas de 100 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$|^$",
            message = "El correo electronico no tiene un formato valido (ejemplo: nombre@dominio.com)")
    @Schema(example = "juana@example.com")
    private String correoElectronico;

    @NotBlank(message = "El embedding facial es obligatorio")
    @Size(max = 20000, message = "El embedding facial es demasiado largo")
    @Schema(example = "0.12,0.55,-0.31", description = "Numeros decimales separados por coma")
    private String embeddingFacial;
}