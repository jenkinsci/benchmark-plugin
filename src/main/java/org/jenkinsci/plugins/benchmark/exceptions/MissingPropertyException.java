package org.jenkinsci.plugins.benchmark.exceptions;

/**
 * Benchmark Plugin specific Missing Property exception
 *
 * @author Daniel Mercier
 * @since 5/10/2017
 */
public class MissingPropertyException extends Exception {

    public MissingPropertyException(){ super(); }
    public MissingPropertyException(String message) { super(message); }
    public MissingPropertyException(String message, Throwable cause) { super(message, cause); }
    public MissingPropertyException(Throwable cause) { super(cause); }
}
