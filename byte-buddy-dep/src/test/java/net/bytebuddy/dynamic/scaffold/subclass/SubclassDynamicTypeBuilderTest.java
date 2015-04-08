package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.SuperMethodCall;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.TextConstant;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.modifier.Ownership;
import net.bytebuddy.modifier.Visibility;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Type;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import static org.mockito.Mockito.*;

public class SubclassDynamicTypeBuilderTest {
//
//    private static final String BOOLEAN_FIELD = "booleanField";
//
//    private static final String BYTE_FIELD = "byteField";
//
//    private static final String SHORT_FIELD = "shortField";
//
//    private static final String CHARACTER_FIELD = "charField";
//
//    private static final String INTEGER_FIELD = "intField";
//
//    private static final String LONG_FIELD = "longField";
//
//    private static final String LONG_FIELD_FROM_INT = "longFieldFromInt";
//
//    private static final String FLOAT_FIELD = "floatField";
//
//    private static final String DOUBLE_FIELD = "doubleField";
//
//    private static final String STRING_FIELD = "stringField";
//
//    private static final boolean BOOLEAN_VALUE = true;
//
//    private static final byte BYTE_VALUE = 42;
//
//    private static final short SHORT_VALUE = 42;
//
//    private static final char CHARACTER_VALUE = '@';
//
//    private static final int INTEGER_VALUE = 42;
//
//    private static final long LONG_VALUE = 42L;
//
//    private static final float FLOAT_VALUE = 42f;
//
//    private static final double DOUBLE_VALUE = 42d;
//
//    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";
//
//    @Rule
//    public TestRule mockitoRule = new MockitoRule(this);
//
//    @Mock
//    private Instrumentation simpleInstrumentation,
//            preparingInstrumentation,
//            typeInitializerInstrumentation,
//            fieldCacheInstrumentation;
//
//    @Mock
//    private ByteCodeAppender byteCodeAppender, typeInitializerAppender, fieldCacheAppender;
//
//    @Mock
//    private LoadedTypeInitializer loadedTypeInitializer;
//
//    @Before
//    public void setUp() throws Exception {
//        when(simpleInstrumentation.prepare(any(InstrumentedType.class))).then(new Answer<InstrumentedType>() {
//            @Override
//            public InstrumentedType answer(InvocationOnMock invocation) throws Throwable {
//                return (InstrumentedType) invocation.getArguments()[0];
//            }
//        });
//        when(simpleInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(byteCodeAppender);
//        when(byteCodeAppender.appendsCode()).thenReturn(true);
//        when(byteCodeAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class)))
//                .thenAnswer(new Answer<ByteCodeAppender.Size>() {
//                    @Override
//                    public ByteCodeAppender.Size answer(InvocationOnMock invocation) throws Throwable {
//                        MethodVisitor methodVisitor = (MethodVisitor) invocation.getArguments()[0];
//                        MethodDescription methodDescription = (MethodDescription) invocation.getArguments()[2];
//                        methodVisitor.visitInsn(Opcodes.ICONST_0);
//                        methodVisitor.visitInsn(Opcodes.IRETURN);
//                        return new ByteCodeAppender.Size(1, methodDescription.getStackSize());
//                    }
//                });
//        when(preparingInstrumentation.prepare(any(InstrumentedType.class))).then(new Answer<InstrumentedType>() {
//            @Override
//            public InstrumentedType answer(InvocationOnMock invocation) throws Throwable {
//                return ((InstrumentedType) invocation.getArguments()[0])
//                        .withField(BAZ,
//                                new TypeDescription.ForLoadedType(Object.class),
//                                0)
//                        .withMethod(QUX,
//                                new TypeDescription.ForLoadedType(int.class),
//                                Collections.<TypeDescription>emptyList(),
//                                Collections.<TypeDescription>emptyList(),
//                                0)
//                        .withInitializer(loadedTypeInitializer);
//            }
//        });
//        when(preparingInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(byteCodeAppender);
//        when(typeInitializerInstrumentation.prepare(any(InstrumentedType.class))).then(new Answer<InstrumentedType>() {
//            @Override
//            public InstrumentedType answer(InvocationOnMock invocation) throws Throwable {
//                return (InstrumentedType) invocation.getArguments()[0];
//            }
//        });
//        when(typeInitializerInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(typeInitializerAppender);
//        when(typeInitializerAppender.appendsCode()).thenReturn(true);
//        when(typeInitializerAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class)))
//                .thenAnswer(new Answer<ByteCodeAppender.Size>() {
//                    @Override
//                    public ByteCodeAppender.Size answer(InvocationOnMock invocation) throws Throwable {
//                        MethodVisitor methodVisitor = (MethodVisitor) invocation.getArguments()[0];
//                        MethodDescription methodDescription = (MethodDescription) invocation.getArguments()[2];
//                        methodVisitor.visitInsn(Opcodes.ICONST_1);
//                        methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC,
//                                Type.getInternalName(Foo.class),
//                                FOO,
//                                Type.getDescriptor(int.class));
//                        methodVisitor.visitInsn(Opcodes.RETURN);
//                        return new ByteCodeAppender.Size(1, methodDescription.getStackSize());
//                    }
//                });
//        when(fieldCacheInstrumentation.prepare(any(InstrumentedType.class))).then(new Answer<InstrumentedType>() {
//            @Override
//            public InstrumentedType answer(InvocationOnMock invocation) throws Throwable {
//                return (InstrumentedType) invocation.getArguments()[0];
//            }
//        });
//        when(fieldCacheInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(fieldCacheAppender);
//        when(fieldCacheAppender.appendsCode()).thenReturn(true);
//        when(fieldCacheAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class)))
//                .thenAnswer(new Answer<ByteCodeAppender.Size>() {
//                    @Override
//                    public ByteCodeAppender.Size answer(InvocationOnMock invocation) throws Throwable {
//                        MethodVisitor methodVisitor = (MethodVisitor) invocation.getArguments()[0];
//                        Instrumentation.Context instrumentationContext = (Instrumentation.Context) invocation.getArguments()[1];
//                        MethodDescription methodDescription = (MethodDescription) invocation.getArguments()[2];
//                        FieldDescription fieldDescription = instrumentationContext.cache(new TextConstant(BAR),
//                                new TypeDescription.ForLoadedType(String.class));
//                        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC,
//                                fieldDescription.getDeclaringType().getInternalName(),
//                                fieldDescription.getInternalName(),
//                                fieldDescription.getDescriptor());
//                        methodVisitor.visitInsn(Opcodes.ARETURN);
//                        return new ByteCodeAppender.Size(1, methodDescription.getStackSize());
//                    }
//                });
//    }
//
//    @Test
//    public void testPlainSubclass() throws Exception {
//        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
//                new NamingStrategy.Fixed(FOO),
//                new TypeDescription.ForLoadedType(Object.class),
//                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
//                Opcodes.ACC_PUBLIC,
//                TypeAttributeAppender.NoOp.INSTANCE,
//                none(),
//                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
//                new ClassVisitorWrapper.Chain(),
//                new FieldRegistry.Default(),
//                new MethodRegistry.Default(),
//                MethodLookupEngine.Default.Factory.INSTANCE,
//                FieldAttributeAppender.NoOp.INSTANCE,
//                MethodAttributeAppender.NoOp.INSTANCE,
//                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
//                .make()
//                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                .getLoaded();
//        assertThat(loaded.getName(), is(FOO));
//        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC));
//        assertEquals(Object.class, loaded.getSuperclass());
//        assertThat(loaded.getInterfaces().length, is(1));
//        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
//        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
//        assertThat(loaded.getDeclaredMethods().length, is(0));
//        assertThat(loaded.getDeclaredAnnotations().length, is(0));
//        assertThat(loaded.getDeclaredFields().length, is(0));
//        assertThat(loaded.getDeclaredConstructors().length, is(1));
//        assertThat(loaded.getDeclaredConstructor().newInstance(), notNullValue());
//    }
//
//    @Test
//    public void testSubclassWithDefinedField() throws Exception {
//        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
//                new NamingStrategy.Fixed(FOO),
//                new TypeDescription.ForLoadedType(Object.class),
//                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
//                Opcodes.ACC_PUBLIC,
//                TypeAttributeAppender.NoOp.INSTANCE,
//                none(),
//                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
//                new ClassVisitorWrapper.Chain(),
//                new FieldRegistry.Default(),
//                new MethodRegistry.Default(),
//                MethodLookupEngine.Default.Factory.INSTANCE,
//                FieldAttributeAppender.NoOp.INSTANCE,
//                MethodAttributeAppender.NoOp.INSTANCE,
//                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
//                .defineField(BAR, long.class, Visibility.PUBLIC)
//                .make()
//                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                .getLoaded();
//        assertThat(loaded.getName(), is(FOO));
//        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC));
//        assertEquals(Object.class, loaded.getSuperclass());
//        assertThat(loaded.getInterfaces().length, is(1));
//        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
//        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
//        assertThat(loaded.getDeclaredMethods().length, is(0));
//        assertThat(loaded.getDeclaredAnnotations().length, is(0));
//        assertThat(loaded.getDeclaredFields().length, is(1));
//        Field field = loaded.getDeclaredFields()[0];
//        assertThat(field.getName(), is(BAR));
//        assertThat(field.getDeclaredAnnotations().length, is(0));
//        assertEquals(long.class, field.getType());
//        assertThat(field.getModifiers(), is(Opcodes.ACC_PUBLIC));
//        assertThat(loaded.getDeclaredConstructors().length, is(1));
//        assertThat(loaded.getDeclaredConstructor().newInstance(), notNullValue());
//    }
//
//    @Test
//    public void testSubclassWithDefinedMethod() throws Exception {
//        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
//                new NamingStrategy.Fixed(FOO),
//                new TypeDescription.ForLoadedType(Object.class),
//                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
//                Opcodes.ACC_PUBLIC,
//                TypeAttributeAppender.NoOp.INSTANCE,
//                none(),
//                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
//                new ClassVisitorWrapper.Chain(),
//                new FieldRegistry.Default(),
//                new MethodRegistry.Default(),
//                MethodLookupEngine.Default.Factory.INSTANCE,
//                FieldAttributeAppender.NoOp.INSTANCE,
//                MethodAttributeAppender.NoOp.INSTANCE,
//                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
//                .defineMethod(BAR, int.class, Arrays.<Class<?>>asList(long.class, Object.class), Visibility.PUBLIC)
//                .intercept(simpleInstrumentation)
//                .make()
//                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                .getLoaded();
//        assertThat(loaded.getName(), is(FOO));
//        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC));
//        assertEquals(Object.class, loaded.getSuperclass());
//        assertThat(loaded.getInterfaces().length, is(1));
//        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
//        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
//        assertThat(loaded.getDeclaredAnnotations().length, is(0));
//        assertThat(loaded.getDeclaredFields().length, is(0));
//        assertThat(loaded.getDeclaredMethods().length, is(1));
//        Method method = loaded.getDeclaredMethod(BAR, long.class, Object.class);
//        assertThat(method.getName(), is(BAR));
//        assertThat(method.getDeclaredAnnotations().length, is(0));
//        assertEquals(int.class, method.getReturnType());
//        assertThat(method.getModifiers(), is(Opcodes.ACC_PUBLIC));
//        assertThat(loaded.getDeclaredConstructors().length, is(1));
//        assertThat(loaded.getDeclaredConstructor().newInstance(), notNullValue());
//        verify(simpleInstrumentation).prepare(any(InstrumentedType.class));
//        verify(simpleInstrumentation).appender(any(Instrumentation.Target.class));
//        verifyNoMoreInteractions(simpleInstrumentation);
//        verify(byteCodeAppender).appendsCode();
//        verify(byteCodeAppender).apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class));
//        verifyNoMoreInteractions(byteCodeAppender);
//    }
//
//    @Test
//    public void testSubclassWithDefinedAbstractMethod() throws Exception {
//        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
//                new NamingStrategy.Fixed(FOO),
//                new TypeDescription.ForLoadedType(Object.class),
//                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
//                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
//                TypeAttributeAppender.NoOp.INSTANCE,
//                none(),
//                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
//                new ClassVisitorWrapper.Chain(),
//                new FieldRegistry.Default(),
//                new MethodRegistry.Default(),
//                MethodLookupEngine.Default.Factory.INSTANCE,
//                FieldAttributeAppender.NoOp.INSTANCE,
//                MethodAttributeAppender.NoOp.INSTANCE,
//                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
//                .defineMethod(BAR, int.class, Arrays.<Class<?>>asList(long.class, Object.class), Visibility.PUBLIC)
//                .throwing(IOException.class)
//                .withoutCode()
//                .make()
//                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                .getLoaded();
//        assertThat(loaded.getName(), is(FOO));
//        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT));
//        assertEquals(Object.class, loaded.getSuperclass());
//        assertThat(loaded.getInterfaces().length, is(1));
//        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
//        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
//        assertThat(loaded.getDeclaredAnnotations().length, is(0));
//        assertThat(loaded.getDeclaredFields().length, is(0));
//        assertThat(loaded.getDeclaredMethods().length, is(1));
//        Method method = loaded.getDeclaredMethod(BAR, long.class, Object.class);
//        assertThat(method.getName(), is(BAR));
//        assertThat(method.getExceptionTypes().length, is(1));
//        assertThat(Arrays.asList(method.getExceptionTypes()), hasItem(IOException.class));
//        assertThat(method.getDeclaredAnnotations().length, is(0));
//        assertEquals(int.class, method.getReturnType());
//        assertThat(method.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT));
//        assertThat(loaded.getDeclaredConstructors().length, is(1));
//    }
//
//    @Test
//    public void testSubclassWithDefinedConstructor() throws Exception {
//        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
//                new NamingStrategy.Fixed(FOO),
//                new TypeDescription.ForLoadedType(Object.class),
//                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
//                Opcodes.ACC_PUBLIC,
//                TypeAttributeAppender.NoOp.INSTANCE,
//                none(),
//                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
//                new ClassVisitorWrapper.Chain(),
//                new FieldRegistry.Default(),
//                new MethodRegistry.Default(),
//                MethodLookupEngine.Default.Factory.INSTANCE,
//                FieldAttributeAppender.NoOp.INSTANCE,
//                MethodAttributeAppender.NoOp.INSTANCE,
//                ConstructorStrategy.Default.NO_CONSTRUCTORS)
//                .defineConstructor(Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
//                .throwing(IOException.class)
//                .intercept(SuperMethodCall.INSTANCE)
//                .make()
//                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                .getLoaded();
//        assertThat(loaded.getName(), is(FOO));
//        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC));
//        assertEquals(Object.class, loaded.getSuperclass());
//        assertThat(loaded.getInterfaces().length, is(1));
//        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
//        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
//        assertThat(loaded.getDeclaredAnnotations().length, is(0));
//        assertThat(loaded.getDeclaredFields().length, is(0));
//        assertThat(loaded.getDeclaredConstructors().length, is(1));
//        Constructor<?> constructor = loaded.getDeclaredConstructor();
//        assertThat(constructor.getExceptionTypes().length, is(1));
//        assertThat(Arrays.asList(constructor.getExceptionTypes()), hasItem(IOException.class));
//        assertThat(constructor.getDeclaredAnnotations().length, is(0));
//        assertThat(constructor.getModifiers(), is(Opcodes.ACC_PUBLIC));
//        assertThat(loaded.getDeclaredMethods().length, is(0));
//    }
//
//    @Test
//    public void testSubclassWithDefinedInstrumentationPreparation() throws Exception {
//        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
//                new NamingStrategy.Fixed(FOO),
//                new TypeDescription.ForLoadedType(Object.class),
//                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
//                Opcodes.ACC_PUBLIC,
//                TypeAttributeAppender.NoOp.INSTANCE,
//                none(),
//                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
//                new ClassVisitorWrapper.Chain(),
//                new FieldRegistry.Default(),
//                new MethodRegistry.Default(),
//                MethodLookupEngine.Default.Factory.INSTANCE,
//                FieldAttributeAppender.NoOp.INSTANCE,
//                MethodAttributeAppender.NoOp.INSTANCE,
//                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
//                .defineMethod(BAR, int.class, Arrays.<Class<?>>asList(long.class, Object.class), Visibility.PUBLIC)
//                .intercept(preparingInstrumentation)
//                .make()
//                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                .getLoaded();
//        assertThat(loaded.getName(), is(FOO));
//        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC));
//        assertEquals(Object.class, loaded.getSuperclass());
//        assertThat(loaded.getInterfaces().length, is(1));
//        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
//        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
//        assertThat(loaded.getDeclaredAnnotations().length, is(0));
//        assertThat(loaded.getDeclaredFields().length, is(1));
//        Field field = loaded.getDeclaredField(BAZ);
//        assertThat(field.getModifiers(), is(0));
//        assertThat(loaded.getDeclaredMethods().length, is(2));
//        Method method = loaded.getDeclaredMethod(BAR, long.class, Object.class);
//        assertThat(method.getDeclaredAnnotations().length, is(0));
//        assertEquals(int.class, method.getReturnType());
//        assertThat(method.getModifiers(), is(Opcodes.ACC_PUBLIC));
//        Method preparedMethod = loaded.getDeclaredMethod(QUX);
//        assertThat(preparedMethod.getDeclaredAnnotations().length, is(0));
//        assertEquals(int.class, preparedMethod.getReturnType());
//        assertThat(preparedMethod.getModifiers(), is(0));
//        assertThat(loaded.getDeclaredConstructors().length, is(1));
//        assertThat(loaded.getDeclaredConstructor().newInstance(), notNullValue());
//        verify(preparingInstrumentation).prepare(any(InstrumentedType.class));
//        verify(preparingInstrumentation).appender(any(Instrumentation.Target.class));
//        verifyNoMoreInteractions(preparingInstrumentation);
//        verify(byteCodeAppender, times(2)).appendsCode();
//        verify(byteCodeAppender, times(2)).apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class));
//        verifyNoMoreInteractions(byteCodeAppender);
//        verify(loadedTypeInitializer).onLoad(loaded);
//        verifyNoMoreInteractions(loadedTypeInitializer);
//    }
//
//    @Test
//    public void testFieldWithDefaultValue() throws Exception {
//        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
//                new NamingStrategy.Fixed(FOO),
//                new TypeDescription.ForLoadedType(Object.class),
//                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
//                Opcodes.ACC_PUBLIC,
//                TypeAttributeAppender.NoOp.INSTANCE,
//                none(),
//                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
//                new ClassVisitorWrapper.Chain(),
//                new FieldRegistry.Default(),
//                new MethodRegistry.Default(),
//                MethodLookupEngine.Default.Factory.INSTANCE,
//                FieldAttributeAppender.NoOp.INSTANCE,
//                MethodAttributeAppender.NoOp.INSTANCE,
//                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
//                .defineField(BOOLEAN_FIELD, boolean.class, Ownership.STATIC).value(BOOLEAN_VALUE)
//                .defineField(BYTE_FIELD, byte.class, Ownership.STATIC).value(BYTE_VALUE)
//                .defineField(SHORT_FIELD, short.class, Ownership.STATIC).value(SHORT_VALUE)
//                .defineField(CHARACTER_FIELD, char.class, Ownership.STATIC).value(CHARACTER_VALUE)
//                .defineField(INTEGER_FIELD, int.class, Ownership.STATIC).value(INTEGER_VALUE)
//                .defineField(LONG_FIELD, long.class, Ownership.STATIC).value(LONG_VALUE)
//                .defineField(LONG_FIELD_FROM_INT, long.class, Ownership.STATIC).value(INTEGER_VALUE)
//                .defineField(FLOAT_FIELD, float.class, Ownership.STATIC).value(FLOAT_VALUE)
//                .defineField(DOUBLE_FIELD, double.class, Ownership.STATIC).value(DOUBLE_VALUE)
//                .defineField(STRING_FIELD, String.class, Ownership.STATIC).value(FOO)
//                .make()
//                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                .getLoaded();
//        Field booleanField = loaded.getDeclaredField(BOOLEAN_FIELD);
//        booleanField.setAccessible(true);
//        assertThat(booleanField.get(null), is((Object) BOOLEAN_VALUE));
//        Field byteField = loaded.getDeclaredField(BYTE_FIELD);
//        byteField.setAccessible(true);
//        assertThat(byteField.get(null), is((Object) BYTE_VALUE));
//        Field shortField = loaded.getDeclaredField(SHORT_FIELD);
//        shortField.setAccessible(true);
//        assertThat(shortField.get(null), is((Object) SHORT_VALUE));
//        Field characterField = loaded.getDeclaredField(CHARACTER_FIELD);
//        characterField.setAccessible(true);
//        assertThat(characterField.get(null), is((Object) CHARACTER_VALUE));
//        Field integerField = loaded.getDeclaredField(INTEGER_FIELD);
//        integerField.setAccessible(true);
//        assertThat(integerField.get(null), is((Object) INTEGER_VALUE));
//        Field longField = loaded.getDeclaredField(LONG_FIELD);
//        longField.setAccessible(true);
//        assertThat(longField.get(null), is((Object) LONG_VALUE));
//        Field longFieldFromInt = loaded.getDeclaredField(LONG_FIELD_FROM_INT);
//        longFieldFromInt.setAccessible(true);
//        assertThat(longFieldFromInt.get(null), is((Object) (long) INTEGER_VALUE));
//        Field floatField = loaded.getDeclaredField(FLOAT_FIELD);
//        floatField.setAccessible(true);
//        assertThat(floatField.get(null), is((Object) FLOAT_VALUE));
//        Field doubleField = loaded.getDeclaredField(DOUBLE_FIELD);
//        doubleField.setAccessible(true);
//        assertThat(doubleField.get(null), is((Object) DOUBLE_VALUE));
//        Field stringField = loaded.getDeclaredField(STRING_FIELD);
//        stringField.setAccessible(true);
//        assertThat(stringField.get(null), is((Object) FOO));
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testDefineStaticConstructorThrowsException() throws Exception {
//        new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
//                new NamingStrategy.Fixed(FOO),
//                new TypeDescription.ForLoadedType(Object.class),
//                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
//                Opcodes.ACC_PUBLIC,
//                TypeAttributeAppender.NoOp.INSTANCE,
//                none(),
//                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
//                new ClassVisitorWrapper.Chain(),
//                new FieldRegistry.Default(),
//                new MethodRegistry.Default(),
//                MethodLookupEngine.Default.Factory.INSTANCE,
//                FieldAttributeAppender.NoOp.INSTANCE,
//                MethodAttributeAppender.NoOp.INSTANCE,
//                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
//                .defineConstructor(Collections.<TypeDescription>emptyList(), Ownership.STATIC);
//    }
//
//    @Test
//    public void testInterceptTypeInitializerWithoutFieldCache() throws Exception {
//        Foo.foo = 0;
//        new SubclassDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
//                new NamingStrategy.Fixed(FOO),
//                new TypeDescription.ForLoadedType(Foo.class),
//                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
//                Opcodes.ACC_PUBLIC,
//                TypeAttributeAppender.NoOp.INSTANCE,
//                none(),
//                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
//                new ClassVisitorWrapper.Chain(),
//                new FieldRegistry.Default(),
//                new MethodRegistry.Default(),
//                MethodLookupEngine.Default.Factory.INSTANCE,
//                FieldAttributeAppender.NoOp.INSTANCE,
//                MethodAttributeAppender.NoOp.INSTANCE,
//                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
//                .invokable(isTypeInitializer()).intercept(typeInitializerInstrumentation)
//                .make()
//                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                .getLoaded().newInstance(); // make sure type initializer is executed
//        assertThat(Foo.foo, is(1));
//    }
//
//    @Test
//    public void testInterceptTypeInitializerWithFieldCache() throws Exception {
//        Foo.foo = 0;
//        assertThat(new SubclassDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
//                new NamingStrategy.Fixed(FOO),
//                new TypeDescription.ForLoadedType(Foo.class),
//                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
//                Opcodes.ACC_PUBLIC,
//                TypeAttributeAppender.NoOp.INSTANCE,
//                none(),
//                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
//                new ClassVisitorWrapper.Chain(),
//                new FieldRegistry.Default(),
//                new MethodRegistry.Default(),
//                MethodLookupEngine.Default.Factory.INSTANCE,
//                FieldAttributeAppender.NoOp.INSTANCE,
//                MethodAttributeAppender.NoOp.INSTANCE,
//                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
//                .method(isToString()).intercept(fieldCacheInstrumentation)
//                .invokable(isTypeInitializer()).intercept(typeInitializerInstrumentation)
//                .make()
//                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                .getLoaded().newInstance().toString(), is(BAR)); // make sure type initializer is executed
//        assertThat(Foo.foo, is(1));
//    }
//
//    @Test
//    public void testObjectProperties() throws Exception {
//        ObjectPropertyAssertion.of(SubclassDynamicTypeBuilder.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
//            @Override
//            public List<?> create() {
//                return Collections.singletonList(mock(TypeDescription.class));
//            }
//        }).apply();
//    }
//
//    public static class Foo {
//
//        public static int foo = 0;
//    }
}
