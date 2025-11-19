package com.tms.backend;

public class ErrorResponse {

    // 1. Define fields for the data you want to return
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    // 2. Define the public constructor that matches the instantiation call
    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // 3. Provide public getter methods for serialization (Spring/Jackson needs these)
    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}
