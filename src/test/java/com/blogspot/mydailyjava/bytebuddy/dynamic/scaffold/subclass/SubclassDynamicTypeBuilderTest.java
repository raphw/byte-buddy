package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.NamingStrategy;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.ClassLoadingStrategy;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.FieldRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import com.blogspot.mydailyjava.bytebuddy.modifier.MemberVisibility;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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
        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFormatVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
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
        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFormatVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
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
        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFormatVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
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
        Class<?> loaded = new SubclassDynamicTypeBuilder<Object>(ClassFormatVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .defineMethod(BAR, int.class, Arrays.<Class<?>>asList(long.class, Object.class), MemberVisibility.PUBLIC)
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
        assertThat(method.getDeclaredAnnotations().length, is(0));
        assertEquals(int.class, method.getReturnType());
        assertThat(method.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT));
        assertThat(loaded.getDeclaredConstructors().length, is(1));
    }
}
