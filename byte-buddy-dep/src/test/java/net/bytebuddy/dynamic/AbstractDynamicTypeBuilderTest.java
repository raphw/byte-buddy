package net.bytebuddy.dynamic;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.MethodManifestation;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.ClassFileExtraction;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public abstract class AbstractDynamicTypeBuilderTest {

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", TO_STRING = "toString";

    private static final int MODIFIERS = Opcodes.ACC_PUBLIC;

    private static final boolean BOOLEAN_VALUE = true;

    private static final int INTEGER_VALUE = 42;

    private static final long LONG_VALUE = 42L;

    private static final float FLOAT_VALUE = 42f;

    private static final double DOUBLE_VALUE = 42d;

    private static final String BOOLEAN_FIELD = "booleanField";

    private static final String BYTE_FIELD = "byteField";

    private static final String CHARACTER_FIELD = "characterField";

    private static final String SHORT_FIELD = "shortField";

    private static final String INTEGER_FIELD = "integerField";

    private static final String LONG_FIELD = "longField";

    private static final String FLOAT_FIELD = "floatField";

    private static final String DOUBLE_FIELD = "doubleField";

    private static final String STRING_FIELD = "stringField";

    protected abstract DynamicType.Builder<?> createPlain();

    @Test
    public void testMethodDefinition() throws Exception {
        Class<?> type = createPlain()
                .defineMethod(FOO, Object.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .throwing(Exception.class)
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Method method = type.getDeclaredMethod(FOO);
        assertEquals(Object.class, method.getReturnType());
        assertArrayEquals(new Class<?>[]{Exception.class}, method.getExceptionTypes());
        assertThat(method.getModifiers(), is(Modifier.PUBLIC));
        assertThat(method.invoke(type.newInstance()), is((Object) FOO));
    }

    @Test
    public void testAbstractMethodDefinition() throws Exception {
        Class<?> type = createPlain()
                .modifiers(Visibility.PUBLIC, TypeManifestation.ABSTRACT)
                .defineMethod(FOO, Object.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .throwing(Exception.class)
                .withoutCode()
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Method method = type.getDeclaredMethod(FOO);
        assertEquals(Object.class, method.getReturnType());
        assertArrayEquals(new Class<?>[]{Exception.class}, method.getExceptionTypes());
        assertThat(method.getModifiers(), is(Modifier.PUBLIC | Modifier.ABSTRACT));
    }

    @Test
    public void testConstructorDefinition() throws Exception {
        Class<?> type = createPlain()
                .defineConstructor(Collections.<Class<?>>singletonList(Void.class), Visibility.PUBLIC)
                .throwing(Exception.class)
                .intercept(MethodCall.invoke(Object.class.getDeclaredConstructor()))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Constructor<?> constructor = type.getDeclaredConstructor(Void.class);
        assertArrayEquals(new Class<?>[]{Exception.class}, constructor.getExceptionTypes());
        assertThat(constructor.getModifiers(), is(Modifier.PUBLIC));
        assertThat(constructor.newInstance((Object) null), notNullValue(Object.class));
    }

    @Test
    public void testFieldDefinition() throws Exception {
        Class<?> type = createPlain()
                .defineField(FOO, Void.class, Visibility.PUBLIC)
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Field field = type.getDeclaredField(FOO);
        assertEquals(Void.class, field.getType());
        assertThat(field.getModifiers(), is(Modifier.PUBLIC));
    }

    @Test
    public void testFieldDefaultValueDefinition() throws Exception {
        Class<?> type = createPlain()
                .defineField(BOOLEAN_FIELD, boolean.class, Visibility.PUBLIC, Ownership.STATIC).value(BOOLEAN_VALUE)
                .defineField(BYTE_FIELD, byte.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(SHORT_FIELD, short.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(CHARACTER_FIELD, char.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(INTEGER_FIELD, int.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(LONG_FIELD, long.class, Visibility.PUBLIC, Ownership.STATIC).value(LONG_VALUE)
                .defineField(FLOAT_FIELD, float.class, Visibility.PUBLIC, Ownership.STATIC).value(FLOAT_VALUE)
                .defineField(DOUBLE_FIELD, double.class, Visibility.PUBLIC, Ownership.STATIC).value(DOUBLE_VALUE)
                .defineField(STRING_FIELD, String.class, Visibility.PUBLIC, Ownership.STATIC).value(FOO)
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredField(BOOLEAN_FIELD).get(null), is((Object) BOOLEAN_VALUE));
        assertThat(type.getDeclaredField(BYTE_FIELD).get(null), is((Object) (byte) INTEGER_VALUE));
        assertThat(type.getDeclaredField(SHORT_FIELD).get(null), is((Object) (short) INTEGER_VALUE));
        assertThat(type.getDeclaredField(CHARACTER_FIELD).get(null), is((Object) (char) INTEGER_VALUE));
        assertThat(type.getDeclaredField(INTEGER_FIELD).get(null), is((Object) INTEGER_VALUE));
        assertThat(type.getDeclaredField(LONG_FIELD).get(null), is((Object) LONG_VALUE));
        assertThat(type.getDeclaredField(FLOAT_FIELD).get(null), is((Object) FLOAT_VALUE));
        assertThat(type.getDeclaredField(DOUBLE_FIELD).get(null), is((Object) DOUBLE_VALUE));
        assertThat(type.getDeclaredField(STRING_FIELD).get(null), is((Object) FOO));
    }

    @Test
    public void testApplicationOrder() throws Exception {
        assertThat(createPlain()
                .method(named(TO_STRING)).intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .method(named(TO_STRING)).intercept(new Implementation.Simple(new TextConstant(BAR), MethodReturn.REFERENCE))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .toString(), is(BAR));
    }

    @Test
    public void testTypeInitializer() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                ClassFileExtraction.of(Bar.class),
                DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Class<?> type = createPlain()
                .invokable(isTypeInitializer()).intercept(MethodCall.invoke(Bar.class.getDeclaredMethod("invoke")))
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.newInstance(), notNullValue(Object.class));
        Class<?> foo = classLoader.loadClass(Bar.class.getName());
        assertThat(foo.getDeclaredField(FOO).get(null), is((Object) FOO));
    }

    @Test
    public void testConstructorInvokingMethod() throws Exception {
        Class<?> type = createPlain()
                .defineMethod(FOO, Object.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Method method = type.getDeclaredMethod(FOO);
        assertThat(method.invoke(type.newInstance()), is((Object) FOO));
    }

    @Test
    public void testMethodTransformation() throws Exception {
        Class<?> type = createPlain()
                .method(named(TO_STRING))
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE),
                        MethodTransformer.Simple.withModifiers(MethodManifestation.FINAL))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.newInstance().toString(), is(FOO));
        assertThat(type.getDeclaredMethod(TO_STRING).getModifiers(), is(Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC));
    }

    @Test
    public void testIgnoredMethod() throws Exception {
        Class<?> type = createPlain()
                .ignoreMethods(named(TO_STRING))
                .method(named(TO_STRING)).intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.newInstance().toString(), CoreMatchers.not(FOO));
    }

    @Test
    public void testIgnoredMethodDoesNotApplyForDefined() throws Exception {
        Class<?> type = createPlain()
                .ignoreMethods(named(FOO))
                .defineMethod(FOO, String.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
    }

    @Test
    public void testPreparedField() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                ClassFileExtraction.of(SampleAnnotation.class),
                DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Class<?> type = createPlain()
                .defineMethod(BAR, String.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .intercept(new PreparedField())
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredFields().length, is(1));
        assertThat(type.getDeclaredField(FOO).getName(), is(FOO));
        assertEquals(Object.class, type.getDeclaredField(FOO).getType());
        assertThat(type.getDeclaredField(FOO).getModifiers(), is(MODIFIERS));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        Annotation annotation = type.getDeclaredField(FOO).getAnnotations()[0];
        assertThat(annotation.annotationType().getName(), is(SampleAnnotation.class.getName()));
        Method foo = annotation.annotationType().getDeclaredMethod(FOO);
        assertThat(foo.invoke(annotation), is((Object) BAR));
    }

    @Test
    public void testPreparedMethod() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                ClassFileExtraction.of(SampleAnnotation.class),
                DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Class<?> type = createPlain()
                .defineMethod(BAR, String.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .intercept(new PreparedMethod())
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(2));
        assertThat(type.getDeclaredMethod(FOO, Object.class).getName(), is(FOO));
        assertEquals(Object.class, type.getDeclaredMethod(FOO, Object.class).getReturnType());
        assertThat(type.getDeclaredMethod(FOO, Object.class).getParameterTypes().length, is(1));
        assertEquals(Object.class, type.getDeclaredMethod(FOO, Object.class).getParameterTypes()[0]);
        assertThat(type.getDeclaredMethod(FOO, Object.class).getModifiers(), is(MODIFIERS));
        assertThat(type.getDeclaredMethod(FOO, Object.class).getAnnotations().length, is(1));
        Annotation methodAnnotation = type.getDeclaredMethod(FOO, Object.class).getAnnotations()[0];
        assertThat(methodAnnotation.annotationType().getName(), is(SampleAnnotation.class.getName()));
        Method methodMethod = methodAnnotation.annotationType().getDeclaredMethod(FOO);
        assertThat(methodMethod.invoke(methodAnnotation), is((Object) BAR));
        assertThat(type.getDeclaredMethod(FOO, Object.class).getParameterAnnotations()[0].length, is(1));
        Annotation parameterAnnotation = type.getDeclaredMethod(FOO, Object.class).getParameterAnnotations()[0][0];
        assertThat(parameterAnnotation.annotationType().getName(), is(SampleAnnotation.class.getName()));
        Method parameterMethod = parameterAnnotation.annotationType().getDeclaredMethod(FOO);
        assertThat(parameterMethod.invoke(parameterAnnotation), is((Object) QUX));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SampleAnnotation {

        String foo();
    }

    public static class Foo {
        /* empty */
    }

    public static class Bar {

        public static String foo;

        public static void invoke() {
            foo = FOO;
        }
    }

    public static class BridgeRetention<T> extends CallTraceable {

        public T foo() {
            register(FOO);
            return null;
        }

        public static class Inner extends BridgeRetention<String> {
            /* empty */
        }
    }

    public static class CallSuperMethod<T> extends CallTraceable {

        public T foo(T value) {
            register(FOO);
            return value;
        }

        public static class Inner extends CallSuperMethod<String> {
            /* empty */
        }
    }

    private static class PreparedField implements Implementation {

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withField(new FieldDescription.Token(FOO,
                    MODIFIERS,
                    TypeDescription.OBJECT,
                    Collections.singletonList(AnnotationDescription.Builder.forType(SampleAnnotation.class).define(FOO, BAR).make())));
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new ByteCodeAppender.Simple(NullConstant.INSTANCE, MethodReturn.REFERENCE);
        }
    }

    private static class PreparedMethod implements Implementation {

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withMethod(new MethodDescription.Token(FOO,
                    MODIFIERS,
                    Collections.<GenericTypeDescription>emptyList(),
                    TypeDescription.OBJECT,
                    Collections.singletonList(new ParameterDescription.Token(TypeDescription.OBJECT,
                            Collections.singletonList(AnnotationDescription.Builder.forType(SampleAnnotation.class).define(FOO, QUX).make()))),
                    Collections.singletonList(new TypeDescription.ForLoadedType(Exception.class)),
                    Collections.singletonList(AnnotationDescription.Builder.forType(SampleAnnotation.class).define(FOO, BAR).make()),
                    MethodDescription.NO_DEFAULT_VALUE));
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new ByteCodeAppender.Simple(NullConstant.INSTANCE, MethodReturn.REFERENCE);
        }
    }

    public static class InterfaceOverrideInterceptor {

        public static String intercept(@SuperCall Callable<String> zuper) throws Exception {
            return zuper.call() + BAR;
        }
    }
}
