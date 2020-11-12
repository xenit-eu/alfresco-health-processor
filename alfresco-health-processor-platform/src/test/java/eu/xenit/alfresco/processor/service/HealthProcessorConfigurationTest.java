package eu.xenit.alfresco.processor.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HealthProcessorConfigurationTest {

    private HealthProcessorConfiguration configuration = new HealthProcessorConfiguration(null);

    @Test
    public void propertyConverterScopeUpperCaseTest() {
        test( this::createScope, "ALL", ProcessorService.TransactionScope.ALL);
    }

    @Test
    public void propertyConverterScopeMixedCaseTest() {
        test(this::createScope, "aLl", ProcessorService.TransactionScope.ALL);
    }

    @Test
    public void propertyConverterScopeNullTest() {
        Assertions.assertThrows(InvalidParameterException.class, () -> {
            test(this::createScope, null, ProcessorService.TransactionScope.ALL);
        });
    }

    private ProcessorService.TransactionScope createScope(String value) {
        return configuration.createScope(value);
    }

    private <T> void test(HealthProcessorConfiguration.PropertyConverter<T> propertyConverter, String input, T expected) {
        assertEquals(propertyConverter.from(input), expected);
    }
}