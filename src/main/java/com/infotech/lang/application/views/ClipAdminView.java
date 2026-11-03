package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessmentMediaService;
import com.infotech.lang.application.assessment.MediaUrls;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.security.SecurityService;
import com.infotech.lang.application.video.VideoClip;
import com.infotech.lang.application.video.VideoLibrary;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Admin-only clip library. Upload MP4 clips (or PNG / JPG pictures) to the {@code assessment-clips/} folder of the image container, then:
 * register the new files, let Meta watch the drafts (it writes the English description, the answer in the tested language, accepted
 * variants and a clarity verdict), check them here with the clip playing beside them, and verify. Only verified clips are used in a test.
 */
@Route(value = "admin/clips", layout = MainLayout.class)
@PageTitle("Clips | TestMyLanguage")
@RolesAllowed("ADMIN")
public class ClipAdminView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final int LEVEL = 1;

    private final transient VideoLibrary library;
    private final transient SecurityService securityService;
    private final transient UserRepository userRepository;
    private final String language;

    private final Paragraph counts = new Paragraph();
    private final Grid<VideoClip> grid = new Grid<>(VideoClip.class, false);
    private final Div player = new Div();
    private final Div details = new Div();
    private final transient com.infotech.lang.application.voice.SpeechRegistry speechRegistry;
    private final Button verify = new Button("Verify");
    private final Button edit = new Button("Edit");
    private final Button retire = new Button("Retire");
    private final ComboBox<String> filter = new ComboBox<>("Show");

    public ClipAdminView(VideoLibrary library, SecurityService securityService, UserRepository userRepository,
                         LanguageContext languageContext, com.infotech.lang.application.voice.SpeechRegistry speechRegistry) {
        this.library = library;
        this.speechRegistry = speechRegistry;
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.language = languageContext.getLanguage();

        add(new H2("Clips (" + language + ")"));
        add(new Paragraph("1. Upload MP4 clips (about 3 seconds, no sound, under 300 KB) or PNG / JPG pictures to the folder \""
                + AssessmentMediaService.VIDEO_PREFIX + "\" of the container \"" + AssessmentMediaService.VIDEO_CONTAINER
                + "\" in Azure (portal: Containers, Upload). 2. Press \"Register new files\". 3. Press \"Let Meta watch the drafts\" and refresh "
                + "after a minute. 4. Check each draft with the clip beside it, fix the " + language + " if needed, and verify. "
                + "Only verified clips are used in a test, and a learner does not see the same clip twice."));
        add(counts);

        Button register = new Button("Register new files", e -> {
            try {
                int n = library.registerAll(language, LEVEL);
                Notification.show(n == 0 ? "No new files found." : n + " new file(s) registered as drafts.");
            } catch (RuntimeException ex) {
                Notification.show("Could not register the files (has the V088 migration been run?).");
            }
            reload();
        });
        Button watch = new Button("Let Meta watch the drafts", e -> {
            int n = library.analyzeDraftsInBackground(language);
            Notification.show(n == 0 ? "No drafts are waiting for a description."
                    : "Meta is watching " + n + " draft(s) in the background. Press Refresh in a minute.");
        });
        Button verifyAll = new Button("Verify all that Meta judged clear", e -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Verify all clear drafts?");
            dialog.setText("Every draft with a description, an answer and Meta's verdict YES becomes verified and can be used in tests. "
                    + "Check a few of them first.");
            dialog.setCancelable(true);
            dialog.setConfirmText("Verify them");
            dialog.addConfirmListener(c -> {
                int n = library.verifyAllClear(language, adminId());
                Notification.show(n + " clip(s) verified.");
                reload();
            });
            dialog.open();
        });
        Button refresh = new Button("Refresh", e -> reload());
        filter.setItems("All", "Drafts", "Verified", "Retired", "Meta said not clear");
        filter.setValue("All");
        filter.addValueChangeListener(e -> reload());
        add(new HorizontalLayout(register, watch, verifyAll, refresh, filter));

        grid.addColumn(VideoClip::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(VideoClip::getStatus).setHeader("Status").setAutoWidth(true);
        grid.addColumn(c -> c.getBlobName().substring(c.getBlobName().lastIndexOf('/') + 1)).setHeader("File").setAutoWidth(true);
        grid.addColumn(VideoClip::getEnglish).setHeader("English").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(VideoClip::getExpectedText).setHeader(language).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(VideoClip::getMetaClear).setHeader("Clear?").setAutoWidth(true);
        grid.addColumn(VideoClip::getTimesUsed).setHeader("Used").setAutoWidth(true);
        grid.setHeight("340px");
        grid.asSingleSelect().addValueChangeListener(e -> showSelection(e.getValue()));

        verify.addClickListener(e -> run(() -> library.verify(selectedId(), adminId()), "Verified."));
        retire.addClickListener(e -> run(() -> library.retire(selectedId()), "Retired."));
        edit.addClickListener(e -> openEditor());
        add(grid, player, details, new HorizontalLayout(verify, edit, retire));
        setWidthFull();
        reload();
    }

    private void reload() {
        try {
            List<VideoClip> all = library.list(language);
            String f = filter.getValue() == null ? "All" : filter.getValue();
            grid.setItems(all.stream().filter(c -> switch (f) {
                case "Drafts" -> VideoClip.DRAFT.equals(c.getStatus());
                case "Verified" -> VideoClip.VERIFIED.equals(c.getStatus());
                case "Retired" -> VideoClip.RETIRED.equals(c.getStatus());
                case "Meta said not clear" -> "NO".equals(c.getMetaClear());
                default -> true;
            }).toList());
            long verified = all.stream().filter(c -> VideoClip.VERIFIED.equals(c.getStatus())).count();
            long drafts = all.stream().filter(c -> VideoClip.DRAFT.equals(c.getStatus())).count();
            long waiting = all.stream().filter(c -> VideoClip.DRAFT.equals(c.getStatus()) && !c.hasAnswer()).count();
            counts.setText(verified + " verified, " + drafts + " drafts (" + waiting + " still without a description), "
                    + library.unregistered(language).size() + " new file(s) waiting to be registered. A test needs at least 8 verified clips.");
        } catch (RuntimeException e) {
            Notification.show("The clip table is not available yet (has the V088 migration been run?).");
        }
        showSelection(null);
    }

    private void showSelection(VideoClip c) {
        verify.setEnabled(c != null && !VideoClip.VERIFIED.equals(c.getStatus()) && !VideoClip.RETIRED.equals(c.getStatus()));
        edit.setEnabled(c != null && !VideoClip.RETIRED.equals(c.getStatus()));
        retire.setEnabled(c != null && !VideoClip.RETIRED.equals(c.getStatus()));
        player.removeAll();
        if (c == null) {
            details.removeAll();
            return;
        }
        String url = MediaUrls.forBlob("video", AssessmentMediaService.VIDEO_CONTAINER, c.getBlobName());
        if (c.isVideo()) {
            player.add(new Html("<video src=\"" + url + "\" autoplay muted loop playsinline controls style=\"max-width:360px;width:100%\"></video>"));
        } else {
            player.add(new Html("<img src=\"" + url.replace("/video/", "/image/") + "\" alt=\"\" style=\"max-width:360px;width:100%\"/>"));
        }
        // every line in the tested language has its Listen button right after it, on the same line
        details.removeAll();
        details.add(detailLine("English: " + nz(c.getEnglish()), null));
        details.add(detailLine(language + ": ", c.getExpectedText()));
        boolean first = true;
        for (String variant : nz(c.getVariants()).split(";")) {
            if (!variant.isBlank()) {
                details.add(detailLine(first ? "Other accepted: " : "                      ", variant.trim()));
                first = false;
            }
        }
        if (first) {
            details.add(detailLine("Other accepted: ", null));
        }
        details.add(detailLine("Meta says clear: " + nz(c.getMetaClear()) + ". Possible problems: " + nz(c.getMetaProblems())
                + "  [" + c.getDifficulty() + "]", null));
    }

    /** A line of text; when {@code spoken} is given it follows the label and is followed directly by its Listen button. */
    private Div detailLine(String label, String spoken) {
        Div d = new Div();
        d.getStyle().set("white-space", "pre-wrap").set("padding", "2px 0");
        d.add(new com.vaadin.flow.component.html.Span(label));
        if (spoken != null && !spoken.isBlank()) {
            d.add(new com.vaadin.flow.component.html.Span(spoken), new com.vaadin.flow.component.html.Span(" "),
                    new SpeakButton(spoken, language, speechRegistry));
        }
        return d;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private long selectedId() {
        VideoClip selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            throw new IllegalStateException("Select a clip first.");
        }
        return selected.getId();
    }

    private UUID adminId() {
        UserDetails user = securityService.getAuthenticatedUser();
        return user == null ? null : userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }

    private void run(Runnable action, String done) {
        try {
            action.run();
            Notification.show(done);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Notification.show(e.getMessage());
        }
        reload();
    }

    private void openEditor() {
        VideoClip c = grid.asSingleSelect().getValue();
        if (c == null) {
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit clip " + c.getId() + (VideoClip.VERIFIED.equals(c.getStatus()) ? " (goes back to draft until verified again)" : ""));
        dialog.setWidth("640px");
        TextField english = new TextField("What is happening (English, one simple sentence)");
        english.setValue(nz(c.getEnglish()));
        english.setWidthFull();
        TextField expected = new TextField("Expected answer (" + language + ")");
        expected.setValue(nz(c.getExpectedText()));
        expected.setWidthFull();
        TextArea variants = new TextArea("Other accepted answers (" + language + "), separated by ;");
        variants.setValue(nz(c.getVariants()));
        variants.setWidthFull();
        ComboBox<String> difficulty = new ComboBox<>("Difficulty");
        difficulty.setItems("EASY", "MEDIUM");
        difficulty.setValue(c.getDifficulty());
        Button save = new Button("Save", e -> {
            c.setEnglish(english.getValue().trim());
            c.setExpectedText(expected.getValue().trim());
            c.setVariants(variants.getValue().trim());
            c.setDifficulty(difficulty.getValue() == null ? "EASY" : difficulty.getValue());
            try {
                library.saveEdits(c);
                Notification.show("Saved.");
                dialog.close();
                reload();
            } catch (RuntimeException ex) {
                Notification.show("Could not save the changes.");
            }
        });
        dialog.add(new VerticalLayout(english, expected, variants, difficulty));
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }
}
