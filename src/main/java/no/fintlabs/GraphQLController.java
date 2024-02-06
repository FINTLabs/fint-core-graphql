package no.fintlabs;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.util.Date;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@Slf4j
public class GraphQLController {

    private final GraphQL graphQL;

    @PostMapping
    public ResponseEntity<Object> executeOperation(@AuthenticationPrincipal CorePrincipal corePrincipal,
                                                   @RequestBody Map<String, Object> request,
                                                   ServerWebExchange serverWebExchange) {
        return ResponseEntity.ok().body(getExecutionResult(request, serverWebExchange, corePrincipal).toSpecification());
    }

    private ExecutionResult getExecutionResult(Map<String, Object> request, ServerWebExchange serverWebExchange, CorePrincipal corePrincipal) {
        return graphQL.execute(getExecutionInput(request.get("query").toString(), serverWebExchange, corePrincipal));
    }

    private ExecutionInput getExecutionInput(String query, ServerWebExchange serverWebExchange, CorePrincipal corePrincipal) {
        return ExecutionInput.newExecutionInput()
                .query(query)
                .graphQLContext(getGraphQLContext(serverWebExchange, corePrincipal))
                .build();
    }

    private Map<?, Object> getGraphQLContext(ServerWebExchange serverWebExchange, CorePrincipal corePrincipal) {
        return Map.of(
                ServerWebExchange.class, serverWebExchange,
                CorePrincipal.class, corePrincipal,
                Date.class, new Date(),
                "counter", 0
        );
    }

}
