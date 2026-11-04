package com.infotech.lang.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.infotech.lang.application.guest.GuestPurgeJob;
import com.infotech.lang.application.guest.GuestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/** Same start-up check as {@link ApplicationStartsTest}, with guest mode switched on (also builds the clean-up job). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:startcheckguest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "guest.enabled=true"
})
class ApplicationStartsWithGuestModeTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void theWholeApplicationStartsWithGuestModeOn() {
        assertNotNull(context.getBean(GuestService.class));
        assertNotNull(context.getBean(GuestPurgeJob.class));
    }
}
