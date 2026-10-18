package com.example.ebookrecommendsystem.ai;

/** Only fixed, safe messages reach clients. Never wrap provider bodies or credentials. */
public class AiServiceException extends RuntimeException {
    private final int status;
    public AiServiceException(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
}
