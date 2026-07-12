package com.suyash.smarturl.exception;

public class QrCodeGenerationException
        extends RuntimeException {

    public QrCodeGenerationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

}