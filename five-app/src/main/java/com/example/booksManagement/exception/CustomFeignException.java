package com.example.booksManagement.exception;

import java.util.Collection;
import java.util.Map;

public class CustomFeignException extends RuntimeException {

  private final int status;
  private final Map<String, Collection<String>> headers;

  public CustomFeignException(int status, String message, Map<String, Collection<String>> headers) {
    super(message);
    this.status = status;
    this.headers = headers;
  }

  public int getStatus() {
    return status;
  }

  public Map<String, Collection<String>> getHeaders() {
    return headers;
  }
}