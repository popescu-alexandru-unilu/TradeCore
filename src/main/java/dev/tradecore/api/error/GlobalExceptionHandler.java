@RestControllerAdvice
public class GlobalExceptionHandler{
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound (ResourceNotFoundException ex,HttpServletRequest request){
        HttpStatus status = HttpStatus.NOT_FOUND;
       ApiError error = new ApiError(status.value(),"RESOURCE_NOT_FOUND", request.getRequestURI(),ex.getMessage());
       return ResponseEntity.status(status).body(error);
    }
}
