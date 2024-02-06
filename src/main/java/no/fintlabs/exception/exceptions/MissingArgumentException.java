package no.fintlabs.exception.exceptions;

public class MissingArgumentException extends RuntimeException implements FintGraphQLException {

    public MissingArgumentException() {
        super("Missing required argument");
    }

}
