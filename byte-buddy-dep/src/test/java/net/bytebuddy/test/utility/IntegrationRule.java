package net.bytebuddy.test.utility;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.logging.Logger;

public class IntegrationRule implements MethodRule {

    private static final String PROPERTY_KEY = "net.bytebuddy.test.integration";

    private final boolean integration;

    public IntegrationRule() {
        integration = Boolean.getBoolean(PROPERTY_KEY);
    }

    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return !integration && method.getAnnotation(Enforce.class) != null
                ? new NoOpStatement()
                : base;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enforce {
        /* empty */
    }

    private static class NoOpStatement extends Statement {

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").warning("Ignored test case that is only to be run on the CI server due to long runtime");
        }
    }
}
