package no.fintlabs.exceptions;

public class MissingArgumentException extends RuntimeException implements FintGraphQLException {

    public MissingArgumentException() {
        super("Missing required argument");
    }

}
