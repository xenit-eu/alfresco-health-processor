package eu.xenit.alfresco.healthprocessor.extensibility.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API elements marked with this annotation should only be used in the indicated extension types.
 *
 * Some API elements are only designed and tested to be used within the context of a certain extension type. This marker
 * annotation makes the usage assumptions explicit and indicates which extension type(s) are expected to use a certain
 * method, field or constructor.
 *
 * There are no technical limitations derived by the use of this annotation, it only serves an advisory purpose.
 *
 * @see NotForUseIn
 */
@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@InternalUseOnly
public @interface OnlyForUseIn {

    /**
     * @return Extension types in which usage of the marked API element is supported
     */
    ExtensionType[] value();
}
