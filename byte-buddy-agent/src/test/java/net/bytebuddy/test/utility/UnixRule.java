package net.bytebuddy.test.utility;

import com.sun.jna.Platform;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

public class UnixRule implements MethodRule {

    private final boolean enabled;

    public UnixRule() {
        this.enabled = !Platform.isWindows() && !Platform.isWindowsCE() && !System.getProperty("java.vm.vendor").contains("OpenJ9");
    }

    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return enabled || method.getAnnotation(Enforce.class) == null
                ? base
                : new NoOpStatement();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Enforce {

    }

    private static class NoOpStatement extends Statement {

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").warning("Ignoring Unix sockets on this machine");
        }
    }
}

