package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.SuperMethodCall;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.modifier.MemberVisibility;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
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

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SubclassDynamicTypeBuilderTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation instrumentation;
    @Mock
    private ByteCodeAppender byteCodeAppender;

    @Before
    public void setUp() throws Exception {
        when(instrumentation.prepare(any(InstrumentedType.class))).thenAnswer(new Answer<InstrumentedType>() {
            @Override
            public InstrumentedType answer(InvocationOnMock invocation) throws Throwable {
                return (InstrumentedType) invocation.getArguments()[0];
            }
        });
        when(instrumentation.appender(any(TypeDescription.class))).thenReturn(byteCodeAppender);
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
    }

    @Test
    public void testPlainSubclass() throws Exception {
        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOO));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertEquals(Object.class, loaded.getSuperclass());
        assertThat(loaded.getInterfaces().length, is(1));
        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
        assertThat(loaded.getDeclaredMethods().length, is(0));
        assertThat(loaded.getDeclaredAnnotations().length, is(0));
        assertThat(loaded.getDeclaredFields().length, is(0));
        assertThat(loaded.getDeclaredConstructors().length, is(1));
        assertThat(loaded.getDeclaredConstructor().newInstance(), notNullValue());
    }

    @Test
    public void testSubclassWithDefinedField() throws Exception {
        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .defineField(BAR, long.class, MemberVisibility.PUBLIC)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOO));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertEquals(Object.class, loaded.getSuperclass());
        assertThat(loaded.getInterfaces().length, is(1));
        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
        assertThat(loaded.getDeclaredMethods().length, is(0));
        assertThat(loaded.getDeclaredAnnotations().length, is(0));
        assertThat(loaded.getDeclaredFields().length, is(1));
        Field field = loaded.getDeclaredFields()[0];
        assertThat(field.getName(), is(BAR));
        assertThat(field.getDeclaredAnnotations().length, is(0));
        assertEquals(long.class, field.getType());
        assertThat(field.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredConstructors().length, is(1));
        assertThat(loaded.getDeclaredConstructor().newInstance(), notNullValue());
    }

    @Test
    public void testSubclassWithDefinedMethod() throws Exception {
        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .defineMethod(BAR, int.class, Arrays.<Class<?>>asList(long.class, Object.class), MemberVisibility.PUBLIC)
                .intercept(instrumentation)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOO));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertEquals(Object.class, loaded.getSuperclass());
        assertThat(loaded.getInterfaces().length, is(1));
        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
        assertThat(loaded.getDeclaredAnnotations().length, is(0));
        assertThat(loaded.getDeclaredFields().length, is(0));
        assertThat(loaded.getDeclaredMethods().length, is(1));
        Method method = loaded.getDeclaredMethod(BAR, long.class, Object.class);
        assertThat(method.getName(), is(BAR));
        assertThat(method.getDeclaredAnnotations().length, is(0));
        assertEquals(int.class, method.getReturnType());
        assertThat(method.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredConstructors().length, is(1));
        assertThat(loaded.getDeclaredConstructor().newInstance(), notNullValue());
        verify(instrumentation).prepare(any(InstrumentedType.class));
        verify(instrumentation).appender(any(TypeDescription.class));
        verifyNoMoreInteractions(instrumentation);
        verify(byteCodeAppender).appendsCode();
        verify(byteCodeAppender).apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class));
        verifyNoMoreInteractions(byteCodeAppender);
    }

    @Test
    public void testSubclassWithDefinedAbstractMethod() throws Exception {
        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .defineMethod(BAR, int.class, Arrays.<Class<?>>asList(long.class, Object.class), MemberVisibility.PUBLIC)
                .throwing(IOException.class)
                .withoutCode()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOO));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT));
        assertEquals(Object.class, loaded.getSuperclass());
        assertThat(loaded.getInterfaces().length, is(1));
        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
        assertThat(loaded.getDeclaredAnnotations().length, is(0));
        assertThat(loaded.getDeclaredFields().length, is(0));
        assertThat(loaded.getDeclaredMethods().length, is(1));
        Method method = loaded.getDeclaredMethod(BAR, long.class, Object.class);
        assertThat(method.getName(), is(BAR));
        assertThat(method.getExceptionTypes().length, is(1));
        assertThat(Arrays.asList(method.getExceptionTypes()), hasItem(IOException.class));
        assertThat(method.getDeclaredAnnotations().length, is(0));
        assertEquals(int.class, method.getReturnType());
        assertThat(method.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT));
        assertThat(loaded.getDeclaredConstructors().length, is(1));
    }

    @Test
    public void testSubclassWithDefinedConstructor() throws Exception {
        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFileVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineConstructor(Arrays.<Class<?>>asList(), MemberVisibility.PUBLIC)
                .throwing(IOException.class)
                .intercept(SuperMethodCall.INSTANCE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(loaded.getName(), is(FOO));
        assertThat(loaded.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertEquals(Object.class, loaded.getSuperclass());
        assertThat(loaded.getInterfaces().length, is(1));
        assertThat(loaded.getClassLoader().getParent(), is(getClass().getClassLoader()));
        assertEquals(Serializable.class, loaded.getInterfaces()[0]);
        assertThat(loaded.getDeclaredAnnotations().length, is(0));
        assertThat(loaded.getDeclaredFields().length, is(0));
        assertThat(loaded.getDeclaredConstructors().length, is(1));
        Constructor<?> constructor = loaded.getDeclaredConstructor();
        assertThat(constructor.getExceptionTypes().length, is(1));
        assertThat(Arrays.asList(constructor.getExceptionTypes()), hasItem(IOException.class));
        assertThat(constructor.getDeclaredAnnotations().length, is(0));
        assertThat(constructor.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(loaded.getDeclaredMethods().length, is(0));
    }
}
