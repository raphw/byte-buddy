package net.bytebuddy.test.utility;

import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

/**
 * A rule that only executes tests if the access controller is available and
 * if
 */
public class AccessControllerRule implements MethodRule  {

    private final boolean available;

    public AccessControllerRule() {
        boolean available;
        try {
            Class.forName("java.security.AccessController", false, null);
            available = Boolean.parseBoolean(System.getProperty("net.bytebuddy.securitymanager", "true"));
        } catch (ClassNotFoundException ignored) {
            available = false;
        }
        this.available = available;
    }

    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return available || method.getAnnotation(Enforce.class) == null
                ? base
                : new NoOpStatement();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Enforce {
        /* empty */
    }

    private static class NoOpStatement extends Statement {

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").warning("Ignoring test case: Requires access controller API");
        }
    }
}
