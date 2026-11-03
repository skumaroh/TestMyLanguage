package com.infotech.lang.application.handwriting;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;
import java.util.Base64;

/** A pad to draw one character on. New code for this app; see {@code frontend/components/drawing-pad.js}. */
@Tag("drawing-pad")
@JsModule("./components/drawing-pad.js")
public class DrawingPad extends Component {

    public void clearPad() {
        getElement().callJsFunction("clearPad");
    }

    /** Asks the browser to send the drawing; the answer arrives as a {@link DrawingExportedEvent}. */
    public void exportDrawing() {
        getElement().callJsFunction("exportDrawing");
    }

    public Registration addDrawingExportedListener(ComponentEventListener<DrawingExportedEvent> listener) {
        return addListener(DrawingExportedEvent.class, listener);
    }

    @DomEvent("drawing-exported")
    public static class DrawingExportedEvent extends ComponentEvent<DrawingPad> {

        private final String dataUrl;
        private final boolean blank;
        private final int strokeCount;

        public DrawingExportedEvent(DrawingPad source, boolean fromClient,
                                    @EventData("event.detail.dataUrl") String dataUrl,
                                    @EventData("event.detail.blank") boolean blank,
                                    @EventData("event.detail.strokeCount") int strokeCount) {
            super(source, fromClient);
            this.dataUrl = dataUrl;
            this.blank = blank;
            this.strokeCount = strokeCount;
        }

        public boolean isBlank() {
            return blank;
        }

        public int getStrokeCount() {
            return strokeCount;
        }

        /** The PNG bytes, or null if the browser sent nothing usable. */
        public byte[] png() {
            if (dataUrl == null) {
                return null;
            }
            int comma = dataUrl.indexOf(',');
            if (!dataUrl.startsWith("data:image/png;base64,") || comma < 0) {
                return null;
            }
            try {
                return Base64.getDecoder().decode(dataUrl.substring(comma + 1));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
