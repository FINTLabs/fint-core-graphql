package no.fintlabs.exception.exceptions;

public class UnexpectedErrorException extends RuntimeException implements FintGraphQLException {

    public UnexpectedErrorException() {
        super("An unhandled error occured, please contact FINT for further troubleshooting.");
    }

}
