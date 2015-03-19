package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.TextConstant;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.modifier.Visibility;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.RandomString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.asm.Type;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class InlineDynamicTypeBuilderTest {

    private static final String FOOBAR = "foo.Bar", FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation simpleInstrumentation,
            preparingInstrumentation,
            typeInitializerInstrumentation,
            fieldCacheInstrumentation;

    @Mock
    private ByteCodeAppender byteCodeAppender, typeInitializerAppender, fieldCacheAppender;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Before
    public void setUp() throws Exception {
        when(simpleInstrumentation.prepare(any(InstrumentedType.class))).then(new Answer<InstrumentedType>() {
            @Override
            public InstrumentedType answer(InvocationOnMock invocation) throws Throwable {
                return (InstrumentedType) invocation.getArguments()[0];
            }
        });
        when(simpleInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(byteCodeAppender);
        when(byteCodeAppender.appendsCode()).thenReturn(true);
        when(byteCodeAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class)))
                .thenAnswer(new Answer<ByteCodeAppender.Size>() {
                    @Override
                    public ByteCodeAppender.Size answer(InvocationOnMock invocation) throws Throwable {
                        MethodVisitor methodVisitor = (MethodVisitor) invocation.getArguments()[0];
                        MethodDescription methodDescription = (MethodDescription) invocation.getArguments()[2];
                        methodVisitor.visitInsn(Opcodes.ICONST_0);
                        methodVisitor.visitInsn(Opcodes.IRETURN);
                        return new ByteCodeAppender.Size(1, methodDescription.getStackSize());
                    }
                });
        when(preparingInstrumentation.prepare(any(InstrumentedType.class))).then(new Answer<InstrumentedType>() {
            @Override
            public InstrumentedType answer(InvocationOnMock invocation) throws Throwable {
                return ((InstrumentedType) invocation.getArguments()[0])
                        .withField(BAZ,
                                new TypeDescription.ForLoadedType(Object.class),
                                0)
                        .withMethod(QUX,
                                new TypeDescription.ForLoadedType(int.class),
                                Collections.<TypeDescription>emptyList(),
                                Collections.<TypeDescription>emptyList(),
                                0)
                        .withInitializer(loadedTypeInitializer);
            }
        });
        when(preparingInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(byteCodeAppender);
        when(typeInitializerInstrumentation.prepare(any(InstrumentedType.class))).then(new Answer<InstrumentedType>() {
            @Override
            public InstrumentedType answer(InvocationOnMock invocation) throws Throwable {
                return (InstrumentedType) invocation.getArguments()[0];
            }
        });
        when(typeInitializerInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(typeInitializerAppender);
        when(typeInitializerAppender.appendsCode()).thenReturn(true);
        when(typeInitializerAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class)))
                .thenAnswer(new Answer<ByteCodeAppender.Size>() {
                    @Override
                    public ByteCodeAppender.Size answer(InvocationOnMock invocation) throws Throwable {
                        MethodVisitor methodVisitor = (MethodVisitor) invocation.getArguments()[0];
                        MethodDescription methodDescription = (MethodDescription) invocation.getArguments()[2];
                        methodVisitor.visitInsn(Opcodes.ICONST_1);
                        methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC,
                                Type.getInternalName(Foo.class),
                                FOO,
                                Type.getDescriptor(int.class));
                        methodVisitor.visitInsn(Opcodes.RETURN);
                        return new ByteCodeAppender.Size(1, methodDescription.getStackSize());
                    }
                });
        when(fieldCacheInstrumentation.prepare(any(InstrumentedType.class))).then(new Answer<InstrumentedType>() {
            @Override
            public InstrumentedType answer(InvocationOnMock invocation) throws Throwable {
                return (InstrumentedType) invocation.getArguments()[0];
            }
        });
        when(fieldCacheInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(fieldCacheAppender);
        when(fieldCacheAppender.appendsCode()).thenReturn(true);
        when(fieldCacheAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class)))
                .thenAnswer(new Answer<ByteCodeAppender.Size>() {
                    @Override
                    public ByteCodeAppender.Size answer(InvocationOnMock invocation) throws Throwable {
                        MethodVisitor methodVisitor = (MethodVisitor) invocation.getArguments()[0];
                        Instrumentation.Context instrumentationContext = (Instrumentation.Context) invocation.getArguments()[1];
                        MethodDescription methodDescription = (MethodDescription) invocation.getArguments()[2];
                        FieldDescription fieldDescription = instrumentationContext.cache(new TextConstant(BAR),
                                new TypeDescription.ForLoadedType(String.class));
                        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC,
                                fieldDescription.getDeclaringType().getInternalName(),
                                fieldDescription.getInternalName(),
                                fieldDescription.getDescriptor());
                        methodVisitor.visitInsn(Opcodes.ARETURN);
                        return new ByteCodeAppender.Size(1, methodDescription.getStackSize());
                    }
                });
    }

    @Test
    public void testPlainRebasing() throws Exception {
        Class<?> loaded = new InlineDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
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
                ClassFileLocator.ForClassLoader.ofClassPath(),
                new InlineDynamicTypeBuilder.TargetHandler.ForRebaseInstrumentation(new MethodRebaseResolver.MethodNameTransformer.Suffixing()))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOOBAR));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)); // Static is implicit due to inner class inheritance.
        assertThat(loaded.getAnnotation(Bar.class), notNullValue());
        assertThat(Serializable.class.isAssignableFrom(loaded), is(true));
        assertThat(loaded.getDeclaredFields().length, is(1));
        assertThat(loaded.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL));
        assertThat(loaded.getDeclaredField(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredField(FOO).getType());
        assertThat(loaded.getDeclaredMethods().length, is(2));
        assertThat(loaded.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethod(FOO).getAnnotation(Bar.class), notNullValue());
        assertThat(new MethodList.ForLoadedType(loaded).filter(not(named(FOO)).and(isMethod()))
                .getOnly().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertEquals(String.class, loaded.getDeclaredMethod(FOO).getReturnType());
        assertThat(loaded.getDeclaredConstructors().length, is(2));
        assertThat(loaded.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredConstructor().getAnnotation(Bar.class), notNullValue());
        assertThat(new MethodList.ForLoadedType(loaded).filter(takesArguments(1).and(isConstructor()))
                .getOnly().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertThat(loaded.getDeclaredMethod(FOO).invoke(loaded.newInstance()), is((Object) FOO));
    }

    @Test
    public void testPlainRedefinition() throws Exception {
        Class<?> loaded = new InlineDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
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
                ClassFileLocator.ForClassLoader.ofClassPath(),
                InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOOBAR));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)); // Static is implicit due to inner class inheritance.
        assertThat(loaded.getAnnotation(Bar.class), notNullValue());
        assertThat(Serializable.class.isAssignableFrom(loaded), is(true));
        assertThat(loaded.getDeclaredFields().length, is(1));
        assertThat(loaded.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL));
        assertThat(loaded.getDeclaredField(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredField(FOO).getType());
        assertThat(loaded.getDeclaredMethods().length, is(1));
        assertThat(loaded.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethod(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredMethod(FOO).getReturnType());
        assertThat(loaded.getDeclaredConstructors().length, is(1));
        assertThat(loaded.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredConstructor().getAnnotation(Bar.class), notNullValue());
        assertThat(loaded.getDeclaredMethod(FOO).invoke(loaded.newInstance()), is((Object) FOO));
    }

    @Test
    public void testRebasingWithDefinedField() throws Exception {
        Class<?> loaded = new InlineDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
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
                ClassFileLocator.ForClassLoader.ofClassPath(),
                new InlineDynamicTypeBuilder.TargetHandler.ForRebaseInstrumentation(new MethodRebaseResolver.MethodNameTransformer.Suffixing()))
                .defineField(BAR, long.class, Visibility.PUBLIC)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOOBAR));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)); // Static is implicit due to inner class inheritance.
        assertThat(loaded.getAnnotation(Bar.class), notNullValue());
        assertThat(Serializable.class.isAssignableFrom(loaded), is(true));
        assertThat(loaded.getDeclaredFields().length, is(2));
        assertThat(loaded.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL));
        assertThat(loaded.getDeclaredField(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredField(FOO).getType());
        assertThat(loaded.getDeclaredField(BAR).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertEquals(long.class, loaded.getDeclaredField(BAR).getType());
        assertThat(loaded.getDeclaredMethods().length, is(2));
        assertThat(loaded.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethod(FOO).getAnnotation(Bar.class), notNullValue());
        assertThat(new MethodList.ForLoadedType(loaded).filter(not(named(FOO)).and(isMethod()))
                .getOnly().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertEquals(String.class, loaded.getDeclaredMethod(FOO).getReturnType());
        assertThat(loaded.getDeclaredConstructors().length, is(2));
        assertThat(loaded.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredConstructor().getAnnotation(Bar.class), notNullValue());
        assertThat(new MethodList.ForLoadedType(loaded).filter(takesArguments(1).and(isConstructor()))
                .getOnly().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertThat(loaded.getDeclaredMethod(FOO).invoke(loaded.newInstance()), is((Object) FOO));
    }

    @Test
    public void testRedefinitionWithDefinedField() throws Exception {
        Class<?> loaded = new InlineDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
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
                ClassFileLocator.ForClassLoader.ofClassPath(),
                InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE)
                .defineField(BAR, long.class, Visibility.PUBLIC)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOOBAR));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)); // Static is implicit due to inner class inheritance.
        assertThat(loaded.getAnnotation(Bar.class), notNullValue());
        assertThat(Serializable.class.isAssignableFrom(loaded), is(true));
        assertThat(loaded.getDeclaredFields().length, is(2));
        assertThat(loaded.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL));
        assertThat(loaded.getDeclaredField(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredField(FOO).getType());
        assertThat(loaded.getDeclaredField(BAR).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertEquals(long.class, loaded.getDeclaredField(BAR).getType());
        assertThat(loaded.getDeclaredMethods().length, is(1));
        assertThat(loaded.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethod(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredMethod(FOO).getReturnType());
        assertThat(loaded.getDeclaredConstructors().length, is(1));
        assertThat(loaded.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredConstructor().getAnnotation(Bar.class), notNullValue());
        assertThat(loaded.getDeclaredMethod(FOO).invoke(loaded.newInstance()), is((Object) FOO));
    }

    @Test
    public void testRebasingWithDefinedMethod() throws Exception {
        Class<?> loaded = new InlineDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
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
                ClassFileLocator.ForClassLoader.ofClassPath(),
                new InlineDynamicTypeBuilder.TargetHandler.ForRebaseInstrumentation(new MethodRebaseResolver.MethodNameTransformer.Suffixing()))
                .defineMethod(BAR, int.class, Arrays.asList(long.class, Object.class), Visibility.PUBLIC)
                .intercept(simpleInstrumentation)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOOBAR));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)); // Static is implicit due to inner class inheritance.
        assertThat(loaded.getAnnotation(Bar.class), notNullValue());
        assertThat(Serializable.class.isAssignableFrom(loaded), is(true));
        assertThat(loaded.getDeclaredFields().length, is(1));
        assertThat(loaded.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL));
        assertThat(loaded.getDeclaredField(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredField(FOO).getType());
        assertThat(loaded.getDeclaredMethods().length, is(3));
        Method method = loaded.getDeclaredMethod(BAR, long.class, Object.class);
        assertThat(method.getName(), is(BAR));
        assertThat(method.getDeclaredAnnotations().length, is(0));
        assertEquals(int.class, method.getReturnType());
        assertThat(method.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethod(FOO).getAnnotation(Bar.class), notNullValue());
        assertThat(new MethodList.ForLoadedType(loaded).filter(not(named(FOO).or(named(BAR))).and(isMethod()))
                .getOnly().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertEquals(String.class, loaded.getDeclaredMethod(FOO).getReturnType());
        assertThat(loaded.getDeclaredConstructors().length, is(2));
        assertThat(loaded.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredConstructor().getAnnotation(Bar.class), notNullValue());
        assertThat(new MethodList.ForLoadedType(loaded).filter(takesArguments(1).and(isConstructor()))
                .getOnly().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertThat(loaded.getDeclaredMethod(FOO).invoke(loaded.newInstance()), is((Object) FOO));
    }

    @Test
    public void testRedefinitionWithDefinedMethod() throws Exception {
        Class<?> loaded = new InlineDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
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
                ClassFileLocator.ForClassLoader.ofClassPath(),
                InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE)
                .defineMethod(BAR, int.class, Arrays.asList(long.class, Object.class), Visibility.PUBLIC)
                .intercept(simpleInstrumentation)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOOBAR));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)); // Static is implicit due to inner class inheritance.
        assertThat(loaded.getAnnotation(Bar.class), notNullValue());
        assertThat(Serializable.class.isAssignableFrom(loaded), is(true));
        assertThat(loaded.getDeclaredFields().length, is(1));
        assertThat(loaded.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL));
        assertThat(loaded.getDeclaredField(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredField(FOO).getType());
        assertThat(loaded.getDeclaredMethods().length, is(2));
        Method method = loaded.getDeclaredMethod(BAR, long.class, Object.class);
        assertThat(method.getName(), is(BAR));
        assertThat(method.getDeclaredAnnotations().length, is(0));
        assertEquals(int.class, method.getReturnType());
        assertThat(method.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethod(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredMethod(FOO).getReturnType());
        assertThat(loaded.getDeclaredConstructors().length, is(1));
        assertThat(loaded.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredConstructor().getAnnotation(Bar.class), notNullValue());
        assertThat(loaded.getDeclaredMethod(FOO).invoke(loaded.newInstance()), is((Object) FOO));
    }

    @Test
    public void testRebasingWithDefinedAbstractMethod() throws Exception {
        Class<?> loaded = new InlineDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
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
                ClassFileLocator.ForClassLoader.ofClassPath(),
                new InlineDynamicTypeBuilder.TargetHandler.ForRebaseInstrumentation(new MethodRebaseResolver.MethodNameTransformer.Suffixing()))
                .defineMethod(BAR, int.class, Arrays.asList(long.class, Object.class), Visibility.PUBLIC)
                .throwing(IOException.class)
                .withoutCode()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOOBAR));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)); // Static is implicit due to inner class inheritance.
        assertThat(loaded.getAnnotation(Bar.class), notNullValue());
        assertThat(Serializable.class.isAssignableFrom(loaded), is(true));
        assertThat(loaded.getDeclaredFields().length, is(1));
        assertThat(loaded.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL));
        assertThat(loaded.getDeclaredField(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredField(FOO).getType());
        assertThat(loaded.getDeclaredMethods().length, is(3));
        Method method = loaded.getDeclaredMethod(BAR, long.class, Object.class);
        assertThat(method.getName(), is(BAR));
        assertThat(method.getExceptionTypes().length, is(1));
        assertThat(Arrays.asList(method.getExceptionTypes()), hasItem(IOException.class));
        assertThat(method.getDeclaredAnnotations().length, is(0));
        assertThat(loaded.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethod(FOO).getAnnotation(Bar.class), notNullValue());
        assertThat(new MethodList.ForLoadedType(loaded).filter(not(named(FOO).or(named(BAR))).and(isMethod()))
                .getOnly().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertEquals(String.class, loaded.getDeclaredMethod(FOO).getReturnType());
        assertThat(loaded.getDeclaredConstructors().length, is(2));
        assertThat(loaded.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredConstructor().getAnnotation(Bar.class), notNullValue());
        assertThat(new MethodList.ForLoadedType(loaded).filter(takesArguments(1).and(isConstructor()))
                .getOnly().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertThat(loaded.getDeclaredMethod(FOO).invoke(loaded.newInstance()), is((Object) FOO));
    }

    @Test
    public void testRedefinitionWithDefinedAbstractMethod() throws Exception {
        Class<?> loaded = new InlineDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
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
                ClassFileLocator.ForClassLoader.ofClassPath(),
                InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE)
                .defineMethod(BAR, int.class, Arrays.asList(long.class, Object.class), Visibility.PUBLIC)
                .throwing(IOException.class)
                .withoutCode()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOOBAR));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)); // Static is implicit due to inner class inheritance.
        assertThat(loaded.getAnnotation(Bar.class), notNullValue());
        assertThat(Serializable.class.isAssignableFrom(loaded), is(true));
        assertThat(loaded.getDeclaredFields().length, is(1));
        assertThat(loaded.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL));
        assertThat(loaded.getDeclaredField(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredField(FOO).getType());
        assertThat(loaded.getDeclaredMethods().length, is(2));
        Method method = loaded.getDeclaredMethod(BAR, long.class, Object.class);
        assertThat(method.getName(), is(BAR));
        assertThat(method.getExceptionTypes().length, is(1));
        assertThat(Arrays.asList(method.getExceptionTypes()), hasItem(IOException.class));
        assertThat(method.getDeclaredAnnotations().length, is(0));
        assertThat(loaded.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethod(FOO).getAnnotation(Bar.class), notNullValue());
        assertEquals(String.class, loaded.getDeclaredMethod(FOO).getReturnType());
        assertThat(loaded.getDeclaredConstructors().length, is(1));
        assertThat(loaded.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredConstructor().getAnnotation(Bar.class), notNullValue());
        assertThat(loaded.getDeclaredMethod(FOO).invoke(loaded.newInstance()), is((Object) FOO));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InlineDynamicTypeBuilder.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Arrays.asList(new Object());
            }
        }).apply();
        ObjectPropertyAssertion.of(InlineDynamicTypeBuilder.TargetHandler.Prepared.ForRebaseInstrumentation.class).refine(new ObjectPropertyAssertion.Refinement<DynamicType>() {
            @Override
            public void apply(DynamicType mock) {
                when(mock.getTypeDescription()).thenReturn(Mockito.mock(TypeDescription.class));
            }
        }).refine(new ObjectPropertyAssertion.Refinement<RandomString>() {
            @Override
            public void apply(RandomString mock) {
                when(mock.nextString()).thenReturn(FOO + System.identityHashCode(mock));
            }
        }).apply();
        ObjectPropertyAssertion.of(InlineDynamicTypeBuilder.TargetHandler.Prepared.ForRebaseInstrumentation.MethodRebaseDelegation.class).apply();
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
