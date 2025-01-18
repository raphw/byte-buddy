package net.bytebuddy;

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.AsmClassReader;
import net.bytebuddy.utility.AsmClassWriter;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyTest {

    @Rule
    public JavaVersionRule javaVersionRule = new JavaVersionRule();

    @Test(expected = IllegalArgumentException.class)
    public void testEnumWithoutValuesIsIllegal() throws Exception {
        new ByteBuddy().makeEnumeration();
    }

    @Test
    public void testEnumeration() throws Exception {
        Class<?> type = new ByteBuddy()
                .makeEnumeration("foo")
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(Modifier.isPublic(type.getModifiers()), is(true));
        assertThat(type.isEnum(), is(true));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testInterface() throws Exception {
        Class<?> type = new ByteBuddy()
                .makeInterface()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(Modifier.isPublic(type.getModifiers()), is(true));
        assertThat(type.isEnum(), is(false));
        assertThat(type.isInterface(), is(true));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testAnnotation() throws Exception {
        Class<?> type = new ByteBuddy()
                .makeAnnotation()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(Modifier.isPublic(type.getModifiers()), is(true));
        assertThat(type.isEnum(), is(false));
        assertThat(type.isInterface(), is(true));
        assertThat(type.isAnnotation(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(16)
    public void testRecordWithoutMember() throws Exception {
        Class<?> type = new ByteBuddy()
                .with(ClassFileVersion.JAVA_V16)
                .makeRecord()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat((Boolean) Class.class.getMethod("isRecord").invoke(type), is(true));
        Object record = type.getConstructor().newInstance();
        assertThat(type.getMethod("hashCode").invoke(record), is((Object) 0));
        assertThat(type.getMethod("equals", Object.class).invoke(record, new Object()), is((Object) false));
        assertThat(type.getMethod("equals", Object.class).invoke(record, record), is((Object) true));
        assertThat(type.getMethod("toString").invoke(record), is((Object) (type.getSimpleName() + "[]")));
    }

    @Test
    @JavaVersionRule.Enforce(16)
    public void testRecordWithMember() throws Exception {
        Class<?> type = new ByteBuddy()
                .with(ClassFileVersion.JAVA_V16)
                .makeRecord()
                .defineRecordComponent("foo", String.class)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat((Boolean) Class.class.getMethod("isRecord").invoke(type), is(true));
        Object record = type.getConstructor(String.class).newInstance("bar");
        assertThat(type.getMethod("foo").invoke(record), is((Object) "bar"));
        assertThat(type.getMethod("hashCode").invoke(record), is((Object) "bar".hashCode()));
        assertThat(type.getMethod("equals", Object.class).invoke(record, new Object()), is((Object) false));
        assertThat(type.getMethod("equals", Object.class).invoke(record, record), is((Object) true));
        assertThat(type.getMethod("toString").invoke(record), is((Object) (type.getSimpleName() + "[foo=bar]")));
        Object[] parameter = (Object[]) Constructor.class.getMethod("getParameters").invoke(type.getDeclaredConstructor(String.class));
        assertThat(parameter.length, is(1));
        assertThat(Class.forName("java.lang.reflect.Parameter").getMethod("getName").invoke(parameter[0]), is((Object) "foo"));
        assertThat(Class.forName("java.lang.reflect.Parameter").getMethod("getModifiers").invoke(parameter[0]), is((Object) 0));
    }

    @Test
    public void testTypeInitializerInstrumentation() throws Exception {
        Recorder recorder = new Recorder();
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .invokable(isTypeInitializer())
                .intercept(MethodDelegation.to(recorder))
                .make(new TypeResolutionStrategy.Active())
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance(), instanceOf(type));
        assertThat(recorder.counter, is(1));
    }

    @Test
    public void testImplicitStrategyBootstrap() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER)
                .getLoaded();
        assertThat(type.getClassLoader(), notNullValue(ClassLoader.class));
    }

    @Test
    public void testImplicitStrategyNonBootstrap() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .make()
                .load(classLoader)
                .getLoaded();
        assertThat(type.getClassLoader(), not(classLoader));
    }

    @Test
    public void testImplicitStrategyInjectable() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, false, Collections.<String, byte[]>emptyMap());
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .make()
                .load(classLoader)
                .getLoaded();
        assertThat(type.getClassLoader(), is(classLoader));
    }

    @Test
    public void testClassWithManyMethods() throws Exception {
        DynamicType.Builder<?> builder = new ByteBuddy().subclass(Object.class);
        for (int index = 0; index < 1000; index++) {
            builder = builder.defineMethod("method" + index, void.class, Visibility.PUBLIC).intercept(StubMethod.INSTANCE);
        }
        Class<?> type = builder.make().load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getDeclaredMethods().length, is(1000));
        DynamicType.Builder<?> subclassBuilder = new ByteBuddy().subclass(type);
        for (Method method : type.getDeclaredMethods()) {
            subclassBuilder = subclassBuilder.method(ElementMatchers.is(method)).intercept(StubMethod.INSTANCE);
        }
        Class<?> subclass = subclassBuilder.make().load(type.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(subclass.getDeclaredMethods().length, is(1000));
    }

    @Test
    public void testClassCompiledToJsr14() throws Exception {
        assertThat(new ByteBuddy()
                .redefine(Class.forName("net.bytebuddy.test.precompiled.v4jsr14.Jsr14Sample"))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getConstructor()
                .newInstance(), notNullValue());
    }

    @Test
    public void testCallerSuffixNamingStrategy() throws Exception {
        Class<?> type = new ByteBuddy()
                .with(new NamingStrategy.Suffixing("SuffixedName", new NamingStrategy.Suffixing.BaseNameResolver.WithCallerSuffix(
                        new NamingStrategy.Suffixing.BaseNameResolver.ForFixedValue("foo.Bar"))))
                .subclass(Object.class)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getName(), is("foo.Bar$"
                + ByteBuddyTest.class.getName().replace('.', '$')
                + "$testCallerSuffixNamingStrategy$SuffixedName"));
    }

    @Test
    @JavaVersionRule.Enforce(24)
    public void testCanUseClassFileApiReaderAndWriter() throws Exception {
        Class<?> type = new ByteBuddy()
                .with(AsmClassReader.Factory.Default.CLASS_FILE_API_ONLY)
                .with(AsmClassWriter.Factory.Default.CLASS_FILE_API_ONLY)
                .redefine(Recorder.class)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object object = type.getConstructor().newInstance();
        type.getMethod("instrument").invoke(object);
        assertThat(type.getField("counter").get(object), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(24)
    public void testCanUseClassFileApiReaderOnly() throws Exception {
        Class<?> type = new ByteBuddy()
                .with(AsmClassReader.Factory.Default.CLASS_FILE_API_ONLY)
                .redefine(Recorder.class)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object object = type.getConstructor().newInstance();
        type.getMethod("instrument").invoke(object);
        assertThat(type.getField("counter").get(object), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(24)
    public void testCanUseClassFileApiWriterOnly() throws Exception {
        Class<?> type = new ByteBuddy()
                .with(AsmClassWriter.Factory.Default.CLASS_FILE_API_ONLY)
                .redefine(Recorder.class)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object object = type.getConstructor().newInstance();
        type.getMethod("instrument").invoke(object);
        assertThat(type.getField("counter").get(object), is((Object) 1));
    }

    public static class Recorder {

        public int counter;

        public void instrument() {
            counter++;
        }
    }
}
