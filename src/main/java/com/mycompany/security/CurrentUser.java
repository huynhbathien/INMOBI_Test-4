package com.mycompany.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Annotation to inject the current authenticated user into controller/service
 * methods
 * Usage: public void someMethod(@CurrentUser
 * CustomUserDetailsService.CustomUserDetails user)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {
}
