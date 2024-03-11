package no.fintlabs.service;

import no.fintlabs.reflection.ReflectionService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EndPointServiceTest {

    @Test
    public void getEndpoint() {
        ReflectionService reflectionService = new ReflectionService();
        EndpointService endpointService = new EndpointService(reflectionService, reflectionService.getFintObjects());
        Set<String> endpoints = endpointService.getEndpoints("no.fint.model.felles.Person");
        assertTrue(endpoints.contains("/utdanning/elev/person"));
    }

}
