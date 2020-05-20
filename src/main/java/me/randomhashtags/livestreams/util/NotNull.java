package me.randomhashtags.livestreams.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/*
    Indicates that the target parameter/method should never be/return null, otherwise it will cause issues
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface NotNull {
}