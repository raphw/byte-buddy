package net.bytebuddy;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.StubMethod;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.PrecompiledTypeClassLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ByteBuddyParameterRetentionTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String PARAMETER_NAME_CLASS = "net.bytebuddy.test.precompiled.ParameterNames";

    private static final Object STATIC_FIELD = null;

    private static final String INTERFACE_STATIC_FIELD_NAME = "FOO";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testParameterMetaDataSubclassForLoaded() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(classLoader.loadClass(PARAMETER_NAME_CLASS))
                .method(named(FOO)).intercept(StubMethod.INSTANCE)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethods().length, is(1));
        Class<?> executable = Class.forName("java.lang.reflect.Executable");
        Method getParameters = executable.getDeclaredMethod("getParameters");
        Class<?> parameter = Class.forName("java.lang.reflect.Parameter");
        Method getName = parameter.getDeclaredMethod("getName");
        Method getModifiers = parameter.getDeclaredMethod("getModifiers");
        Method first = dynamicType.getDeclaredMethod("foo", String.class, long.class, int.class);
        Object[] methodParameter = (Object[]) getParameters.invoke(first);
        assertThat(getName.invoke(methodParameter[0]), is((Object) "first"));
        assertThat(getName.invoke(methodParameter[1]), is((Object) "second"));
        assertThat(getName.invoke(methodParameter[2]), is((Object) "third"));
        assertThat(getModifiers.invoke(methodParameter[0]), is((Object) Opcodes.ACC_FINAL));
        assertThat(getModifiers.invoke(methodParameter[1]), is((Object) 0));
        assertThat(getModifiers.invoke(methodParameter[2]), is((Object) 0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testParameterMetaDataRedefinitionForLoaded() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .redefine(classLoader.loadClass(PARAMETER_NAME_CLASS))
                .method(named(FOO)).intercept(StubMethod.INSTANCE)
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethods().length, is(2));
        Class<?> executable = Class.forName("java.lang.reflect.Executable");
        Method getParameters = executable.getDeclaredMethod("getParameters");
        Class<?> parameter = Class.forName("java.lang.reflect.Parameter");
        Method getName = parameter.getDeclaredMethod("getName");
        Method getModifiers = parameter.getDeclaredMethod("getModifiers");
        Method first = dynamicType.getDeclaredMethod("foo", String.class, long.class, int.class);
        Object[] methodParameter = (Object[]) getParameters.invoke(first);
        assertThat(getName.invoke(methodParameter[0]), is((Object) "first"));
        assertThat(getName.invoke(methodParameter[1]), is((Object) "second"));
        assertThat(getName.invoke(methodParameter[2]), is((Object) "third"));
        assertThat(getModifiers.invoke(methodParameter[0]), is((Object) Opcodes.ACC_FINAL));
        assertThat(getModifiers.invoke(methodParameter[1]), is((Object) 0));
        assertThat(getModifiers.invoke(methodParameter[2]), is((Object) 0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testParameterMetaDataRebaseForLoaded() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .rebase(classLoader.loadClass(PARAMETER_NAME_CLASS))
                .method(named(FOO)).intercept(StubMethod.INSTANCE)
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethods().length, is(7));
        Class<?> executable = Class.forName("java.lang.reflect.Executable");
        Method getParameters = executable.getDeclaredMethod("getParameters");
        Class<?> parameter = Class.forName("java.lang.reflect.Parameter");
        Method getName = parameter.getDeclaredMethod("getName");
        Method getModifiers = parameter.getDeclaredMethod("getModifiers");
        Method first = dynamicType.getDeclaredMethod("foo", String.class, long.class, int.class);
        Object[] methodParameter = (Object[]) getParameters.invoke(first);
        assertThat(getName.invoke(methodParameter[0]), is((Object) "first"));
        assertThat(getName.invoke(methodParameter[1]), is((Object) "second"));
        assertThat(getName.invoke(methodParameter[2]), is((Object) "third"));
        assertThat(getModifiers.invoke(methodParameter[0]), is((Object) Opcodes.ACC_FINAL));
        assertThat(getModifiers.invoke(methodParameter[1]), is((Object) 0));
        assertThat(getModifiers.invoke(methodParameter[2]), is((Object) 0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testParameterMetaDataSubclassForTypePool() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(typePool.describe(PARAMETER_NAME_CLASS).resolve())
                .method(named(FOO)).intercept(StubMethod.INSTANCE)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethods().length, is(1));
        Class<?> executable = Class.forName("java.lang.reflect.Executable");
        Method getParameters = executable.getDeclaredMethod("getParameters");
        Class<?> parameter = Class.forName("java.lang.reflect.Parameter");
        Method getName = parameter.getDeclaredMethod("getName");
        Method getModifiers = parameter.getDeclaredMethod("getModifiers");
        Method first = dynamicType.getDeclaredMethod("foo", String.class, long.class, int.class);
        Object[] methodParameter = (Object[]) getParameters.invoke(first);
        assertThat(getName.invoke(methodParameter[0]), is((Object) "first"));
        assertThat(getName.invoke(methodParameter[1]), is((Object) "second"));
        assertThat(getName.invoke(methodParameter[2]), is((Object) "third"));
        assertThat(getModifiers.invoke(methodParameter[0]), is((Object) Opcodes.ACC_FINAL));
        assertThat(getModifiers.invoke(methodParameter[1]), is((Object) 0));
        assertThat(getModifiers.invoke(methodParameter[2]), is((Object) 0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testParameterMetaDataRedefinitionForTypePool() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .redefine(typePool.describe(PARAMETER_NAME_CLASS).resolve(), ClassFileLocator.ForClassLoader.ofClassPath())
                .method(named(FOO)).intercept(StubMethod.INSTANCE)
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethods().length, is(2));
        Class<?> executable = Class.forName("java.lang.reflect.Executable");
        Method getParameters = executable.getDeclaredMethod("getParameters");
        Class<?> parameter = Class.forName("java.lang.reflect.Parameter");
        Method getName = parameter.getDeclaredMethod("getName");
        Method getModifiers = parameter.getDeclaredMethod("getModifiers");
        Method first = dynamicType.getDeclaredMethod("foo", String.class, long.class, int.class);
        Object[] methodParameter = (Object[]) getParameters.invoke(first);
        assertThat(getName.invoke(methodParameter[0]), is((Object) "first"));
        assertThat(getName.invoke(methodParameter[1]), is((Object) "second"));
        assertThat(getName.invoke(methodParameter[2]), is((Object) "third"));
        assertThat(getModifiers.invoke(methodParameter[0]), is((Object) Opcodes.ACC_FINAL));
        assertThat(getModifiers.invoke(methodParameter[1]), is((Object) 0));
        assertThat(getModifiers.invoke(methodParameter[2]), is((Object) 0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testParameterMetaDataRebaseForTypePool() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .rebase(typePool.describe(PARAMETER_NAME_CLASS).resolve(), ClassFileLocator.ForClassLoader.ofClassPath())
                .method(named(FOO)).intercept(StubMethod.INSTANCE)
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethods().length, is(7));
        Class<?> executable = Class.forName("java.lang.reflect.Executable");
        Method getParameters = executable.getDeclaredMethod("getParameters");
        Class<?> parameter = Class.forName("java.lang.reflect.Parameter");
        Method getName = parameter.getDeclaredMethod("getName");
        Method getModifiers = parameter.getDeclaredMethod("getModifiers");
        Method first = dynamicType.getDeclaredMethod("foo", String.class, long.class, int.class);
        Object[] methodParameter = (Object[]) getParameters.invoke(first);
        assertThat(getName.invoke(methodParameter[0]), is((Object) "first"));
        assertThat(getName.invoke(methodParameter[1]), is((Object) "second"));
        assertThat(getName.invoke(methodParameter[2]), is((Object) "third"));
        assertThat(getModifiers.invoke(methodParameter[0]), is((Object) Opcodes.ACC_FINAL));
        assertThat(getModifiers.invoke(methodParameter[1]), is((Object) 0));
        assertThat(getModifiers.invoke(methodParameter[2]), is((Object) 0));
    }
}
