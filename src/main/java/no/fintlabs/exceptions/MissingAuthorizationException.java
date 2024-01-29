package no.fintlabs.exceptions;

public class MissingAuthorizationException extends RuntimeException implements FintGraphQLException {

    public MissingAuthorizationException() {
        super("Missing required Authorization header");
    }

}
