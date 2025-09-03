package com.rhydon.rhydon.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
  @NotBlank String fullName,
  @Email @NotBlank String email,
  @Size(min = 6, max = 72) String password
) {}