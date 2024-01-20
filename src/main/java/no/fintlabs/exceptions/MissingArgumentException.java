package no.fintlabs.exceptions;

public class MissingArgumentException extends RuntimeException {

    public MissingArgumentException() {
        super("Missing required argument");
    }

}
