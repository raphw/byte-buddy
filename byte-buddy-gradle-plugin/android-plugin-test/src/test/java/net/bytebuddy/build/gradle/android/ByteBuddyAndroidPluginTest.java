package net.bytebuddy.build.gradle.android;

import net.bytebuddy.test.utility.BaseAndroidGradleTest;
import net.bytebuddy.test.utility.ProjectInfo;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.util.Collections.emptyList;
import static net.bytebuddy.build.gradle.android.utils.Many.listOf;
import static org.junit.Assert.assertEquals;

public class ByteBuddyAndroidPluginTest extends BaseAndroidGradleTest {

    private static final String GRADLE_VERSION = "7.3.3";

    @Test
    public void transformAndroidProjectJavaClasses() {
        ProjectInfo appProject = initAppProject("basic_app", emptyList(), listOf("bytebuddy project(':basic_jar')"));
        initJarProject("basic_jar", emptyList(), listOf(getBytebuddyImplementation()));

        buildProject(appProject, listOf("assembleDebug"));

        ClassLoader classLoader = getAppClassloader(appProject);
        verifyClassIsInstrumented(classLoader, "SomeClass");
    }

    @Test
    public void transformJavaAndKotlinClassesFromAndroidProject() {
        ProjectInfo appProject = initAppProject(
                "app_with_kotlin",
                listOf("id 'org.jetbrains.kotlin.android'"),
                listOf("bytebuddy project(':jar_with_kotlin')")
        );
        initJarProject(
                "jar_with_kotlin",
                listOf("id 'org.jetbrains.kotlin.jvm'"),
                listOf(getBytebuddyImplementation())
        );

        buildProject(appProject, listOf("assembleDebug"));

        ClassLoader classLoader = getAppClassloader(appProject);
        verifyClassIsInstrumented(classLoader, "SomeClass");
        verifyClassIsInstrumented(classLoader, "SomeKotlinClass");
    }

    @Test
    public void transformAndroidProjectJavaClassesFromAAR() {
        File basicAarFile = getAssetFile("artifacts/basic_aar-debug.aar");
        ProjectInfo appProject = initAppProject("basic_app", emptyList(), listOf(String.format("bytebuddy files('%s')", basicAarFile.getAbsolutePath())));

        buildProject(appProject, listOf("assembleDebug"));

        ClassLoader classLoader = getAppClassloader(appProject);
        verifyClassIsInstrumented(classLoader, "SomeClass");
    }

    @Test
    public void transformAndroidProjectClassesFromItselfAndItsLibraries() {
        ProjectInfo appProject = initAppProject(
                "basic_app", emptyList(), listOf(
                        "bytebuddy project(':jar_targeting_libraries')",
                        "implementation project(':dummy_target_jar')"
                )
        );
        initJarProject(
                "jar_targeting_libraries",
                emptyList(),
                listOf(getBytebuddyImplementation())
        );
        initJarProject("dummy_target_jar", emptyList(), emptyList());

        buildProject(appProject, listOf("assembleDebug"));

        ClassLoader classLoader = getAppClassloader(appProject);
        verifyClassIsInstrumented(classLoader, "SomeClass");
        verifyClassIsInstrumented(classLoader, "SomeLibClass");
    }

    @Test
    public void transformAndroidProjectJavaClassesFromAARModule() {
        ProjectInfo appProject = initAppProject("basic_app", emptyList(), listOf("bytebuddy project(':basic_aar')"));
        initAarProject("basic_aar", emptyList(), listOf(getBytebuddyImplementation()));

        buildProject(appProject, listOf("assembleDebug"));

        ClassLoader classLoader = getAppClassloader(appProject);
        verifyClassIsInstrumented(classLoader, "SomeClass");
    }

    private void verifyClassIsInstrumented(ClassLoader classLoader, String className) {
        Class<?> theClass = null;
        try {
            theClass = classLoader.loadClass(className);
            Method getMessage = theClass.getDeclaredMethod("getMessage");
            Object helloInstance = theClass.newInstance();
            String message = (String) getMessage.invoke(helloInstance);
            assertEquals("Instrumented message in lib", message);
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String getBytebuddyImplementation() {
        return String.format("implementation %s", getBytebuddyArtifactDependency());
    }

    @Override
    protected String getGradleVersion() {
        return GRADLE_VERSION;
    }
}