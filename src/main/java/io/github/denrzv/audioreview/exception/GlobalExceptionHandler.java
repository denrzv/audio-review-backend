package io.github.denrzv.audioreview.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler to handle custom and validation exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String ERROR = "error";
    
    /**
     * Handle ResourceNotFoundException.
     *
     * @param ex      the exception
     * @param request the web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put(ERROR, ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle ResourceAlreadyExistsException.
     *
     * @param ex      the exception
     * @param request the web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Object> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex, WebRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put(ERROR, ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }
    
    /**
     * Handle MethodArgumentNotValidException for validation errors.
     *
     * @param ex      the exception
     * @param headers the HTTP headers
     * @param status  the HTTP status
     * @param request the web request
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, 
                                                             HttpHeaders headers, 
                                                             HttpStatus status, 
                                                             WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle AccessDeniedException for unauthorized access.
     *
     * @param ex      the exception
     * @param request the web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put(ERROR, "Access denied");
        
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Handle generic exceptions.
     *
     * @param ex      the exception
     * @param request the web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put(ERROR, "An unexpected error occurred");
        
        ex.printStackTrace();
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}