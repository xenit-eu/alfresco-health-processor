package eu.xenit.alfresco.processor.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.InvalidParameterException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HealthProcessorConfigurationTest {

    private final HealthProcessorConfiguration configuration = new HealthProcessorConfiguration(null);

    @Test
    public void propertyConverterScopeUpperCaseTest() {
        test( this::createScope, "ALL");
    }

    @Test
    public void propertyConverterScopeMixedCaseTest() {
        test(this::createScope, "aLl");
    }

    @Test
    public void propertyConverterScopeNullTest() {
        Assertions.assertThrows(InvalidParameterException.class, () -> {
            test(this::createScope, null);
        });
    }

    private ProcessorService.TransactionScope createScope(String value) {
        return configuration.createScope(value);
    }

    private <T> void test(Function<String, T> convertor, String input) {
        assertEquals(convertor.apply(input), ProcessorService.TransactionScope.ALL);
    }
}