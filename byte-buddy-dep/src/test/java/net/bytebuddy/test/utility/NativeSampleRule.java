package net.bytebuddy.test.utility;

import net.bytebuddy.test.c.NativeSample;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.logging.Logger;

public class NativeSampleRule implements MethodRule {

    private final String error;

    public NativeSampleRule() {
        String error;
        try {
            int result = new NativeSample().foo(42, 2);
            if (result != 84) {
                throw new IllegalStateException("Unexptected result: " + result);
            }
            error = null;
        } catch (Throwable throwable) {
            if (throwable instanceof ExceptionInInitializerError) {
                throwable = ((ExceptionInInitializerError) throwable).getException();
            }
            error = throwable.getMessage() == null
                    ? "failed initialization - " + throwable.getClass().getName()
                    : throwable.getMessage();
        }
        this.error = error;
    }

    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return error != null && method.getAnnotation(Enforce.class) != null
                ? new NoOpStatement(error)
                : base;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enforce {
        /* empty */
    }

    private static class NoOpStatement extends Statement {

        private final String error;

        private NoOpStatement(String error) {
            this.error = error;
        }

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").info("Omitting test case that requires the native sample class: " + error);
        }
    }
}
