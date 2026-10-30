package com.infotech.lang.application.config;

import com.infotech.lang.application.data.service.AzureBlobService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports whether Azure Blob Storage is reachable, as the "blobStorage" component of /actuator/health.
 * It checks that the container named by {@code assessment.blob-check.container} exists. Status:
 * UP when it exists, DOWN when Azure is configured but the check fails, UNKNOWN when nothing is configured to check.
 * It is not part of the readiness group, so a Blob outage never takes the pod out of service.
 */
@Component("blobStorage")
public class BlobStorageHealthIndicator implements HealthIndicator {

    private final AzureBlobService blobService;
    private final String container;

    public BlobStorageHealthIndicator(AzureBlobService blobService,
                                      @Value("${assessment.blob-check.container:}") String container) {
        this.blobService = blobService;
        this.container = container;
    }

    @Override
    public Health health() {
        if (!blobService.isConfigured() || container == null || container.isBlank()) {
            return Health.unknown().build();
        }
        return blobService.containerExists(container) ? Health.up().build() : Health.down().build();
    }
}
