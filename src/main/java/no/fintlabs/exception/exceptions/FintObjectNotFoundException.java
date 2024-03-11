package no.fintlabs.exception.exceptions;

public class FintObjectNotFoundException extends RuntimeException {

    public FintObjectNotFoundException(String fintObjectName) {
        super("FintObject was not found: " + fintObjectName);
    }
}
