package com.rhydon.rhydon.dto;

public record ErrorResponse(
  String timestamp,  
  int status,         
  String error,       
  String message,     
  String path,       
  Object details      
) {}
