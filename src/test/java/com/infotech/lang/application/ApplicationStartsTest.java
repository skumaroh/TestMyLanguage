package com.infotech.lang.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.infotech.lang.application.guest.GuestPurgeJob;
import com.infotech.lang.application.guest.GuestService;
import com.infotech.lang.application.security.GoogleOidcUserService;
import com.infotech.lang.application.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Start-up check: builds the whole application (every bean, the security configuration with Google sign-in and guest mode switched on) against an
 * in-memory database, so a wiring mistake fails here, before a merge, instead of crashing the live pods. Nothing outside the machine
 * is contacted: no real database, no Meta, no Azure.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:startcheck;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        // switches the Google sign-in path on, with made-up values
        "spring.security.oauth2.client.registration.google.client-id=startcheck-0000000000.apps.googleusercontent.com",
        "spring.security.oauth2.client.registration.google.client-secret=startcheck-secret",
        // and guest mode, which builds the guest clean-up job as well
        "guest.enabled=true"
})
class ApplicationStartsTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void theWholeApplicationStarts() {
        assertNotNull(context.getBean(SecurityConfig.class));
        assertNotNull(context.getBean(GoogleOidcUserService.class));
        assertNotNull(context.getBean(GuestService.class));
        assertNotNull(context.getBean(GuestPurgeJob.class));
    }
}
