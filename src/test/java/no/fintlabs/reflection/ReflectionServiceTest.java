package no.fintlabs.reflection;

import no.fintlabs.exception.exceptions.FintObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionServiceTest {

    private ReflectionService reflectionService;

    @BeforeEach
    void setUp() {
        reflectionService = new ReflectionService();
    }

    @Test
    void getFintObjectExists() {
        assertDoesNotThrow(() -> reflectionService.getFintObject("no.fint.model.utdanning.elev.Elev"));
    }

    @Test
    void getFintObjectDoesNotExist() {
        Exception exception = assertThrows(RuntimeException.class, () -> reflectionService.getFintObject("NonExistentName"));
        assertInstanceOf(FintObjectNotFoundException.class, exception);
    }

}