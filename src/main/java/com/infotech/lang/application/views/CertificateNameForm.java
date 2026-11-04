package com.infotech.lang.application.views;

import com.infotech.lang.application.certificate.CertificateName;
import com.infotech.lang.application.certificate.CertificateNameService;
import com.infotech.lang.application.certificate.NameScreen;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The form for the one name printed on certificates. The name can be saved only once and cannot be changed by the learner afterwards, so
 * the form says so and asks the learner to tick that they checked it. The name must pass the plain rules and the Meta screening.
 */
public class CertificateNameForm extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(CertificateNameForm.class);

    public CertificateNameForm(CertificateNameService names, NameScreen screen, UUID userId, String suggestion, Consumer<String> onSaved) {
        setPadding(false);
        add(new Paragraph("This is the name that will be printed on your certificate. You can save it only once and you cannot change it "
                + "afterwards, so please check the spelling. If you make a mistake, an administrator can reset it."));
        TextField name = new TextField("Name on certificate");
        name.setValue(suggestion == null ? "" : suggestion);
        name.setMaxLength(CertificateName.MAX);
        name.setWidth("28em");
        Checkbox sure = new Checkbox("I have checked the spelling. I understand I cannot change it later.");
        Button save = new Button("Save my name", e -> {
            String cleaned = CertificateName.clean(name.getValue());
            String problem = CertificateName.problem(cleaned);
            if (problem != null) {
                Notification.show(problem);
                return;
            }
            if (!sure.getValue()) {
                Notification.show("Please tick the box to confirm the spelling.");
                return;
            }
            try {
                NameScreen.Result checked = screen.check(cleaned);
                if (!checked.acceptable()) {
                    Notification.show("That does not look like a person's name. Please enter your own name.");
                    return;
                }
                if (!names.setOnce(userId, cleaned)) {
                    Notification.show("Your name is already saved and cannot be changed.");
                } else {
                    onSaved.accept(cleaned);
                }
            } catch (RuntimeException ex) {
                logger.error("Could not save the certificate name", ex);
                Notification.show("Sorry, your name could not be saved right now. Please try again.");
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(name, sure, save);
    }
}
