package controller;

import service.AssistantRateLimitException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = BisIsiAssistantController.class
)
public class AssistantExceptionHandler {

    @ExceptionHandler(AssistantRateLimitException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(
            AssistantRateLimitException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getMessage()
        );

        problem.setTitle("Chatbot rate limit reached");

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(
                        HttpHeaders.RETRY_AFTER,
                        String.valueOf(
                                exception.getRetryAfterSeconds()
                        )
                )
                .body(problem);
    }
}
