package com.clothing.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "usernameOrEmail is required")
    @JsonAlias({"username", "email"})
    private String usernameOrEmail;

    @NotBlank(message = "password is required")
    private String password;
}
