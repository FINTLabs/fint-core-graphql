package no.fintlabs.exception.exceptions;

public class CacheNotFoundException extends RuntimeException implements FintGraphQLException {

    public CacheNotFoundException(String endpoint) {
        super("The cache is empty at: " + endpoint + "\n Populate the cache before requesting data.");
    }

}
