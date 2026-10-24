package com.vbforge.slidecraft.engine;

import java.io.IOException;

/**
 * The file was readable but is not a valid SlideCraft presentation. The message is written for
 * end users ("Invalid file: &lt;message&gt;").
 */
public class PresentationFormatException extends IOException {

    private static final long serialVersionUID = 1L;

    public PresentationFormatException(String message) {
        super(message);
    }

    public PresentationFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
