package eu.xenit.alfresco.processor.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class HealthProcessorConfigurationTest {

    @Spy
    private HealthProcessorConfiguration configuration;

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