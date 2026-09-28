package service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BisIsiAssistantRateLimiter {

    private final Bucket globalBucket;
    private final int perUserLimit;

    private final ConcurrentMap<Long, Bucket> userBuckets =
            new ConcurrentHashMap<>();

    private final Set<Long> activeUsers =
            ConcurrentHashMap.newKeySet();

    public BisIsiAssistantRateLimiter(
            @Value("${assistant.rate-limit.per-user-per-minute:10}")
            int perUserLimit,

            @Value("${assistant.rate-limit.global-per-minute:25}")
            int globalLimit
    ) {
        if (perUserLimit < 1 || globalLimit < 1) {
            throw new IllegalArgumentException(
                    "Assistant rate limits must be positive."
            );
        }

        this.perUserLimit = perUserLimit;
        this.globalBucket = createBucket(globalLimit);
    }

    public Permit acquire(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "An authenticated user is required."
            );
        }

        /*
         * Prevent one Super Admin from starting multiple Groq
         * requests simultaneously.
         */
        if (!activeUsers.add(userId)) {
            throw new AssistantRateLimitException(
                    "You already have an AI request in progress.",
                    2
            );
        }

        boolean accepted = false;

        try {
            Bucket userBucket = userBuckets.computeIfAbsent(
                    userId,
                    ignored -> createBucket(perUserLimit)
            );

            ConsumptionProbe userProbe =
                    userBucket.tryConsumeAndReturnRemaining(1);

            if (!userProbe.isConsumed()) {
                throw rateLimitExceeded(
                        "You have reached the chatbot request limit.",
                        userProbe.getNanosToWaitForRefill()
                );
            }

            ConsumptionProbe globalProbe =
                    globalBucket.tryConsumeAndReturnRemaining(1);

            if (!globalProbe.isConsumed()) {
                /*
                 * Return the user token because the request was rejected
                 * by the global limit.
                 */
                userBucket.addTokens(1);

                throw rateLimitExceeded(
                        "The AI assistant is currently busy.",
                        globalProbe.getNanosToWaitForRefill()
                );
            }

            accepted = true;

            return new Permit(userId, this);

        } finally {
            if (!accepted) {
                activeUsers.remove(userId);
            }
        }
    }

    private Bucket createBucket(long capacity) {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(capacity)
                        .refillGreedy(
                                capacity,
                                Duration.ofMinutes(1)
                        )
                )
                .build();
    }

    private AssistantRateLimitException rateLimitExceeded(
            String message,
            long nanosToWait
    ) {
        long secondsToWait = Math.max(
                1,
                (long) Math.ceil(
                        nanosToWait / 1_000_000_000.0
                )
        );

        return new AssistantRateLimitException(
                message,
                secondsToWait
        );
    }

    private void release(Long userId) {
        activeUsers.remove(userId);
    }

    public static final class Permit implements AutoCloseable {

        private final Long userId;
        private final BisIsiAssistantRateLimiter limiter;
        private boolean closed;

        private Permit(
                Long userId,
                BisIsiAssistantRateLimiter limiter
        ) {
            this.userId = userId;
            this.limiter = limiter;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                limiter.release(userId);
            }
        }
    }
}
