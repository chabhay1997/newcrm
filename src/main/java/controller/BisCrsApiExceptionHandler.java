package controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import service.DuplicateBisCrsRenewalException;

@RestControllerAdvice(assignableTypes = BisCrsApiController.class)
public class BisCrsApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalid(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid BIS CRS request", exception.getMessage());
    }

    @ExceptionHandler(DuplicateBisCrsRenewalException.class)
    public ResponseEntity<ProblemDetail> duplicate(DuplicateBisCrsRenewalException exception) {
        return problem(HttpStatus.CONFLICT, "Duplicate BIS CRS record", exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> malformed(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed JSON request", "Request body contains invalid JSON or date values");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        return ResponseEntity.status(status).cacheControl(org.springframework.http.CacheControl.noStore()).body(body);
    }
}
