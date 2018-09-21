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

    private final boolean hotSpot;

    public JavaVersionRule() {
        currentVersion = ClassFileVersion.ofThisVm();
        hotSpot = System.getProperty("java.vm.name", "").toLowerCase(Locale.US).contains("hotspot");
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
            if (enforce.value() != UNDEFINED && !version.isAtLeast(ClassFileVersion.ofJavaVersion(enforce.value()))) {
                return new NoOpStatement(enforce.value(), "at least");
            } else if (enforce.atMost() != UNDEFINED && !version.isAtMost(ClassFileVersion.ofJavaVersion(enforce.atMost()))) {
                return new NoOpStatement(enforce.atMost(), "at most");
            } else if (!hotSpot) {
                for (int javaVersion : enforce.hotSpot()) {
                    if (version.getJavaVersion() == javaVersion) {
                        return new NoOpHotSpotStatement(javaVersion);
                    }
                }
            }
        }
        return base;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Enforce {

        int value() default UNDEFINED;

        int atMost() default UNDEFINED;

        int[] hotSpot() default {};

        Class<?> target() default void.class;
    }

    private static class NoOpStatement extends Statement {

        private final int requiredVersion;

        private final String sort;

        private NoOpStatement(int requiredVersion, String sort) {
            this.requiredVersion = requiredVersion;
            this.sort = sort;
        }

        public void evaluate() throws Throwable {
            Logger.getLogger("net.bytebuddy").warning("Ignoring test case: Requires a Java version of " + sort + " " + requiredVersion);
        }
    }

    private static class NoOpHotSpotStatement extends Statement {

        private final int restrictedVersion;

        private NoOpHotSpotStatement(int restrictedVersion) {
            this.restrictedVersion = restrictedVersion;
        }

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").warning("Ignoring test case: Only works on HotSpot for Java version " + restrictedVersion);
        }
    }
}