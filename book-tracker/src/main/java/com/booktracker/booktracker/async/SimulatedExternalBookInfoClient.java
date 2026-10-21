package com.booktracker.booktracker.async;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatedExternalBookInfoClient implements ExternalBookInfoClient {

    private final long simulatedDelayMs;

    public SimulatedExternalBookInfoClient(@Value("${app.async.simulated-delay-ms:1000}") long simulatedDelayMs) {
        this.simulatedDelayMs = simulatedDelayMs;
    }

    @Override
    public double fetchRating(Long bookId) {
        SimulatedLatency.pause(simulatedDelayMs);
        return 4.5;
    }

    @Override
    public String fetchAuthorBio(Long authorId) {
        SimulatedLatency.pause(simulatedDelayMs);
        return "Biography of author #" + authorId;
    }
}