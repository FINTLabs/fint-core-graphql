package no.fintlabs.exception;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.exception.exceptions.FintGraphQLException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class FintDataFetcherExceptionHandler implements DataFetcherExceptionHandler {

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
            DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        CorePrincipal corePrincipal = handlerParameters.getDataFetchingEnvironment().getGraphQlContext().get(CorePrincipal.class);

        logException(exception, corePrincipal);

        return CompletableFuture.completedFuture(exceptionHandlerResult(
                exception,
                handlerParameters.getSourceLocation(),
                handlerParameters.getPath())
        );
    }

    private DataFetcherExceptionHandlerResult exceptionHandlerResult(Throwable exception,
                                                                     SourceLocation sourceLocation,
                                                                     ResultPath resultPath) {
        return DataFetcherExceptionHandlerResult.newResult(getError(exception, sourceLocation, resultPath)).build();
    }

    private GraphQLError getError(Throwable exception, SourceLocation sourceLocation, ResultPath resultPath) {
        return GraphQLError.newError()
                .message(exception.getMessage())
                .location(sourceLocation)
                .path(resultPath)
                .errorType(ErrorClassification.errorClassification(exception.getClass().getSimpleName()))
                .build();
    }

    private void logException(Throwable exception, CorePrincipal corePrincipal) {
        if (exception instanceof FintGraphQLException || exception instanceof HttpClientErrorException) {
            log.error("{}: {}", corePrincipal.getUsername(), exception.getMessage());
        } else {
            log.error("An unexpected exception occurred: ", exception);
        }
    }

}
