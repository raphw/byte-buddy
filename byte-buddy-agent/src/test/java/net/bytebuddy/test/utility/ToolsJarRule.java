package net.bytebuddy.test.utility;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.Logger;

/**
 * This rules assures that the running JVM is a JDK JVM with an existing
 * <a href="https://blogs.oracle.com/CoreJavaTechTips/entry/the_attach_api">'tools.jar'</a>.
 */
public class ToolsJarRule implements MethodRule {

    public static final String JAVA_HOME_PROPERTY = "java.home";

    public static final String TOOLS_JAR_LOCATION = "/../lib/tools.jar";

    private final boolean toolsJarExists;

    public ToolsJarRule() {
        toolsJarExists = new File(System.getProperty(JAVA_HOME_PROPERTY).replace('\\', '/') + TOOLS_JAR_LOCATION).isFile();
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return toolsJarExists || method.getAnnotation(Enforce.class) == null
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
