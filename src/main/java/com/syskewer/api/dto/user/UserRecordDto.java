package com.syskewer.api.dto.user;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserRecordDto(
        @Schema(example = "João Silva")
        @NotBlank(message = "O nome não pode estar em branco")
        String name,

        @Schema(example = "joaosilva")
        @NotBlank(message = "O nome de usuário não pode estar em branco")
        String username,

        @Schema(example = "joao@email.com")
        @NotBlank(message = "O email não pode estar em branco")
        @Email(message = "O formato do email é inválido")
        String email,

        @Schema(example = "senha123", type = "string", format = "password")
        @NotBlank(message = "A senha não pode estar em branco")
        String password,

        @Schema(example = "1")
        @NotNull(message = "O ID do cargo é obrigatório") 
        Integer roleId
) {
}
