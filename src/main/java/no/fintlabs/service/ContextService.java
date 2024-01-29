package no.fintlabs.service;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.fintlabs.exception.exceptions.BlockedAccessException;
import no.fintlabs.exception.exceptions.MissingAuthorizationException;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@RequiredArgsConstructor
public class ContextService {

    private final BlacklistService blacklistService;

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
