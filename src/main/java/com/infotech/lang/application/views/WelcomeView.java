package com.infotech.lang.application.views;

import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.proctor.ProctorTextService;
import com.infotech.lang.application.proctor.ProctorWarmUp;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Value;

/** Older address of the welcome texts; the same texts are in the Help popup on the home page. */
@Route(value = "welcome", layout = MainLayout.class)
@PageTitle("Welcome | TestMyLanguage")
@PermitAll
public class WelcomeView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    public WelcomeView(SecurityService securityService, UserRepository userRepository, ProctorTextService textService,
                       ProctorWarmUp warmUp, @Value("${proctor.level:1}") int level) {
        setAlignItems(Alignment.CENTER);
        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");
        add(new H2("Welcome"), new WelcomeIntro(securityService, userRepository, textService, warmUp, level));
        add(new com.vaadin.flow.router.RouterLink("Go to the home page", HomeView.class));
    }
}
