package no.fintlabs;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutionId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
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
    public ResponseEntity<Object> executeOperation(@RequestBody Map<String, Object> request,
                                                   ServerWebExchange serverWebExchange) {
        String query = request.get("query").toString();

        ExecutionInput build = ExecutionInput.newExecutionInput()
                .query(query)
                .graphQLContext(getGraphQLContext(serverWebExchange))
                .build();

        ExecutionResult result = graphQL.execute(build);
        return new ResponseEntity<>(result.toSpecification(), HttpStatus.OK);
    }

    private Map<?, Object> getGraphQLContext(ServerWebExchange serverWebExchange) {
        return Map.of(
                ServerWebExchange.class, serverWebExchange
        );
    }

}
