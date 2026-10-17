package com.booktracker.booktracker.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class RetryOnTransientFailureAspect {

    @Around("@annotation(retry)")
    public Object retry(ProceedingJoinPoint pjp, RetryOnTransientFailure retry) throws Throwable {
        int maxAttempts = Math.max(1, retry.maxAttempts());
        TransientDataAccessException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return pjp.proceed();
            } catch (TransientDataAccessException ex) {
                lastFailure = ex;
                log.warn("Transient failure in {} (attempt {}/{}): {}",
                        pjp.toShortString(), attempt, maxAttempts, ex.getMessage());
            }
        }
        throw lastFailure;
    }
}