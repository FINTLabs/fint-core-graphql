package no.fintlabs.service;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import no.fintlabs.exception.exceptions.BlockedAccessException;
import no.fintlabs.exception.exceptions.MissingArgumentException;
import no.fintlabs.exception.exceptions.MissingAuthorizationException;
import no.fintlabs.service.datafetcher.ContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

class ContextServiceTest {

    @Mock
    private BlacklistService blacklistService;
    @Mock
    private DataFetchingEnvironment environment;
    @Mock
    private GraphQLContext graphQLContext;
    @Mock
    private ServerWebExchange serverWebExchange;
    @Mock
    private ServerHttpRequest serverHttpRequest;

    private ContextService contextService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        contextService = new ContextService(blacklistService);
    }

    @Test
    void getFirstArgument_ReturnsFirstArgument_WhenPresent() {
        Map<String, Object> arguments = Collections.singletonMap("key", "value");
        when(environment.getArguments()).thenReturn(arguments);

        Map.Entry<String, Object> firstArgument = contextService.getFirstArgument(environment);

        assertEquals("key", firstArgument.getKey());
        assertEquals("value", firstArgument.getValue());
    }

    @Test
    void getFirstArgument_ThrowsException_WhenNoArguments() {
        when(environment.getArguments()).thenReturn(Collections.emptyMap());

        assertThrows(MissingArgumentException.class, () -> contextService.getFirstArgument(environment));
    }

    @Test
    void checkIfUserIsBlocked_ThrowsBlockedAccessException_IfUserIsBlacklisted() {
        when(environment.getGraphQlContext()).thenReturn(graphQLContext);
        when(graphQLContext.get(ServerWebExchange.class)).thenReturn(serverWebExchange);
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpRequest.getRemoteAddress()).thenReturn(new InetSocketAddress("1.2.3.4", 80));
        when(blacklistService.isBlacklisted(anyString())).thenReturn(true);

        assertThrows(BlockedAccessException.class, () -> contextService.checkIfUserIsBlocked(environment));
    }

    @Test
    void setAuthorizationValueToContext_SetsAuthorization_IfNotPresent() {
        when(environment.getGraphQlContext()).thenReturn(graphQLContext);
        when(graphQLContext.hasKey(AUTHORIZATION)).thenReturn(false);
        when(graphQLContext.get(ServerWebExchange.class)).thenReturn(serverWebExchange);
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(AUTHORIZATION, "Bearer token");
        when(serverHttpRequest.getHeaders()).thenReturn(httpHeaders);

        contextService.setAuthorizationValueToContext(environment);

        verify(graphQLContext).put(eq(AUTHORIZATION), anyString());
    }

    @Test
    void getAuthorizationValue_ReturnsAuthorizationValue_WhenPresent() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(AUTHORIZATION, "Bearer token");
        when(serverHttpRequest.getHeaders()).thenReturn(httpHeaders);

        String authValue = contextService.getAuthorizationValue(serverHttpRequest);

        assertEquals("Bearer token", authValue);
    }

    @Test
    void getAuthorizationValue_ThrowsMissingAuthorizationException_WhenNotPresent() {
        when(serverHttpRequest.getHeaders()).thenReturn(new HttpHeaders());

        assertThrows(MissingAuthorizationException.class, () -> contextService.getAuthorizationValue(serverHttpRequest));
    }

    @Test
    void getServerHttpRequest_ReturnsRequest_WhenServerWebExchangePresent() {
        when(graphQLContext.get(ServerWebExchange.class)).thenReturn(serverWebExchange);
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);

        ServerHttpRequest request = contextService.getServerHttpRequest(graphQLContext);

        assertNotNull(request);
    }

    @Test
    void getServerHttpRequest_ThrowsRuntimeException_WhenServerWebExchangeNotPresent() {
        when(graphQLContext.get(ServerWebExchange.class)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> contextService.getServerHttpRequest(graphQLContext));
    }
}
