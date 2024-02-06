package no.fintlabs.exception.exceptions;

public class CacheNotFoundException extends RuntimeException implements FintGraphQLException {

    public CacheNotFoundException() {
        super("The cache is empty, populate it before requesting data.");
    }

}
