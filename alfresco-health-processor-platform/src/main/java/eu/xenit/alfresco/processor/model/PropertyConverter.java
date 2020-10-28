package eu.xenit.alfresco.processor.model;

import eu.xenit.alfresco.processor.service.ProcessorService;
import eu.xenit.alfresco.processor.service.ProcessorServiceImpl;

import java.security.InvalidParameterException;

public interface PropertyConverter<T> {
    PropertyConverter<String> STRING = String::toString;
    PropertyConverter<Integer> INTEGER = Integer::parseInt;
    PropertyConverter<Long> LONG = Long::parseLong;
    PropertyConverter<Boolean> BOOLEAN = Boolean::parseBoolean;
    PropertyConverter<ProcessorServiceImpl.Scope> SCOPE = value -> createScope(value);

    static ProcessorService.Scope createScope(String value) {
        if(value == null || value.length() < 1) {
            throw new InvalidParameterException("value cannot be null or empty!");
        }
        return ProcessorServiceImpl.Scope.valueOf(value.toUpperCase());
    }

    T from(String value);
}
