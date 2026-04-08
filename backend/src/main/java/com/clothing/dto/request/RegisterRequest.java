package com.clothing.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username must be 3-50 characters")
    private String username;

    @NotBlank(message = "email is required")
    @Email(message = "email is invalid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100, message = "password must be 6-100 characters")
    private String password;

    @NotBlank(message = "fullName is required")
    @Size(max = 100, message = "fullName max length is 100")
    private String fullName;

    @Pattern(regexp = "^[0-9+\\-\\s]{8,20}$", message = "phone is invalid")
    private String phone;
}
