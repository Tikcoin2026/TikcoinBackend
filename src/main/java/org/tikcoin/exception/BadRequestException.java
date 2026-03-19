package org.tikcoin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    private final boolean status;

    public BadRequestException(String message) {
        super(message);
        this.status = false;
    }

    public BadRequestException(String message, boolean status) {
        super(message);
        this.status = status;
    }

    public boolean getStatus() {
        return status;
    }
}
