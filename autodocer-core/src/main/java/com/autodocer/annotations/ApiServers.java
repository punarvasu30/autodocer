package com.autodocer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation to allow specifying multiple @ServerInfo annotations
 * on the main application class.
 */
@Target(ElementType.TYPE) // Can be applied to classes
@Retention(RetentionPolicy.RUNTIME) // Must be available at runtime
public @interface ApiServers {
    /**
     * An array of one or more ServerInfo annotations.
     */
    ServerInfo[] value();
}