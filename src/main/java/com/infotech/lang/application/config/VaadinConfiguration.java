package com.infotech.lang.application.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Vaadin configuration for production mode.
 * Ensures Vaadin runs in production mode on Azure without requiring
 * the full vaadinBuildFrontend task (which has reflection issues in CI).
 */
@Component
public class VaadinConfiguration implements VaadinServiceInitListener {

    @Value("${vaadin.productionMode:true}")
    private boolean productionMode;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        // Production mode is configured via application.properties
        // This listener can be used for additional Vaadin initialization if needed
        if (productionMode) {
            System.out.println("Vaadin running in PRODUCTION mode");
        } else {
            System.out.println("Vaadin running in DEVELOPMENT mode");
        }
    }
}
