package com.infotech.lang.application.guest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** At most {@value #MAX_STARTS} guest sessions per visitor per hour, so nobody can loop it and run up Meta costs. In memory only. */
@Component
public class GuestThrottle {

    static final int MAX_STARTS = 10;
    static final int WINDOW_MINUTES = 60;
    private static final int MAX_VISITORS = 5000;

    private final Map<String, Deque<Long>> seen = new ConcurrentHashMap<>();

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
            if (times.size() >= MAX_STARTS) {
                return false;
            }
            times.addLast(nowMillis);
            return true;
        }
    }
}
