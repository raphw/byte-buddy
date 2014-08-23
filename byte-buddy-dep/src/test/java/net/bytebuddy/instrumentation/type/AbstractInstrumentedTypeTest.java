package net.bytebuddy.instrumentation.type;

import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.*;

public abstract class AbstractInstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    protected abstract InstrumentedType makePlainInstrumentedType();

    @Test
    public void testWithField() throws Exception {
        TypeDescription fieldType = mock(TypeDescription.class);
        when(fieldType.getName()).thenReturn(FOO);
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
        when(returnType.getName()).thenReturn(FOO);
        when(parameterType.getName()).thenReturn(QUX);
        when(exceptionType.getName()).thenReturn(BAZ);
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
    public void testIsAssignableFrom() {
        assertThat(makePlainInstrumentedType().isAssignableFrom(Object.class), is(false));
        assertThat(makePlainInstrumentedType().isAssignableFrom(Serializable.class), is(false));
        assertThat(makePlainInstrumentedType().isAssignableFrom(Integer.class), is(false));
        TypeDescription objectTypeDescription = new TypeDescription.ForLoadedType(Object.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(objectTypeDescription), is(false));
        TypeDescription serializableTypeDescription = new TypeDescription.ForLoadedType(Serializable.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(serializableTypeDescription), is(false));
        TypeDescription integerTypeDescription = new TypeDescription.ForLoadedType(Integer.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(integerTypeDescription), is(false));
    }

    @Test
    public void testIsAssignableTo() {
        assertThat(makePlainInstrumentedType().isAssignableTo(Object.class), is(true));
        assertThat(makePlainInstrumentedType().isAssignableTo(Serializable.class), is(true));
        assertThat(makePlainInstrumentedType().isAssignableTo(Integer.class), is(false));
        TypeDescription objectTypeDescription = new TypeDescription.ForLoadedType(Object.class);
        assertThat(makePlainInstrumentedType().isAssignableTo(objectTypeDescription), is(true));
        TypeDescription serializableTypeDescription = new TypeDescription.ForLoadedType(Serializable.class);
        assertThat(makePlainInstrumentedType().isAssignableTo(serializableTypeDescription), is(true));
        TypeDescription integerTypeDescription = new TypeDescription.ForLoadedType(Integer.class);
        assertThat(makePlainInstrumentedType().isAssignableTo(integerTypeDescription), is(false));
    }

    @Test
    public void testRepresents() {
        assertThat(makePlainInstrumentedType().represents(Object.class), is(false));
        assertThat(makePlainInstrumentedType().represents(Serializable.class), is(false));
        assertThat(makePlainInstrumentedType().represents(Integer.class), is(false));
    }

    @Test
    public void testSupertype() {
        assertThat(makePlainInstrumentedType().getSupertype(), is((TypeDescription) new TypeDescription.ForLoadedType(Object.class)));
        assertThat(makePlainInstrumentedType().getSupertype(), not(is((TypeDescription) new TypeDescription.ForLoadedType(Integer.class))));
        assertThat(makePlainInstrumentedType().getSupertype(), not(is((TypeDescription) new TypeDescription.ForLoadedType(Serializable.class))));
    }

    @Test
    public void testInterfaces() {
        TypeList interfaces = makePlainInstrumentedType().getInterfaces();
        assertThat(interfaces.size(), is(1));
        assertThat(interfaces.get(0), is(is((TypeDescription) new TypeDescription.ForLoadedType(Serializable.class))));
    }

    @Test
    public void testPackageName() {
        assertThat(makePlainInstrumentedType().getPackageName(), is(FOO));
    }

    @Test
    public void testSimpleName() {
        assertThat(makePlainInstrumentedType().getSimpleName(), is(BAR));
    }
}
