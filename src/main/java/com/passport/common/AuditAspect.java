package com.passport.common;

import com.passport.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        UUID actorId = SecurityUtils.getCurrentUserId();
        Object result = joinPoint.proceed();

        try {
            String resourceId = audited.resourceType() + ":" + joinPoint.getSignature().getName();
            auditService.record(
                actorId.toString(),
                audited.action(),
                resourceId,
                "Args count: " + joinPoint.getArgs().length
            );
        } catch (Exception e) {
            log.error("Failed to record audit log in aspect: {}", e.getMessage(), e);
        }

        return result;
    }
}
