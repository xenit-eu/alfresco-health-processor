package eu.xenit.alfresco.healthprocessor.extensibility.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API Elements marked with this annotation are not part of the public API and should not be used by any extension.
 *
 * Some API elements are in the public API package for technical reasons, but are not part of the public API and should
 * not be used by extensions.
 *
 * A very meta-example: this annotation is not part of the public API and is only used to mark elements for
 * documentation purposes.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
@InternalUseOnly
public @interface InternalUseOnly {

}
