package no.fintlabs.service;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CompletableFuture;

@Configuration
@Slf4j
public class CounterInstrumentation implements Instrumentation {


    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        if (parameters.getQuery() == null || !parameters.getQuery().contains("__schema")) {
            // TODO: Add logging
        }
        return CompletableFuture.completedFuture(executionResult);
    }

}
