package com.infotech.lang.application.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.infotech.lang.application.data.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** The service is created by Spring, not by hand: this fails if Spring cannot pick its constructor. */
class GoogleOidcUserServiceWiringTest {

    @Test
    void springCanCreateTheService() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(UserRepository.class, () -> mock(UserRepository.class));
            context.register(GoogleOidcUserService.class);
            context.refresh();
            assertNotNull(context.getBean(GoogleOidcUserService.class));
        }
    }
}
