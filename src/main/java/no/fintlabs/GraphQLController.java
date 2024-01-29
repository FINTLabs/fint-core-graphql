package no.fintlabs;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;


@RestController
@RequiredArgsConstructor
public class GraphQLController {

    private final GraphQL graphQL;

    @PostMapping("/graphql")
    public ResponseEntity<Object> executeOperation(@RequestBody Map<String, Object> request, ServerWebExchange serverWebExchange) {
        return ResponseEntity.ok().body(getExecutionResult(request, serverWebExchange).toSpecification());
    }

    private ExecutionResult getExecutionResult(Map<String, Object> request, ServerWebExchange serverWebExchange) {
        return graphQL.execute(getExecutionInput(request.get("query").toString(), serverWebExchange));
    }

    private ExecutionInput getExecutionInput(String query, ServerWebExchange serverWebExchange) {
        return ExecutionInput.newExecutionInput()
                .query(query)
                .graphQLContext(getGraphQLContext(serverWebExchange))
                .build();
    }

    private Map<?, Object> getGraphQLContext(ServerWebExchange serverWebExchange) {
        return Map.of(
                ServerWebExchange.class, serverWebExchange
        );
    }

}
