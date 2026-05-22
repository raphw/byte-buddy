package net.bytebuddy.test.utility;

import net.bytebuddy.ClassFileVersion;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.logging.Logger;

public class JavaVersionRule implements MethodRule {

    private static final int UNDEFINED = -1;

    private final ClassFileVersion currentVersion;

    private final boolean j9;

    public JavaVersionRule() {
        currentVersion = ClassFileVersion.ofThisVm();
        j9 = System.getProperty("java.vm.name", "").toUpperCase(Locale.US).contains("J9");
    }

    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        Enforce enforce = method.getAnnotation(Enforce.class);
        if (enforce != null) {
            ClassFileVersion version;
            try {
                version = enforce.target() == void.class
                        ? currentVersion
                        : ClassFileVersion.of(enforce.target());
            } catch (IOException exception) {
                throw new AssertionError(exception);
            }
            if (j9 && !enforce.j9()) {
                return new OpenJ9Statement();
            } else if (enforce.value() != UNDEFINED && !version.isAtLeast(ClassFileVersion.ofJavaVersion(enforce.value()))) {
                return new NoOpStatement(enforce.value(), "at least", enforce.target());
            } else if (enforce.atMost() != UNDEFINED && !version.isAtMost(ClassFileVersion.ofJavaVersion(enforce.atMost()))) {
                return new NoOpStatement(enforce.atMost(), "at most", enforce.target());
            }
        }
        return base;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Enforce {

        int value() default UNDEFINED;

        int atMost() default UNDEFINED;

        boolean j9() default true;

        Class<?> target() default void.class;
    }

    private static class NoOpStatement extends Statement {

        private final int requiredVersion;

        private final String sort;

        private final Class<?> target;

        private NoOpStatement(int requiredVersion, String sort, Class<?> target) {
            this.requiredVersion = requiredVersion;
            this.sort = sort;
            this.target = target;
        }

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").info("Omitting test case: Requires a Java version " +
                    "of " + sort + " " + requiredVersion
                    + (target == void.class ? "" : (" for target " + target)));
        }
    }

    private static class OpenJ9Statement extends Statement {

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").info("Omitting test case: Test not supported on OpenJ9");
        }
    }
}
