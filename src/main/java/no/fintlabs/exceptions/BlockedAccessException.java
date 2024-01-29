package no.fintlabs.exceptions;

public class BlockedAccessException extends RuntimeException {

    public BlockedAccessException() {
        super("Blocked access, contact FINT for further troubleshooting");
    }

}
