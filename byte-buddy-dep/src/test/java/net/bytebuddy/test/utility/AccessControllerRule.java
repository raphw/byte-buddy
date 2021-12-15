package net.bytebuddy.test.utility;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

/**
 * A rule that only executes tests if the access controller is available and
 * if
 */
public class AccessControllerRule implements MethodRule {

    private final boolean available;

    private final boolean enabled;

    public AccessControllerRule() {
        boolean available, enabled;
        try {
            Class.forName("java.security.AccessController", false, null);
            available = true;
            enabled = Boolean.parseBoolean(System.getProperty("net.bytebuddy.securitymanager", "true"));
        } catch (ClassNotFoundException ignored) {
            available = false;
            enabled = false;
        }
        this.available = available;
        this.enabled = enabled;
    }

    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return enabled || method.getAnnotation(Enforce.class) == null || available && method.getAnnotation(Enforce.class).force()
                ? base
                : new NoOpStatement();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Enforce {

        boolean force() default false;
    }

    private static class NoOpStatement extends Statement {

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").info("Omitting test case: Requires access controller API");
        }
    }
}
