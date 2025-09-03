package com.rhydon.rhydon.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
  @Nullable String fullName,
  @Email @Nullable String email,
  @Size(min = 6, max = 72) @Nullable String password
) {}