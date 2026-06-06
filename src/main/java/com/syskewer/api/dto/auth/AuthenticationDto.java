package com.syskewer.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationDto(
    @NotBlank(message = "Username é obrigatório")
    String username,

    @NotBlank(message = "Password é obrigatório")
    String password
) {}
