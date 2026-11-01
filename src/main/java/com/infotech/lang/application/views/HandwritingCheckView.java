package com.infotech.lang.application.views;

import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.service.LetterShapeComparisonService;
import com.infotech.lang.application.handwriting.CombinedHandwritingCheck;
import com.infotech.lang.application.handwriting.DrawingPad;
import com.infotech.lang.application.handwriting.HandwritingReader;
import com.infotech.lang.application.handwriting.LetterCandidate;
import com.infotech.lang.application.handwriting.ReferenceImages;
import com.infotech.lang.application.handwriting.ShapeRanking;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Admin test page for the handwriting check. Draw one character and type the letter you meant. It shows (1) the combined
 * check (old shape comparison weighs more, plus Meta's blind reading), (2) the old shape comparison against every letter of
 * the same group, ranked, and (3) Meta choosing among those letters. Nothing is saved except model-call log rows.
 */
@Route("admin/handwriting-check")
@PageTitle("Handwriting check | TestMyLanguage")
@RolesAllowed("ADMIN")
public class HandwritingCheckView extends VerticalLayout {

    private static final int MAX_CANDIDATES = 40;
    /** Meta chooses among the old method's best few letters plus the expected one: a short list is faster and easier. */
    private static final int SHORTLIST = 5;

    private final CombinedHandwritingCheck check;
    private final ReferenceImages references;
    private final LetterShapeComparisonService shape;
    private final HandwritingReader reader;
    private final String language;
    private final TextField expected = new TextField("The letter you meant to draw");
    private final Paragraph result = new Paragraph("Draw one character, then press Check.");
    private final Pre details = new Pre();
    private final Checkbox askMeta = new Checkbox("Also ask Meta (slow: 10 to 25 seconds)", false);
    private final Image sent = new Image();
    private final Image reference = new Image();

    public HandwritingCheckView(CombinedHandwritingCheck check, ReferenceImages references,
                                LetterShapeComparisonService shape, HandwritingReader reader,
                                LanguageContext languageContext) {
        this.check = check;
        this.references = references;
        this.shape = shape;
        this.reader = reader;
        this.language = languageContext.getLanguage();
        setAlignItems(Alignment.CENTER);
        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");
        details.getStyle().set("white-space", "pre-wrap").set("width", "100%");
        sent.getStyle().set("max-width", "256px").set("border", "1px solid #9aa0a6");
        sent.setVisible(false);
        reference.getStyle().set("max-width", "256px").set("border", "1px solid #9aa0a6").set("background", "#fff");
        reference.setVisible(false);

        DrawingPad pad = new DrawingPad();
        pad.addDrawingExportedListener(e -> run(e.png(), e.isBlank()));

        Button clear = new Button("Clear", e -> pad.clearPad());
        Button checkButton = new Button("Check", e -> pad.exportDrawing());
        add(new H2("Handwriting check (" + language + ")"),
                new Paragraph("Three results per drawing: the combined check (old shape comparison weighs more, plus Meta "
                        + "reading blind), the old shape comparison against every letter of the same group, and Meta choosing "
                        + "among those letters. Nothing is stored except model-call log rows. A check takes several seconds."),
                expected, askMeta, pad, new HorizontalLayout(clear, checkButton), result, new Paragraph("The picture that was sent, and the reference image of the letter you meant:"),
                new HorizontalLayout(sent, reference), details);
    }

    private void run(byte[] png, boolean blank) {
        String meant = expected.getValue() == null ? "" : expected.getValue().trim();
        if (blank || png == null) {
            Notification.show("Draw a character first.");
            return;
        }
        if (meant.isEmpty()) {
            Notification.show("Type the letter you meant to draw.");
            return;
        }
        sent.setSrc("data:image/png;base64," + Base64.getEncoder().encodeToString(png));
        sent.setAlt("the drawing that was sent");
        sent.setVisible(true);
        references.pngFor(meant).ifPresentOrElse(ref -> {
            reference.setSrc("data:image/png;base64," + Base64.getEncoder().encodeToString(ref));
            reference.setAlt("reference image of " + meant);
            reference.setVisible(true);
        }, () -> reference.setVisible(false));

        List<LetterCandidate> candidates = references.candidatesFor(meant, MAX_CANDIDATES);
        List<ShapeRanking.Ranked> ranked = candidates.size() < 2 ? List.of() : ShapeRanking.rank(shape, png, candidates);
        // Meta chooses among the old method's best few letters plus the expected one
        List<String> options = new ArrayList<>();
        for (ShapeRanking.Ranked x : ranked) {
            if (options.size() < SHORTLIST) {
                options.add(x.letter());
            }
        }
        if (!ranked.isEmpty() && !options.contains(meant) && candidates.stream().anyMatch(c -> c.letter().equals(meant))) {
            options.add(meant);
        }

        boolean withMeta = askMeta.getValue();
        if (!withMeta) {
            Double only = ranked.stream().filter(x -> x.letter().equals(meant)).map(ShapeRanking.Ranked::score).findFirst().orElse(null);
            result.setText("Old shape comparison for the letter you meant: "
                    + (only == null ? "none (no reference image)" : String.format(Locale.ROOT, "%.2f", only))
                    + ". Meta was not asked (tick the box to ask it).");
        }
        CompletableFuture<CombinedHandwritingCheck.Result> combinedFuture = !withMeta ? CompletableFuture.completedFuture(null)
                : CompletableFuture.supplyAsync(() -> check.check(png, language, meant));
        CompletableFuture<HandwritingReader.Choice> choiceFuture = !withMeta || options.size() < 2
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.supplyAsync(() -> reader.choose(png, language, options));
        CombinedHandwritingCheck.Result r = combinedFuture.join();
        HandwritingReader.Choice choice = choiceFuture.join();

        if (r != null) {
            String local = r.localScore() == null ? "none (no reference image)" : String.format(Locale.ROOT, "%.2f", r.localScore());
            String meta = r.metaAnswered()
                    ? String.format(Locale.ROOT, "read \"%s\", confidence %.2f, counted %.2f", r.metaRead(), r.metaConfidence(), r.metaScore())
                    : "no answer";
            String combined = r.combined() == null ? "none" : String.format(Locale.ROOT, "%.2f", r.combined());
            result.setText("Combined check: old shape comparison " + local + "; Meta (blind) " + meta + " (" + r.metaMs()
                    + " ms); combined " + combined + ". " + r.note());
        }

        StringBuilder text = new StringBuilder();
        if (options.size() < 2) {
            text.append("Not enough reference images in this letter's group to rank (found ").append(options.size()).append(").\n");
        } else {
            text.append("Old shape comparison against ").append(ranked.size()).append(" letters of the group (best first):\n");
            ranked.stream().limit(6).forEach(x -> text.append(String.format(Locale.ROOT, "  %s  %.2f%s%n", x.letter(), x.score(),
                    x.letter().equals(meant) ? "   <- the letter you meant" : "")));
            for (int i = 6; i < ranked.size(); i++) {
                if (ranked.get(i).letter().equals(meant)) {
                    text.append(String.format(Locale.ROOT, "  ... %s  %.2f   <- the letter you meant, number %d of %d%n", meant,
                            ranked.get(i).score(), i + 1, ranked.size()));
                }
            }
            text.append(String.format(Locale.ROOT, "  best: %s, lead over the second: %.2f%n%n", ranked.get(0).letter(), ShapeRanking.margin(ranked)));
            if (!withMeta) {
                text.append("Meta was not asked.\n");
            } else if (choice == null || !choice.ok()) {
                text.append("Meta choosing among the letters: no answer").append(choice == null ? "" : " (" + choice.error() + ")").append('\n');
            } else {
                text.append("Meta choosing among the old method's best ").append(options.size()).append(" (").append(String.join(" ", options))
                        .append(") (").append(choice.latencyMs()).append(" ms):\n");
                Map<String, Double> sorted = choice.confidences();
                sorted.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(6)
                        .forEach(en -> text.append(String.format(Locale.ROOT, "  %s  %.2f%s%n", en.getKey(), en.getValue(),
                                en.getKey().equals(meant) ? "   <- the letter you meant" : "")));
                text.append("  best: ").append(choice.best()).append('\n');
            }
        }
        details.setText(text.toString());
    }
}
