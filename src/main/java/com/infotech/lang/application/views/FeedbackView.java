package com.infotech.lang.application.views;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.feedback.FeedbackService;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

/** The Feedback page: stars, how the experience was, and suggestions or flaws. Open to every signed-in user. */
@Route(value = "feedback", layout = MainLayout.class)
@PageTitle("Feedback | TestMyLanguage")
@PermitAll
public class FeedbackView extends VerticalLayout implements HasUrlParameter<String> {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(FeedbackView.class);

    private final transient FeedbackService service;
    private final transient SecurityService securityService;
    private final transient UserRepository userRepository;
    private String source = "menu";

    public FeedbackView(FeedbackService service, SecurityService securityService, UserRepository userRepository) {
        this.service = service;
        this.securityService = securityService;
        this.userRepository = userRepository;
        setMaxWidth("720px");
    }

    /** {@code /feedback/results} remembers that the link came from the results page. */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String from) {
        source = "results".equals(from) ? "results" : "menu";
        build();
    }

    private void build() {
        removeAll();
        add(new H2("Feedback"), new Paragraph("Tell us how it went. It helps us make the test better. "
                + "Please do not write your real address or phone number."));

        RadioButtonGroup<Integer> stars = new RadioButtonGroup<>("How many stars? (5 is best)");
        stars.setItems(1, 2, 3, 4, 5);
        stars.setItemLabelGenerator(n -> "★".repeat(n));
        TextArea experience = box("How was your experience?");
        TextArea suggestions = box("Suggestions, or anything that did not work");

        Button send = new Button("Send feedback");
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        send.addClickListener(e -> {
            if (stars.getValue() == null) {
                Notification.show("Please choose 1 to 5 stars.");
                return;
            }
            send.setEnabled(false);
            try {
                FeedbackService.Result r = service.submit(userId(), source, stars.getValue(), experience.getValue(), suggestions.getValue());
                if (r.ok()) {
                    removeAll();
                    add(new H2("Thank you!"), new Paragraph(r.message()),
                            new Button("Back to the welcome page", x -> getUI().ifPresent(ui -> ui.navigate("welcome"))));
                    return;
                }
                Notification.show(r.message());
            } catch (RuntimeException ex) {
                logger.error("Could not save feedback", ex);
                Notification.show("Sorry, your feedback could not be saved right now. Please try again later.");
            }
            send.setEnabled(true);
        });
        add(stars, experience, suggestions, send);
    }

    private static TextArea box(String label) {
        TextArea t = new TextArea(label);
        t.setWidthFull();
        t.setMinHeight("110px");
        t.setMaxLength(FeedbackService.MAX_CHARS);
        t.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        return t;
    }

    private UUID userId() {
        UserDetails user = securityService.getAuthenticatedUser();
        return user == null ? null : userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }
}
