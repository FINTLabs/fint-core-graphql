package no.fintlabs.service;

import no.fintlabs.reflection.ReflectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EndPointServiceTest {

    private EndpointService endpointService;

    @BeforeEach
    void setUp() {
        ReflectionService reflectionService = new ReflectionService();
        endpointService = new EndpointService(reflectionService, reflectionService.getFintObjects());
    }

    @Test
    public void getEndpoint() {
        Set<String> endpoints = endpointService.getEndpoints("no.fint.model.felles.Person");
        assertTrue(endpoints.contains("/utdanning/elev/person"));
    }

}
