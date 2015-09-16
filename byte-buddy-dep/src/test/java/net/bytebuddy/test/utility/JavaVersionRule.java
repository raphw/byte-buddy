package net.bytebuddy.test.utility;

import net.bytebuddy.ClassFileVersion;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

public class JavaVersionRule implements MethodRule {

    private final ClassFileVersion supportedVersion;

    public JavaVersionRule() {
        supportedVersion = ClassFileVersion.forCurrentJavaVersion();
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        Enforce enforce = method.getAnnotation(Enforce.class);
        return enforce == null || ClassFileVersion.forKnownJavaVersion(enforce.value()).compareTo(supportedVersion) <= 0
                ? base
                : new NoOpStatement(enforce.value());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Enforce {

        int value();
    }

    private class NoOpStatement extends Statement {

        private final int requiredVersion;

        public NoOpStatement(int requiredVersion) {
            this.requiredVersion = requiredVersion;
        }

        @Override
        public void evaluate() throws Throwable {
            Logger.getAnonymousLogger().warning("Ignored test case that requires a Java version of at least " + requiredVersion);
        }
    }
}
