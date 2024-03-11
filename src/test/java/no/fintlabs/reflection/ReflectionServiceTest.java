package no.fintlabs.reflection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionServiceTest {

    @Test
    void getFintObjectExists() {
        ReflectionService service = new ReflectionService();
        assertDoesNotThrow(() -> service.getFintObject("no.fint.model.utdanning.elev.Elev"));
    }

    @Test
    void getFintObjectDoesNotExist() {
        ReflectionService service = new ReflectionService();
        Exception exception = assertThrows(RuntimeException.class, () -> service.getFintObject("NonExistentName"));
        assertTrue(exception.getMessage().contains("Could not find FintObject with name"));
    }

}