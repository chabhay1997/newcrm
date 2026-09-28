package service;

public class AssistantRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public AssistantRateLimitException(
            String message,
            long retryAfterSeconds
    ) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

