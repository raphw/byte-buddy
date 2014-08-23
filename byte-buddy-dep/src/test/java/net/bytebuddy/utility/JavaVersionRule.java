package net.bytebuddy.utility;

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

    private final boolean supportsVersion;

    public JavaVersionRule(int javaVersion) {
        supportsVersion = ClassFileVersion.forCurrentJavaVersion().compareTo(ClassFileVersion.forKnownJavaVersion(javaVersion)) >= 0;
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return supportsVersion || method.getAnnotation(Enforce.class) == null
                ? base
                : new NoOpStatement();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface Enforce {
        /* empty */
    }

    private static class NoOpStatement extends Statement {

        @Override
        public void evaluate() throws Throwable {
            Logger.getAnonymousLogger().warning("Ignored test case that requires a newer JVM version");
        }
    }
}
