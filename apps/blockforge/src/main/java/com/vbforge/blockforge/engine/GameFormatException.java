package com.vbforge.blockforge.engine;

import java.io.IOException;

/**
 * The file was readable but is not a valid {@code .blockforge} game.
 * The message is written for end users ("Invalid file: &lt;message&gt;").
 */
public class GameFormatException extends IOException {

    private static final long serialVersionUID = 1L;

    public GameFormatException(String message) {
        super(message);
    }

    public GameFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}