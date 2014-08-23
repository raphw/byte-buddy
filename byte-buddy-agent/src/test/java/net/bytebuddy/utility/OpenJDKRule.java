package net.bytebuddy.utility;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

public class OpenJDKRule implements MethodRule {

    public static final String JAVA_VM_NAME_PROPERTY = "java.vm.name";

    public static final String HOT_SPOT = "HotSpot";

    public static final String JAVA_HOME_PROPERTY = "java.home";

    public static final String TOOLS_JAR_LOCATION = "/../lib/tools.jar";

    private final boolean openJDK;

    public OpenJDKRule() {
        System.out.println(System.getProperty(JAVA_VM_NAME_PROPERTY));
        System.out.println(new File(System.getProperty(JAVA_HOME_PROPERTY).replace('\\', '/') + TOOLS_JAR_LOCATION).isFile());
        openJDK = System.getProperty(JAVA_VM_NAME_PROPERTY).contains(HOT_SPOT)
                && new File(System.getProperty(JAVA_HOME_PROPERTY).replace('\\', '/') + TOOLS_JAR_LOCATION).isFile();
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return openJDK || method.getAnnotation(Enforce.class) == null
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
            Logger.getAnonymousLogger().warning("Ignored test case that requires an OpenJDK installation with tools.jar");
        }
    }
}
