package net.bytebuddy.test.utility;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.management.ManagementFactory;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class DexCompilerRule implements MethodRule {

    private static volatile boolean initialized = false;

    private static volatile boolean dexCompilerOnClassPath = false;

    private static synchronized void initialize() {
        if (!initialized) {
            initialized = true;
            if (ManagementFactory.getRuntimeMXBean().getClassPath().contains("dx.jar")) {
                dexCompilerOnClassPath = true;
                return;
            }
            String directory = System.getProperty("user.dir");
            if (directory == null) {
                Logger.getAnonymousLogger().warning("Cannot find user.dir system property");
                return;
            }
            File dxJar = new File(directory, "byte-buddy-android"
                    + File.separator + "android"
                    + File.separator + "dx.jar");
            if (!dxJar.isFile()) {
                return;
            }
            try {
                ByteBuddyAgent.installOnOpenJDK().appendToSystemClassLoaderSearch(new JarFile(dxJar));
                dexCompilerOnClassPath = true;
            } catch (Exception e) {
                Logger.getAnonymousLogger().warning("Cannot append dx.jar to class path: " + e.getMessage());
            }
        }
    }

    public DexCompilerRule() {
        initialize();
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return dexCompilerOnClassPath || method.getAnnotation(Enforce.class) == null
                ? base
                : new NoOpStatement();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface Enforce {
        /* empty */
    }

    private class NoOpStatement extends Statement {

        @Override
        public void evaluate() throws Throwable {
            Logger.getAnonymousLogger().warning("Ignored test case that requires dx.jar on class path");
        }
    }
}
