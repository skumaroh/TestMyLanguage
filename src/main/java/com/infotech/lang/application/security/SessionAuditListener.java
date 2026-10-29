package com.infotech.lang.application.security;

import com.infotech.lang.application.data.service.UserSessionLogService;
import org.springframework.context.ApplicationListener;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

/**
 * Records the logout time for the login activity report whenever a session
 * ends - explicit sign-out, session timeout, or browser close all invalidate
 * the {@code HttpSession} and fire this event the same way.
 */
@Component
public class SessionAuditListener implements ApplicationListener<HttpSessionDestroyedEvent> {

    private final UserSessionLogService userSessionLogService;

    public SessionAuditListener(UserSessionLogService userSessionLogService) {
        this.userSessionLogService = userSessionLogService;
    }

    @Override
    public void onApplicationEvent(HttpSessionDestroyedEvent event) {
        userSessionLogService.recordLogout(event.getId());
    }
}
