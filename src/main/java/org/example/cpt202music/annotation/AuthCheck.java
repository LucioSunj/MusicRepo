package org.example.cpt202music.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation for methods
// The purpose of using this annotation is to perform user permission checks.
// If no role is required, do not use this annotation.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * Must have a specific role
     * @return
     */
    String mustRole() default "";
}
