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
        return enforce == null || enforce.type().matches(ClassFileVersion.forKnownJavaVersion(enforce.value()).compareTo(supportedVersion))
                ? base
                : new NoOpStatement(enforce.value(), enforce.type());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Enforce {

        int value();

        Type type() default Type.AT_LEAST;
    }

    private class NoOpStatement extends Statement {

        private final int requiredVersion;

        private final Type type;

        public NoOpStatement(int requiredVersion, Type type) {
            this.requiredVersion = requiredVersion;
            this.type = type;
        }

        @Override
        public void evaluate() throws Throwable {
            Logger.getAnonymousLogger().warning("Ignored test case that requires a Java version " + type.toMessageString() + " " + requiredVersion);
        }
    }

    public enum Type {

        AT_LEAST("of at least") {
            @Override
            protected boolean matches(int comparison) {
                return comparison <= 0;
            }
        },

        LESS_THEN("less than") {
            @Override
            protected boolean matches(int comparison) {
                return comparison > 0;
            }
        };

        protected abstract boolean matches(int comparison);

        private final String message;

        Type(String message) {
            this.message = message;
        }

        public String toMessageString() {
            return message;
        }
    }
}
