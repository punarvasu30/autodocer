package com.autodocer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a single server URL for the API.
 */
@Target(ElementType.ANNOTATION_TYPE) // Can only be used within other annotations
@Retention(RetentionPolicy.RUNTIME) // Must be available at runtime for reflection
public @interface ServerInfo {
    /**
     * The URL of the server (e.g., "http://localhost:8080"). Required.
     */
    String url();

    /**
     * An optional description for the server (e.g., "Development Server").
     */
    String description() default "";
}