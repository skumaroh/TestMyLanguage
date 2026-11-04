package com.infotech.lang.application.certificate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * A courtesy limit on the public verification page: at most {@value #MAX_CHECKS} checks per visitor in {@value #WINDOW_MINUTES} minutes, so
 * nobody can sit there trying codes. (A code has about 10^17 possibilities, so guessing is hopeless anyway.) In memory only; it resets when
 * the app restarts.
 */
@Component
public class VerifyThrottle {

    static final int MAX_CHECKS = 30;
    static final int WINDOW_MINUTES = 10;
    private static final int MAX_VISITORS = 5000;

    private final Map<String, Deque<Long>> seen = new ConcurrentHashMap<>();

    /** True when this visitor may check another code now (and counts the check). */
    public boolean allow(String visitor, long nowMillis) {
        if (seen.size() > MAX_VISITORS) {
            seen.clear();
        }
        Deque<Long> times = seen.computeIfAbsent(visitor == null ? "?" : visitor, k -> new ArrayDeque<>());
        synchronized (times) {
            long cutoff = nowMillis - WINDOW_MINUTES * 60_000L;
            while (!times.isEmpty() && times.peekFirst() < cutoff) {
                times.pollFirst();
            }
            if (times.size() >= MAX_CHECKS) {
                return false;
            }
            times.addLast(nowMillis);
            return true;
        }
    }
}
