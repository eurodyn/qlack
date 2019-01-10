package com.eurodyn.qlack.common.exceptions;

/**
 * A generic exception representing a "does not exist" condition.
 *
 * @author EUROPEAN DYNAMICS SA
 */
public class QDoesNotExistException extends QException {

    public QDoesNotExistException() {
        super();
    }

    public QDoesNotExistException(String message) {
        super(message);
    }

    public QDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
