package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessAttempt;
import com.infotech.lang.application.certificate.CertificateNameService;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.interest.InterestService;
import com.infotech.lang.application.report.Certificate;
import com.infotech.lang.application.report.ReportService;
import com.infotech.lang.application.report.ReportSnapshot;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * "What would you like next?" A short poll for learners who earned a certificate: how interested they are in the next level, which topics,
 * their opinion and suggestions, and whether we may contact them. Name and email are filled in from the account. The learner can come back
 * and change their answer. Only the learner themselves can answer for their own attempt.
 */
@Route(value = "assessment/next-level", layout = MainLayout.class)
@PageTitle("What next? | TestMyLanguage")
@PermitAll
public class InterestView extends VerticalLayout implements BeforeEnterObserver {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(InterestView.class);
    private static final Map<String, String> INTEREST_LABELS = Map.of("VERY", "Very interested", "MAYBE", "Maybe, tell me more",
            "NOT_NOW", "Not right now");
    private static final List<String> TOPICS = List.of("More words and pictures", "Writing more letters", "Reading and writing sentences",
            "Speaking and conversation", "Listening practice", "Grammar", "Another language", "Certificates for higher levels");

    private final transient ReportService reports;
    private final transient SecurityService securityService;
    private final transient UserRepository userRepository;
    private final transient CertificateNameService names;
    private final transient InterestService interest;

    public InterestView(ReportService reports, SecurityService securityService, UserRepository userRepository,
                        CertificateNameService names, InterestService interest) {
        this.reports = reports;
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.names = names;
        this.interest = interest;
        setMaxWidth("720px");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        try {
            UserDetails details = securityService.getAuthenticatedUser();
            Optional<User> me = details == null ? Optional.empty() : userRepository.findByEmailIgnoreCase(details.getUsername());
            List<String> param = event.getLocation().getQueryParameters().getParameters().get("attempt");
            Optional<AssessAttempt> attempt = me.isEmpty() || param == null || param.isEmpty() ? Optional.empty()
                    : parse(param.get(0)).flatMap(reports::attempt).filter(a -> me.get().getId().equals(a.getUserId()));
            Optional<ReportSnapshot> report = attempt.flatMap(a -> reports.load(a.getId())).filter(Certificate::eligible);
            if (report.isEmpty()) {
                add(new H2("What would you like next?"), new Paragraph("This short form is for learners who earned a certificate."));
                return;
            }
            show(me.get(), attempt.get(), report.get());
        } catch (RuntimeException e) {
            logger.error("Could not show the interest form", e);
            add(new H2("What would you like next?"), new Paragraph("Sorry, the form could not be shown right now. Please try again later."));
        }
    }

    private void show(User me, AssessAttempt attempt, ReportSnapshot r) {
        String language = Certificate.languageName(r);
        add(new H2("What would you like next?"), new Paragraph("Congratulations on finishing " + Certificate.levelLabel(r.level()) + " in "
                + language + ". Very few learners reach this point. We are building the next levels and would love to know what you want from "
                + "them. Your answers are optional and you can change them later."));

        String name = names.get(me.getId()).orElse(Certificate.fullName(me.getFirstName(), me.getLastName()));
        TextField nameField = new TextField("Name");
        nameField.setValue(name == null ? "" : name);
        nameField.setReadOnly(true);
        nameField.setWidth("28em");
        TextField emailField = new TextField("Email");
        emailField.setValue(me.getEmail() == null ? "" : me.getEmail());
        emailField.setReadOnly(true);
        emailField.setWidth("28em");

        Optional<InterestService.Response> before = interest.get(me.getId(), r.language(), r.level());
        RadioButtonGroup<String> level = new RadioButtonGroup<>("Would you like to continue to the next level?");
        level.setItems(InterestService.INTERESTS);
        level.setItemLabelGenerator(INTEREST_LABELS::get);
        before.ifPresent(b -> level.setValue(b.interest()));
        CheckboxGroup<String> topics = new CheckboxGroup<>("What would you like to learn more about?");
        topics.setItems(TOPICS);
        before.ifPresent(b -> topics.setValue(Set.copyOf(InterestService.topicList(b.topics()))));
        TextArea opinion = box("What did you think of the assessment?", before.map(InterestService.Response::opinion).orElse(""));
        TextArea suggestions = box("Suggestions, or something you would like us to add", before.map(InterestService.Response::suggestions).orElse(""));
        Checkbox contact = new Checkbox("You may email me about the next levels and new modules. I can withdraw this at any time by "
                + "coming back to this form and unticking it.");
        contact.setValue(before.map(InterestService.Response::contactOk).orElse(false));

        Button save = new Button("Save my answers", e -> {
            if (level.getValue() == null) {
                Notification.show("Please choose how interested you are.");
                return;
            }
            try {
                interest.save(me.getId(), r.language(), r.level(), attempt.getId(), nameField.getValue(), emailField.getValue(), level.getValue(),
                        topics.getValue().stream().sorted().toList(), opinion.getValue(), suggestions.getValue(), contact.getValue());
                removeAll();
                add(new H2("Thank you!"), new Paragraph("Your answers are saved. You can come back to this page to change them."),
                        new Button("Back to my certificate", x -> getUI().ifPresent(ui -> ui.navigate("assessment/certificate",
                                com.vaadin.flow.router.QueryParameters.simple(Map.of("attempt", String.valueOf(r.attemptId())))))));
            } catch (RuntimeException ex) {
                logger.error("Could not save the interest answers", ex);
                Notification.show("Sorry, your answers could not be saved right now. Please try again later.");
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(nameField, emailField, level, topics, opinion, suggestions, contact, save);
    }

    private static TextArea box(String label, String value) {
        TextArea t = new TextArea(label);
        t.setWidthFull();
        t.setMinHeight("100px");
        t.setMaxLength(InterestService.MAX_TEXT);
        t.setValue(value == null ? "" : value);
        return t;
    }

    private static Optional<Long> parse(String text) {
        try {
            return Optional.of(Long.valueOf(text.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
