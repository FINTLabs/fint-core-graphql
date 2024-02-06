package no.fintlabs.exception.exceptions;

public class MissingLinkException extends RuntimeException implements FintGraphQLException {
    public MissingLinkException(String resourceName) {
        super("No links available for the resource: " + resourceName);
    }
}