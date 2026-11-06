package com.booktracker.booktracker.aop;

import com.booktracker.booktracker.service.exception.TooManyRequestsException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitedAspect {

    private final Clock clock;
    private final Map<String, Deque<Long>> calls = new ConcurrentHashMap<>();

    public RateLimitedAspect(Clock clock) {
        this.clock = clock;
    }

    @Before("@annotation(rateLimited)")
    public void enforce(JoinPoint joinPoint, RateLimited rateLimited) {
        String key = SecurityUtils.currentUsername() + "|" + joinPoint.toShortString();
        long now = clock.millis();
        long windowStart = now - rateLimited.perSeconds() * 1000L;

        Deque<Long> timestamps = calls.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= rateLimited.maxCalls()) {
                throw new TooManyRequestsException("Rate limit exceeded: at most "
                        + rateLimited.maxCalls() + " calls per " + rateLimited.perSeconds() + " s");
            }
            timestamps.addLast(now);
        }
    }
}