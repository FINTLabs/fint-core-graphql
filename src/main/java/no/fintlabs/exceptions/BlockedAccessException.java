package no.fintlabs.exceptions;

public class BlockedAccessException extends RuntimeException implements FintGraphQLException {

    public BlockedAccessException() {
        super("Blocked access, contact FINT for further troubleshooting");
    }

}
