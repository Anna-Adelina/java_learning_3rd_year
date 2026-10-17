package com.booktracker.booktracker.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Aspect
@Component
public class AuditedAspect {

    private final AuditTrail auditTrail;
    private final Clock clock;

    public AuditedAspect(AuditTrail auditTrail, Clock clock) {
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void audit(JoinPoint joinPoint, Audited audited, Object result) {
        AuditEntry entry = new AuditEntry(
                clock.instant(),
                SecurityUtils.currentUsername(),
                audited.action(),
                joinPoint.getSignature().toShortString(),
                resolveResourceId(joinPoint, result));
        auditTrail.record(entry);
    }

    private String resolveResourceId(JoinPoint joinPoint, Object result) {
        if (result != null) {
            BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(result);
            if (wrapper.isReadableProperty("id") && wrapper.getPropertyValue("id") != null) {
                return String.valueOf(wrapper.getPropertyValue("id"));
            }
        }
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long || arg instanceof Integer) {
                return String.valueOf(arg);
            }
        }
        return null;
    }
}