package com.rhydon.rhydon.dto;

public record UserUpdateRequest(
  String fullName,              
  @jakarta.validation.constraints.Email(message="Invalid e-mail")
  String email,                 
  @jakarta.validation.constraints.Size(min=6, message="Password must have at least 6 chars")
  String password                
) {}
