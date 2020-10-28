package eu.xenit.alfresco.processor.model;

import eu.xenit.alfresco.processor.service.ProcessorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PropertyConverterTest {
    @Test
    public void propertyConverterStringTest() {
        String sample = "sample";
        test(PropertyConverter.STRING, sample, sample);
    }

    @Test
    public void propertyConverterIntegerTest() {
        test(PropertyConverter.INTEGER, "123", 123);
    }

    @Test
    public void propertyConverterLongTest() {
        test(PropertyConverter.LONG, "123", 123L);
    }

    @Test
    public void propertyConverterBooleanMixedCaseTest() {
        test(PropertyConverter.BOOLEAN, "tRUe", true);
    }

    @Test
    public void propertyConverterScopeUpperCaseTest() {
        test(PropertyConverter.SCOPE, "ALL", ProcessorService.Scope.ALL);
    }

    @Test
    public void propertyConverterScopeMixedCaseTest() {
        test(PropertyConverter.SCOPE, "aLl", ProcessorService.Scope.ALL);
    }

    @Test
    public void propertyConverterScopeNullTest() {
        Assertions.assertThrows(InvalidParameterException.class, () -> {
            test(PropertyConverter.SCOPE, null, ProcessorService.Scope.ALL);
        });
    }

    private <T> void test(PropertyConverter<T> propertyConverter, String input, T expected) {
        assertEquals(propertyConverter.from(input), expected);
    }
}