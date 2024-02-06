package no.fintlabs.exception.exceptions;

public class ForbiddenAccessException extends RuntimeException implements FintGraphQLException {

    public ForbiddenAccessException(String endpoint) {
        super("Your client does not have access to: " + endpoint);
    }

}
