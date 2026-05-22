package net.bytebuddy.test.utility;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

public class UnsafeAccessRule implements MethodRule {

    private static final String SAFE_PROPERTY = "net.bytebuddy.safe";

    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        Enforce enforce = method.getAnnotation(Enforce.class);
        if (enforce != null && !"false".equalsIgnoreCase(System.getProperty(SAFE_PROPERTY))) {
            return new NoOpStatement();
        }
        return base;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Enforce {
    }

    private static class NoOpStatement extends Statement {

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").info("Omitting test case: Requires "
                    + SAFE_PROPERTY + "=false to be set");
        }
    }
}
