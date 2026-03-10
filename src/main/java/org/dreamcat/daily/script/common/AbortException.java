package org.dreamcat.daily.script.common;

/**
 * @author Jerry Will
 * @version 2026-03-05
 */
public class AbortException extends RuntimeException {

    public AbortException(String message) {
        super(message);
    }

    public AbortException(String message, Throwable cause) {
        super(message, cause);
    }
}

