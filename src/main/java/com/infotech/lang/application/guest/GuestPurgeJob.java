package com.infotech.lang.application.guest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every half hour, deletes the data of guests older than the retention time. Only exists while guest mode is switched on. */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "guest.enabled", havingValue = "true")
public class GuestPurgeJob {

    private static final Logger logger = LoggerFactory.getLogger(GuestPurgeJob.class);

    private final GuestService guests;
    private final int hours;

    public GuestPurgeJob(GuestService guests, @Value("${guest.retention-hours:24}") int hours) {
        this.guests = guests;
        this.hours = Math.max(1, hours);
    }

    @Scheduled(initialDelay = 5 * 60_000L, fixedDelay = 30 * 60_000L)
    public void run() {
        int removed = guests.purge(hours);
        if (removed > 0) {
            logger.info("Deleted {} guest account(s) older than {} hours", removed, hours);
        }
    }
}
