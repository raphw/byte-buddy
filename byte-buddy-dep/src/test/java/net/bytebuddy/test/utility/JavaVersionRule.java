package net.bytebuddy.test.utility;

import net.bytebuddy.ClassFileVersion;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.logging.Logger;

public class JavaVersionRule implements MethodRule {

    private final ClassFileVersion currentVersion;

    private final boolean hotSpot;

    public JavaVersionRule() {
        currentVersion = ClassFileVersion.forCurrentJavaVersion();
        hotSpot = System.getProperty("java.vm.name", "").toLowerCase(Locale.US).contains("hotspot");
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        Enforce enforce = method.getAnnotation(Enforce.class);
        if (enforce != null) {
            if (ClassFileVersion.ofJavaVersion(enforce.value()).compareTo(currentVersion) <= 0) {
                return new NoOpStatement(enforce.value());
            } else if (!hotSpot) {
                for (int javaVersion : enforce.hotSpot()) {
                    if (currentVersion.getJavaVersion() == javaVersion) {
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

        int value();

        int[] hotSpot() default {};
    }

    private static class NoOpStatement extends Statement {

        private final int requiredVersion;

        public NoOpStatement(int requiredVersion) {
            this.requiredVersion = requiredVersion;
        }

        @Override
        public void evaluate() throws Throwable {
            Logger.getAnonymousLogger().warning("Ignoring test case: Requires a Java version of at least " + requiredVersion);
        }
    }

    private static class NoOpHotSpotStatement extends Statement {

        private final int restrictedVersion;

        public NoOpHotSpotStatement(int restrictedVersion) {
            this.restrictedVersion = restrictedVersion;
        }

        @Override
        public void evaluate() throws Throwable {
            Logger.getAnonymousLogger().warning("Ignoring test case: Only works on HotSpot for Java version " + restrictedVersion);
        }
    }
}
