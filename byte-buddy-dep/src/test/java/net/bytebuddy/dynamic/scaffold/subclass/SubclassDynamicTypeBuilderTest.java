package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.AbstractDynamicTypeBuilderTest;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.test.utility.PrecompiledTypeClassLoader;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotEquals;

public class SubclassDynamicTypeBuilderTest extends AbstractDynamicTypeBuilderTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final String DEFAULT_METHOD_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final String PARAMETER_NAME_CLASS = "net.bytebuddy.test.precompiled.ParameterNames";

    private static final Object STATIC_FIELD = null;

    private static final String INTERFACE_STATIC_FIELD_NAME = "FOO";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
    }

    @Override
    protected DynamicType.Builder<?> createPlain() {
        return new ByteBuddy().subclass(Object.class);
    }

    @Test
    public void testSimpleSubclass() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredConstructor(), notNullValue(Constructor.class));
        assertThat(Object.class.isAssignableFrom(type), is(true));
        assertNotEquals(Object.class, type);
        assertThat(type.newInstance(), notNullValue(Object.class));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testSimpleSubclassWithoutConstructor() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
        assertThat(type.getDeclaredConstructors().length, is(0));
        assertThat(Object.class.isAssignableFrom(type), is(true));
        assertNotEquals(Object.class, type);
        assertThat(type.isInterface(), is(false));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testSimpleSubclassWithDefaultConstructor() throws Exception {
        Class<? extends DefaultConstructor> type = new ByteBuddy()
                .subclass(DefaultConstructor.class, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredConstructor(), notNullValue(Constructor.class));
        assertThat(DefaultConstructor.class.isAssignableFrom(type), is(true));
        assertNotEquals(DefaultConstructor.class, type);
        assertThat(type.newInstance(), notNullValue(DefaultConstructor.class));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testInterfaceDefinition() throws Exception {
        Class<? extends SimpleInterface> type = new ByteBuddy()
                .makeInterface(SimpleInterface.class)
                .defineMethod(FOO, void.class, Collections.<Class<?>>singletonList(Void.class), Visibility.PUBLIC)
                .withoutCode()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(1));
        assertThat(type.getDeclaredMethod(FOO, Void.class), notNullValue(Method.class));
        assertThat(type.getDeclaredConstructors().length, is(0));
        assertThat(SimpleInterface.class.isAssignableFrom(type), is(true));
        assertNotEquals(SimpleInterface.class, type);
        assertThat(type.isInterface(), is(true));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testAnnotationDefinition() throws Exception {
        Class<?> type = new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, int.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .withoutCode()
                .defineMethod(BAR, String.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .withDefaultValue(FOO)
                .defineMethod(QUX, SimpleEnum.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .withDefaultValue(SimpleEnum.FIRST, SimpleEnum.class)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(3));
        assertThat(type.getDeclaredMethod(FOO), notNullValue(Method.class));
        assertThat(type.getDeclaredMethod(BAR).getDefaultValue(), is((Object) FOO));
        assertThat(type.getDeclaredMethod(QUX).getDefaultValue(), is((Object) SimpleEnum.FIRST));
        assertThat(type.getDeclaredConstructors().length, is(0));
        assertThat(Annotation.class.isAssignableFrom(type), is(true));
        assertNotEquals(Annotation.class, type);
        assertThat(type.isInterface(), is(true));
        assertThat(type.isAnnotation(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodNonOverridden() throws Exception {
        Class<?> interfaceType = classLoader.loadClass(DEFAULT_METHOD_INTERFACE);
        Object interfaceMarker = interfaceType.getDeclaredField(INTERFACE_STATIC_FIELD_NAME).get(STATIC_FIELD);
        Method interfaceMethod = interfaceType.getDeclaredMethod(FOO);
        Class<?> dynamicType = new ByteBuddy()
                .subclass(interfaceType)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredFields().length, Is.is(0));
        assertThat(dynamicType.getDeclaredMethods().length, Is.is(0));
        assertThat(interfaceMethod.invoke(dynamicType.newInstance()), Is.is(interfaceMarker));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodOverridden() throws Exception {
        Class<?> interfaceType = classLoader.loadClass(DEFAULT_METHOD_INTERFACE);
        Method interfaceMethod = interfaceType.getDeclaredMethod(FOO);
        Class<?> dynamicType = new ByteBuddy()
                .subclass(interfaceType)
                .method(isDeclaredBy(interfaceType)).intercept(FixedValue.value(BAR))
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredFields().length, Is.is(0));
        assertThat(dynamicType.getDeclaredMethods().length, Is.is(1));
        assertThat(interfaceMethod.invoke(dynamicType.newInstance()), Is.is((Object) BAR));
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
        assertThat(dynamicType.getDeclaredMethods().length, Is.is(1));
        Class<?> executable = Class.forName("java.lang.reflect.Executable");
        Method getParameters = executable.getDeclaredMethod("getParameters");
        Class<?> parameter = Class.forName("java.lang.reflect.Parameter");
        Method getName = parameter.getDeclaredMethod("getName");
        Method getModifiers = parameter.getDeclaredMethod("getModifiers");
        Method first = dynamicType.getDeclaredMethod("foo", String.class, long.class, int.class);
        Object[] methodParameter = (Object[]) getParameters.invoke(first);
        assertThat(getName.invoke(methodParameter[0]), Is.is((Object) "first"));
        assertThat(getName.invoke(methodParameter[1]), Is.is((Object) "second"));
        assertThat(getName.invoke(methodParameter[2]), Is.is((Object) "third"));
        assertThat(getModifiers.invoke(methodParameter[0]), Is.is((Object) Opcodes.ACC_FINAL));
        assertThat(getModifiers.invoke(methodParameter[1]), Is.is((Object) 0));
        assertThat(getModifiers.invoke(methodParameter[2]), Is.is((Object) 0));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassDynamicTypeBuilder.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Collections.singletonList(new Object());
            }
        }).apply();
    }

    public enum SimpleEnum {
        FIRST,
        SECOND
    }

    public interface SimpleInterface {

        void bar(Void arg);
    }

    public static class DefaultConstructor {

        public DefaultConstructor() {
            /* empty */
        }

        public DefaultConstructor(Void arg) {
            /* empty */
        }
    }
}
