package com.closetly.closetly_backend.common;

import java.time.LocalDateTime;
import java.util.List;

public class ApiError {
    private int status;
    private String message;
    private List<String> errors;
    private LocalDateTime timestamp;

    public ApiError() {
    }

    public ApiError(int status, String message, List<String> errors, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
