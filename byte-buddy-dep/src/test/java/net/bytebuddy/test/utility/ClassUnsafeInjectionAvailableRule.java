package net.bytebuddy.test.utility;

import net.bytebuddy.dynamic.loading.ClassInjector;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.logging.Logger;

public class ClassUnsafeInjectionAvailableRule implements MethodRule {

    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return !ClassInjector.UsingUnsafe.isAvailable() && method.getAnnotation(Enforce.class) != null
                ? new NoOpStatement()
                : base;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enforce {
        /* empty */
    }

    private static class NoOpStatement extends Statement {

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").info("Ignored test case that can only be executed if class file injection via sun.misc.Unsafe is available");
        }
    }
}
