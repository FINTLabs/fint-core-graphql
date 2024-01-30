package no.fintlabs.exception.exceptions;

import no.fintlabs.exception.exceptions.FintGraphQLException;

public class MissingAuthorizationException extends RuntimeException implements FintGraphQLException {

    public MissingAuthorizationException() {
        super("Missing required Authorization header");
    }

}
