package no.fintlabs.exception.exceptions;

public class MissingAuthorizationException extends RuntimeException implements FintGraphQLException {

    public MissingAuthorizationException() {
        super("Missing required Authorization header");
    }

}
