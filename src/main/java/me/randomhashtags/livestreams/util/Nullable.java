package me.randomhashtags.livestreams.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/*
    Indicates that the target parameter/method can be/return null, and it won't cause issues being not null either
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface Nullable {
}
