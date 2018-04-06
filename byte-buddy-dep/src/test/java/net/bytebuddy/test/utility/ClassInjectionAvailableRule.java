package net.bytebuddy.test.utility;

import net.bytebuddy.dynamic.loading.ClassInjector;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.logging.Logger;

public class ClassInjectionAvailableRule implements MethodRule {

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return !ClassInjector.UsingReflection.isAvailable() && method.getAnnotation(Enforce.class) != null
                ? new NoOpStatement()
                : base;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enforce {
        /* empty */
    }

    private static class NoOpStatement extends Statement {

        @Override
        public void evaluate() {
            Logger.getLogger("net.bytebuddy").info("Ignored test case that can only be executed if class file injection is available");
        }
    }
}
