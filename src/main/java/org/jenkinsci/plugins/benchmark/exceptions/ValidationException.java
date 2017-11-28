package org.jenkinsci.plugins.benchmark.exceptions;

/**
 * Benchmark Plugin specific Validation exception
 *
 * @author Daniel Mercier
 * @since 5/10/2017
 */
public class ValidationException extends Exception {

    public ValidationException(){ super(); }
    public ValidationException(String message) { super(message); }
    public ValidationException(String message, Throwable cause) { super(message, cause); }
    public ValidationException(Throwable cause) { super(cause); }
}
