package com.syskewer.api.dto.user;

import com.syskewer.api.model.user.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRecordDto(
        @NotBlank(message = "O nome não pode estar em branco")
        String name,

        @NotBlank(message = "O nome de usuário não pode estar em branco")
        String username,

        @NotBlank(message = "O email não pode estar em branco")
        @Email(message = "O formato do email é inválido")
        String email,

        @NotBlank(message = "A senha não pode estar em branco")
        String password,

        @NotNull(message = "O cargo (role) é obrigatório")
        Role role
) {
}
