package com.clothing.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @Email(message = "Email is invalid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;
}
