package net.bytebuddy.utility.dispatcher;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class JavaDispatcherTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private final boolean generate;

    public JavaDispatcherTest(boolean generate) {
        this.generate = generate;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true},
                {false}
        });
    }

    @Test
    public void testConstructor() throws Exception {
        assertThat(JavaDispatcher.of(Constructor.class, null, generate).run().make(), instanceOf(Object.class));
    }

    @Test
    public void testStaticDispatcher() throws Exception {
        assertThat(JavaDispatcher.of(StaticSample.class, null, generate).run().forName(Object.class.getName()), is((Object) Object.class));
    }

    @Test
    public void testStaticAdjustedDispatcher() throws Exception {
        assertThat(JavaDispatcher.of(StaticAdjustedSample.class, null, generate).run().forName(Object.class.getName()), is((Object) Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticAdjustedIllegalDispatcher() throws Exception {
        assertThat(JavaDispatcher.of(StaticAdjustedIllegalSample.class, null, generate).run().forName(null), is((Object) Object.class));
    }

    @Test
    public void testNonStaticDispatcher() throws Exception {
        assertThat(JavaDispatcher.of(NonStaticSample.class, null, generate).run().getName(Object.class), is(Object.class.getName()));
    }

    @Test
    public void testNonStaticAdjustedDispatcher() throws Exception {
        assertThat(JavaDispatcher.of(NonStaticAdjustedSample.class, null, generate).run().getMethod(Object.class, "equals", new Class<?>[]{Object.class}),
                is(Object.class.getMethod("equals", Object.class)));
    }

    @Test(expected = IllegalStateException.class)
    public void testNonStaticAdjustedIllegalDispatcher() throws Exception {
        JavaDispatcher.of(NonStaticAdjustedIllegalSample.class, null, generate).run().getMethod(Object.class, "equals", null);
    }

    @Test
    public void testNonStaticRenamedDispatcher() throws Exception {
        assertThat(JavaDispatcher.of(NonStaticRenamedSample.class, null, generate).run().getNameRenamed(Object.class), is(Object.class.getName()));
    }

    @Test
    public void testIsInstance() throws Exception {
        IsInstanceSample sample = JavaDispatcher.of(IsInstanceSample.class, null, generate).run();
        assertThat(sample.isInstance(Object.class), is(true));
        assertThat(sample.isInstance(new Object()), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testIsInstanceIllegal() throws Exception {
        JavaDispatcher.of(IsInstanceIllegalSample.class, null, generate).run().isInstance(null);
    }

    @Test
    public void testContainer() throws Exception {
        Class<?>[] array = JavaDispatcher.of(ContainerSample.class, null, generate).run().toArray(1);
        assertThat(array.length, is(1));
        assertThat(array[0], nullValue(Class.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testContainerIllegal() throws Exception {
        JavaDispatcher.of(IllegalContainerSample.class, null, generate).run().toArray(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonExistentType() throws Exception {
        JavaDispatcher.of(NonExistentTypeSample.class, null, generate).run().foo();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonExistentMethod() throws Exception {
        JavaDispatcher.of(NonExistentMethodSample.class, null, generate).run().foo();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonInterface() throws Exception {
        JavaDispatcher.of(Object.class, null, generate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonAnnotated() throws Exception {
        JavaDispatcher.of(Runnable.class, null, generate);
    }

    @Test(expected = IOException.class)
    public void testDeclaredException() throws Exception {
        File file = mock(File.class);
        when(file.getCanonicalPath()).thenThrow(new IOException());
        JavaDispatcher.of(DeclaredExceptionSample.class, null, generate).run().getCanonicalPath(file);
    }

    @Test(expected = IllegalStateException.class)
    public void testUndeclaredException() throws Exception {
        JavaDispatcher.of(UndeclaredExceptionSample.class, null, generate).run().getCanonicalPath(mock(File.class));
    }

    @Test
    public void testProxy() {
        assertThat(Proxy.isProxyClass(JavaDispatcher.of(StaticSample.class, null, generate).run().getClass()), is(!generate));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJavaSecurity() {
        JavaDispatcher.of(ProtectionDomain.class, null, generate);
    }

    @JavaVersionRule.Enforce(9)
    @Test(expected = UnsupportedOperationException.class)
    public void testMethodHandleLookup() {
        JavaDispatcher.of(MethodHandles.class, null, generate).run().lookup();
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testCallerSeparateClassLoader() throws Exception {
        Class<?> caller = JavaDispatcher.of(JavaDispatcherCaller.class, JavaDispatcherTest.class.getClassLoader(), generate).run().caller();
        assertThat(caller.getClassLoader(), not(sameInstance(JavaDispatcher.class.getClassLoader())));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testCallerInheritsPermissions() throws Exception {
        Class<?> caller = JavaDispatcher.of(JavaDispatcherCaller.class, JavaDispatcherTest.class.getClassLoader(), generate).run().caller();
        PermissionCollection permissions = caller.getProtectionDomain().getPermissions();
        Enumeration<Permission> enumeration = JavaDispatcher.class.getProtectionDomain().getPermissions().elements();
        while (enumeration.hasMoreElements()) {
            assertThat(permissions.implies(enumeration.nextElement()), is(true));
        }
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testCallerDoesNotEscalatePermissions() throws Exception {
        Class<?> caller = JavaDispatcher.of(JavaDispatcherCaller.class, JavaDispatcherTest.class.getClassLoader(), generate).run().caller();
        PermissionCollection permissions = JavaDispatcher.class.getProtectionDomain().getPermissions();
        Enumeration<Permission> enumeration = caller.getProtectionDomain().getPermissions().elements();
        while (enumeration.hasMoreElements()) {
            assertThat(permissions.implies(enumeration.nextElement()), is(true));
        }
    }

    @Test
    public void testDynamicClassLoaderResolverType() throws Exception {
        Field resolver = JavaDispatcher.class.getDeclaredField("RESOLVER");
        resolver.setAccessible(true);
        assertThat(resolver.get(null), instanceOf(ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V9)
                ? JavaDispatcher.DynamicClassLoader.Resolver.ForModuleSystem.class
                : JavaDispatcher.DynamicClassLoader.Resolver.NoOp.class));
    }

    @SuppressWarnings({"unchecked", "unused"})
    public static Class<?> caller() throws Exception {
        Class<?> type = Class.forName("java.lang.StackWalker");
        Class<?> option = Class.forName("java.lang.StackWalker$Option");
        return (Class<?>) type.getMethod("getCallerClass").invoke(type.getMethod("getInstance", option).invoke(
                null,
                Enum.valueOf((Class) option, "RETAIN_CLASS_REFERENCE")));
    }

    @JavaDispatcher.Proxied("java.lang.Object")
    public interface Constructor {

        @JavaDispatcher.IsConstructor
        Object make();
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface StaticSample {

        @JavaDispatcher.IsStatic
        Class<?> forName(String name) throws ClassNotFoundException;
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface StaticAdjustedSample {

        @JavaDispatcher.IsStatic
        Class<?> forName(@JavaDispatcher.Proxied("java.lang.String") Object name) throws ClassNotFoundException;
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface StaticAdjustedIllegalSample {

        @JavaDispatcher.IsStatic
        Class<?> forName(@JavaDispatcher.Proxied("java.lang.String") Void name);
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface NonStaticSample {

        String getName(Class<?> target);
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface NonStaticAdjustedSample {

        Method getMethod(Class<?> target,
                         @JavaDispatcher.Proxied("java.lang.String") Object name,
                         @JavaDispatcher.Proxied("java.lang.Class") Object[] argument) throws Exception;
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface NonStaticAdjustedIllegalSample {

        Method getMethod(Class<?> target,
                         @JavaDispatcher.Proxied("java.lang.String") Object name,
                         @JavaDispatcher.Proxied("java.lang.Class") Void[] argument);
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface NonStaticRenamedSample {

        @JavaDispatcher.Proxied("getName")
        String getNameRenamed(Class<?> target);
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface IsInstanceSample {

        @JavaDispatcher.Instance
        boolean isInstance(Object target);
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface IsInstanceIllegalSample {

        @JavaDispatcher.Instance
        boolean isInstance(Void target);
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface ContainerSample {

        @JavaDispatcher.Container
        Class<?>[] toArray(int arity);
    }

    @JavaDispatcher.Proxied("java.lang.Class")
    public interface IllegalContainerSample {

        @JavaDispatcher.Container
        Class<?>[] toArray(Void arity);
    }

    @JavaDispatcher.Proxied("does.not.Exist")
    public interface NonExistentTypeSample {

        void foo();
    }

    @JavaDispatcher.Proxied("java.lang.Object")
    public interface NonExistentMethodSample {

        void foo();
    }

    @JavaDispatcher.Proxied("java.io.File")
    public interface DeclaredExceptionSample {

        String getCanonicalPath(Object target) throws IOException;
    }

    @JavaDispatcher.Proxied("java.io.File")
    public interface UndeclaredExceptionSample {

        String getCanonicalPath(Object target);
    }

    @JavaDispatcher.Proxied("java.lang.invoke.MethodHandles")
    public interface MethodHandles {

        @JavaDispatcher.IsStatic
        Object lookup();
    }

    @JavaDispatcher.Proxied("net.bytebuddy.utility.dispatcher.JavaDispatcherTest")
    public interface JavaDispatcherCaller {

        @JavaDispatcher.IsStatic
        Class<?> caller() throws Exception;
    }
}