package no.fintlabs.service.datafetcher;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.fintlabs.exception.exceptions.BlockedAccessException;
import no.fintlabs.exception.exceptions.MissingArgumentException;
import no.fintlabs.exception.exceptions.MissingAuthorizationException;
import no.fintlabs.service.BlacklistService;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@RequiredArgsConstructor
public class ContextService {

    private final BlacklistService blacklistService;

    public Map.Entry<String, Object> getFirstArgument(DataFetchingEnvironment environment) {
        return environment.getArguments().entrySet().stream()
                .findFirst()
                .orElseThrow(MissingArgumentException::new);
    }

    public void checkIfUserIsBlocked(DataFetchingEnvironment environment) {
        ServerHttpRequest serverHttpRequest = getServerHttpRequest(environment.getGraphQlContext());
        if (blacklistService.isBlacklisted(serverHttpRequest.getRemoteAddress().getAddress().getHostAddress())) {
            throw new BlockedAccessException();
        }
    }

    public void setAuthorizationValueToContext(DataFetchingEnvironment environment) {
        if (environment.getGraphQlContext().hasKey(AUTHORIZATION)) {
            return;
        }
        environment.getGraphQlContext().put(AUTHORIZATION,
                getAuthorizationValue(getServerHttpRequest(environment.getGraphQlContext()))
        );
    }

    public String getAuthorizationValue(ServerHttpRequest serverHttpRequest) {
        return Optional.ofNullable(serverHttpRequest.getHeaders().get(AUTHORIZATION))
                .map(List::getFirst)
                .orElseThrow(MissingAuthorizationException::new);
    }

    public ServerHttpRequest getServerHttpRequest(GraphQLContext ctx) {
        return Optional.ofNullable(ctx.<ServerWebExchange>get(ServerWebExchange.class))
                .map(ServerWebExchange::getRequest)
                .orElseThrow(() -> new RuntimeException("ServerWebExchange not found in GraphQLContext"));
    }

}
