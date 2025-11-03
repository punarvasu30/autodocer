package com.autodocer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.TYPE) // Can be applied to classes
@Retention(RetentionPolicy.RUNTIME) // Must be available at runtime
public @interface ApiServers {

    ServerInfo[] value();
}