package no.fintlabs.exceptions;

public class MissingLinkException extends RuntimeException {
    public MissingLinkException(String resourceName) {
        super("No links available for the resource: " + resourceName);
    }
}