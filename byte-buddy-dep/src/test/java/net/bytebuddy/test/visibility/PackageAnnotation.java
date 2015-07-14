package net.bytebuddy.test.visibility;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PackageAnnotation {
    /* empty */
}
