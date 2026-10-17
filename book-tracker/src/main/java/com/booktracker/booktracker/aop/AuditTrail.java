package com.booktracker.booktracker.aop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Slf4j
@Component
public class AuditTrail {

    private static final int MAX_ENTRIES = 1000;

    private final Deque<AuditEntry> entries = new ArrayDeque<>();

    public synchronized void record(AuditEntry entry) {
        if (entries.size() >= MAX_ENTRIES) {
            entries.pollFirst();
        }
        entries.addLast(entry);
        log.info("AUDIT user={} action={} method={} resourceId={}",
                entry.user(), entry.action(), entry.method(), entry.resourceId());
    }

    public synchronized List<AuditEntry> entries() {
        return List.copyOf(entries);
    }
}