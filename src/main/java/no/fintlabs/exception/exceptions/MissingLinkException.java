package no.fintlabs.exception.exceptions;

import no.fintlabs.exception.exceptions.FintGraphQLException;

public class MissingLinkException extends RuntimeException implements FintGraphQLException {
    public MissingLinkException(String resourceName) {
        super("No links available for the resource: " + resourceName);
    }
}