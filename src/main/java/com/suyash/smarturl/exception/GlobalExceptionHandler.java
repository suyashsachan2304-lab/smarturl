package com.suyash.smarturl.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(InvalidUrlException.class)
        public ResponseEntity<ErrorResponse> handleInvalidUrl(
                        InvalidUrlException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(UrlNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(
                        UrlNotFoundException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.NOT_FOUND,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(UrlExpiredException.class)
        public ResponseEntity<ErrorResponse> handleExpiredUrl(
                        UrlExpiredException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.GONE,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                List<String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(FieldError::getDefaultMessage)
                                .toList();

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Validation failed.",
                                request,
                                errors);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex,
                        HttpServletRequest request) {

                List<String> errors = ex.getConstraintViolations()
                                .stream()
                                .map(v -> v.getMessage())
                                .toList();

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Validation failed.",
                                request,
                                errors);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrity(
                        DataIntegrityViolationException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.CONFLICT,
                                "Duplicate resource or database constraint violation.",
                                request,
                                null);
        }

        @ExceptionHandler(ErrorResponseException.class)
        public ResponseEntity<ErrorResponse> handleSpringErrors(
                        ErrorResponseException ex,
                        HttpServletRequest request) {

                HttpStatusCode statusCode = ex.getStatusCode();

                HttpStatus status = HttpStatus.valueOf(statusCode.value());

                return buildResponse(
                                status,
                                ex.getBody().getDetail(),
                                request,
                                null);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(
                        Exception ex,
                        HttpServletRequest request) {

                ex.printStackTrace();

                return buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Something went wrong.",
                                request,
                                null);
        }

        private ResponseEntity<ErrorResponse> buildResponse(
                        HttpStatus status,
                        String message,
                        HttpServletRequest request,
                        List<String> validationErrors) {

                ErrorResponse response = ErrorResponse.builder()
                                .status(status.value())
                                .error(status.getReasonPhrase())
                                .message(message)
                                .path(request.getRequestURI())
                                .validationErrors(validationErrors)
                                .build();

                return ResponseEntity.status(status).body(response);
        }

}