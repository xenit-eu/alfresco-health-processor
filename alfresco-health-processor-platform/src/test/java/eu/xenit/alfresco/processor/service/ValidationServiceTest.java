package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.modules.NodeValidator;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ValidationServiceTest {
    private final NodeRef nodeRefA =
            new NodeRef("workspace://SpacesStore/a88561b3-c631-44cb-a883-180c6107a60b");
    private final NodeRef nodeRefB =
            new NodeRef("workspace://SpacesStore/b88561b3-c631-44cb-a883-180c6107a60b");

    @Mock
    private NodeValidator validator;

    @Mock
    private NodeValidator secondaryValidator;

    @Test
    public void invokeValidationTest(){
        final List<NodeRef> nodeRefs = Arrays.asList(nodeRefA, nodeRefB);
        final AtomicBoolean executed = new AtomicBoolean(false);
        new ValidationService(createEternalOptimisticValidatorsList(executed))
                .validate(nodeRefs);
        assertTrue(executed.get());
    }

    @Test
    public void shortCircuitValidationTest(){
        final List<NodeRef> nodeRefs = Arrays.asList(nodeRefA, nodeRefB);
        final AtomicBoolean validatorExecuted = new AtomicBoolean(false);
        when(validator.validate(nodeRefA))
                .thenAnswer(invocation -> {
                    validatorExecuted.set(true);
                    return false;
                });
        final List<NodeValidator> validators = Arrays.asList(validator, secondaryValidator);
        new ValidationService(validators)
                .validate(nodeRefs);

        // Assert that nodeRefA failed to validate and sit short-circuited the remaining validators
        // validator should hit, but not secondaryValidator
        // No validation required for secondaryValidator, since it's a mock and we didn't define any expected action
        // If anything is called on secondaryValidator, it will throw an exception and fail this test!
        assertTrue(validatorExecuted.get());
    }

    @Test
    public void invokeValidationWithNoNodeRefsTest(){
        assertThrows(InvalidParameterException.class, () -> {
            new ValidationService(new ArrayList<>())
                    .validate(new ArrayList<>());
        });
    }

    @Test
    public void invokeValidationWithNullNodeRefsTest(){
        assertThrows(InvalidParameterException.class, () -> {
            new ValidationService(new ArrayList<>())
                    .validate(null);
        });
    }

    @Test
    public void invokeValidationWithNoValidatorsTest(){
        final List<NodeRef> nodeRefs = Arrays.asList(nodeRefA, nodeRefB);
        new ValidationService(new ArrayList<>())
                .validate(nodeRefs);
        // Gracefully aborted
        assertTrue(true);
    }

    @Test
    public void invokeValidationWithNullValidatorsTest(){
        final List<NodeRef> nodeRefs = Arrays.asList(nodeRefA, nodeRefB);
        new ValidationService(null)
                .validate(nodeRefs);
        // Gracefully aborted
        assertTrue(true);
    }

    private List<NodeValidator> createEternalOptimisticValidatorsList(final AtomicBoolean executed) {
        when(validator.validate(any(NodeRef.class)))
                .thenAnswer(invocation -> {
                    if(executed != null) {
                        executed.set(true);
                    }
                    return true;
                });
        return Collections.singletonList(validator);
    }
}
