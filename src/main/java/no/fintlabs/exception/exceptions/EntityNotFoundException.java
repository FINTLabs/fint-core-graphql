package no.fintlabs.exception.exceptions;

public class EntityNotFoundException extends RuntimeException implements FintGraphQLException {

    public EntityNotFoundException() {
        super("Entity not found");
    }

}
