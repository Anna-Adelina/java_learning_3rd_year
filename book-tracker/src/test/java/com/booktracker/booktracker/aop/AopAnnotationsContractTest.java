package com.booktracker.booktracker.aop;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AopAnnotationsContractTest {

    @Test
    void customAopAnnotations_haveRuntimeRetentionAndMethodTarget() {
        List<Class<? extends Annotation>> types =
                List.of(RetryOnTransientFailure.class, RateLimited.class, Audited.class);

        for (Class<? extends Annotation> type : types) {
            assertEquals(RetentionPolicy.RUNTIME, type.getAnnotation(Retention.class).value(),
                    type.getSimpleName() + " must be RUNTIME");
            assertArrayEquals(new ElementType[]{ElementType.METHOD},
                    type.getAnnotation(Target.class).value(),
                    type.getSimpleName() + " must target METHOD");
        }
    }
}