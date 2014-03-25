package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public abstract class AbstractInstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    protected abstract InstrumentedType makePlainInstrumentedType();

    @Test
    public void testWithField() throws Exception {
        TypeDescription fieldType = mock(TypeDescription.class);
        when(fieldType.getInternalName()).thenReturn(FOO);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withField(BAR, fieldType, Opcodes.ACC_PUBLIC);
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getFieldType(), is(fieldType));
        assertThat(fieldDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(fieldDescription.getName(), is(BAR));
    }

    @Test
    public void testWithFieldOfInstrumentedType() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withField(BAR, instrumentedType, Opcodes.ACC_PUBLIC);
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getFieldType(), is((TypeDescription) instrumentedType));
        assertThat(fieldDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(fieldDescription.getName(), is(BAR));
    }

    @Test
    public void testWithMethod() throws Exception {
        TypeDescription returnType = mock(TypeDescription.class);
        TypeDescription parameterType = mock(TypeDescription.class);
        TypeDescription exceptionType = mock(TypeDescription.class);
        when(returnType.getInternalName()).thenReturn(FOO);
        when(parameterType.getInternalName()).thenReturn(QUX);
        when(exceptionType.getInternalName()).thenReturn(BAZ);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withMethod(BAR,
                returnType,
                Arrays.asList(parameterType),
                Arrays.asList(exceptionType),
                Opcodes.ACC_PUBLIC);
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getReturnType(), is(returnType));
        assertThat(methodDescription.getParameterTypes().size(), is(1));
        assertThat(methodDescription.getParameterTypes(), is(Arrays.asList(parameterType)));
        assertThat(methodDescription.getExceptionTypes().size(), is(1));
        assertThat(methodDescription.getExceptionTypes(), is(Arrays.asList(exceptionType)));
        assertThat(methodDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(methodDescription.getName(), is(BAR));
    }

    @Test
    public void testWithMethodOfInstrumentedType() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withMethod(BAR,
                instrumentedType,
                Arrays.asList(instrumentedType),
                Collections.<TypeDescription>emptyList(),
                Opcodes.ACC_PUBLIC);
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getReturnType(), is((TypeDescription) instrumentedType));
        assertThat(methodDescription.getParameterTypes().size(), is(1));
        assertThat(methodDescription.getParameterTypes(), is(Arrays.asList((TypeDescription) instrumentedType)));
        assertThat(methodDescription.getExceptionTypes().size(), is(0));
        assertThat(methodDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(methodDescription.getName(), is(BAR));
    }

    @Test
    public void testGetStackSize() throws Exception {
        assertThat(makePlainInstrumentedType().getStackSize(), is(StackSize.SINGLE));
    }

    @Test
    public void testHashCode() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.hashCode(), is(instrumentedType.getName().hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        TypeDescription other = mock(TypeDescription.class);
        when(other.getName()).thenReturn(instrumentedType.getName());
        assertThat(instrumentedType.equals(other), is(true));
        verify(other, atLeast(1)).getName();
    }

    @Test
    public abstract void testIsAssignableFrom();

    @Test
    public abstract void testIsAssignableTo();

    @Test
    public abstract void testRepresents();

    @Test
    public abstract void testSupertype();

    @Test
    public abstract void testInterfaces();

    @Test
    public abstract void testPackageName();

    @Test
    public abstract void testSimpleName();
}
