package no.fintlabs.exceptions;

public class MissingAuthorizationException extends RuntimeException {

    public MissingAuthorizationException() {
        super("Missing required Authorization header");
    }

}
