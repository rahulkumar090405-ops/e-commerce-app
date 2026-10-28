package com.spring.userauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @NotBlank(message = "Gender is required")
        @Pattern(
                regexp = "(?i)male|female|other|prefer not to say",
                message = "Gender must be Male, Female, Other, or Prefer not to say")
        String gender,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "\\+?[0-9]{7,15}", message = "Enter a valid mobile number")
        String mobile
) {
}
