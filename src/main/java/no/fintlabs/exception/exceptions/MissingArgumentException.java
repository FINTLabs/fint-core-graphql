package no.fintlabs.exception.exceptions;

import no.fintlabs.exception.exceptions.FintGraphQLException;

public class MissingArgumentException extends RuntimeException implements FintGraphQLException {

    public MissingArgumentException() {
        super("Missing required argument");
    }

}
