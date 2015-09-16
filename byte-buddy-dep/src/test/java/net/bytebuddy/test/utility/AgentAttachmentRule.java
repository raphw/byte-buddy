package net.bytebuddy.test.utility;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

/**
 * This rules assures that the running JVM is a JDK JVM with an available
 * <a href="https://blogs.oracle.com/CoreJavaTechTips/entry/the_attach_api">attach API</a>.
 */
public class AgentAttachmentRule implements MethodRule {

    private final boolean available;

    public AgentAttachmentRule() {
        available = ByteBuddyAgent.AttachmentProvider.DEFAULT.attempt().isAvailable();
    }

    @Override
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

        @Override
        public void evaluate() throws Throwable {
            Logger.getAnonymousLogger().warning("Ignored test case that requires an OpenJDK installation with tools.jar");
        }
    }
}
