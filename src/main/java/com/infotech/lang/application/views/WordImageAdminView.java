package com.infotech.lang.application.views;

import com.infotech.lang.application.ai.MetaImageGateway;
import com.infotech.lang.application.assessment.AssessWord;
import com.infotech.lang.application.assessment.AssessWordRepository;
import com.infotech.lang.application.assessment.AssessmentMediaService;
import com.infotech.lang.application.assessment.MediaUrls;
import com.infotech.lang.application.wordimages.ImageResizer;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Admin-only: the Module 1 word pictures, one by one. For each word the admin can ask Meta's image model for a new picture, see it beside
 * the current one, and keep it or not. A kept picture is saved as the word's newest picture; the old one stays in the store as the backup,
 * and "Use the earlier picture again" removes only a picture made here. Nothing changes until the admin presses Use this picture.
 */
@Route(value = "admin/word-pictures", layout = MainLayout.class)
@PageTitle("Word pictures | TestMyLanguage")
@RolesAllowed("ADMIN")
public class WordImageAdminView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(WordImageAdminView.class);
    static final String DEFAULT_STYLE = "A simple, friendly, flat illustration for a language-learning flash card. One clear subject, "
            + "centred and filling most of the frame, on a plain white background. Soft colours and clean outlines. No text, no letters, "
            + "no numbers, no watermark, no border.";

    /** One word: {@code savedAddress} words have a picture address stored with them, which this page cannot change yet. */
    record Row(String word, boolean savedAddress, String current, int versions, boolean madeHere) {
    }

    private final transient AssessWordRepository words;
    private final transient AssessmentMediaService media;
    private final transient MetaImageGateway images;
    private final Grid<Row> grid = new Grid<>(Row.class, false);
    private final Paragraph summary = new Paragraph();
    private final TextArea style = new TextArea("Picture style (used for every word)");
    private final Button batchButton = new Button("Make pictures for the selected words");
    /** At most this many words per batch (each picture costs about one cent), made three at a time. */
    static final int MAX_BATCH = 10;
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "word-picture");
        t.setDaemon(true);
        return t;
    });

    public WordImageAdminView(AssessWordRepository words, AssessmentMediaService media, MetaImageGateway images) {
        this.words = words;
        this.media = media;
        this.images = images;
        setMaxWidth("980px");
        getStyle().set("margin", "0 auto");
        add(new H2("Word pictures (Module 1)"), new Paragraph("Pick a word, make a new picture with Meta's image model, compare it with "
                + "the current one, and keep it only if it looks right. The old picture stays in the store as a backup."));
        add(new Paragraph(images.configured() ? "Meta image model: " + images.model() : "The Meta model key is not set, so pictures cannot be made."));
        add(new Paragraph(media.canWritePictures() ? "Saving pictures is allowed."
                : "Saving pictures is not set up yet (no write permission for the image store), so you can try pictures but not keep them."));
        style.setValue(DEFAULT_STYLE);
        style.setWidthFull();
        style.setMinHeight("110px");
        add(style, summary);
        batchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        batchButton.setEnabled(false);
        batchButton.addClickListener(e -> openBatch(new ArrayList<>(grid.getSelectedItems())));
        add(batchButton);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        ((GridMultiSelectionModel<Row>) grid.getSelectionModel())
                .setSelectAllCheckboxVisibility(GridMultiSelectionModel.SelectAllCheckboxVisibility.HIDDEN);
        grid.addSelectionListener(e -> {
            int n = e.getAllSelectedItems().size();
            batchButton.setEnabled(n > 0 && n <= MAX_BATCH && images.configured());
            batchButton.setText(n == 0 ? "Make pictures for the selected words"
                    : n > MAX_BATCH ? "Select at most " + MAX_BATCH + " words (" + n + " selected)"
                    : "Make pictures for the " + n + " selected word" + (n == 1 ? "" : "s"));
        });

        grid.addComponentColumn(row -> {
            Image thumb = new Image(row.current() == null ? "" : MediaUrls.forBlob("image", AssessmentMediaService.IMAGE_CONTAINER, row.current()),
                    row.word());
            thumb.setHeight("56px");
            thumb.setVisible(row.current() != null);
            return thumb;
        }).setHeader("Picture").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Row::word).setHeader("Word").setSortable(true).setAutoWidth(true);
        grid.addColumn(r -> r.current() == null ? "no picture" : r.savedAddress() ? "fixed address (cannot change here)"
                : r.madeHere() ? "made here by Meta" : "earlier picture").setHeader("Status").setAutoWidth(true);
        grid.addColumn(Row::versions).setHeader("Versions").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(row -> {
            HorizontalLayout buttons = new HorizontalLayout();
            Button replace = new Button("Make new picture", e -> openDialog(row));
            replace.addThemeVariants(ButtonVariant.LUMO_SMALL);
            replace.setEnabled(!row.savedAddress() && images.configured());
            buttons.add(replace);
            if (row.madeHere() && row.versions() > 1) {
                Button undo = new Button("Use the earlier picture again", e -> confirmUndo(row));
                undo.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                undo.setEnabled(media.canWritePictures());
                buttons.add(undo);
            }
            return buttons;
        }).setHeader("").setAutoWidth(true);
        grid.setAllRowsVisible(false);
        grid.setHeight("640px");
        add(grid);
        refresh();
    }

    private void refresh() {
        Map<String, List<String>> pictures = media.listPictures();
        List<Row> rows = new ArrayList<>();
        int madeHere = 0;
        for (AssessWord w : words.findHouseWords(1)) {
            String word = w.getQuestionText() == null ? "" : w.getQuestionText().trim();
            if (word.isEmpty()) {
                continue;
            }
            boolean saved = w.getImageUrl() != null && !w.getImageUrl().isBlank();
            List<String> versions = pictures.getOrDefault(word.toLowerCase(Locale.ROOT), List.of());
            if (versions.isEmpty()) {
                versions = pictures.getOrDefault(word.toLowerCase(Locale.ROOT).replace(' ', '_'), List.of());
            }
            String current = saved ? savedBlob(w.getImageUrl()) : versions.isEmpty() ? null : versions.get(0);
            boolean mine = !saved && current != null && AssessmentMediaService.madeByAdminPage(current);
            madeHere += mine ? 1 : 0;
            rows.add(new Row(word, saved, current, versions.size(), mine));
        }
        grid.setItems(rows);
        long withPicture = rows.stream().filter(r -> r.current() != null).count();
        summary.setText(rows.size() + " words, " + withPicture + " with a picture, " + madeHere + " replaced here by Meta's image model.");
    }

    private static String savedBlob(String url) {
        String[] parts = url.trim().split("/", 5);
        return parts.length < 5 ? null : java.net.URLDecoder.decode(parts[4], java.nio.charset.StandardCharsets.UTF_8);
    }

    private void openDialog(Row row) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Picture for \"" + row.word() + "\"");
        dialog.setWidth("92vw");
        dialog.setMaxWidth("860px");
        Optional<byte[]> oldBytes = row.current() == null ? Optional.empty()
                : media.read(AssessmentMediaService.IMAGE_CONTAINER, row.current());
        ImageResizer.Size oldSize = oldBytes.map(ImageResizer::sizeOf).orElse(null);
        String ext = row.current() != null && row.current().toLowerCase(Locale.ROOT).matches(".*\\.jpe?g$") ? "jpg" : "png";

        VerticalLayout left = new VerticalLayout(new Paragraph("Current" + (oldSize == null ? "" : " (" + oldSize.width() + " × " + oldSize.height() + ")")));
        left.setPadding(false);
        if (row.current() != null) {
            Image current = new Image(MediaUrls.forBlob("image", AssessmentMediaService.IMAGE_CONTAINER, row.current()), "current");
            current.setMaxWidth("100%");
            current.setMaxHeight("320px");
            left.add(current);
        }
        VerticalLayout right = new VerticalLayout(new Paragraph("New"));
        right.setPadding(false);
        Div newBox = new Div(new Paragraph("Press Make picture."));
        right.add(newBox);

        TextField notes = new TextField("Notes for this picture (optional)");
        notes.setPlaceholder("for example: a green bus, seen from the side");
        notes.setWidthFull();
        ProgressBar busy = new ProgressBar();
        busy.setIndeterminate(true);
        busy.setVisible(false);
        Button make = new Button("Make picture");
        make.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button keep = new Button("Use this picture");
        keep.setEnabled(false);
        Button close = new Button("Close", e -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        byte[][] made = new byte[1][];

        make.addClickListener(e -> {
            UI ui = UI.getCurrent();
            make.setEnabled(false);
            keep.setEnabled(false);
            busy.setVisible(true);
            newBox.removeAll();
            newBox.add(new Paragraph("Asking Meta for a picture… this can take up to a minute."));
            ui.setPollInterval(1000);
            String prompt = prompt(style.getValue(), row.word(), notes.getValue());
            CompletableFuture.supplyAsync(() -> images.generate(prompt)).thenAccept(result -> ui.access(() -> {
                ui.setPollInterval(-1);
                busy.setVisible(false);
                make.setEnabled(true);
                newBox.removeAll();
                if (!result.ok()) {
                    newBox.add(new Paragraph("No picture: " + result.error() + (result.httpStatus() > 0 ? " (HTTP " + result.httpStatus() + ")" : "")));
                    return;
                }
                try {
                    int w = oldSize == null ? 800 : oldSize.width();
                    int h = oldSize == null ? 800 : oldSize.height();
                    made[0] = ImageResizer.fit(result.bytes(), w, h, ext);
                    byte[] shown = made[0];
                    Image img = new Image("data:image/" + ("jpg".equals(ext) ? "jpeg" : "png") + ";base64,"
                            + java.util.Base64.getEncoder().encodeToString(shown), "new");
                    img.setMaxWidth("100%");
                    img.setMaxHeight("320px");
                    newBox.add(img, new Paragraph(w + " × " + h + " – same size as the current picture"));
                    keep.setEnabled(media.canWritePictures());
                } catch (IOException ex) {
                    newBox.add(new Paragraph("The picture could not be read."));
                }
            })).exceptionally(ex -> {
                ui.access(() -> {
                    ui.setPollInterval(-1);
                    busy.setVisible(false);
                    make.setEnabled(true);
                    newBox.removeAll();
                    newBox.add(new Paragraph("Something went wrong. Please try again."));
                });
                return null;
            });
        });
        keep.addClickListener(e -> {
            if (made[0] == null) {
                return;
            }
            try {
                String blob = media.savePicture(row.word(), made[0], ext);
                Optional<String> now = media.imageBlobForWord(row.word());
                if (now.isPresent() && now.get().equals(blob)) {
                    Notification.show("Saved. The new picture is now used for \"" + row.word() + "\". The old one is kept as a backup.");
                } else {
                    Notification.show("Saved, but another file may still take priority for this word. Please check the Module 1 page.");
                }
                dialog.close();
                refresh();
            } catch (RuntimeException ex) {
                logger.error("Could not save the picture", ex);
                Notification.show("Could not save the picture.");
            }
        });

        dialog.add(new HorizontalLayout(left, right), notes, busy);
        dialog.getFooter().add(close, make, keep);
        dialog.open();
    }

    /** One word in a batch: its current picture, the new one when it arrives, and the tick to keep it. */
    private static final class Card {
        final Row row;
        final String ext;
        final ImageResizer.Size size;
        final Div result = new Div(new Paragraph("Waiting…"));
        final Checkbox keep = new Checkbox("Use this picture");
        volatile byte[] bytes;

        Card(Row row, String ext, ImageResizer.Size size) {
            this.row = row;
            this.ext = ext;
            this.size = size;
            keep.setEnabled(false);
        }
    }

    /** Makes a picture for each selected word, three at a time, and lets the admin tick the ones to keep. Nothing is saved until then. */
    private void openBatch(List<Row> selected) {
        List<Row> rows = selected.stream().filter(r -> !r.savedAddress()).limit(MAX_BATCH).toList();
        int skipped = selected.size() - rows.size();
        if (rows.isEmpty()) {
            Notification.show("None of the selected words can be changed here (they have a fixed picture address).");
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(rows.size() + " new picture" + (rows.size() == 1 ? "" : "s"));
        dialog.setWidth("94vw");
        dialog.setMaxWidth("900px");
        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        List<Card> cards = new ArrayList<>();
        for (Row row : rows) {
            Optional<byte[]> oldBytes = row.current() == null ? Optional.empty() : media.read(AssessmentMediaService.IMAGE_CONTAINER, row.current());
            ImageResizer.Size size = oldBytes.map(ImageResizer::sizeOf).orElse(null);
            String ext = row.current() != null && row.current().toLowerCase(Locale.ROOT).matches(".*\\.jpe?g$") ? "jpg" : "png";
            Card card = new Card(row, ext, size);
            cards.add(card);
            Div oldBox = new Div();
            if (row.current() != null) {
                Image old = new Image(MediaUrls.forBlob("image", AssessmentMediaService.IMAGE_CONTAINER, row.current()), "current");
                old.setMaxHeight("150px");
                old.setMaxWidth("100%");
                oldBox.add(new Paragraph("Current"), old);
            }
            VerticalLayout word = new VerticalLayout(new Paragraph(row.word()), card.keep);
            word.setPadding(false);
            HorizontalLayout line = new HorizontalLayout(word, oldBox, card.result);
            line.setWidthFull();
            line.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.START);
            list.add(line);
        }
        Paragraph note = new Paragraph("Meta is making the pictures, a few at a time. Tick the ones you like, then press Use ticked pictures. "
                + "Nothing is saved before that."
                + (skipped > 0 ? " " + skipped + " selected word(s) with a fixed picture address were left out." : ""));
        ProgressBar busy = new ProgressBar();
        busy.setIndeterminate(true);
        Button save = new Button("Use ticked pictures");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setEnabled(false);
        Button tickAll = new Button("Tick all", e -> cards.stream().filter(c -> c.bytes != null).forEach(c -> c.keep.setValue(true)));
        tickAll.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button close = new Button("Close", e -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.add(note, busy, list);
        dialog.getFooter().add(close, tickAll, save);
        dialog.open();

        UI ui = UI.getCurrent();
        AtomicInteger pending = new AtomicInteger(cards.size());
        ui.setPollInterval(1000);
        String styleText = style.getValue();
        for (Card card : cards) {
            CompletableFuture.supplyAsync(() -> {
                ui.access(() -> {
                    card.result.removeAll();
                    card.result.add(new Paragraph("Asking Meta…"));
                });
                return images.generate(prompt(styleText, card.row.word(), ""));
            }, POOL).whenComplete((result, error) -> ui.access(() -> {
                card.result.removeAll();
                if (error != null || result == null || !result.ok()) {
                    card.result.add(new Paragraph("No picture: " + (result == null ? "something went wrong" : result.error()
                            + (result.httpStatus() > 0 ? " (HTTP " + result.httpStatus() + ")" : ""))));
                } else {
                    try {
                        int w = card.size == null ? 800 : card.size.width();
                        int h = card.size == null ? 800 : card.size.height();
                        card.bytes = ImageResizer.fit(result.bytes(), w, h, card.ext);
                        Image img = new Image("data:image/" + ("jpg".equals(card.ext) ? "jpeg" : "png") + ";base64,"
                                + java.util.Base64.getEncoder().encodeToString(card.bytes), "new");
                        img.setMaxHeight("150px");
                        img.setMaxWidth("100%");
                        card.result.add(new Paragraph("New"), img);
                        card.keep.setEnabled(media.canWritePictures());
                    } catch (IOException ex) {
                        card.result.add(new Paragraph("The picture could not be read."));
                    }
                }
                if (pending.decrementAndGet() == 0) {
                    ui.setPollInterval(-1);
                    busy.setVisible(false);
                    save.setEnabled(media.canWritePictures());
                }
            }));
        }
        save.addClickListener(e -> {
            int saved = 0;
            int failed = 0;
            for (Card card : cards) {
                if (card.keep.getValue() && card.bytes != null) {
                    try {
                        media.savePicture(card.row.word(), card.bytes, card.ext);
                        saved++;
                    } catch (RuntimeException ex) {
                        logger.error("Could not save the picture of {}", card.row.word(), ex);
                        failed++;
                    }
                }
            }
            Notification.show(saved + " picture" + (saved == 1 ? "" : "s") + " saved" + (failed > 0 ? ", " + failed + " could not be saved" : "")
                    + ". The old pictures are kept as backups.");
            dialog.close();
            grid.deselectAll();
            refresh();
        });
    }

    static String prompt(String style, String word, String notes) {
        String n = notes == null || notes.isBlank() ? "" : " " + notes.trim();
        return style.trim() + " The subject is: \"" + word + "\"." + n;
    }

    private void confirmUndo(Row row) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Use the earlier picture again?");
        confirm.setText("The picture made here for \"" + row.word() + "\" is removed, and the earlier one is used again.");
        confirm.setCancelable(true);
        confirm.setConfirmText("Use the earlier picture");
        confirm.addConfirmListener(e -> {
            try {
                media.removeAdminPicture(row.current());
                refresh();
            } catch (RuntimeException ex) {
                logger.error("Could not remove the picture", ex);
                Notification.show("Could not remove the picture.");
            }
        });
        confirm.open();
    }
}
