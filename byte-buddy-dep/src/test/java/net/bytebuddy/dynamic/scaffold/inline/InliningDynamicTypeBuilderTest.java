package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class InliningDynamicTypeBuilderTest {

    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = null;

    private static final String FOOBAR = "foo.Bar", FOO = "foo";

    @Test
    public void testPlainRebasing() throws Exception {
        Class<?> foo = new InliningDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOOBAR),
                new TypeDescription.ForLoadedType(Foo.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                isDeclaredBy(Object.class),
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                MethodLookupEngine.Default.Factory.INSTANCE,
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ClassFileLocator.Default.CLASS_PATH,
                InliningDynamicTypeBuilder.TargetHandler.ForRebaseInstrumentation.INSTANCE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(foo.getName(), is(FOOBAR));
        assertThat(foo.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)); // Static is implicit due to inner class inheritance.
        assertThat(foo.getAnnotation(Bar.class), notNullValue());
        assertThat(Serializable.class.isAssignableFrom(foo), is(true));
        assertThat(foo.getDeclaredFields().length, is(1));
        assertThat(foo.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL));
        assertThat(foo.getDeclaredField(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, foo.getDeclaredField(FOO).getType());
        assertThat(foo.getDeclaredMethods().length, is(2));
        assertThat(foo.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(foo.getDeclaredMethod(FOO).getAnnotation(Bar.class), notNullValue());
        assertThat(new MethodList.ForLoadedType(foo).filter(not(named(FOO)).and(isMethod()))
                .getOnly().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertEquals(String.class, foo.getDeclaredMethod(FOO).getReturnType());
        assertThat(foo.getDeclaredConstructors().length, is(2));
        assertThat(foo.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(foo.getDeclaredConstructor().getAnnotation(Bar.class), notNullValue());
        assertThat(new MethodList.ForLoadedType(foo).filter(takesArguments(1).and(isConstructor()))
                .getOnly().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertThat(foo.getDeclaredMethod(FOO).invoke(foo.newInstance()), is((Object) FOO));
    }

    @Test
    public void testPlainRedefinition() throws Exception {
        Class<?> foo = new InliningDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOOBAR),
                new TypeDescription.ForLoadedType(Foo.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                isDeclaredBy(Object.class),
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                MethodLookupEngine.Default.Factory.INSTANCE,
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ClassFileLocator.Default.CLASS_PATH,
                InliningDynamicTypeBuilder.TargetHandler.ForSubclassInstrumentation.INSTANCE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(foo.getName(), is(FOOBAR));
        assertThat(foo.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)); // Static is implicit due to inner class inheritance.
        assertThat(foo.getAnnotation(Bar.class), notNullValue());
        assertThat(Serializable.class.isAssignableFrom(foo), is(true));
        assertThat(foo.getDeclaredFields().length, is(1));
        assertThat(foo.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL));
        assertThat(foo.getDeclaredField(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, foo.getDeclaredField(FOO).getType());
        assertThat(foo.getDeclaredMethods().length, is(1));
        assertThat(foo.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(foo.getDeclaredMethod(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, foo.getDeclaredMethod(FOO).getReturnType());
        assertThat(foo.getDeclaredConstructors().length, is(1));
        assertThat(foo.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(foo.getDeclaredConstructor().getAnnotation(Bar.class), notNullValue());
        assertThat(foo.getDeclaredMethod(FOO).invoke(foo.newInstance()), is((Object) FOO));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Bar {
        /* example annotation */
    }

    @Bar
    public static class Foo {

        @Bar
        private final String foo;

        @Bar
        public Foo() {
            foo = FOO;
        }

        @Bar
        public String foo() {
            return FOO;
        }
    }
}
