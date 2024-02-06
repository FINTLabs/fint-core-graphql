package no.fintlabs.config;

import graphql.ExecutionResult;
import graphql.GraphQLContext;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

@Configuration
@Slf4j
public class CounterInstrumentation implements Instrumentation {


    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        if (parameters.getQuery() == null || !parameters.getQuery().contains("__schema")) {
            GraphQLContext graphQLContext = parameters.getGraphQLContext();
            CorePrincipal corePrincipal = graphQLContext.get(CorePrincipal.class);
            log.info("User: {}, TimeInSeconds: {}, RestCalls: {}, \nQuery: {}",
                    corePrincipal.getUsername(),
                    calculateTimeTakenInSeconds(graphQLContext.get(Date.class)),
                    graphQLContext.get("counter"),
                    filterQuery(parameters.getQuery()));
        }
        return CompletableFuture.completedFuture(executionResult);
    }

    private String filterQuery(String query) {
        String pattern = "fodselsnummer\\s*:\\s*\"\\d+\"";
        return query.replaceAll(pattern, "fodselsnummer: \"censored\"");
    }

    private long calculateTimeTakenInSeconds(Date startDate) {
        return (new Date().getTime() - startDate.getTime()) / 1000;
    }

}
