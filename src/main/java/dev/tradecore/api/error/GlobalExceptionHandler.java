package dev.tradecore.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound (ResourceNotFoundException ex,HttpServletRequest request){
        HttpStatus status = HttpStatus.NOT_FOUND;
       ApiError error = new ApiError(status.value(),"RESOURCE_NOT_FOUND", request.getRequestURI(),ex.getMessage());
       return ResponseEntity.status(status).body(error);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleInvalid(IllegalArgumentException ex,HttpServletRequest request){
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError error = new ApiError(status.value(),"BAD_REQUEST",request.getRequestURI(),ex.getMessage());
        return ResponseEntity.status(status).body(error);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex,HttpServletRequest request){
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiError error = new ApiError(status.value(),"INTERNAL_SERVER_ERROR",request.getRequestURI(),"Something went wrong");
        return ResponseEntity.status(status).body(error);
    }
}
