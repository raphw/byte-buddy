package net.bytebuddy.test.utility;

import net.bytebuddy.agent.VirtualMachine;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

public class AttachmentEmulationRule implements MethodRule {

    private final boolean enabled;

    public AttachmentEmulationRule() {
        boolean enabled;
        try {
            VirtualMachine.Resolver.INSTANCE.run();
            enabled = true;
        } catch (UnsupportedOperationException ignored) {
            enabled = false;
        }
        this.enabled = enabled;
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
            Logger.getLogger("net.bytebuddy").warning("Cannot emulate attach API on this machine");
        }
    }
}

